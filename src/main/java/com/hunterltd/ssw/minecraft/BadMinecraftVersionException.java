package com.hunterltd.ssw.minecraft;

public class BadMinecraftVersionException extends Exception {
    public BadMinecraftVersionException(String versionString) {
        super(versionString + " is not a valid Minecraft version");
    }
}
