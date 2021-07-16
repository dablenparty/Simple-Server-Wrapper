package com.hunterltd.ssw.cli.tasks;

import com.hunterltd.ssw.server.ConnectionListener;
import com.hunterltd.ssw.server.MinecraftServer;

import java.io.IOException;

import static com.hunterltd.ssw.cli.ServerWrapperCLI.printlnWithTimeAndThread;

public class AliveStateCheckTask extends ServerBasedRunnable {
    public AliveStateCheckTask(MinecraftServer server) {
        super(server);
    }

    @Override
    public void run() {
        MinecraftServer server = getServer();
        if (!server.shouldBeRunning() && server.isRunning() && !server.isShuttingDown()) {
            Process serverProcess = server.getServerProcess();
            try {
                server.stop();
                if (server.getServerSettings().getShutdown()) {
                    ConnectionListener.start(server.getPort());
                    printlnWithTimeAndThread(System.out,"Listener opened");
                }
            } catch (IOException exception) {
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
                printlnWithTimeAndThread(System.out,"Listener closed, starting server...");
            }
        }
    }
}
