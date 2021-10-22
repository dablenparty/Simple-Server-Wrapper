package com.hunterltd.ssw.cli.tasks;

import com.hunterltd.ssw.server.MinecraftServer;
import com.hunterltd.ssw.utilities.NamedExecutorService;
import com.hunterltd.ssw.utilities.network.ServerListPing;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ServerPingTask extends ServerBasedRunnable {
    private final ServerListPing pinger = new ServerListPing();
    private volatile NamedExecutorService scheduledShutdownService = null;

    protected ServerPingTask(MinecraftServer minecraftServer) {
        super(minecraftServer);
        pinger.setAddress(new InetSocketAddress(minecraftServer.getPort()));
        String serviceName = "MinecraftServer Shutdown Service";
        ScheduledExecutorService service = Executors.newScheduledThreadPool(1, ThreadUtils.newNamedThreadFactory(serviceName));
        scheduledShutdownService = new NamedExecutorService(serviceName, service);
    }

    @Override
    public void run() {

    }
}
