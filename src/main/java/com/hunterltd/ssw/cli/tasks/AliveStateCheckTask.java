package com.hunterltd.ssw.cli.tasks;

import com.hunterltd.ssw.server.ConnectionListener;
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
        if (!server.shouldBeRunning() && server.isRunning()) {
            Process serverProcess = server.getServerProcess();
            try {
                server.stop();
                serverProcess.waitFor(5L, TimeUnit.SECONDS);
                // closes server streams
                serverProcess.getOutputStream().close();
                serverProcess.getInputStream().close();
                serverProcess.getErrorStream().close();
                if (server.getServerSettings().getShutdown()) {
                    ConnectionListener.start(server.getPort());
                    System.out.println("Listener opened");
                }
            } catch (IOException | InterruptedException exception) {
                exception.printStackTrace();
                serverProcess.destroy();
            } finally {
                server.setShuttingDown(false);
            }
        } else if (server.getServerSettings().getShutdown()
                && ConnectionListener.isConnectionAttempted()
                && !server.isRunning()) {
            try {
                server.run();
                server.setShouldBeRunning(true);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                ConnectionListener.stop();
                System.out.println("Listener closed, starting server...");
            }
        }
    }
}
