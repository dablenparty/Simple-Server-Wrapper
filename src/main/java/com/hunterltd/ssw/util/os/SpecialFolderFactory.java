package com.hunterltd.ssw.util.os;

import com.hunterltd.ssw.util.MavenUtils;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

public class SpecialFolderFactory {
    public static final Path APP_DATA_PATH;

    static {
        Properties mavenProperties = MavenUtils.MAVEN_PROPERTIES;
        String artifactId = mavenProperties.getProperty("artifactId");

        String osName = System.getProperty("os.name");
        osName = osName.toLowerCase(Locale.ROOT);
        String userHome = System.getProperty("user.home");

        Path result;
        if (osName.startsWith("windows"))
            result = Path.of(userHome, "AppData", "Local", artifactId);
        else if (osName.startsWith("mac"))
            result = Path.of(userHome, "Library", "Application Support", artifactId);
        else
            result = Path.of(userHome, ".config", artifactId);
        APP_DATA_PATH = result;
    }

    public static void main(String[] args) {
        System.out.println(APP_DATA_PATH);
    }
}
