package com.hunterltd.ssw.cli.commands;

import com.hunterltd.ssw.server.MinecraftServer;

public interface SswCliCommand {
    void runCommand(MinecraftServer server);
}
