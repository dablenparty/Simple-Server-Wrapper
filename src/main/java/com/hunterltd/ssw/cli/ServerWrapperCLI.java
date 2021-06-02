package com.hunterltd.ssw.cli;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ServerWrapperCLI {
    public static void main(String[] args) throws IOException {
        System.out.println("Hello from the CLI");
        showVersion();
    }

    public static void showVersion() throws IOException {
        Properties mavenProperties = new Properties();
        try (InputStream resourceStream =
                     ServerWrapperCLI.class.getClassLoader().getResourceAsStream("project.properties")) {
            mavenProperties.load(resourceStream);
        }
        System.out.println(mavenProperties.getProperty("artifactId") + " v" + mavenProperties.getProperty("version"));
    }
}
