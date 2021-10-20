package com.hunterltd.ssw.cli.tasks;

import com.hunterltd.ssw.server.MinecraftServer;

public abstract class ServerBasedRunnable implements Runnable {
    private final MinecraftServer minecraftServer;

    protected ServerBasedRunnable(MinecraftServer minecraftServer) {
        this.minecraftServer = minecraftServer;
    }

    public MinecraftServer getMinecraftServer() {
        return minecraftServer;
    }
}
