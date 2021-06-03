package com.hunterltd.ssw.cli;

import com.hunterltd.ssw.server.MinecraftServer;
import com.hunterltd.ssw.utilities.ServerListPing;

import java.io.IOException;
import java.net.InetSocketAddress;

public class ServerPingTask implements Runnable {
    private final MinecraftServer server;
    private final ServerListPing pinger = new ServerListPing();

    public ServerPingTask(MinecraftServer server) {
        this.server = server;
        pinger.setAddress(new InetSocketAddress(server.getPort()));
    }

    @Override
    public void run() {
        if (!server.isRunning() || server.isShuttingDown()) return;

        try {
            ServerListPing.StatusResponse response = pinger.fetchData();
            if (server.isRunning() && !server.isShuttingDown()) {
                if (response.getPlayers().getOnline() == 0) {
                    // shutdown timer begins
                }
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
