package com.hunterltd.ssw.minecraft;

import java.net.URL;
import java.time.ZonedDateTime;
import java.util.Set;

public class VersionManifest {
    private static class Latest {
        private String release;
        private String snapshot;
    }

    public static class MinecraftVersion {
        private String id;
        private String type;
        private URL url;
        private ZonedDateTime time;
        private ZonedDateTime releaseTime;
    }

    private Latest latest;
    private Set<MinecraftVersion> versions;
}
