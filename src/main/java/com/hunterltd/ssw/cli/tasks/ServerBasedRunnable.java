package com.hunterltd.ssw.cli.tasks;

import com.hunterltd.ssw.server.MinecraftServer;
import com.hunterltd.ssw.util.concurrency.NamedExecutorService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ServerBasedRunnable implements Runnable {
    private final MinecraftServer minecraftServer;
    private final List<NamedExecutorService> childServices = new ArrayList<>(1);

    protected ServerBasedRunnable(MinecraftServer minecraftServer) {
        this.minecraftServer = minecraftServer;
    }

    public MinecraftServer getMinecraftServer() {
        return minecraftServer;
    }

    protected final void addChildService(NamedExecutorService service) {
        childServices.add(service);
    }

    protected final void removeChildService(NamedExecutorService service) {
        childServices.remove(service);
    }

    protected final NamedExecutorService removeChildService(int index) {
        return childServices.remove(index);
    }

    /**
     * Gets list of child services as an unmodifiable list
     *
     * @return List of services spawned by this task
     */
    public final List<NamedExecutorService> getChildServices() {
        return Collections.unmodifiableList(childServices);
    }
}
