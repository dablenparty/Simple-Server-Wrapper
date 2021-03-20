package com.hunterltd.ssw.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class ConnectionListener {
    private static AsynchronousServerSocketChannel listener;
    private static boolean connectionAttempted = false;

    public static void start(int port) throws IOException {
        connectionAttempted = false;
        listener = AsynchronousServerSocketChannel.open().bind(new InetSocketAddress(port));

        listener.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
            @Override
            public void completed(AsynchronousSocketChannel result, Void attachment) {
                listener.accept(null, this);
                System.out.println("Someone attempted to connect to the server. Launching it...");
                connectionAttempted = true;
                if (result.isOpen()) {
                    try {
                        result.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void failed(Throwable exc, Void attachment) {

                if (exc instanceof AsynchronousCloseException) {
                    System.out.println("Listener successfully closed");
                } else {
                    System.err.println("An error occurred closing the listener\n|\nV"); // shows an arrow pointing down
                    exc.printStackTrace();
                }

            }
        });
    }

    public static void stop() {
        try {
            listener.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isConnectionAttempted() {
        return connectionAttempted;
    }
}
