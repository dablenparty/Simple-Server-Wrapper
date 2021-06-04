package com.hunterltd.ssw.cli.tasks;

import com.hunterltd.ssw.server.MinecraftServer;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class AliveStateCheckTask extends ServerBasedRunnable {
    public AliveStateCheckTask(MinecraftServer server) {
        super(server);
    }

    @Override
    public void run() {
        MinecraftServer server = getServer();
        if (server.getServerProcess() != null && !server.shouldBeRunning() && server.isRunning()) {
            Process serverProcess = server.getServerProcess();
            try {
                server.stop();
                serverProcess.waitFor(5L, TimeUnit.SECONDS);
                // closes server streams
                serverProcess.getOutputStream().close();
                serverProcess.getInputStream().close();
                serverProcess.getErrorStream().close();
            } catch (IOException | InterruptedException exception) {
                exception.printStackTrace();
                serverProcess.destroy();
            } finally {
                server.setShuttingDown(false);
            }
        }
    }
}
