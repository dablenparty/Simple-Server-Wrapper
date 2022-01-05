package com.hunterltd.ssw.util.os;

import com.hunterltd.ssw.util.MavenUtils;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

public class SpecialFolderFactory {
    private static Path appData = null;

    public static Path getAppDataPath() {
        if (appData == null) {
            Properties mavenProperties = MavenUtils.getMavenProperties();
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
            appData = result;
        }
        return appData;
    }

    public static void main(String[] args) {
        System.out.println(getAppDataPath());
    }
}
