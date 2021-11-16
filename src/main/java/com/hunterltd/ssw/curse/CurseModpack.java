package com.hunterltd.ssw.curse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.hunterltd.ssw.curse.api.CurseAddon;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.RandomStringUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.Scanner;

import static com.hunterltd.ssw.utilities.concurrency.ThreadUtils.printfWithTimeAndThread;

public class CurseModpack implements AutoCloseable {
    private final Path extractedPath;
    private MinecraftOptions minecraft;
    private String manifestType;
    private int manifestVersion;
    private String name;
    private String version;
    private String author;
    private CurseMod[] files;
    private String overrides;

    private CurseModpack(Path extractedPath) {
        this.extractedPath = extractedPath;
    }

    public static CurseModpack createCurseModpack(ZipFile modpackZip) throws IOException {
        // extracts to a randomly named folder
        // use letters + numbers
        String folderName = RandomStringUtils.random(16, true, true);
        Path extracted = Paths.get(modpackZip.getFile().getParent(), folderName);
        modpackZip.extractAll(extracted.toString());
        Path manifest = Paths.get(extracted.toString(), "manifest.json");
        String jsonString = Files.readString(manifest);
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(
                CurseModpack.class,
                new CurseModpackInstanceCreator(extracted)
        );
        Gson gson = gsonBuilder.create();
        return gson.fromJson(jsonString, CurseModpack.class);
    }

    public static void main(String[] args) {
        try (CurseModpack modpack = createCurseModpack(new ZipFile(new File("C:\\Users\\Hunter\\Documents\\Calm+Craft+v4.1-v4.1.zip")))) {
            System.out.println(modpack.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return name + " on " + minecraft.version + " (" + files.length + " mods)";
    }

    /**
     * Cleans up the extracted files
     *
     * @throws IOException If an I/O error occurs deleting the extracted files
     */
    public void cleanUpExtractedFiles() throws IOException {
        FileUtils.deleteDirectory(extractedPath.toFile());
    }

    public Path getExtractedPath() {
        return extractedPath;
    }

    public String getManifestType() {
        return manifestType;
    }

    public int getManifestVersion() {
        return manifestVersion;
    }

    public String getOverrides() {
        return overrides;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getAuthor() {
        return author;
    }

    public CurseMod[] getFiles() {
        return files;
    }

    public String getMinecraftVersion() {
        return minecraft.version;
    }

    public ModLoader[] getMinecraftModLoader() {
        return minecraft.modLoaders;
    }

    @Override
    public void close() throws IOException {
        cleanUpExtractedFiles();
    }

    public boolean install(Path serverFolder) {
        return install(serverFolder, false);
    }

    public boolean install(Path serverFolder, boolean prettyPrint) {
        CurseMod[] files = getFiles();
        String serverFolderString = serverFolder.toString();

        Scanner scanner = new Scanner(System.in);
        String[] foldersToCheck = new String[]{ "mods", "resourcepacks", "config" };
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

        for (int i = 0, filesLength = files.length; i < filesLength; i++) {
            CurseMod mod = files[i];
            String target = String.format("https://addons-ecs.forgesvc.net/api/v2/addon/%d/file/%d",
                    mod.getProjectID(),
                    mod.getFileID());
            Response response = client.target(target).request(MediaType.APPLICATION_JSON).get();
            CurseAddon addon = CurseAddon.newCurseAddon(response.readEntity(String.class));
            String formatString = "%sDownloading mod %d of %d: %s";
            String prefix;
            if (prettyPrint)
                prefix = "\r\u001B[2K";
            else {
                prefix = "";
                formatString += "%n";
            }
            System.out.printf(formatString, prefix, i + 1, filesLength, addon);
            try {
                addon.download(serverFolderString);
            } catch (IOException e) {
                if (prettyPrint)
                    System.out.println();
                printfWithTimeAndThread(System.err, "There was an error downloading mod #%d '%s':%n", i + 1, addon);
                e.printStackTrace();
            }
        }
        if (prettyPrint)
            System.out.println();
        Path overrideFolder = Paths.get(getExtractedPath().toString(), "overrides");
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
        return true;
    }

    private static final class MinecraftOptions {
        private String version;
        private ModLoader[] modLoaders;

        public String getVersion() {
            return version;
        }

        public ModLoader[] getModLoaders() {
            return modLoaders;
        }

        @Override
        public String toString() {
            return "MinecraftOptions{" +
                    "version='" + version + '\'' +
                    ", modLoaders=" + Arrays.toString(modLoaders) +
                    '}';
        }
    }

    public static final class ModLoader {
        private String id;
        private boolean primary;

        public String getId() {
            return id;
        }

        public boolean isPrimary() {
            return primary;
        }

        @Override
        public String toString() {
            return "ModLoader{" +
                    "id='" + id + '\'' +
                    ", primary=" + primary +
                    '}';
        }
    }

    private record CurseModpackInstanceCreator(Path extractedPath) implements InstanceCreator<CurseModpack> {
        @Override
        public CurseModpack createInstance(Type type) {
            return new CurseModpack(extractedPath);
        }
    }
}
