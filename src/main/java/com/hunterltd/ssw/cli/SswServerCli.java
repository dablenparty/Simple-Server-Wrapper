package com.hunterltd.ssw.cli;

import com.hunterltd.ssw.minecraft.tasks.ServerBasedRunnable;
import com.hunterltd.ssw.curse.CurseCli;
import com.hunterltd.ssw.minecraft.MinecraftServer;
import com.hunterltd.ssw.minecraft.MinecraftVersion;
import com.hunterltd.ssw.util.MavenUtils;
import com.hunterltd.ssw.util.concurrency.NamedExecutorService;
import com.hunterltd.ssw.util.concurrency.ThreadUtils;
import com.hunterltd.ssw.util.events.EventCallback;
import net.lingala.zip4j.ZipFile;
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
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.hunterltd.ssw.util.concurrency.ThreadUtils.printlnWithTimeAndThread;

public class SswServerCli {
    private final int port;
    private final MinecraftServer minecraftServer;
    private final Map<SswClientHandler, NamedExecutorService> clientHandlerToExecutorMap = new HashMap<>();
    private final Map<String, SswCliCommand> commandMap = new HashMap<>();
    private final File logFile;
    private List<NamedExecutorService> serviceList;
    private ServerSocket serverSocket;
    private volatile boolean cancel = false;
    private int clientId = 0;

    SswServerCli(int port, MinecraftServer minecraftServer) throws FileNotFoundException {
        this.port = port;
        this.minecraftServer = minecraftServer;
        logFile = Path.of(minecraftServer.getServerPath().getParent().toString(), "ssw", "ssw.log").toFile();
        // make the parent directory
        //noinspection ResultOfMethodCallIgnored
        logFile.getParentFile().mkdir();

        PrintStream logStream = new PrintStream(logFile);
        System.setOut(logStream);
        System.setErr(logStream);
    }

    public static Namespace parseArgs(String[] args) {
        Properties mavenProperties = MavenUtils.MAVEN_PROPERTIES;
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
                new CurseCli(new ZipFile(modpack), server).run();
            }
            return;
        }

        MinecraftServer minecraftServer = new MinecraftServer(server);
        MinecraftServer.ServerSettings serverSettings = minecraftServer.getServerSettings();
        if (serverSettings.getVersion() == null) {
            System.out.println("The Minecraft version this server runs on could not be detected");
            System.out.println("This information is used to patch the Log4Shell exploit in specific versions of Minecraft");
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            MinecraftVersion minecraftVersion;
            do {
                System.out.print("Please enter the version of Minecraft you are playing (e.g., 1.18.1): ");
                String versionString = reader.readLine();
                minecraftVersion = MinecraftVersion.of(versionString);
                if (minecraftVersion == null)
                    System.out.println("Invalid version");
                else
                    break;
            } while (true);
            serverSettings.setVersion(minecraftVersion);
            serverSettings.writeData();
        }
        int port = namespace.getInt("port");
        SswServerCli serverCli = new SswServerCli(port, minecraftServer);
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
        SswCommandRunnable startCommand = client -> {
            // running is handled in AliveStateCheckTask
            if (!minecraftServer.isRunning()) {
                client.printlnToServerAndClient("Starting server...");
                minecraftServer.setShouldBeRunning(true);
            } else
                client.printlnToServerAndClient("Server is already running");

        };
        commandMap.put("start", new SswCliCommand(startCommand, false));

        SswCommandRunnable stopCommand = client -> {
            if (minecraftServer.isRunning()) {
                client.printlnToServerAndClient("Stopping server...");
                // don't allow a restart when manually stopping
                minecraftServer.setShouldRestart(false);
                minecraftServer.setShouldBeRunning(false);
            } else
                client.printlnToServerAndClient("No server is running");
        };
        commandMap.put("stop", new SswCliCommand(stopCommand, false));

        SswCommandRunnable closeCommand = client -> {
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
        commandMap.put("close", new SswCliCommand(closeCommand, true));

        Path serverParentFolder = minecraftServer.getServerPath().getParent();
        File minecraftLog = Path.of(serverParentFolder.toString(), "logs", "latest.log").toFile();
        SswCliCommand logCommand = new SswCliCommand(client -> sendLogToClient(client, minecraftLog), false);
        commandMap.put("log", logCommand);

        SswCliCommand debugLogCommand = new SswCliCommand(client -> sendLogToClient(client, logFile), false);
        commandMap.put("debuglog", debugLogCommand);

        SswCliCommand logoutCommand = new SswCliCommand(client ->
                printlnWithTimeAndThread(System.out, "Closing client connection..."),
                true);
        commandMap.put("logout", logoutCommand);
        commandMap.put("exit", logoutCommand);
    }

    private void sendLogToClient(SswClientHandler client, File logFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            reader.lines().forEach(client.out::println); // send each line to the client
        } catch (IOException e) {
            client.printfToServerAndClient("There was an error reading the log file at '%s'%n", logFile);
            printExceptionToOut(e);
        }
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
     * Starts the SSW server
     *
     * @throws IOException if an I/O exception occurs while opening the socket, setting the timeout, or waiting for a
     *                     connection
     */
    public void start() throws IOException {
        serverSocket = new ServerSocket(port, 0, InetAddress.getLoopbackAddress());
        serverSocket.setSoTimeout(5000);
        serviceList = minecraftServer.startAllBackgroundServices();
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

    private interface SswCommandRunnable {
        void runCommand(SswClientHandler client);
    }

    private record SswCliCommand(SswCommandRunnable command, boolean shouldBreak) {
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
                    SswCliCommand command = commandMap.get(message);
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
