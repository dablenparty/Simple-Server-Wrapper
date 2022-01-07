package com.hunterltd.ssw.minecraft;

import com.google.gson.*;
import com.hunterltd.ssw.util.os.OsConstants;
import com.hunterltd.ssw.util.serial.JsonUtils;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class VersionManifestV2 {
    public static final Path DEFAULT_PATH;

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
    }

    private Latest latest;
    private List<MinecraftVersion> versions;

    public static VersionManifestV2 parseManifestFile(Path manifestPath) throws IOException {
        GsonBuilder builder = JsonUtils.GSON_BUILDER;
        builder.registerTypeAdapter(VersionType.class, new VersionTypeDeserializer());
        Gson gson = builder.create();
        String jsonString = Files.readString(manifestPath);
        return gson.fromJson(jsonString, VersionManifestV2.class);
    }

    public static void download(Path destinationPath) throws IOException {
        Files.createDirectories(destinationPath.getParent());
        URL downloadUrl = new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json");
        FileUtils.copyURLToFile(downloadUrl, destinationPath.toFile());
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
        RELEASE,
        SNAPSHOT;

        public static VersionType parseString(String typeString) {
            return switch (typeString) {
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

    public static class MinecraftVersion {
        private String id;
        private VersionType type;
        private URL url;
        private String time;
        private String releaseTime;
        private String sha1;
        private short complianceLevel;

        public String getSha1() {
            return sha1;
        }

        public short getComplianceLevel() {
            return complianceLevel;
        }

        public String getId() {
            return id;
        }

        public VersionType getType() {
            return type;
        }

        public URL getUrl() {
            return url;
        }

        public String getTime() {
            return time;
        }

        public String getReleaseTime() {
            return releaseTime;
        }
    }

    private static class VersionTypeDeserializer implements JsonDeserializer<VersionType> {
        @Override
        public VersionType deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            return VersionType.parseString(jsonElement.getAsString());
        }
    }
}
