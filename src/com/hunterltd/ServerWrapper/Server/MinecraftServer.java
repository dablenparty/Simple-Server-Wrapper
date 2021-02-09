package com.hunterltd.ServerWrapper.Server;

import java.io.File;
import java.io.IOException;

public class MinecraftServer {
    private Process serverProcess;

    public MinecraftServer(String serverPath, int initialHeap, int maxHeap) {
        ProcessBuilder pB = new ProcessBuilder();
        pB.directory(new File("./"));
//        String opSys = System.getProperty("os.name"); // TODO: OS compatibility
        pB.command("cmd.exe", "/c", "echo",  "hello from the server!");
        try {
            serverProcess = pB.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
