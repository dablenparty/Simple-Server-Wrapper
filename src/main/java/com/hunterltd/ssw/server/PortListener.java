package com.hunterltd.ssw.server;

import com.dablenparty.jsevents.EventEmitter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class PortListener extends EventEmitter {
    private final int port;
    private AsynchronousServerSocketChannel socketChannel;
    private final CompletionHandler<AsynchronousSocketChannel, Void> completionHandler = new CompletionHandler<>() {
        @Override
        public void completed(AsynchronousSocketChannel result, Void attachment) {
            socketChannel.accept(null, null);
            emit("connection");
            if (result.isOpen()) {
                try {
                    result.close();
                } catch (IOException e) {
                    emit("error", e);
                }
            }
        }

        @Override
        public void failed(Throwable exc, Void attachment) {
            if (exc instanceof AsynchronousCloseException) emit("close");
            else emit("error", exc);
        }
    };

    public PortListener(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        socketChannel = AsynchronousServerSocketChannel.open().bind(new InetSocketAddress(port));
        socketChannel.accept(null, completionHandler);
    }

    public void stop() {
        try {
            socketChannel.close();
        } catch (IOException e) {
            emit("error", e);
        } finally {
            emit("stop");
        }
    }
}
