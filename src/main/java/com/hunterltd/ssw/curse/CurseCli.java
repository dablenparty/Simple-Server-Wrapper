package com.hunterltd.ssw.curse;

import com.hunterltd.ssw.curse.api.CurseAddon;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Scanner;

import static com.hunterltd.ssw.utilities.concurrency.ThreadUtils.printfWithTimeAndThread;
import static com.hunterltd.ssw.utilities.concurrency.ThreadUtils.printlnWithTimeAndThread;

public class CurseCli {
    private final ZipFile modpackZip;
    private final Path serverFolder;

    public CurseCli(ZipFile modpackZip, File serverFile) {
        this.modpackZip = modpackZip;
        this.serverFolder = serverFile.isDirectory() ? serverFile.toPath() : serverFile.getParentFile().toPath();
    }

    public boolean run() {
        printlnWithTimeAndThread(System.out, "Extracting modpack...");
        try (CurseModpack modpack = CurseModpack.createCurseModpack(modpackZip)) {
            CurseMod[] files = modpack.getFiles();
            String serverFolderString = serverFolder.toString();

            Scanner scanner = new Scanner(System.in);
            String[] foldersToCheck = new String[]{ "mods", "resourcepacks" };
            for (String folderName : foldersToCheck) {
                File folder = Paths.get(serverFolderString, folderName).toFile();
                try {
                    if (folder.exists() && Objects.requireNonNull(folder.listFiles()).length != 0) {
                        System.out.printf("The '%s' folder is not empty, would you like to overwrite it? (y/N): ", folderName);
                        if (scanner.next().startsWith("y")) {
                            FileUtils.deleteDirectory(folder);
                            scanner.nextLine();
                        } else
                            return false;
                    }
                } catch (NullPointerException ignored) {
                    // file is not a directory, can continue
                } catch (IOException e) {
                    System.err.printf("An error occurred deleting '%s'%n", folder);
                    e.printStackTrace();
                }
            }
            scanner.close();

            Client client = ClientBuilder.newClient();
            int filesLength = files.length;

            for (int i = 0; i < filesLength; i++) {
                CurseMod mod = files[i];
                String target = String.format("https://addons-ecs.forgesvc.net/api/v2/addon/%d/file/%d",
                        mod.getProjectID(),
                        mod.getFileID());
                Response response = client.target(target).request(MediaType.APPLICATION_JSON).get();
                CurseAddon addon = CurseAddon.newCurseAddon(response.readEntity(String.class));
                System.out.printf("\r\u001B[2KDownloading mod %d of %d: %s", i + 1, filesLength, addon);
                try {
                    addon.download(serverFolderString);
                } catch (IOException e) {
                    printfWithTimeAndThread(System.err, "There was an error downloading mod #%d '%s':%n", i + 1, addon);
                    e.printStackTrace();
                }
            }
            System.out.println();
            Path overrideFolder = Paths.get(modpack.getExtractedPath().toString(), "overrides");
            try {
                Files.list(overrideFolder).map(Path::toFile).forEach(file -> {
                    try {
                        System.out.printf("Copying '%s' override%n", FilenameUtils.getBaseName(file.toString()));
                        File copyTo;
                        if (file.isDirectory()) {
                            copyTo = Paths.get(serverFolderString, file.getName()).toFile();
                            FileUtils.copyDirectory(file, copyTo);
                        } else {
                            copyTo = serverFolder.toFile();
                            FileUtils.copyFileToDirectory(file, copyTo);
                        }
                    } catch (IOException exception) {
                        exception.printStackTrace();
                    }
                });
            } catch (IOException e) {
                System.err.println("There was an error copying the overrides");
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
