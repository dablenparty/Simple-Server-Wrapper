package com.hunterltd.ssw.cli;

import com.hunterltd.ssw.curse.CurseAddon;
import com.hunterltd.ssw.curse.CurseModpack;
import com.hunterltd.ssw.curse.data.CurseManifestFileEntry;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Scanner;

public class CurseCli {
    private final CurseModpack curseModpack;
    private final File serverFolder;

    public CurseCli(File modpackZipFile, File serverFile) {
        serverFolder = serverFile.isDirectory() ? serverFile : serverFile.getParentFile();
        curseModpack = new CurseModpack(modpackZipFile);
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);
        try {
            extractModpack(scanner);
        } catch (IOException | ParseException exception) {
            exception.printStackTrace();
        }

        System.out.println("Reading manifest...");
        CurseManifestFileEntry[] files = curseModpack.getManifest().getFiles();
        File modsFolder = Paths.get(serverFolder.toString(), "mods").toFile();
        try {
            if (Objects.requireNonNull(modsFolder.listFiles()).length != 0)
                FileUtils.deleteDirectory(modsFolder);
            else return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } catch (NullPointerException ignored) {}

        Client client = ClientBuilder.newClient();
        int filesLength = files.length;

        for (int i = 0; i < filesLength; i++) {
            CurseManifestFileEntry manifestFileEntry = files[i];
            String target = String.format("https://addons-ecs.forgesvc.net/api/v2/addon/%d/file/%d",
                    manifestFileEntry.getProjectID(),
                    manifestFileEntry.getFileID());
            Response response = client.target(target).request(MediaType.APPLICATION_JSON).get();
            try {
                JSONObject responseData = (JSONObject) new JSONParser().parse(response.readEntity(String.class));
                CurseAddon addon = new CurseAddon(responseData);
                System.out.printf("Mod %d of %d: %s%n", i + 1, filesLength, addon);
                addon.download(serverFolder.toString());
            } catch (ParseException | IOException e) {
                String errorMessage = e instanceof ParseException ?
                        "Failed to parse JSON data for an addon" : "Failed to download an addon";
                System.err.println(errorMessage);
            }
        }
    }

    private void extractModpack(Scanner inputScanner) throws IOException, ParseException {
        if (curseModpack.isExtracted()) {
            System.out.print("%s has already been extracted, overwrite? (y/N): ");
            if (!inputScanner.next().startsWith("y")) {
                curseModpack.getManifest().load();
                return;
            }
        }
        FileUtils.deleteDirectory(curseModpack.getExtractFolder());
        curseModpack.extractAll();
    }
}
