package com.hunterltd.ssw.utilities;

import com.dablenparty.jsondata.UserDataObject;
import org.json.simple.JSONArray;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unchecked")
public class MinecraftServerSettings extends UserDataObject {
    public MinecraftServerSettings(String appName) throws IOException {
        super(appName);
    }

    public MinecraftServerSettings(Path settingsPath) throws IOException {
        super(settingsPath);
    }

    /**
     * Creates a new settings file in the default path ({serverFile.parent}/ssw/wrapperSettings.json)
     *
     * @param serverFile Server file object
     * @return MinecraftServerSettings object
     */
    public static MinecraftServerSettings getSettingsFromDefaultPath(File serverFile) {
        Path settingsPath = Paths.get(serverFile.getParent(), "ssw", "wrapperSettings.json");
        try {
            return new MinecraftServerSettings(settingsPath);
        } catch (IOException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    @Override
    public void populateDefaults() {
        // Tab 1
        this.put("memory", 1024);
        this.put("extraArgs", new JSONArray());

        //Tab 2
        this.put("autoRestart", false);
        this.put("restartInterval", 1); // hours
        this.put("autoShutdown", false);
        this.put("shutdownInterval", 30); // minutes, defaults to 1 minecraft day
    }

    public int getMemory() {
        try {
            return ((Long) this.get("memory")).intValue();
        } catch (ClassCastException e) {
            return (int) this.get("memory");
        }
    }

    public void setMemory(int value) {
        this.replace("memory", value);
    }

    public boolean getRestart() {
        return (boolean) this.get("autoRestart");
    }

    public void setRestart(boolean value) {
        this.replace("autoRestart", value);
    }

    public boolean getShutdown() {
        return (boolean) this.get("autoShutdown");
    }

    public void setShutdown(boolean value) {
        this.replace("autoShutdown", value);
    }

    public List<String> getExtraArgs() {
        return (List<String>) this.get("extraArgs");
    }

    public void setExtraArgs(String[] newArgs) {
        JSONArray args = new JSONArray();
        args.addAll(Arrays.asList(newArgs));
        this.replace("extraArgs", args);
    }

    public boolean hasExtraArgs() {
        List<String> list = ((List<String>) this.get("extraArgs"));
        return list.size() > 0 && !list.get(0).equalsIgnoreCase("");
    }

    public int getRestartInterval() {
        // Sometimes it parses as a Long, sometimes it parses as an Integer
        // I haven't found a pattern to identify which it chooses (yet)
        try {
            return ((Long) this.get("restartInterval")).intValue();
        } catch (ClassCastException e) {
            return (int) this.get("restartInterval");
        }
    }

    public void setRestartInterval(int value) {
        this.replace("restartInterval", value);
    }

    public int getShutdownInterval() {
        try {
            return ((Long) this.get("shutdownInterval")).intValue();
        } catch (ClassCastException e) {
            return (int) this.get("shutdownInterval");
        }
    }

    public void setShutdownInterval(int value) {
        this.replace("shutdownInterval", value);
    }
}
