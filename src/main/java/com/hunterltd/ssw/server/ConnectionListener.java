package com.hunterltd.ssw.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class ConnectionListener {
    public static void start(int port) throws IOException {
        final AsynchronousServerSocketChannel listener =
                AsynchronousServerSocketChannel.open().bind(new InetSocketAddress(port));

        listener.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
            @Override
            public void completed(AsynchronousSocketChannel result, Void attachment) {
                listener.accept(null, this);
                System.out.println("Connection accepted");
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
                System.err.println("uhhh something went bad\n");
                exc.printStackTrace();
            }
        });
    }
}
