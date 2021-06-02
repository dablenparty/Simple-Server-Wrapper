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
import java.util.concurrent.TimeUnit;

public class ServerWrapperCLI {
    private final Properties mavenProperties;
    private final boolean propertiesLoaded;
    private volatile MinecraftServer minecraftServer;

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            // TODO: write help message
            throw new IllegalArgumentException((args.length > 1 ? "Too many" : "Missing") + " arguments");
        }

        ServerWrapperCLI wrapperCli = new ServerWrapperCLI(new File(args[0]));
        MinecraftServer minecraftServer = wrapperCli.getMinecraftServer();
        ExecutorService inputService = null, errorService = null;
        wrapperCli.showVersion();

        doLoop: do {
            Scanner inputScanner = new Scanner(System.in);
            String command = inputScanner.nextLine();
            switch (command) {
                case "start":
                    System.out.println("Starting server...");
                    minecraftServer.run();
                    inputService = StreamGobbler.execute(minecraftServer.getServerProcess().getInputStream(), System.out::println);
                    errorService = StreamGobbler.execute(minecraftServer.getServerProcess().getErrorStream(), System.err::println);
                    break;
                case "close":
                    if (minecraftServer.isRunning()) {
                        try {
                            minecraftServer.stop();
                            minecraftServer.getServerProcess().waitFor(5L, TimeUnit.SECONDS);
                        } catch (IOException | InterruptedException exception) {
                            System.err.println(exception.getLocalizedMessage());
                            minecraftServer.getServerProcess().destroy();
                        }
                    }
                    if (inputService != null) {
                        inputService.shutdown();
                        errorService.shutdown();
                    }
                    inputScanner.close();
                    break doLoop;
                default:
                    if (minecraftServer.isRunning()) minecraftServer.sendCommand(command.strip());
                    break;
            }
        } while (true);
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
