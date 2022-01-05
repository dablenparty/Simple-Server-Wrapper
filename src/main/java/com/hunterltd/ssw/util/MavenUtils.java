package com.hunterltd.ssw.util;

import com.hunterltd.ssw.cli.SswClientCli;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MavenUtils {
    private static Properties mavenProperties = null;

    public static Properties getMavenProperties() {
        if (mavenProperties == null) {
            mavenProperties = new Properties();
            try (InputStream resourceStream = SswClientCli.class.getClassLoader().getResourceAsStream("project.properties")) {
                mavenProperties.load(resourceStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return mavenProperties;
    }
}
