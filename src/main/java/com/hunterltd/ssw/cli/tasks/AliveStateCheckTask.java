package com.hunterltd.ssw.cli.tasks;

import com.hunterltd.ssw.server.MinecraftServer;

import java.io.IOException;

public class AliveStateCheckTask extends ServerBasedRunnable {
    public AliveStateCheckTask(MinecraftServer minecraftServer) {
        super(minecraftServer);
    }

    @Override
    public void run() {
        MinecraftServer server = getMinecraftServer();
        if (!server.shouldBeRunning() && server.isRunning() && !server.isShuttingDown()) {
            server.stop();
            // this is where the PortListener would start, and this thread would be its owner rather than main
        } else if (server.shouldBeRunning() && !server.isRunning()) {
            try {
                server.run();
            } catch (IOException e) {
                // an error occurred starting the process
                System.err.println(e.getLocalizedMessage());
                server.setShouldBeRunning(false);
            }
        }
    }
}
