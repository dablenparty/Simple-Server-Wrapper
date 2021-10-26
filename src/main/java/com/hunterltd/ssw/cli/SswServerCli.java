package com.hunterltd.ssw.cli;

import com.dablenparty.jsevents.EventCallback;
import com.hunterltd.ssw.cli.tasks.AliveStateCheckTask;
import com.hunterltd.ssw.cli.tasks.ServerBasedRunnable;
import com.hunterltd.ssw.cli.tasks.ServerPingTask;
import com.hunterltd.ssw.curse.CurseCli;
import com.hunterltd.ssw.server.MinecraftServer;
import com.hunterltd.ssw.utilities.MavenUtils;
import com.hunterltd.ssw.utilities.concurrency.NamedExecutorService;
import com.hunterltd.ssw.utilities.concurrency.ThreadUtils;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.hunterltd.ssw.utilities.concurrency.ThreadUtils.printlnWithTimeAndThread;

public class SswServerCli {
    private final int port;
    private final MinecraftServer minecraftServer;
    private final List<NamedExecutorService> serviceList = new ArrayList<>();
    private final Map<SswClientHandler, NamedExecutorService> clientHandlerToExecutorMap = new HashMap<>();
    private final Map<String, BooleanSswCliCommand> commandMap = new HashMap<>();
    private ServerSocket serverSocket;
    private volatile boolean cancel = false;
    private int clientId = 0;

    SswServerCli(int port, File serverFile) {
        this.port = port;
        minecraftServer = new MinecraftServer(serverFile, MinecraftServer.ServerSettings.getSettingsFromDefaultPath(serverFile));
    }

    public static Namespace parseArgs(String[] args) {
        Properties mavenProperties = MavenUtils.getMavenProperties();
        ArgumentParser parser = ArgumentParsers.newFor("ssw-server").build()
                .defaultHelp(true)
                .version(String.format("${prog} v%s", mavenProperties.getProperty("version")))
                .description("Server-side commandline interface for running and interacting with a Minecraft server." +
                        " This also allows for remote or local connections from the client-side counterpart to this program");
        parser.addArgument("-v", "--version")
                .action(Arguments.version());
        parser.addArgument("server")
                .action(Arguments.store())
                .metavar("SERVER_PATH")
                .required(true)
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
        File server = new File(namespace.getString("server")).getCanonicalFile();
        String modpackPath = namespace.getString("modpack");
        if (modpackPath != null) {
            File modpack = new File(modpackPath).getCanonicalFile();
            if (modpack.isDirectory() || !modpackPath.toLowerCase().endsWith(".zip")) {
                System.out.printf("'%s' is not a ZIP archive", modpackPath);
            } else {
                new CurseCli(modpack, server).run();
            }
            return;
        }

        File logFile = Paths.get(server.getParentFile().toString(), "ssw", "ssw.log").toFile();
        // make the parent directory
        //noinspection ResultOfMethodCallIgnored
        logFile.getParentFile().mkdir();

        PrintStream logStream = new PrintStream(logFile);
        System.setOut(logStream);
        System.setErr(logStream);
        int port = namespace.getInt("port");
        SswServerCli serverCli = new SswServerCli(port, server);
        serverCli.start();
        serverCli.stop();
    }

    /**
     * Thread stamps and prints an exception message to standard out, then prints the stack trace
     *
     * @param e Exception to print
     */
    public static void printExceptionToOut(Exception e) {
        String threadStampString = ThreadUtils.threadStampString(String.format("Error: %s", e.getLocalizedMessage()));
        System.out.println(threadStampString);
        e.printStackTrace();
    }

    private void registerCliCommands() {
        SswCliCommand startCommand = client -> {
            // running is handled in AliveStateCheckTask
            if (!minecraftServer.isRunning()) {
                client.printlnToServerAndClient("Starting server...");
                minecraftServer.setShouldBeRunning(true);
            } else
                client.printlnToServerAndClient("Server is already running");

        };
        commandMap.put("start", new BooleanSswCliCommand(startCommand, false));

        SswCliCommand stopCommand = client -> {
            if (minecraftServer.isRunning()) {
                client.printlnToServerAndClient("Stopping server...");
                minecraftServer.setShouldBeRunning(false);
            } else
                client.printlnToServerAndClient("No server is running");
        };
        commandMap.put("stop", new BooleanSswCliCommand(stopCommand, false));

        SswCliCommand closeCommand = client -> {
            printlnWithTimeAndThread(System.out, "Closing client connection...");
            if (clientHandlerToExecutorMap.size() == 1 && !cancel) {
                cancel = true;
                if (minecraftServer.isRunning()) {
                    minecraftServer.setShouldBeRunning(false);
                    // prevents the port listener from opening back up automatically
                    minecraftServer.getServerSettings().setShutdown(false);
                }
            }
        };
        commandMap.put("close", new BooleanSswCliCommand(closeCommand, true));

        BooleanSswCliCommand logCommand = new BooleanSswCliCommand(client -> {
            Path serverParentFolder = minecraftServer.getServerPath().getParent();
            File logFile = Paths.get(serverParentFolder.toString(), "logs", "latest.log").toFile();
            try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                reader.lines().forEach(client.out::println); // send each line to the client
            } catch (IOException e) {
                client.printfToServerAndClient("There was an error reading the log file at '%s'%n", logFile);
                printExceptionToOut(e);
            }
        }, false);
        commandMap.put("log", logCommand);
        commandMap.put("backlog", logCommand);
        commandMap.put("printlog", logCommand);

        BooleanSswCliCommand logoutCommand = new BooleanSswCliCommand(client ->
                printlnWithTimeAndThread(System.out, "Closing client connection..."),
                true);
        commandMap.put("logout", logoutCommand);
        commandMap.put("exit", logoutCommand);
    }

    /**
     * Finds all client handlers whose sockets are closed and terminates their corresponding ExecutorService, then
     * removes them from the map of handlers & services
     */
    private void pruneClientHandlers() {
        Set<SswClientHandler> clientHandlerSet = clientHandlerToExecutorMap.keySet();
        Iterator<SswClientHandler> iterator = clientHandlerSet.iterator();
        // using foreach will produce a ConcurrentModificationException
        while (iterator.hasNext()) {
            SswClientHandler clientHandler = iterator.next();
            NamedExecutorService executorService = clientHandlerToExecutorMap.get(clientHandler);
            if (clientHandler.isClosed()) {
                ThreadUtils.tryShutdownNamedExecutorService(executorService);
                iterator.remove();
            }
        }
    }

    /**
     * Starts all background services used by the SSW server and adds them to the {@link SswServerCli#serviceList}
     */
    private void startAllServices() {
        ScheduledExecutorService aliveScheduledService = Executors.newSingleThreadScheduledExecutor(ThreadUtils.newNamedThreadFactory("Alive State Check"));
        NamedExecutorService aliveNamedService = new NamedExecutorService("Alive State Check", aliveScheduledService);
        serviceList.add(aliveNamedService);
        AliveStateCheckTask stateCheckTask = new AliveStateCheckTask(minecraftServer);
        aliveScheduledService.scheduleWithFixedDelay(stateCheckTask, 1L, 1L, TimeUnit.SECONDS);
        stateCheckTask.getChildServices().forEach(aliveNamedService::addChildService);
        MinecraftServer.ServerSettings serverSettings = minecraftServer.getServerSettings();
        if (serverSettings.getShutdown()) {
            printlnWithTimeAndThread(System.out, "Auto startup/shutdown is enabled");
            // make a new thread
            ScheduledExecutorService pingScheduledService = Executors.newSingleThreadScheduledExecutor(ThreadUtils.newNamedThreadFactory("Server Ping Service"));
            ServerPingTask pingTask = new ServerPingTask(minecraftServer);
            // make the named service and add the ping tasks' child service
            NamedExecutorService serverPingService = new NamedExecutorService("Server Ping Service", pingScheduledService);
            pingTask.getChildServices().forEach(serverPingService::addChildService);
            serviceList.add(serverPingService);
            // lastly, schedule the task
            pingScheduledService.scheduleWithFixedDelay(pingTask, 2L, 2L, TimeUnit.SECONDS);
        }
    }

    /**
     * Starts the SSW server
     *
     * @throws IOException if an I/O exception occurs while opening the socket, setting the timeout, or waiting for a
     *                     connection
     */
    public void start() throws IOException {
        serverSocket = new ServerSocket(port, 0, InetAddress.getLoopbackAddress());
        serverSocket.setSoTimeout(5000);
        startAllServices();
        registerCliCommands();
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
            String clientServiceName = String.format("ClientService#%d", clientId++);
            ExecutorService clientService = Executors.newSingleThreadExecutor(ThreadUtils.newNamedThreadFactory(clientServiceName));
            NamedExecutorService namedClientService = new NamedExecutorService(clientServiceName, clientService);
            clientHandlerToExecutorMap.put(clientHandler, namedClientService);
            clientService.submit(clientHandler);
        }
        stop();
    }

    /**
     * Stops the SSW server, closing all remaining client handlers, all services in use, and the socket.
     */
    public void stop() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            printExceptionToOut(e);
        }
        serviceList.forEach(NamedExecutorService::shutdown);
        // these should all be closed at this point, but it's good to clean up anyways
        clientHandlerToExecutorMap.forEach((sswClientHandler, executorService) -> ThreadUtils.tryShutdownNamedExecutorService(executorService));
    }

    private interface SswCliCommand {
        void runCommand(SswClientHandler client);
    }

    private record BooleanSswCliCommand(SswCliCommand command, boolean shouldBreak) {
        public void runCommand(SswClientHandler client) {
            command.runCommand(client);
        }
    }

    /**
     * Client handler running on its own thread
     */
    private class SswClientHandler extends ServerBasedRunnable {
        private final Socket clientSocket;
        private PrintWriter out;

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
            EventCallback exitCallback = objects -> {
                printlnToServerAndClient("Server successfully stopped!");
                minecraftServer.setShouldBeRunning(false); // prevents launch loops
                if (minecraftServer.shouldRestart()) {
                    minecraftServer.setShouldBeRunning(true);
                    minecraftServer.setShouldRestart(false);
                }
            };
            minecraftServer.on("data", dataCallback);
            minecraftServer.on("exiting", exitingCallback);
            minecraftServer.on("exit", exitCallback);

            try (PrintWriter ignored = out = new PrintWriter(clientSocket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

                String message;
                while ((message = in.readLine()) != null) {
                    printlnWithTimeAndThread(System.out, message);
                    BooleanSswCliCommand command = commandMap.get(message);
                    if (command == null) {
                        if (minecraftServer.isRunning())
                            minecraftServer.sendCommand(message.trim());
                        else
                            printfToServerAndClient("Unknown command: %s%n", message);
                        continue;
                    }
                    command.runCommand(this);
                    if (command.shouldBreak()) break;
                }
            } catch (IOException e) {
                printExceptionToOut(e);
                minecraftServer.setShouldBeRunning(false);
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    printExceptionToOut(e);
                } finally {
                    minecraftServer.removeListener("data", dataCallback);
                    minecraftServer.removeListener("exiting", exitingCallback);
                    minecraftServer.removeListener("exit", exitCallback);
                }
            }
        }

        /**
         * Calls printf on both {@link System#out} and this.out. This method also thread-stamps the message.
         * <p>
         * For more detailed documentation, see {@link PrintStream#printf}
         *
         * @param formatString Format string
         * @param args         Arguments for format string
         * @see PrintStream#printf
         */
        private void printfToServerAndClient(String formatString, Object... args) {
            System.out.printf(formatString, args);
            this.out.printf(formatString, args);
        }

        /**
         * Calls println on both {@link System#out} and this.out. This method also thread-stamps the message.
         *
         * @param string String to print
         */
        private void printlnToServerAndClient(String string) {
            String message = ThreadUtils.threadStampString(string);
            printlnToServerAndClientRaw(message);
        }

        /**
         * Calls println on both {@link System#out} and this.out.
         *
         * @param message String to print
         */
        private void printlnToServerAndClientRaw(String message) {
            System.out.println(message);
            this.out.println(message);
        }

        /**
         * Checks if the client socket is closed or not
         *
         * @return {@link Socket#isClosed}
         */
        public boolean isClosed() {
            return clientSocket.isClosed();
        }
    }
}
