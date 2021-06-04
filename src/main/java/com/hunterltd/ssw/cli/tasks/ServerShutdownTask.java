package com.hunterltd.ssw.cli.tasks;

import com.hunterltd.ssw.server.MinecraftServer;

public class ServerShutdownTask extends ServerBasedRunnable {
    public ServerShutdownTask(MinecraftServer server) {
        super(server);
    }

    @Override
    public void run() {
        System.out.println("Nobody has joined in a while, closing the server...");
        MinecraftServer server = getServer();
        server.setShouldBeRunning(false);
        server.setShuttingDown(true);
    }
}
