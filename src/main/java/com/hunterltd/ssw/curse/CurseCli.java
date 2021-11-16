package com.hunterltd.ssw.curse;

import net.lingala.zip4j.ZipFile;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Scanner;

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
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }


//        Client client = ClientBuilder.newClient();
//        int filesLength = files.length;
//
//        for (int i = 0; i < filesLength; i++) {
//            CurseMod mod = files[i];
//            String target = String.format("https://addons-ecs.forgesvc.net/api/v2/addon/%d/file/%d",
//                    mod.getProjectID(),
//                    mod.getFileID());
//            Response response = client.target(target).request(MediaType.APPLICATION_JSON).get();
//            try {
//                JSONObject responseData = (JSONObject) new JSONParser().parse(response.readEntity(String.class));
//                CurseAddon addon = new CurseAddon(responseData);
//                String message = String.format("\r\u001B[2KDownloading mod %d of %d: %s", i + 1, filesLength, addon);
//                System.out.print(message);
//                addon.download(serverFolderString);
//            } catch (ParseException | IOException e) {
//                // message has to be prefixed with '\n' because the previous lines don't end with one
//                String errorMessage = e instanceof ParseException ?
//                        "\nFailed to parse JSON data for an addon" : "\nFailed to download an addon";
//                printlnWithTimeAndThread(System.err, errorMessage);
//            }
//        }
//        System.out.println();
//        File overrideFolder = Paths.get(curseModpack.getExtractFolder().toString(), "overrides").toFile();
//        File[] overrides = overrideFolder.listFiles();
//        if (overrides != null) {
//            for (int i = 0, overridesLength = overrides.length; i < overridesLength; i++) {
//                File file = overrides[i];
//                try {
//                    System.out.printf("\r\u001B[2KCopying override %d of %d: %s", i + 1, overridesLength, file);
//                    File copyTo;
//                    if (file.isDirectory()) {
//                        copyTo = Paths.get(serverFolderString, file.getName()).toFile();
//                        FileUtils.copyDirectory(file, copyTo);
//                    } else {
//                        copyTo = serverFolder;
//                        FileUtils.copyFileToDirectory(file, copyTo);
//                    }
//                } catch (IOException exception) {
//                    exception.printStackTrace();
//                }
//            }
//            System.out.println();
//        }
//        try {
//            FileUtils.deleteDirectory(curseModpack.getExtractFolder());
//        } catch (IOException e) {
//            printlnWithTimeAndThread(System.err, "Error occurred cleaning up modpack files");
//        }
        return true;
    }

}
