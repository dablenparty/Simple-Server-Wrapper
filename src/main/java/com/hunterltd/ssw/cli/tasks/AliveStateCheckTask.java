package com.hunterltd.ssw.cli.tasks;

import com.hunterltd.ssw.server.MinecraftServer;

import java.io.IOException;

public class AliveStateCheckTask extends ServerBasedRunnable {
    protected AliveStateCheckTask(MinecraftServer minecraftServer) {
        super(minecraftServer);
    }

    @Override
    public void run() {
        MinecraftServer server = getMinecraftServer();
        if (!server.shouldBeRunning() && server.isRunning() && !server.isShuttingDown()) {
            try {
                server.stop();
                // this is where the PortListener would start, and this thread would be its owner rather than main
            } catch (IOException exception) {
                // happens if one of the streams on the server doesn't close properly
                System.err.println(exception.getLocalizedMessage());
            } finally {
                server.setShuttingDown(false);
            }
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
