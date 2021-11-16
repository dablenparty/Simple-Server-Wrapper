package com.hunterltd.ssw.curse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class CurseModpack {
    private final Path extractedPath;
    private MinecraftOptions minecraft;
    private String manifestType;
    private int manifestVersion;
    private String name;
    private String version;
    private String author;
    private CurseMod[] files;
    private String overrides;

    public CurseModpack(Path extractedPath) {
        this.extractedPath = extractedPath;
    }


    public static CurseModpack createCurseModpack(ZipFile modpackZip) throws IOException {
        File zipFile = modpackZip.getFile();
        Path extracted = Paths.get(zipFile.getParent(), FilenameUtils.getBaseName(modpackZip.toString()));
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

    public static void main(String[] args) throws IOException {
        CurseModpack modpack = createCurseModpack(new ZipFile(new File("C:\\Users\\Hunter\\Documents\\Calm+Craft+v4.1-v4.1.zip")));
        System.out.println(modpack.toString());
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
