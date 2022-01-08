package com.hunterltd.ssw.util.concurrency;

import javafx.application.Platform;
import org.glassfish.jersey.internal.guava.ThreadFactoryBuilder;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class ThreadUtils {
    private static final SimpleDateFormat SIMPLE_TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    /**
     * Creates a new {@link ThreadFactory} with the supplied name
     *
     * @param threadName name of the thread
     * @return {@link ThreadFactory}
     */
    public static ThreadFactory newNamedThreadFactory(String threadName) {
        return new ThreadFactoryBuilder().setNameFormat(threadName).build();
    }

    /**
     * Tries to properly shut down an {@link ExecutorService} and forces shutdown if it does not comply
     *
     * @param namedService Service to shutdown
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void tryShutdownNamedExecutorService(NamedExecutorService namedService) {
        String name = namedService.name();
        ExecutorService service = namedService.service();
        printfWithTimeAndThread(System.out, "Shutting down '%s' service...", name);
        service.shutdown();
        try {
            service.awaitTermination(5L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            printlnWithTimeAndThread(System.err, e.getLocalizedMessage());
        } finally {
            if (!service.isTerminated())
                printfWithTimeAndThread(System.err, "'%s' service didn't terminate, forcing shutdown", name);
            service.shutdownNow();
            printfWithTimeAndThread(System.out, "'%s' service closed", name);
        }
    }

    /**
     * Prints a message to {@code stream} with a timestamp and the current thread name and formats the output using
     * {@link String#format(String, Object...)}
     *
     * @param stream Stream to print to
     * @param string String pattern to format
     * @param args   Args to format into {@code string}
     */
    public static void printfWithTimeAndThread(PrintStream stream, String string, Object... args) {
        printlnWithTimeAndThread(stream, String.format(string, args));
    }

    /**
     * Prints a message to {@code stream} with a timestamp and the current thread name
     *
     * @param stream Stream to print to
     * @param string Message
     */
    public static void printlnWithTimeAndThread(PrintStream stream, String string) {
        stream.println(threadStampString(string));
    }

    /**
     * Adds a timestamp and current thread name to a string
     *
     * @param string String to stamp
     * @return Stamped string
     */
    public static String threadStampString(String string) {
        return String.format("[%s] [ssw/%s]: %s", SIMPLE_TIME_FORMAT.format(System.currentTimeMillis()), Thread.currentThread().getName(), string);
    }

    /**
     * Executes a runnable on the JavaFX FX thread.
     * <p>
     * If this method is called from the FX thread, the runnable is executed immediately. Otherwise, it is passed to
     * {@link Platform#runLater(Runnable)}
     *
     * @param runnable Runnable to execute
     */
    public static void runOnFxThread(Runnable runnable) {
        if (Platform.isFxApplicationThread())
            runnable.run();
        else
            Platform.runLater(runnable);
    }
}
