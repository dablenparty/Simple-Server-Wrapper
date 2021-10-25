package com.hunterltd.ssw.curse;

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

import static com.hunterltd.ssw.utilities.concurrency.ThreadUtils.printlnWithTimeAndThread;

public class CurseCli {
    private final CurseModpack curseModpack;
    private final File serverFolder;

    public CurseCli(File modpackZipFile, File serverFile) {
        serverFolder = serverFile.isDirectory() ? serverFile : serverFile.getParentFile();
        curseModpack = new CurseModpack(modpackZipFile);
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);
        printlnWithTimeAndThread(System.out, "Extracting modpack...");
        try {
            extractModpack(scanner);
        } catch (IOException | ParseException exception) {
            exception.printStackTrace();
        }

        printlnWithTimeAndThread(System.out, "Reading manifest...");
        CurseManifestFileEntry[] files = curseModpack.getManifest().getFiles();
        File modsFolder = Paths.get(serverFolder.toString(), "mods").toFile();
        try {
            if (Objects.requireNonNull(modsFolder.listFiles()).length != 0) {
                System.out.print("The mods folder is not empty, would you like to overwrite it? (y/N): ");
                if (scanner.next().startsWith("y"))
                    FileUtils.deleteDirectory(modsFolder);
                else return;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } catch (NullPointerException ignored) {
        } finally {
            scanner.close();
        }

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
                String message = String.format("\r\u001B[2KDownloading mod %d of %d: %s", i + 1, filesLength, addon);
                System.out.print(message);
                addon.download(serverFolder.toString());
            } catch (ParseException | IOException e) {
                // message has to be prefixed with '\n' because the previous lines don't end with one
                String errorMessage = e instanceof ParseException ?
                        "\nFailed to parse JSON data for an addon" : "\nFailed to download an addon";
                printlnWithTimeAndThread(System.err, errorMessage);
            }
        }
        System.out.println();
        File overrideFolder = Paths.get(curseModpack.getExtractFolder().toString(), "overrides").toFile();
        File[] overrides = overrideFolder.listFiles();
        if (overrides != null) {
            for (int i = 0, overridesLength = overrides.length; i < overridesLength; i++) {
                File file = overrides[i];
                try {
                    System.out.printf("\r\u001B[2KCopying override %d of %d: %s", i + 1, overridesLength, file);
                    File copyTo;
                    if (file.isDirectory()) {
                        copyTo = Paths.get(serverFolder.toString(), file.getName()).toFile();
                        FileUtils.copyDirectory(file, copyTo);
                    } else {
                        copyTo = serverFolder;
                        FileUtils.copyFileToDirectory(file, copyTo);
                    }
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }
            System.out.println();
        }
        try {
            FileUtils.deleteDirectory(curseModpack.getExtractFolder());
        } catch (IOException e) {
            printlnWithTimeAndThread(System.err, "Error occurred cleaning up modpack files");
        }
    }

    private void extractModpack(Scanner inputScanner) throws IOException, ParseException {
        if (curseModpack.isExtracted()) {
            System.out.printf("%s has already been extracted, overwrite? (y/N): ", new File(String.valueOf(curseModpack)).getName());
            if (!inputScanner.next().startsWith("y")) {
                curseModpack.getManifest().load();
                return;
            }
        }
        FileUtils.deleteDirectory(curseModpack.getExtractFolder());
        curseModpack.extractAll();
    }
}
