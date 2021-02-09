package com.hunterltd.ServerWrapper.Server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class MinecraftServer {
    private final Process serverProcess;

    public MinecraftServer(String serverFolder, String serverFilename, int initialHeap, int maxHeap) throws IOException {
        ProcessBuilder pB = new ProcessBuilder();
        pB.directory(new File(serverFolder));

        pB.command("java",
                String.format("-Xmx%dM", initialHeap),
                String.format("-Xms%dM", maxHeap),
                "-jar",
                Paths.get(serverFolder, serverFilename).toString(),
                "nogui");

        serverProcess = pB.start();
    }

    public Process getServerProcess() {
        return serverProcess;
    }
}
