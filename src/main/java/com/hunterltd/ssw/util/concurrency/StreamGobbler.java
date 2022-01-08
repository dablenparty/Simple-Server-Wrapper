package com.hunterltd.ssw.util.concurrency;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public record StreamGobbler(InputStream inputStream,
                            Consumer<String> consumer) implements Runnable {

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
