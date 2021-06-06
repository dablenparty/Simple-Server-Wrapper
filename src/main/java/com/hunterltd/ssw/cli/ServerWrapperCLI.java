package com.hunterltd.ssw.cli;

import com.hunterltd.ssw.cli.tasks.AliveStateCheckTask;
import com.hunterltd.ssw.cli.tasks.ServerPingTask;
import com.hunterltd.ssw.server.MinecraftServer;
import com.hunterltd.ssw.utilities.Settings;
import org.glassfish.jersey.internal.guava.ThreadFactoryBuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
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
                    printlnWithTimeAndThread(System.out,"Starting server...");
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
        printlnWithTimeAndThread(System.out,"Shutting down background service...");
        service.shutdown();
        try {
            service.awaitTermination(5L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            printlnWithTimeAndThread(System.err, e.getLocalizedMessage());
        } finally {
            if (!service.isTerminated())
                printlnWithTimeAndThread(System.err, "Service didn't terminate, forcing shutdown");
            service.shutdownNow();
            printlnWithTimeAndThread(System.out,"Service closed");
        }
    }

    public static void printfWithTimeAndThread(PrintStream stream, String toFormat, Object... args) {
        printlnWithTimeAndThread(stream, String.format(toFormat, args));
    }

    public static void printlnWithTimeAndThread(PrintStream stream, String string) {
        stream.printf("[%s] [ssw/%s]: %s%n", SIMPLE_TIME_FORMAT.format(System.currentTimeMillis()), Thread.currentThread().getName(), string);
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
            printlnWithTimeAndThread(System.err, exception.getLocalizedMessage());
            return false;
        }
    }

    public MinecraftServer getMinecraftServer() {
        return minecraftServer;
    }
}
