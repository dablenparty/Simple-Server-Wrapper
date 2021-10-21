package com.hunterltd.ssw.cli;

import com.dablenparty.jsevents.EventCallback;
import com.hunterltd.ssw.cli.tasks.AliveStateCheckTask;
import com.hunterltd.ssw.cli.tasks.ServerBasedRunnable;
import com.hunterltd.ssw.server.MinecraftServer;
import com.hunterltd.ssw.utilities.MavenUtils;
import com.hunterltd.ssw.utilities.MinecraftServerSettings;
import com.hunterltd.ssw.utilities.ThreadUtils;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SswServerCli {
    private final int port;
    private final MinecraftServer minecraftServer;
    private final List<ExecutorService> serviceList = new ArrayList<>();
    private final Map<SswClientHandler, ExecutorService> clientHandlerToExecutorMap = new HashMap<>();
    private ServerSocket serverSocket;
    private volatile boolean cancel = false;
    private int clientId = 0;

    SswServerCli(int port, File serverFile) {
        this.port = port;
        minecraftServer = new MinecraftServer(serverFile, MinecraftServerSettings.getSettingsFromDefaultPath(serverFile));
    }

    public static Namespace parseArgs(String[] args) {
        Properties mavenProperties = MavenUtils.getMavenProperties();
        ArgumentParser parser = ArgumentParsers.newFor("Simple Server Wrapper Server CLI").build()
                .defaultHelp(true)
                .version(String.format("${prog} v%s", mavenProperties.getProperty("version")))
                .description("Server-side commandline interface for running and interacting with a Minecraft server." +
                        " This also allows for remote or local connections from the client-side counterpart to this program");
        parser.addArgument("-v", "--version")
                .action(Arguments.version());

        parser.addArgument("server")
                .action(Arguments.store())
                .metavar("SERVER_PATH")
                .help("path to server jar file");

        parser.addArgument("-m", "--modpack")
                .action(Arguments.store())
                .required(false)
                .help("CurseForge modpack zip file to install to server");

        parser.addArgument("-p", "--port")
                .action(Arguments.store())
                .setDefault(9609)
                .help("port number to connect on");

        return parser.parseArgsOrFail(args);
    }

    public static void main(String[] args) throws IOException {
        Namespace namespace = parseArgs(args);
        File server = new File(namespace.getString("server")).getAbsoluteFile();
        int port = namespace.getInt("port");
        SswServerCli serverCli = new SswServerCli(port, server);
        serverCli.start();
        serverCli.stop();
    }

    private void pruneClientHandlers() {
        Set<SswClientHandler> clientHandlerSet = clientHandlerToExecutorMap.keySet();
        for (SswClientHandler clientHandler : clientHandlerSet) {
            ExecutorService executorService = clientHandlerToExecutorMap.get(clientHandler);
            if (clientHandler.isClosed()) {
                ThreadUtils.tryShutdownExecutorService(executorService);
                clientHandlerToExecutorMap.remove(clientHandler);
            }
        }
    }

    private void startAllServices() {
        ScheduledExecutorService aliveService = Executors.newSingleThreadScheduledExecutor();
        serviceList.add(aliveService);
        aliveService.scheduleWithFixedDelay(new AliveStateCheckTask(minecraftServer), 1L, 1L, TimeUnit.SECONDS);
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port, 0, InetAddress.getLoopbackAddress());
        serverSocket.setSoTimeout(5000);
        startAllServices();
        while (!cancel) {
            pruneClientHandlers();
            Socket socket;
            try {
                socket = serverSocket.accept();
            } catch (SocketTimeoutException ignored) {
                continue;
            }
            SswClientHandler clientHandler = new SswClientHandler(socket, minecraftServer);
            // prunes client handler services
            // maybe extract this to a separate thread made just for pruning handlers...
            ExecutorService clientService = Executors.newSingleThreadExecutor(ThreadUtils.newNamedThreadFactory(String.format("ClientService#%d", clientId++)));
            clientHandlerToExecutorMap.put(clientHandler, clientService);
            clientService.submit(clientHandler);
        }
        stop();
    }

    public void stop() throws IOException {
        serverSocket.close();
        serviceList.forEach(ThreadUtils::tryShutdownExecutorService);
        // these should all be closed at this point, but it's good to clean up anyways
        clientHandlerToExecutorMap.forEach((sswClientHandler, executorService) -> ThreadUtils.tryShutdownExecutorService(executorService));
    }

    private class SswClientHandler extends ServerBasedRunnable {
        private final Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;

        protected SswClientHandler(Socket socket, MinecraftServer minecraftServer) {
            super(minecraftServer);
            clientSocket = socket;
        }

        @Override
        public void run() {
            System.out.printf("Connection accepted from %s on port %d%n",
                    clientSocket.getInetAddress(), clientSocket.getPort());

            EventCallback dataCallback = objects -> printlnToServerAndClientRaw((String) objects[0]);
            EventCallback exitingCallback = objects -> printlnToServerAndClient("Stopping server...");
            EventCallback exitCallback = objects -> printlnToServerAndClient("Server successfully stopped!");
            minecraftServer.on("data", dataCallback);
            minecraftServer.on("exiting", exitingCallback);
            minecraftServer.on("exit", exitCallback);

            try (PrintWriter writer = out = new PrintWriter(clientSocket.getOutputStream(), true);
                 BufferedReader reader = in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

                String message;
                mainLoop:
                while ((message = in.readLine()) != null) {
                    System.out.printf("[Client %s:%s] %s%n",
                            clientSocket.getInetAddress(), clientSocket.getPort(), message);
                    switch (message) {
                        case "start" -> {
                            // running is handled in AliveStateCheckTask
                            if (!minecraftServer.isRunning()) {
                                printlnToServerAndClient("Starting server...");
                                minecraftServer.setShouldBeRunning(true);
                            } else
                                printlnToServerAndClient("Server is already running");}
                        case "stop" -> {
                            if (minecraftServer.isRunning()) {
                                printlnToServerAndClient("Stopping server...");
                                minecraftServer.setShouldBeRunning(false);
                            } else
                                printlnToServerAndClient("No server is running");
                        }
                        case "close" -> {
                            printlnToServerAndClient("Closing client connection...");
                            if (clientHandlerToExecutorMap.size() == 1 && !cancel)
                                cancel = true;
                            break mainLoop;
                        }
                        case "logout" -> {
                            printlnToServerAndClient("Closing client connection...");
                            break mainLoop;
                        }
                        default -> {
                            if (minecraftServer.isRunning())
                                minecraftServer.sendCommand(message.trim());
                            else
                                printfToServerAndClient("Unknown command: %s%n", message);
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println(e.getLocalizedMessage());
                minecraftServer.setShouldBeRunning(false);
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    minecraftServer.removeListener("data", dataCallback);
                    minecraftServer.removeListener("exiting", exitingCallback);
                    minecraftServer.removeListener("exit", exitCallback);
                }
            }
        }

        private void printfToServerAndClient(String formatString, Object... args) {
            // prevents formatting twice
            String message = ThreadUtils.threadStampString(String.format(formatString, args));
            System.out.print(message);
            this.out.print(message);
        }

        private void printlnToServerAndClient(String string) {
            String message = ThreadUtils.threadStampString(string);
            printlnToServerAndClientRaw(message);
        }

        private void printlnToServerAndClientRaw(String message) {
            System.out.println(message);
            this.out.println(message);
        }

        public boolean isClosed() {
            return clientSocket.isClosed();
        }
    }
}
