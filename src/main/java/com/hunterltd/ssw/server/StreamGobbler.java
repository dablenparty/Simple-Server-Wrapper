package com.hunterltd.ssw.server;

import com.hunterltd.ssw.cli.ServerWrapperCLI;
import com.hunterltd.ssw.gui.dialogs.InternalErrorDialog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public record StreamGobbler(InputStream inputStream,
                            Consumer<String> consumer) implements Runnable {

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            reader.lines().forEach(consumer);
        } catch (IOException e) {
            e.printStackTrace();
            new InternalErrorDialog(e);
        }
    }

    public static ExecutorService execute(InputStream inputStream, Consumer<String> consumer, String threadName) {
        StreamGobbler gobbler = new StreamGobbler(inputStream, consumer);
        ExecutorService service = Executors.newSingleThreadExecutor(ServerWrapperCLI.newNamedThreadFactory(threadName));
        service.submit(gobbler);
        return service;
    }
}
