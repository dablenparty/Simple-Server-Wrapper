package com.hunterltd.ssw.cli.tasks;

import com.hunterltd.ssw.cli.ServerWrapperCLI;
import com.hunterltd.ssw.server.MinecraftServer;
import com.hunterltd.ssw.utilities.ServerListPing;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerPingTask extends ServerBasedRunnable {
    private final ServerListPing pinger = new ServerListPing();
    private volatile ScheduledExecutorService shutdownService = null;

    public ServerPingTask(MinecraftServer server) {
        super(server);
        pinger.setAddress(new InetSocketAddress(server.getPort()));
    }

    @Override
    public void run() {
        MinecraftServer server = getServer();
        try {
            if (server.shouldBeRunning() && server.isRunning() && !server.isShuttingDown()) {
                ServerListPing.StatusResponse response = pinger.fetchData();
                if (response.getPlayers().getOnline() == 0 && shutdownService == null) {
                    // shutdown timer begins
                    shutdownService = Executors.newScheduledThreadPool(1, ServerWrapperCLI.newNamedThreadFactory("Server Shutdown Service"));
                    long delay = server.getServerSettings().getShutdownInterval();
                    shutdownService.schedule(new ServerShutdownTask(server), delay, TimeUnit.MINUTES);
                } else {
                    if (response.getPlayers().getOnline() != 0 && shutdownService != null) {
                        ServerWrapperCLI.tryShutdownExecutorService(shutdownService);
                        shutdownService = null;
                    }
                }
            } else {
                if (shutdownService != null) {
                    ServerWrapperCLI.tryShutdownExecutorService(shutdownService);
                    shutdownService = null;
                }
            }
        } catch (IOException | NullPointerException exception) {
//            exception.printStackTrace();
        }
    }

    public ScheduledExecutorService getShutdownService() {
        return shutdownService;
    }
}
