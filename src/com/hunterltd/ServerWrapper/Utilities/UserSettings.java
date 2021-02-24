package com.hunterltd.ServerWrapper.Utilities;

import org.json.simple.JSONArray;
import org.json.simple.JSONAware;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unchecked") // JSONObject does not have type parameters but casts to a Map
public class UserSettings {
    private static JSONObject settingsMap = new JSONObject();
    private static final File settingsFile;
    private static final Path settingsPath;
    private static boolean firstLaunch = false;

    static {
        String osName = System.getProperty("os.name"),
                home = System.getProperty("user.home"),
                appName = "Server Wrapper";
        if (osName.contains("Windows")) {
            settingsPath = Paths.get(home,
                    "AppData",
                    "Local",
                    appName);
        } else if (osName.contains("Linux")) {
            settingsPath = Paths.get(home,
                    ".config",
                    appName);
        }
        else {
            settingsPath = Paths.get(home,
                    "Library",
                    "Application Support",
                    appName);
        }
        settingsFile = new File(Paths.get(settingsPath.toString(), "userSettings.json").toString());
        if (!new File(settingsPath.toString()).mkdirs()) {
            System.out.println(settingsPath.toString() + " already exists");
        }
        try {
            firstLaunch = settingsFile.createNewFile();
            if (firstLaunch) populateDefaults();
            else readData();
        } catch (IOException | ParseException e) {
            e.printStackTrace();
            System.exit(1);
            //TODO: set an error code to be read internally on EDT and shown in dialog
        }
    }

    private static void populateDefaults() throws FileNotFoundException {
        // Tab 1
        settingsMap.put("memory", 1024);
        settingsMap.put("extraArgs", new JSONArray());

        //Tab 2
        settingsMap.put("autoRestart", false);
        settingsMap.put("restartInterval", 1);

        writeData(settingsMap);
    }

    public static int getMemory() {
        try {
            return ((Long) settingsMap.get("memory")).intValue();
        } catch (ClassCastException e) {
            return (int) settingsMap.get("memory");
        }
    }

    public static void setMemory(int value) {
        settingsMap.replace("memory", value);
    }

    public static boolean getRestart() {
        return (boolean) settingsMap.get("autoRestart");
    }

    public static void setRestart(boolean value) {
        settingsMap.replace("autoRestart", value);
    }

    public static List<String> getExtraArgs() {
        return (List<String>) settingsMap.get("extraArgs");
    }

    public static void setExtraArgs(String[] newArgs) {
        JSONArray args = new JSONArray();
        args.addAll(Arrays.asList(newArgs));
        settingsMap.replace("extraArgs",  args);
    }

    public static boolean hasExtraArgs() {
        List<String> list = ((List<String>) settingsMap.get("extraArgs"));
        return list.size() > 0 && !list.get(0).equalsIgnoreCase("");
    }

    public static int getInterval() {
        // Sometimes it parses as a Long, sometimes it parses as an Integer
        // I haven't found a pattern to identify which it chooses (yet)
        try {
            return ((Long) settingsMap.get("restartInterval")).intValue();
        } catch (ClassCastException e) {
            return (int) settingsMap.get("restartInterval");
        }
    }

    public static void setInterval(int value) {
        settingsMap.replace("restartInterval", value);
    }

    private static void readData() throws IOException, ParseException {
        settingsMap = (JSONObject) new JSONParser().parse(new FileReader(settingsFile));
    }

    public static void save() {
        writeData(settingsMap);
    }

    private static void writeData(JSONAware jsonAware) {
        try {
            PrintWriter writer = new PrintWriter(settingsFile);
            writer.write(jsonAware.toJSONString());
            writer.flush();
            writer.close();
            settingsMap = (JSONObject) jsonAware;
        } catch (FileNotFoundException ignored) {
        }

    }

    public static boolean isFirstLaunch() {
        return firstLaunch;
    }
}
