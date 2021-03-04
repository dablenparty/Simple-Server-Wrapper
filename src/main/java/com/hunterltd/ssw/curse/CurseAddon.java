package com.hunterltd.ssw.curse;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;

@SuppressWarnings("unchecked")
public class CurseAddon extends JSONObject {
    public CurseAddon(JSONObject obj) {
        this.putAll(obj);
    }

    public boolean isAvailable() {
        return (boolean) this.get("isAvailable");
    }

    public void download(String destFolder) throws IOException {
        // Spaces don't produce a MalformedURLException, although they will cause IOExceptions because although they're
        // not technically invalid characters, a browser cannot interpret them. Instead, they use the "%20" character
        URL source = new URL(((String) this.get("downloadUrl")).replace(" ", "%20"));
        File dest = new File(String.valueOf(Paths.get(destFolder, "mods", (String) this.get("fileName"))));

        FileUtils.copyURLToFile(source, dest, 10000, 10000);
    }
}
