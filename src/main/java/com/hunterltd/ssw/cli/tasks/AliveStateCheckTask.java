package com.hunterltd.ssw.cli.tasks;

import com.hunterltd.ssw.cli.SswServerCli;
import com.hunterltd.ssw.server.MinecraftServer;
import com.hunterltd.ssw.utilities.concurrency.NamedExecutorService;
import com.hunterltd.ssw.utilities.concurrency.ThreadUtils;
import com.hunterltd.ssw.utilities.network.PortListener;

import java.io.IOException;
import java.util.concurrent.*;

import static com.hunterltd.ssw.utilities.concurrency.ThreadUtils.printfWithTimeAndThread;
import static com.hunterltd.ssw.utilities.concurrency.ThreadUtils.printlnWithTimeAndThread;

public class AliveStateCheckTask extends ServerBasedRunnable {
    private final PortListener portListener;
    private final NamedExecutorService scheduledRestartService;
    private ScheduledFuture<?> restartServiceFuture = null;

    public AliveStateCheckTask(MinecraftServer minecraftServer) {
        super(minecraftServer);
        portListener = new PortListener(minecraftServer.getPort());
        String serviceName = "MinecraftServer Restart Service";
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor(ThreadUtils.newNamedThreadFactory(serviceName));
        scheduledRestartService = new NamedExecutorService(serviceName, service);
    }

    public NamedExecutorService getScheduledRestartService() {
        return scheduledRestartService;
    }

    @Override
    public void run() {
        MinecraftServer server = getMinecraftServer();
        if (!server.shouldBeRunning() && server.isRunning() && !server.isShuttingDown()) {
            server.stop();
            startPortListener(server);
            if (restartServiceFuture != null && !restartServiceFuture.isDone()) {
                if (!restartServiceFuture.cancel(false))
                    restartServiceFuture.cancel(true);

            }
        } else if (server.shouldBeRunning() && !server.isRunning()) {
            if (portListener.isOpen())
                portListener.stop();
            try {
                server.run();
                if (server.getServerSettings().getRestart()) {
                    long delay = server.getServerSettings().getRestartInterval();
                    ScheduledExecutorService service = (ScheduledExecutorService) scheduledRestartService.service();
                    restartServiceFuture = service.schedule(new RestartService(server, delay), 1L, TimeUnit.SECONDS);
                }
            } catch (IOException e) {
                // an error occurred starting the process
                e.printStackTrace();
                server.setShouldBeRunning(false);
            }
        }
    }

    private void startPortListener(MinecraftServer server) {
        if (!server.getServerSettings().getShutdown() || portListener.isOpen()) return;
        printfWithTimeAndThread(System.out, "Attempting to open port listener on port %d", server.getPort());
        try {
            portListener.start();
            portListener.on("connection", objects -> server.setShouldBeRunning(true));
            portListener.on("error", errors -> {
                for (Object e : errors)
                    ((Exception) e).printStackTrace();
            });
            portListener.on("stop", objects -> printlnWithTimeAndThread(System.out, "Port listener closed"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class RestartService extends ServerBasedRunnable {
        private long secondsPassed = 0;
        private final long delayInSeconds;

        private RestartService(MinecraftServer minecraftServer, long delay) {
            super(minecraftServer);
            delayInSeconds = TimeUnit.HOURS.toSeconds(delay);
        }

        @Override
        public void run() {
            secondsPassed++;
            MinecraftServer server = getMinecraftServer();
            String message = "me is restarting in %d %s";
            boolean sendMessage = false;
            // every hour
            if (secondsPassed == delayInSeconds) {
                server.setShouldRestart(true);
                server.setShouldBeRunning(false);
                return;
            }
            if (secondsPassed % 3600 == 0) {
                message = String.format(message, secondsPassed / 60 / 60, "hours");
                sendMessage = true;
            }
            else {
                long difference = delayInSeconds - secondsPassed;
                switch ((int) difference) {
                    case 1800, 900, 600, 300 -> {
                        message = String.format(message, difference / 60, "minutes");
                        sendMessage = true;
                    }
                    case 10, 9, 8, 7, 6, 5, 4, 3, 2, 1 -> {
                        message = String.format(message, difference, "seconds");
                        sendMessage = true;
                    }
                    default -> {
                    }
                }
            }
            ScheduledExecutorService service = (ScheduledExecutorService) scheduledRestartService.service();
            restartServiceFuture = service.schedule(this, 1L, TimeUnit.SECONDS);
            if (!sendMessage) return;
            try {
                server.sendCommand(message);
            } catch (IOException e) {
                SswServerCli.printExceptionToOut(e);
            }
        }
    }
}
