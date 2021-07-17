package com.hunterltd.ssw.curse.data;

import com.dablenparty.jsondata.UserDataObject;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;

/**
 * A CurseForge manifest wrapper class
 */
public class CurseManifest extends JSONObject {
    private final File manifestFile;

    /**
     * Instantiates the wrapper class from the manifest file object
     *
     * @param manifestFile Manifest file
     */
    public CurseManifest(File manifestFile) {
        this.manifestFile = manifestFile;
    }

    /**
     * Loads the manifest.json data
     *
     * @throws IOException    if an I/O error occurs reading the dada
     * @throws ParseException if a parsing error occurs reading the data
     */
    public void load() throws IOException, ParseException {
        UserDataObject.readData(manifestFile, this);
    }

    /**
     * @return The manifest file object
     */
    public File getFile() {
        return manifestFile;
    }

    /**
     * Creates {@link CurseManifestFileEntry} objects from the files array in the manifest
     *
     * @return Array of ManifestEntry objects from the files array in the manifest
     */
    public CurseManifestFileEntry[] getFiles() {
        JSONArray jsonArr = (JSONArray) this.get("files");
        CurseManifestFileEntry[] addons = new CurseManifestFileEntry[jsonArr.size()];
        for (int i = 0; i < jsonArr.size(); i++) {
            addons[i] = new CurseManifestFileEntry((JSONObject) jsonArr.get(i));
        }
        return addons;
    }
}
