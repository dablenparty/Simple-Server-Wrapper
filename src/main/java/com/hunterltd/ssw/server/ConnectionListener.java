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

    /**
     * Listens for a connection on a port and sets a boolean flag on the first accepted connection
     * @param port Port to listen on
     * @throws IOException if an I/O error occurs binding the port
     */
    public static void start(int port) throws IOException {
        connectionAttempted = false;
        listener = AsynchronousServerSocketChannel.open().bind(new InetSocketAddress(port));

        listener.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
            @Override
            public void completed(AsynchronousSocketChannel result, Void attachment) {
                listener.accept(null, this);
                System.out.println("Connection accepted");
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

    /**
     * Stops the listener
     */
    public static void stop() {
        try {
            listener.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return Boolean on whether a connection was accepted by the listener
     */
    public static boolean isConnectionAttempted() {
        return connectionAttempted;
    }
}
