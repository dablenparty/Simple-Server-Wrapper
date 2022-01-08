package com.hunterltd.ssw.minecraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.hunterltd.ssw.util.os.OsConstants;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static com.hunterltd.ssw.util.serial.GsonUtils.GSON_EXCLUDE_STRATEGY;

public class VersionManifestV2 {
    public static final Path DEFAULT_PATH;
    public static final VersionManifestV2 INSTANCE;

    static {
        String firstToken = OsConstants.OS_NAME.split(" ")[0];
        firstToken = firstToken.toLowerCase(Locale.ROOT);
        Path parentFolder = switch (firstToken) {
            case "windows" -> Path.of(OsConstants.USER_HOME, "AppData", "Roaming");
            case "mac" -> Path.of(OsConstants.USER_HOME, "Library", "Application Support");
            case "linux", "freebsd", "sunos" -> Path.of(OsConstants.USER_HOME);
            default -> throw new UnsupportedOperationException(OsConstants.OS_NAME + " is not a supported OS");
        };
        String minecraftFolder = firstToken.equals("mac") ? "minecraft" : ".minecraft";
        DEFAULT_PATH = Path.of(parentFolder.toString(), minecraftFolder, "versions", "version_manifest_v2.json");
        VersionManifestV2 manifest = null;
        try {
            if (!Files.exists(DEFAULT_PATH))
                download();
            manifest = parseManifestFile();
            manifest.versions.sort(MinecraftVersion::compareTo);
        } catch (IOException e) {
            e.printStackTrace();
        }
        INSTANCE = manifest;
    }

    private Latest latest;
    private List<MinecraftVersion> versions;

    private VersionManifestV2() {
    }

    private static VersionManifestV2 parseManifestFile() throws IOException {
        GsonBuilder builder = new GsonBuilder().setExclusionStrategies(GSON_EXCLUDE_STRATEGY);
        builder.registerTypeAdapter(VersionType.class, (JsonDeserializer<VersionType>) (jsonElement, type, jsonDeserializationContext) -> VersionType.parseString(jsonElement.getAsString()));
        Gson gson = builder.create();
        String jsonString = Files.readString(DEFAULT_PATH);
        return gson.fromJson(jsonString, VersionManifestV2.class);
    }

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

        public static VersionType parseString(String typeString) {
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
