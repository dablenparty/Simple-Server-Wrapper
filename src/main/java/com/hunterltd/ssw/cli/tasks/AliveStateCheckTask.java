package com.hunterltd.ssw.cli.tasks;

import com.hunterltd.ssw.server.MinecraftServer;
import com.hunterltd.ssw.utilities.ThreadUtils;
import com.hunterltd.ssw.utilities.network.PortListener;

import java.io.IOException;

public class AliveStateCheckTask extends ServerBasedRunnable {
    private final PortListener portListener;

    public AliveStateCheckTask(MinecraftServer minecraftServer) {
        super(minecraftServer);
        portListener = new PortListener(minecraftServer.getPort());
    }

    @Override
    public void run() {
        MinecraftServer server = getMinecraftServer();
        if (!server.shouldBeRunning() && server.isRunning() && !server.isShuttingDown()) {
            server.stop();
            startPortListener(server);
        } else if (server.shouldBeRunning() && !server.isRunning()) {
            if (portListener.isOpen())
                portListener.stop();
            try {
                server.run();
            } catch (IOException e) {
                // an error occurred starting the process
                e.printStackTrace();
                server.setShouldBeRunning(false);
            }
        }
    }

    private void startPortListener(MinecraftServer server) {
        if (!server.getServerSettings().getShutdown() || portListener.isOpen()) return;
        System.out.println(ThreadUtils.threadStampString(String.format("Attempting to open port listener on port %d", server.getPort())));
        try {
            portListener.start();
            portListener.on("connection", objects -> server.setShouldBeRunning(true));
//            portListener.on("close", objects -> portListener.stop());
            portListener.on("error", errors -> {
                for (Object e : errors)
                    ((Exception) e).printStackTrace();
            });
            portListener.on("stop", objects -> System.out.println(ThreadUtils.threadStampString("Port listener closed")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
