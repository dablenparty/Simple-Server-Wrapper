package com.hunterltd.ssw.curse.api;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

public class CurseAddon {
    private static final int CONNECTION_TIMEOUT = 10000;
    private String displayName;
    private URL downloadUrl;
    private String fileName;

    public static CurseAddon newCurseAddon(String jsonData) {
        return new Gson().fromJson(jsonData, CurseAddon.class);
    }

    public String getDisplayName() {
        return displayName;
    }

    public URL getDownloadUrl() {
        return downloadUrl;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Downloads this curse addon from the {@code downloadUrl} field to the supplied folder, then into either "mods" or
     * "resourcepacks" inside said folder (based upon the file extension).
     *
     * @param destinationPath destination folder
     * @throws IOException if an I/O error occurs copying the URL to the file
     */
    public void download(String destinationPath) throws IOException {
        String folderName = fileName.endsWith(".zip") ? "resourcepacks" : "mods";
        File destinationFile = Path.of(destinationPath, folderName, fileName).toFile();
        FileUtils.copyURLToFile(downloadUrl, destinationFile, CONNECTION_TIMEOUT, CONNECTION_TIMEOUT);
    }
}
