package com.hunterltd.ssw.Server.Properties;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"rawtypes", "unchecked"})
public class ServerProperties extends HashMap implements Map {
    private final File propsFile;
    private final List<String> comments = new ArrayList<>();

    public ServerProperties(File propertiesFile) throws FileNotFoundException {
        propsFile = propertiesFile;
        read();
    }

    public void read() throws FileNotFoundException {
        new BufferedReader(new FileReader(propsFile)).lines().forEach(line -> {
            if (line.startsWith("#")) {
                comments.add(line);
                return; // Ignores commented lines
            }
            String[] split = line.split("=");
            if (split.length == 1) {
                // No value for key
                this.put(split[0], null);
            } else {
                String key = split[0], value = split[1];
                try {
                    this.put(key, Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    switch (value) {
                        case "true":
                            this.put(key, true);
                            break;
                        case "false":
                            this.put(key, false);
                            break;
                        default:
                            // regular string
                            this.put(key, value);
                    }
                }
            }
        });
    }

    public void write() throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(propsFile);
        writer.write(String.join("\n", comments)); // writes the comments back at the start
        this.forEach((key, value) -> {
            String line = value == null ?
                    key + "=" : String.join("=", (String) key, String.valueOf(value));
            writer.append("\n").append(line);
        });
        writer.flush();
        writer.close();
    }

    public static void main(String[] args) throws FileNotFoundException {
        ServerProperties props = new ServerProperties(new File("C:\\Users\\gener\\Desktop\\forge\\server.properties"));
        props.read();
        props.replace("level-seed", 283475);
        props.write();
    }
}
