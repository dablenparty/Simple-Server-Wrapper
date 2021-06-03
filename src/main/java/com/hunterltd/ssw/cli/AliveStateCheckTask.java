package com.hunterltd.ssw.cli;

import com.hunterltd.ssw.server.MinecraftServer;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class AliveStateCheckTask implements Runnable {
    private final MinecraftServer server;

    public AliveStateCheckTask(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void run() {
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
