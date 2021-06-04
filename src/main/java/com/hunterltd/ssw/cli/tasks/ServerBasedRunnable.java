package com.hunterltd.ssw.cli.tasks;

import com.hunterltd.ssw.server.MinecraftServer;

public abstract class ServerBasedRunnable implements Runnable {
    private final MinecraftServer server;

    public ServerBasedRunnable(MinecraftServer server) {
        this.server = server;
    }

    public MinecraftServer getServer() {
        return server;
    }
}
