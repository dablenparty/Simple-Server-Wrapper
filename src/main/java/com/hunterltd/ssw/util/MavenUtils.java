package com.hunterltd.ssw.util;

import com.hunterltd.ssw.cli.SswClientCli;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MavenUtils {
    public static final Properties MAVEN_PROPERTIES;

    static {
        MAVEN_PROPERTIES = new Properties();
        try (InputStream resourceStream = SswClientCli.class.getClassLoader().getResourceAsStream("project.properties")) {
            MAVEN_PROPERTIES.load(resourceStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
