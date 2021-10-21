package com.hunterltd.ssw.cli.tasks;

import com.hunterltd.ssw.server.MinecraftServer;
import com.hunterltd.ssw.utilities.NamedExecutorService;
import com.hunterltd.ssw.utilities.network.ServerListPing;

import java.net.InetSocketAddress;
import java.util.Optional;

public class ServerPingTask extends ServerBasedRunnable {
    private final ServerListPing pinger = new ServerListPing();
    private volatile NamedExecutorService scheduledShutdownService = null;

    protected ServerPingTask(MinecraftServer minecraftServer) {
        super(minecraftServer);
        pinger.setAddress(new InetSocketAddress(minecraftServer.getPort()));
    }

    @Override
    public void run() {

    }
}
