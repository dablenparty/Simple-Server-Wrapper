package com.hunterltd.ssw.cli.tasks;

import com.hunterltd.ssw.server.MinecraftServer;

import static com.hunterltd.ssw.cli.ServerWrapperCLI.printlnWithTimeAndThread;

public class ServerShutdownTask extends ServerBasedRunnable {
    public ServerShutdownTask(MinecraftServer server) {
        super(server);
    }

    @Override
    public void run() {
        printlnWithTimeAndThread(System.out,"Nobody has joined in a while, closing the server...");
        MinecraftServer server = getServer();
        server.setShouldBeRunning(false);
        server.setShuttingDown(true);
    }
}
