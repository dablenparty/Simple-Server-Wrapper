package com.hunterltd.ssw.cli;

import com.hunterltd.ssw.cli.tasks.AliveStateCheckTask;
import com.hunterltd.ssw.cli.tasks.ServerPingTask;
import com.hunterltd.ssw.server.MinecraftServer;
import com.hunterltd.ssw.utilities.Settings;
import org.glassfish.jersey.internal.guava.ThreadFactoryBuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.*;

public class ServerWrapperCLI {
    private static final SimpleDateFormat SIMPLE_TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private final Properties mavenProperties;
    private final boolean propertiesLoaded;
    private final MinecraftServer minecraftServer;

    public static void main(String[] args) throws IOException {
        if (args.length != 1 && args.length != 3) {
            // TODO: write help message
            throw new IllegalArgumentException((args.length > 1 ? "Too many" : "Missing") + " arguments");
        }

        if (args.length > 1 && args[1].equalsIgnoreCase("--modpack")) {
            new CurseCli(new File(args[2]), new File(args[0])).run();
            return;
        }

        ServerWrapperCLI wrapperCli = new ServerWrapperCLI(new File(args[0]));
        MinecraftServer minecraftServer = wrapperCli.getMinecraftServer();
        ScheduledExecutorService
                serverStateService = Executors.newScheduledThreadPool(1, newNamedThreadFactory("Server State Check Service")),
                serverPingService = null;
        ServerPingTask pingTask = null;
        if (minecraftServer.getServerSettings().getShutdown()) {
            serverPingService = Executors.newScheduledThreadPool(1, newNamedThreadFactory("Server Ping Service"));
            pingTask = new ServerPingTask(minecraftServer);
        }
        serverStateService.scheduleWithFixedDelay(new AliveStateCheckTask(minecraftServer), 1L, 1L, TimeUnit.SECONDS);
        wrapperCli.showVersion();

        doLoop: do {
            Scanner inputScanner = new Scanner(System.in);
            String command = inputScanner.nextLine();
            switch (command) {
                case "start":
                    printlnWithTimeAndThread("Starting server...");
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

    public static ThreadFactory newNamedThreadFactory(String threadName) {
        return new ThreadFactoryBuilder().setNameFormat(threadName).build();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void tryShutdownExecutorService(ExecutorService service) {
        printlnWithTimeAndThread("Shutting down background service...");
        service.shutdown();
        try {
            service.awaitTermination(5L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.err.println(e.getLocalizedMessage());
        } finally {
            if (!service.isTerminated())
                System.err.println("Service didn't terminate, forcing shutdown");
            service.shutdownNow();
            printlnWithTimeAndThread("Service closed");
        }
    }

    public static void printfWithTimeAndThread(String toFormat, Object... args) {
        printlnWithTimeAndThread(String.format(toFormat, args));
    }

    public static void printlnWithTimeAndThread(String string) {
        System.out.printf("[%s] [ssw/%s]: %s%n", SIMPLE_TIME_FORMAT.format(System.currentTimeMillis()), Thread.currentThread().getName(), string);
    }

    ServerWrapperCLI(File serverFile) {
        mavenProperties = new Properties();
        propertiesLoaded = loadProperties();
        minecraftServer = new MinecraftServer(serverFile, Settings.getSettingsFromDefaultPath(serverFile));
    }

    public void showVersion() {
        if (propertiesLoaded)
            printlnWithTimeAndThread(mavenProperties.getProperty("artifactId") + " v" + mavenProperties.getProperty("version")
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
