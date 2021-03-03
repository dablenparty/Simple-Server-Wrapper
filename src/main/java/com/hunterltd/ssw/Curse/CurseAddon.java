package com.hunterltd.ssw.Curse;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.io.FilenameUtils;
import org.json.simple.JSONObject;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Paths;

@SuppressWarnings("unchecked")
public class CurseAddon extends JSONObject {
    public CurseAddon(JSONObject obj) {
        this.putAll(obj);
    }

    public boolean isRequired() {
        return (boolean) this.get("required");
    }

    public int getProjectID() {
        return (int) this.get("projectID");
    }

    public int getFileID() {
        return (int) this.get("fileID");
    }

    public static void main(String[] args) {
        Client client = ClientBuilder.newClient();
        Response response = client.target("https://addons-ecs.forgesvc.net/api/v2/addon/310806")
                .request(MediaType.TEXT_PLAIN_TYPE)
                .get();

        System.out.println("status: " + response.getStatus());
        System.out.println("headers: " + response.getHeaders());
        System.out.println("body:" + response.readEntity(String.class));
        System.out.println("Done!");

        FileDialog fd = new FileDialog((Dialog) null, "Choose ZIP", FileDialog.LOAD);
        fd.setVisible(true);
        try {
            ZipFile zip = new ZipFile(String.valueOf(Paths.get(fd.getDirectory(), fd.getFile())));
            zip.extractAll(String.valueOf(Paths.get(fd.getDirectory(), FilenameUtils.getBaseName(fd.getFile()))));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
