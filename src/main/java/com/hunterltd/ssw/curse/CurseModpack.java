package com.hunterltd.ssw.curse;

import com.hunterltd.ssw.curse.data.CurseManifestFileEntry;
import com.hunterltd.ssw.curse.data.CurseManifest;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.io.FilenameUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class CurseModpack extends ZipFile {
    private final String extractPath;
    private final CurseManifest manifest;

    public CurseModpack(File zipFile) {
        super(zipFile);
        extractPath = String.valueOf(Paths.get(zipFile.getParent(), FilenameUtils.getBaseName(zipFile.getName())));
        manifest = new CurseManifest(Paths.get(String.valueOf(extractPath), "manifest.json").toFile());
    }

    public void extractAll() throws IOException, ParseException {
        this.extractAll(String.valueOf(extractPath));
        manifest.load();
    }

    public boolean isExtracted() {
        return new File(extractPath).exists();
    }

    public String getExtractPath() {
        return extractPath;
    }

    public CurseManifest getManifest() {
        return manifest;
    }

    public static void main(String[] args) throws ParseException {
        CurseModpack pack = new CurseModpack(Paths.get(System.getProperty("user.home"), "Downloads", "Minecraft Enhanced-v1.3.zip").toFile());
        try {
            pack.extractAll();
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }
        Client client = ClientBuilder.newClient();
        for (CurseManifestFileEntry addon :
                pack.getManifest().getFiles()) {
            Response response = client.target(
                    String.format("https://addons-ecs.forgesvc.net/api/v2/addon/%d/file/%d",
                            addon.getProjectID(),
                            addon.getFileID()))
                    .request(MediaType.APPLICATION_JSON).get();
            CurseAddon test = new CurseAddon((JSONObject) new JSONParser().parse(response.readEntity(String.class)));
            try {
                test.download(pack.getExtractPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
