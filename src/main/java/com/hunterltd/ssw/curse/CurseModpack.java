package com.hunterltd.ssw.curse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.hunterltd.ssw.curse.api.CurseAddon;
import com.hunterltd.ssw.util.events.EventEmitter;
import com.hunterltd.ssw.util.serial.JsonUtils;
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

import static com.hunterltd.ssw.util.concurrency.ThreadUtils.printfWithTimeAndThread;

public class CurseModpack extends EventEmitter implements AutoCloseable {
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
        Gson gson = JsonUtils.GSON_BUILDER.registerTypeAdapter(
                CurseModpack.class,
                new CurseModpackInstanceCreator(extracted)
        ).create();
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

        Arrays.stream(new String[]{ "mods", "resourcepacks", "config" })
                .map(s -> Paths.get(serverFolderString, s).toFile())
                .forEach(directory -> {
                    if (!directory.exists())
                        return;
                    System.out.printf("Deleting old '%s' folder%n", FilenameUtils.getBaseName(directory.toString()));
                    try {
                        FileUtils.deleteDirectory(directory);
                    } catch (IOException e) {
                        System.err.println("There was an error deleting " + directory);
                        emit("error", e);
                        e.printStackTrace();
                    }
                });

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
            emit("download", addon, i + 1, filesLength);
            try {
                addon.download(serverFolderString);
            } catch (IOException e) {
                if (prettyPrint)
                    System.out.println();
                printfWithTimeAndThread(System.err, "There was an error downloading mod #%d '%s':%n", i + 1, addon);
                emit("error", e);
                e.printStackTrace();
            }
        }
        if (prettyPrint)
            System.out.println();
        Path overrideFolder = Paths.get(getExtractedPath().toString(), "overrides");
        try {
            Files.list(overrideFolder).map(Path::toFile).forEach(file -> {
                try {
                    String baseName = FilenameUtils.getBaseName(file.toString());
                    System.out.printf("Copying '%s' override%n", baseName);
                    emit("override", baseName);
                    File copyTo;
                    if (file.isDirectory()) {
                        copyTo = Paths.get(serverFolderString, file.getName()).toFile();
                        FileUtils.copyDirectory(file, copyTo);
                    } else {
                        copyTo = serverFolder.toFile();
                        FileUtils.copyFileToDirectory(file, copyTo);
                    }
                } catch (IOException exception) {
                    emit("error", exception);
                    exception.printStackTrace();
                }
            });
        } catch (IOException e) {
            System.err.println("There was an error copying the overrides");
            emit("error", e);
            e.printStackTrace();
        }
        emit("done");
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
