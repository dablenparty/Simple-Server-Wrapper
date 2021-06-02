package com.hunterltd.ssw.cli;

import com.hunterltd.ssw.server.MinecraftServer;
import com.hunterltd.ssw.server.StreamGobbler;
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
        if (args.length != 1) {
            // TODO: write help message
            throw new IllegalArgumentException((args.length > 1 ? "Too many" : "Missing") + " arguments");
        }

        ServerWrapperCLI wrapperCli = new ServerWrapperCLI(new File(args[0]));
        MinecraftServer minecraftServer = wrapperCli.getMinecraftServer();
        ExecutorService inputService = null, errorService = null;
        ScheduledExecutorService serverStateService = Executors.newScheduledThreadPool(1);
        serverStateService.scheduleWithFixedDelay(new AliveStateCheckTask(minecraftServer), 1000, 1000, TimeUnit.MILLISECONDS);
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
                    inputService = StreamGobbler.execute(minecraftServer.getServerProcess().getInputStream(), System.out::println);
                    errorService = StreamGobbler.execute(minecraftServer.getServerProcess().getErrorStream(), System.err::println);
                    break;
                case "stop":
                    if (minecraftServer.isRunning()) {
                        minecraftServer.setShouldBeRunning(false);
                    }

                    if (inputService != null) {
                        tryShutdownExecutorService(inputService);
                        tryShutdownExecutorService(errorService);
                    }
                    break;
                case "close":
                    if (minecraftServer.isRunning()) {
                        minecraftServer.setShouldBeRunning(false);
                    }
                    if (inputService != null) {
                        tryShutdownExecutorService(inputService);
                        tryShutdownExecutorService(errorService);
                        tryShutdownExecutorService(serverStateService);
                    }
                    inputScanner.close();
                    break doLoop;
                default:
                    if (minecraftServer.isRunning()) minecraftServer.sendCommand(command.strip());
                    break;
            }
        } while (true);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void tryShutdownExecutorService(ExecutorService service) {
        service.shutdown();
        try {
            service.awaitTermination(5L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.err.println(e.getLocalizedMessage());
        } finally {
            if (!service.isTerminated())
                System.err.println("Task didn't terminate, forcing shutdown");
            service.shutdownNow();
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
