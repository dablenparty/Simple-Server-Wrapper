package com.hunterltd.ssw.minecraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.hunterltd.ssw.util.concurrency.ThreadUtils;
import com.hunterltd.ssw.util.os.OsConstants;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static com.hunterltd.ssw.util.serial.GsonUtils.GSON_EXCLUDE_STRATEGY;

public class VersionManifestV2 {
    /**
     * The default path to {@code version_manifest_v2.json}
     * <p>
     * It is found in the following folders on each corresponding system:
     * <ul>
     *     <li>Windows - %APPDATA%\.minecraft\versions</li>
     *     <li>Mac - ~/Library/Application Support/minecraft/versions</li>
     *     <li>Linux - ~/.minecraft/versions</li>
     * </ul>
     * <p>
     * This constant contains the full path, for example a Linux machine would return
     * {@code ~/.minecraft/versions/version_manifest_v2.json}
     */
    public static final Path DEFAULT_PATH;
    /**
     * This class's instance
     */
    public static final VersionManifestV2 INSTANCE;

    static {
        String firstToken = OsConstants.OS_NAME.split(" ")[0];
        // prevents issues from case differences
        firstToken = firstToken.toLowerCase(Locale.ROOT);
        Path parentFolder = switch (firstToken) {
            case "windows" -> Path.of(OsConstants.USER_HOME, "AppData", "Roaming", ".minecraft");
            case "mac" -> Path.of(OsConstants.USER_HOME, "Library", "Application Support", "minecraft");
            case "linux", "freebsd", "sunos" -> Path.of(OsConstants.USER_HOME, ".minecraft");
            default -> throw new UnsupportedOperationException(OsConstants.OS_NAME + " is not a supported OS");
        };
        DEFAULT_PATH = Path.of(parentFolder.toString(), "versions", "version_manifest_v2.json");
        VersionManifestV2 manifest = null;
        try {
            if (!Files.exists(DEFAULT_PATH))
                download();
            manifest = parseManifestFile();
            manifest.versions.sort(Comparator.reverseOrder());
        } catch (IOException e) {
            e.printStackTrace();
        }
        INSTANCE = manifest;
    }

    private Latest latest;
    private List<MinecraftVersion> versions;

    private VersionManifestV2() {
    }

    /**
     * Loads the version manifest
     *
     * This cheats a little since the loading is actually done in the static block, but by loading the class into memory
     * it forces that block to be run, thus loading all the data (and downloading the manifest if necessary)
     */
    public static void load() {
        ThreadUtils.printlnWithTimeAndThread(System.out, "Loading version manifest...");
    }

    /**
     * Parses the manifest file found at {@link VersionManifestV2#DEFAULT_PATH} into a new instance of
     * {@link VersionManifestV2}
     *
     * @return deserialized {@code VersionManifestV2}
     * @throws IOException if an I/O error occurs reading from the file or a malformed or unmappable byte sequence is
     *                     read (from {@link Files#readString(Path)})
     */
    private static VersionManifestV2 parseManifestFile() throws IOException {
        GsonBuilder builder = new GsonBuilder().setExclusionStrategies(GSON_EXCLUDE_STRATEGY);
        builder.registerTypeAdapter(VersionType.class, (JsonDeserializer<VersionType>) (jsonElement, type, jsonDeserializationContext) -> VersionType.parseString(jsonElement.getAsString()));
        Gson gson = builder.create();
        String jsonString = Files.readString(DEFAULT_PATH);
        return gson.fromJson(jsonString, VersionManifestV2.class);
    }

    /**
     * Downloads the version manifest
     *
     * @throws IOException if an I/O error occurs copying the URL to a file
     */
    private static void download() throws IOException {
        Files.createDirectories(DEFAULT_PATH.getParent());
        URL downloadUrl = new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json");
        FileUtils.copyURLToFile(downloadUrl, DEFAULT_PATH.toFile());
    }

    public List<MinecraftVersion> getVersions() {
        return versions;
    }

    public String getLatestRelease() {
        return latest.release;
    }

    public String getLatestSnapshot() {
        return latest.snapshot;
    }

    public enum VersionType {
        ALPHA,
        BETA,
        RELEASE,
        SNAPSHOT;

        /**
         * Parses a string into a {@code VersionType} constant
         *
         * @param typeString can be either {@code old_alpha}, {@code old_beta}, {@code release}, or {@code snapshot}
         * @return {@code VersionType} constant associated with {@code typeString}, or {@code null} if {@code typeString}
         * is invalid
         */
        private static VersionType parseString(String typeString) {
            return switch (typeString) {
                case "old_alpha" -> VersionType.ALPHA;
                case "old_beta" -> VersionType.BETA;
                case "release" -> VersionType.RELEASE;
                case "snapshot" -> VersionType.SNAPSHOT;
                default -> null;
            };
        }
    }

    private static class Latest {
        private String release;
        private String snapshot;
    }

}
