package com.hunterltd.ssw.curse;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;

/**
 * Wrapper class for a CurseForge addon
 */
@SuppressWarnings("unchecked")
public class CurseAddon extends JSONObject {
    public CurseAddon(JSONObject obj) {
        this.putAll(obj);
    }

    @Override
    public String toString() {
        return (String) this.get("displayName");
    }

    /**
     * @param destFolder Folder to download the addon to
     * @throws IOException if an I/O error occurs downloading or saving the file
     */
    public void download(String destFolder) throws IOException {
        // Spaces don't produce a MalformedURLException, although they will cause IOExceptions because although they're
        // not technically invalid characters, a browser cannot interpret them. Instead, use the "%20" character
        URL source = new URL(((String) this.get("downloadUrl")).replace(" ", "%20"));
        String folderName = ((String) this.get("fileName")).endsWith(".zip") ? "resourcepacks" : "mods";
        File dest = Paths.get(destFolder, folderName, (String) this.get("fileName")).toFile();

        FileUtils.copyURLToFile(source, dest, 10000, 10000);
    }
}
