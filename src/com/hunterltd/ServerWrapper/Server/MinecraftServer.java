package com.hunterltd.ServerWrapper.Server;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

public class MinecraftServer {
    private Process serverProcess;
    private final ProcessBuilder pB;
    private boolean running = false;

    public MinecraftServer(String serverFolder, String serverFilename, int initialHeap, int maxHeap) {
        pB = new ProcessBuilder();
        pB.directory(new File(serverFolder));

        pB.command("java",
                String.format("-Xmx%dM", initialHeap),
                String.format("-Xms%dM", maxHeap),
                "-jar",
                Paths.get(serverFolder, serverFilename).toString(),
                "nogui");
    }

    public Process getServerProcess() {
        return serverProcess;
    }

    public void sendCommand(String cmd) throws IOException {
        if (!cmd.endsWith("\n")) {
            cmd += "\n";
        }
        OutputStream out = serverProcess.getOutputStream();
        out.write(cmd.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    public MinecraftServer run() throws IOException {
        serverProcess = pB.start();
        return this;
    }

    public void stop() throws IOException {
        sendCommand("stop");
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }
}
