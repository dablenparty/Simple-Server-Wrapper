package com.hunterltd.ssw.cli;

import com.hunterltd.ssw.cli.tasks.AliveStateCheckTask;
import com.hunterltd.ssw.cli.tasks.ServerPingTask;
import com.hunterltd.ssw.server.MinecraftServer;
import com.hunterltd.ssw.utilities.Settings;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerWrapperCLI {
    private final Properties mavenProperties;
    private final boolean propertiesLoaded;
    private final MinecraftServer minecraftServer;

    public static void main(String[] args) throws IOException {
        if ((args.length != 1 && args.length != 3) || (args[0].equals("-h") || args[0].equals("--help"))) {
            // wrong number of args or help flag provided
            showHelp();
            return;
        } else if (args.length > 2 && args[1].equalsIgnoreCase("--modpack")) {
            // modpack flag provided
            new CurseCli(new File(args[2]), new File(args[0])).run();
            return;
        }

        ServerWrapperCLI wrapperCli = new ServerWrapperCLI(new File(args[0]));
        MinecraftServer minecraftServer = wrapperCli.getMinecraftServer();
        ScheduledExecutorService serverStateService = Executors.newScheduledThreadPool(1),
                serverPingService = null;
        ServerPingTask pingTask = null;
        if (minecraftServer.getServerSettings().getShutdown()) {
            serverPingService = Executors.newScheduledThreadPool(1);
            pingTask = new ServerPingTask(minecraftServer);
        }
        serverStateService.scheduleWithFixedDelay(new AliveStateCheckTask(minecraftServer), 1L, 1L, TimeUnit.SECONDS);
        wrapperCli.showVersion();

        doLoop: do {
            Scanner inputScanner = new Scanner(System.in);
            String command = inputScanner.nextLine();
            switch (command) {
                case "start":
                    System.out.println("Starting server...");
                    minecraftServer.setShouldBeRunning(true);
                    minecraftServer.run();
                    // submits process streams to stream gobblers to redirect output to standard out and error
                    if (serverPingService != null) {
                        serverPingService.scheduleWithFixedDelay(pingTask, 2L, 2L, TimeUnit.SECONDS);
                    }
                    break;
                case "stop":
                    if (minecraftServer.isRunning()) {
                        minecraftServer.setShouldBeRunning(false);
                    }
                    break;
                case "close":
                    if (minecraftServer.isRunning()) {
                        minecraftServer.setShouldBeRunning(false);
                        minecraftServer.setShuttingDown(true);
                        try {
                            minecraftServer.stop();
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    for (ExecutorService service :
                            new ExecutorService[]{serverPingService, serverStateService}) {
                        if (service != null)
                            tryShutdownExecutorService(service);
                    }
                    if (pingTask != null && pingTask.getShutdownService() != null)
                        tryShutdownExecutorService(pingTask.getShutdownService());
                    inputScanner.close();
                    break doLoop;
                default:
                    if (minecraftServer.isRunning()) minecraftServer.sendCommand(command.strip());
                    break;
            }
        } while (true);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void tryShutdownExecutorService(ExecutorService service) {
        System.out.println("Shutting down background service...");
        service.shutdown();
        try {
            service.awaitTermination(5L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.err.println(e.getLocalizedMessage());
        } finally {
            if (!service.isTerminated())
                System.err.println("Service didn't terminate, forcing shutdown");
            service.shutdownNow();
            System.out.println("Service closed");
        }
    }

    ServerWrapperCLI(File serverFile) {
        mavenProperties = new Properties();
        propertiesLoaded = loadProperties();
        minecraftServer = new MinecraftServer(serverFile, Settings.getSettingsFromDefaultPath(serverFile));
    }

    public void showVersion() {
        if (propertiesLoaded)
            System.out.println(mavenProperties.getProperty("artifactId") + " v" + mavenProperties.getProperty("version")
                    + " on " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd yyyy, HH:mm:ss")));
    }

    public static void showHelp() {
        System.out.println("Usage: ssw-cli.jar <server-file> --modpack [<zip-file>]");
    }

    private boolean loadProperties() {
        try (InputStream resourceStream =
                     ServerWrapperCLI.class.getClassLoader().getResourceAsStream("project.properties")) {
            mavenProperties.load(resourceStream);
            return true;
        } catch (IOException exception) {
            System.err.println(exception.getLocalizedMessage());
            return false;
        }
    }

    public MinecraftServer getMinecraftServer() {
        return minecraftServer;
    }
}
