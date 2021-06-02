package com.hunterltd.ssw.cli;

import com.hunterltd.ssw.server.MinecraftServer;
import com.hunterltd.ssw.utilities.Settings;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class ServerWrapperCLI {
    private final Properties mavenProperties;
    private final boolean propertiesLoaded;
    private volatile MinecraftServer minecraftServer;

    public static void main(String[] args) {
        if (args.length != 1) {
            // TODO: write help message
            throw new IllegalArgumentException((args.length > 1 ? "Too many" : "Missing") + " arguments");
        }

        ServerWrapperCLI wrapperCli = new ServerWrapperCLI(new File(args[0]));
        wrapperCli.showVersion();
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
}
