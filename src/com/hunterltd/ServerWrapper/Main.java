package com.hunterltd.ServerWrapper;

import com.hunterltd.ServerWrapper.Server.MinecraftServer;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello from main");
        MinecraftServer server = new MinecraftServer("temp", 4096, 4096);
    }
}
