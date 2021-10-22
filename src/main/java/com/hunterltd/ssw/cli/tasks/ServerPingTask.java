package com.hunterltd.ssw.cli.tasks;

import com.hunterltd.ssw.cli.SswServerCli;
import com.hunterltd.ssw.server.MinecraftServer;
import com.hunterltd.ssw.utilities.NamedExecutorService;
import com.hunterltd.ssw.utilities.ThreadUtils;
import com.hunterltd.ssw.utilities.network.ServerListPing;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ServerPingTask extends ServerBasedRunnable {
    private final ServerListPing pinger = new ServerListPing();
    private final NamedExecutorService scheduledShutdownService;
    private ScheduledFuture<?> shutdownServiceFuture = null;

    public ServerPingTask(MinecraftServer minecraftServer) {
        super(minecraftServer);
        pinger.setAddress(new InetSocketAddress(minecraftServer.getPort()));
        String serviceName = "MinecraftServer Shutdown Service";
        ScheduledExecutorService service = Executors.newScheduledThreadPool(1, ThreadUtils.newNamedThreadFactory(serviceName));
        scheduledShutdownService = new NamedExecutorService(serviceName, service);
    }

    @Override
    public void run() {
        MinecraftServer minecraftServer = getMinecraftServer();
        if (minecraftServer.shouldBeRunning() && minecraftServer.isRunning() && !minecraftServer.isShuttingDown()) {
            ServerListPing.StatusResponse response = null;
            try {
                response = pinger.fetchData();
            } catch (IOException e) {
                SswServerCli.printExceptionToOut(e);
            }
            if (response == null) return;
            int onlinePlayers = response.getPlayers().getOnline();
            ScheduledExecutorService executorService = (ScheduledExecutorService) scheduledShutdownService.executorService();
            if (onlinePlayers == 0) {
                if (shutdownServiceFuture == null || shutdownServiceFuture.isDone()) {
                    // shutdown timer begins
                    long delay = minecraftServer.getServerSettings().getShutdownInterval();
                    shutdownServiceFuture = executorService.schedule(() -> minecraftServer.setShouldBeRunning(false), delay, TimeUnit.MINUTES);
                }
            } else if (shutdownServiceFuture != null && executorService != null) {
                boolean didShutdown = shutdownServiceFuture.cancel(false);
                if (!didShutdown && !shutdownServiceFuture.isDone())
                    shutdownServiceFuture.cancel(true);
            }
        }
    }
}
