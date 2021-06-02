package com.hunterltd.ssw.server;

import com.hunterltd.ssw.gui.dialogs.InternalErrorDialog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
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
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            reader.lines().forEach(consumer);
        } catch (IOException e) {
            e.printStackTrace();
            new InternalErrorDialog(e);
        }
    }

    public static ExecutorService execute(InputStream inputStream, Consumer<String> consumer) {
        StreamGobbler gobbler = new StreamGobbler(inputStream, consumer);
        ExecutorService service = Executors.newSingleThreadExecutor();
        service.submit(gobbler);
        return service;
    }
}
