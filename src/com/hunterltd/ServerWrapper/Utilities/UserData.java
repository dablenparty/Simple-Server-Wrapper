package com.hunterltd.ServerWrapper.Utilities;

import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public abstract class UserData {
    private static final File dataFile; // do these in the static constructor later
    private static Map<String,?> dataMap = null;
    private static JSONObject updatedJSONObj = null;
    static {
        dataFile = new File("wrapperData.json");
        try {
             if (!dataFile.createNewFile()) readUserData();
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    private static void populateDefaults() {}

    private static void readUserData() throws IOException, ParseException {
        JSONObject obj = (JSONObject) new JSONParser().parse(new FileReader(dataFile));
        dataMap = new HashMap<>(obj.size());
        dataMap = (Map<String, ?>) obj;
    }

    public static Map<String, ?> getDataMap() {
        return dataMap;
    }

    public static void changeItem(String key, JSONAware obj) {
        updatedJSONObj.replace(key, obj);
    }

    public static void writeUserData() throws IOException {
        PrintWriter writer = new PrintWriter(dataFile);
        writer.write(updatedJSONObj.toJSONString());
        writer.flush();
        writer.close();
    }
}
