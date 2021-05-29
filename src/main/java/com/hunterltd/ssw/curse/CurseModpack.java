package com.hunterltd.ssw.curse;

import com.hunterltd.ssw.curse.data.CurseManifestFileEntry;
import com.hunterltd.ssw.curse.data.CurseManifest;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * Wrapper class for a CurseForge modpack
 */
public class CurseModpack extends ZipFile {
    private final String extractPath;
    private final CurseManifest manifest;

    /**
     * Instantiates the wrapper class with the given ZIP archive
     * @param zipFile ZIP archive containing modpack data
     */
    public CurseModpack(File zipFile) {
        super(zipFile);
        extractPath = String.valueOf(Paths.get(zipFile.getParent(), FilenameUtils.getBaseName(zipFile.getName())));
        manifest = new CurseManifest(Paths.get(String.valueOf(extractPath), "manifest.json").toFile());
    }

    /**
     * Extracts the modpack in-place and loads the manifest
     * @throws IOException if an I/O error occurs loading the manifest
     * @throws ParseException if a parsing error occurs loading the manifest
     */
    public void extractAll() throws IOException, ParseException {
        this.extractAll(extractPath);
        manifest.load();
    }

    /**
     * @return Boolean on whether the extracted folder exists
     */
    public boolean isExtracted() {
        return new File(extractPath).exists();
    }

    /**
     * @return Extract folder as a String
     */
    public String getExtractPath() {
        return extractPath;
    }

    /**
     * @return Wrapper for the packs manifest.json
     */
    public CurseManifest getManifest() {
        return manifest;
    }

    /**
     * Installs the modpack in the specified folder
     * @param folder Install location
     * @return Boolean representing if an error occurs or not
     */
    public boolean install(String folder) {
        // this will return a boolean if an error occurs because one file having an error shouldn't block all files from
        // downloading/installing
        boolean error = false;
        Client client = ClientBuilder.newClient();
        for (CurseManifestFileEntry manifestEntry :
                manifest.getFiles()) {
            // for every mod found in the manifest, makes a call to the Curse API to get the download link
            Response response = client.target(
                    String.format("https://addons-ecs.forgesvc.net/api/v2/addon/%d/file/%d",
                            manifestEntry.getProjectID(),
                            manifestEntry.getFileID())
            ).request(MediaType.APPLICATION_JSON).get();
            try {
                CurseAddon addon = new CurseAddon((JSONObject) new JSONParser().parse(response.readEntity(String.class)));
                addon.download(folder);
            } catch (ParseException | IOException e) {
                e.printStackTrace();
                error = true;
            }
        }

        // copies over the override files from the Curse modpack
        File overrides = Paths.get(extractPath, "overrides", "mods").toFile();
        try {
            FileUtils.copyDirectory(overrides, Paths.get(folder, "mods").toFile());
        } catch (IOException e) {
            e.printStackTrace();
            error = true;
        }

        return error;
    }

    public static void main(String[] args) {
        CurseModpack pack = new CurseModpack(Paths.get(System.getProperty("user.home"), "Downloads", "Minecraft Enhanced-v1.6.zip").toFile());
        try {
            pack.extractAll();
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }
        String message = !pack.install(pack.getExtractPath()) ? "Done!" : "Error(s) occurred, see above";
        System.out.println(message);
    }
}
