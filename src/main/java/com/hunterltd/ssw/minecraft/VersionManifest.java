package com.hunterltd.ssw.minecraft;

import com.google.gson.*;
import com.hunterltd.ssw.util.serial.JsonUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public class VersionManifest {
    private Latest latest;
    private Set<MinecraftVersion> versions;

    public static VersionManifest parseManifestFile(Path manifestPath) throws IOException {
        GsonBuilder builder = JsonUtils.GSON_BUILDER;
        builder.registerTypeAdapter(VersionType.class, new VersionTypeDeserializer());
        Gson gson = builder.create();
        InputStream manifestStream = Files.newInputStream(manifestPath);
        return gson.fromJson(new InputStreamReader(manifestStream), VersionManifest.class);
    }

    public Set<MinecraftVersion> getVersions() {
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
