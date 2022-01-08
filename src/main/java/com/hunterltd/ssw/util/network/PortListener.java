package com.hunterltd.ssw.util.network;

import com.hunterltd.ssw.util.events.EventEmitter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class PortListener extends EventEmitter {
    private final int port;
    private AsynchronousServerSocketChannel socketChannel;

    public PortListener(int port) {
        this.port = port;
    }

    /**
     * Starts this listener on the port specified by the {@code port} field
     *
     * @throws IOException if an I/O error occurs opening the asynchronous channel
     */
    public void start() throws IOException {
        socketChannel = AsynchronousServerSocketChannel.open().bind(new InetSocketAddress(port));
        socketChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
            @Override
            public void completed(AsynchronousSocketChannel result, Void attachment) {
                socketChannel.accept(null, this);
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
        });
    }

    /**
     * @return {@code true} if this socket channel is not null and is open. {@code false} otherwise
     */
    public boolean isOpen() {
        return socketChannel != null && socketChannel.isOpen();
    }

    /**
     * Stops this listener
     */
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
