package com.hunterltd.ssw.minecraft;

import com.google.gson.*;
import com.hunterltd.ssw.util.serial.JsonUtils;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class VersionManifestV2 {
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
