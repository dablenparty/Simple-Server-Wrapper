package com.hunterltd.ServerWrapper.Server;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class StreamGobbler implements Runnable {
    private final InputStream inputStream;
    private final Consumer<String> consumer;

    public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
        this.inputStream = inputStream;
        this.consumer = consumer;
    }

    @Override
    public void run() {
        new BufferedReader(new InputStreamReader(inputStream)).lines().forEach(consumer);
    }

    public static void execute(InputStream inputStream, Consumer<String> consumer) {
        StreamGobbler gobbler = new StreamGobbler(inputStream, consumer);
        Executors.newSingleThreadExecutor().submit(gobbler);
    }
}
