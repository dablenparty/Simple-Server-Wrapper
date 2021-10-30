package com.hunterltd.ssw.cli.tasks;

import com.hunterltd.ssw.cli.SswServerCli;
import com.hunterltd.ssw.server.MinecraftServer;
import com.hunterltd.ssw.utilities.concurrency.NamedExecutorService;
import com.hunterltd.ssw.utilities.concurrency.ThreadUtils;
import com.hunterltd.ssw.utilities.network.PortListener;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.hunterltd.ssw.utilities.concurrency.ThreadUtils.printfWithTimeAndThread;
import static com.hunterltd.ssw.utilities.concurrency.ThreadUtils.printlnWithTimeAndThread;

/**
 * Handles starting and stopping the server process
 */
public class AliveStateCheckTask extends ServerBasedRunnable {
    private final PortListener portListener;
    private final NamedExecutorService scheduledRestartService;
    private ScheduledFuture<?> restartServiceFuture = null;

    /**
     * Creates a new instance of this class
     *
     * @param minecraftServer Server object to monitor
     */
    public AliveStateCheckTask(MinecraftServer minecraftServer) {
        super(minecraftServer);
        portListener = new PortListener(minecraftServer.getPort());
        String serviceName = "MinecraftServer Restart Service";
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor(ThreadUtils.newNamedThreadFactory(serviceName));
        scheduledRestartService = new NamedExecutorService(serviceName, service);
        addChildService(scheduledRestartService);
    }

    @Override
    public void run() {
        MinecraftServer server = getMinecraftServer();
        if (!server.shouldBeRunning() && server.isRunning() && !server.isShuttingDown()) {
            server.stop();
            tryStartPortListener(server);
            if (restartServiceFuture != null && !restartServiceFuture.isDone()) {
                if (!restartServiceFuture.cancel(false))
                    restartServiceFuture.cancel(true);
                restartServiceFuture = null;
            }
        } else if (server.shouldBeRunning() && !server.isRunning()) {
            if (portListener.isOpen())
                portListener.stop();
            try {
                server.run();
                if (server.getServerSettings().getRestart() && restartServiceFuture == null) {
                    ScheduledExecutorService service = (ScheduledExecutorService) scheduledRestartService.service();
                    restartServiceFuture = service.schedule(new RestartService(server), 1L, TimeUnit.SECONDS);
                }
            } catch (IOException e) {
                // an error occurred starting the process
                e.printStackTrace();
                server.setShouldBeRunning(false);
            }
        }
    }

    /**
     * Tries to start a {@link PortListener} on the port specified in {@code server}.
     *
     * @param server Server to listen for connection to
     */
    private void tryStartPortListener(MinecraftServer server) {
        if (!server.getServerSettings().getShutdown() || server.shouldRestart() || portListener.isOpen()) return;
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

    /**
     * Inner runnable for restarting server after it's interval
     */
    private class RestartService extends ServerBasedRunnable {
        private final long delayInSeconds;
        private long secondsPassed = 0;

        /**
         * Creates a new instance of this class
         *
         * @param minecraftServer Server to restart
         */
        private RestartService(MinecraftServer minecraftServer) {
            super(minecraftServer);
            long restartInterval = minecraftServer.getServerSettings().getRestartInterval();
            delayInSeconds = TimeUnit.HOURS.toSeconds(restartInterval);
        }

        @Override
        public void run() {
            secondsPassed++;
            MinecraftServer server = getMinecraftServer();
            if (!server.isRunning()) return;
            String message = "me is restarting in %d %s";
            boolean sendMessage = false;
            if (secondsPassed == delayInSeconds) {
                server.setShouldRestart(true);
                server.setShouldBeRunning(false);
                return;
            } else if (secondsPassed > delayInSeconds) {
                // waiting to restart
                return;
            }
            long difference = delayInSeconds - secondsPassed;
            // every hour
            if (secondsPassed % 3600 == 0) {
                message = String.format(message, difference / 60 / 60, "hours");
                sendMessage = true;
            } else {
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
