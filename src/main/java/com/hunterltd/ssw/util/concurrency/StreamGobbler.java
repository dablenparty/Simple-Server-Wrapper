package com.hunterltd.ssw.util.concurrency;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Continuously reads an {@link InputStream} while the lazily-populated {@code Stream} returned by the
 * {@link BufferedReader#lines()} method still has elements and executes a {@link Consumer} on each line
 */
public record StreamGobbler(InputStream inputStream,
                            Consumer<String> consumer) implements Runnable {

    /**
     * Submits a new {@code StreamGobbler} to an {@link ExecutorService}, names it, and returns it
     *
     * @param inputStream the {@code InputStream} to read
     * @param consumer the {@code Consumer} to execute on each line
     * @param threadName the name of the newly spawned thread
     * @return {@link NamedExecutorService} containing the new {@code ExecutorService}
     */
    public static NamedExecutorService execute(InputStream inputStream, Consumer<String> consumer, String threadName) {
        StreamGobbler gobbler = new StreamGobbler(inputStream, consumer);
        ExecutorService service = Executors.newSingleThreadExecutor(ThreadUtils.newNamedThreadFactory(threadName));
        service.submit(gobbler);
        return new NamedExecutorService(threadName, service);
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            reader.lines().forEach(consumer);
        } catch (IOException e) {
            System.err.println(e.getLocalizedMessage());
        }
    }
}
