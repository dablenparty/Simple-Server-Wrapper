package com.hunterltd.ssw.curse.data;

import com.dablenparty.jsondata.UserDataObject;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;

public class CurseManifest extends JSONObject {
    private final File manifestFile;

    public CurseManifest(File manifestFile) {
        this.manifestFile = manifestFile;
    }

    public void load() throws IOException, ParseException {
        UserDataObject.readData(manifestFile, this);
    }

    public File getFile() {
        return manifestFile;
    }

    public CurseManifestFileEntry[] getFiles() {
        JSONArray jsonArr = (JSONArray) this.get("files");
        CurseManifestFileEntry[] addons = new CurseManifestFileEntry[jsonArr.size()];
        for (int i = 0; i < jsonArr.size(); i++) {
            addons[i] = new CurseManifestFileEntry((JSONObject) jsonArr.get(i));
        }
        return addons;
    }
}
