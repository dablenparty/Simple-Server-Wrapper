package com.hunterltd.ssw.minecraft.tasks;

import com.hunterltd.ssw.cli.SswServerCli;
import com.hunterltd.ssw.minecraft.MinecraftServer;
import com.hunterltd.ssw.util.concurrency.NamedExecutorService;
import com.hunterltd.ssw.util.concurrency.ThreadUtils;
import com.hunterltd.ssw.util.network.ServerListPing;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
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
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor(ThreadUtils.newNamedThreadFactory(serviceName));
        scheduledShutdownService = new NamedExecutorService(serviceName, service);
        addChildService(scheduledShutdownService);
    }

    @Override
    public void run() {
        MinecraftServer minecraftServer = getMinecraftServer();
        if (minecraftServer.shouldBeRunning() && minecraftServer.isRunning() && !minecraftServer.isShuttingDown()) {
            ServerListPing.StatusResponse response;
            int onlinePlayers;
            try {
                response = pinger.fetchData();
                onlinePlayers = response.getPlayers().getOnline();
            } catch (NullPointerException | SocketException ignored) {
                return;
            } catch (IOException e) {
                SswServerCli.printExceptionToOut(e);
                return;
            }
            ScheduledExecutorService executorService = (ScheduledExecutorService) scheduledShutdownService.service();
            if (onlinePlayers == 0) {
                if (minecraftServer.shouldBeRunning() && (shutdownServiceFuture == null || shutdownServiceFuture.isDone())) {
                    // shutdown timer begins
                    long delay = minecraftServer.getServerSettings().getShutdownInterval();
                    shutdownServiceFuture = executorService.schedule(() -> minecraftServer.setShouldBeRunning(false), delay, TimeUnit.MINUTES);
                }
            } else if (shutdownServiceFuture != null && executorService != null && !shutdownServiceFuture.isDone()) {
                boolean didShutdown = shutdownServiceFuture.cancel(false);
                if (!didShutdown && !shutdownServiceFuture.isDone())
                    shutdownServiceFuture.cancel(true);
            }
        }
    }
}
