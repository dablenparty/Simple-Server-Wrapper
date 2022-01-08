package com.hunterltd.ssw.util.concurrency;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * An ExecutorService with a {@code name} string
 */
public final class NamedExecutorService {
    private final String name;
    private final ExecutorService executorService;
    private final List<NamedExecutorService> childServices = new ArrayList<>(1);

    public NamedExecutorService(String name, ExecutorService executorService) {
        this.name = name;
        this.executorService = executorService;
    }

    public String name() {
        return name;
    }

    public ExecutorService service() {
        return executorService;
    }

    /**
     * Adds a child service to an internal list of child services. Most often, this is used to track any services
     * spawned by this service
     *
     * @param namedExecutorService child service
     */
    public void addChildService(NamedExecutorService namedExecutorService) {
        childServices.add(namedExecutorService);
    }

    /**
     * Recursively shuts down all child services
     */
    public void shutdownAllChildServices() {
        childServices.forEach(namedService -> {
            if (namedService.hasChildServices())
                namedService.shutdownAllChildServices();
            ThreadUtils.tryShutdownNamedExecutorService(namedService);
        });
    }

    /**
     * Shuts down this service
     */
    public void shutdown() {
        shutdownAllChildServices();
        ThreadUtils.tryShutdownNamedExecutorService(this);
    }

    /**
     * @return {@code true} if this service has child services. {@code false} otherwise
     */
    public boolean hasChildServices() {
        return childServices.size() > 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (NamedExecutorService) obj;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.executorService, that.executorService);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, executorService);
    }

    @Override
    public String toString() {
        return "NamedExecutorService[" +
                "name=" + name + ", " +
                "executorService=" + executorService + ']';
    }
}
