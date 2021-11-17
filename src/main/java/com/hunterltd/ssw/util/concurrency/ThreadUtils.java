package com.hunterltd.ssw.util.concurrency;

import org.glassfish.jersey.internal.guava.ThreadFactoryBuilder;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class ThreadUtils {
    private static final SimpleDateFormat SIMPLE_TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

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
}
