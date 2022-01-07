package com.hunterltd.ssw.minecraft;

import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MinecraftVersion implements Comparable<MinecraftVersion> {
    private String id;
    private VersionManifestV2.VersionType type;
    private URL url;
    private String time;
    private String releaseTime;
    private String sha1;
    private short complianceLevel;

    public static MinecraftVersion of(String versionString) {
        List<MinecraftVersion> versions = VersionManifestV2.INSTANCE.getVersions();
        for (MinecraftVersion version : versions) {
            if (versionString.equals(version.id))
                return version;
        }
        return null;
    }

    public String getSha1() {
        return sha1;
    }

    public short getComplianceLevel() {
        return complianceLevel;
    }

    public String getId() {
        return id;
    }

    public VersionManifestV2.VersionType getType() {
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

    @Override
    public String toString() {
        return String.format("%s %s (comp %d)", type, id, complianceLevel);
    }

    @Override
    public int compareTo(MinecraftVersion o) {
        ZonedDateTime thisTime = ZonedDateTime.parse(this.releaseTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        ZonedDateTime otherTime = ZonedDateTime.parse(o.releaseTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return otherTime.compareTo(thisTime);
    }
}
