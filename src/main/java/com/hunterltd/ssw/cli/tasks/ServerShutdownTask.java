package com.hunterltd.ssw.cli.tasks;

import com.hunterltd.ssw.server.MinecraftServer;

public class ServerShutdownTask implements Runnable {
    private final MinecraftServer server;

    public ServerShutdownTask(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void run() {
        System.out.println("Nobody has joined in a while, closing the server...");
        server.setShouldBeRunning(false);
        server.setShuttingDown(true);
    }
}
