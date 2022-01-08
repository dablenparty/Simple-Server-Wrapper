package com.hunterltd.ssw.minecraft;

import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Represents a version of Minecraft as seen in {@code version_manifest_v2.json}
 */
public class MinecraftVersion implements Comparable<MinecraftVersion> {
    private String id;
    private VersionManifestV2.VersionType type;
    private URL url;
    private String time;
    private String releaseTime;
    private String sha1;
    private short complianceLevel;

    /**
     * Finds the {@code MinecraftVersion} whose ID matches the {@code versionString} parameter.
     * <p>
     * Note that because the list of versions is sorted in descending order by release time, older versions will take
     * longer to find.
     *
     * @param versionString Minecraft version string (e.g., {@code 1.18.1})
     * @return {@code MinecraftVersion} instance whose ID matches {@code versionString}, or {@code null} if such an
     * instance could not be found
     */
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
        // sorts in descending order, meaning the most recent releases are at the start (or have lower indices)
        ZonedDateTime thisTime = ZonedDateTime.parse(this.releaseTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        ZonedDateTime otherTime = ZonedDateTime.parse(o.releaseTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return otherTime.compareTo(thisTime);
    }
}
