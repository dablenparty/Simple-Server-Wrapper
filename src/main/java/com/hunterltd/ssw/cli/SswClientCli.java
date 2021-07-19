package com.hunterltd.ssw.cli;

import com.hunterltd.ssw.utilities.MavenUtils;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class SswClientCli {
    private final AsynchronousSocketChannel client;

    public static Namespace parseArgs(String[] args) {
        Properties mavenProperties = MavenUtils.getMavenProperties();
        ArgumentParser parser = ArgumentParsers.newFor("Simple Server Wrapper Client CLI").build()
                .defaultHelp(true)
                .version(String.format("${prog} v%s", mavenProperties.getProperty("version")))
                .description("Client-side commandline interface for connecting to and interacting with either local " +
                        "or remote instances of this programs server-side counterpart.");
        parser.addArgument("-v", "--version").action(Arguments.version());

        parser.addArgument("-p", "--port")
                .action(Arguments.store())
                .setDefault(9609)
                .help("port number to connect on");

        parser.addArgument("-t", "--target")
                .action(Arguments.store())
                .setDefault("127.0.0.1")
                .help("target address, defaults to 127.0.0.1");
        return parser.parseArgsOrFail(args);
    }

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        Namespace namespace = parseArgs(args);
        SswClientCli clientCli = new SswClientCli(namespace.getInt("port"), namespace.getString("target"));
        Scanner systemInScanner = new Scanner(System.in);
        String userInput;
        while (!(userInput = systemInScanner.nextLine()).equals("close")) {
            String response = clientCli.sendMessage(userInput);
            System.out.printf("[Server] %s%n", response);
        }
        clientCli.sendMessage("close");
        clientCli.stop();
    }

    SswClientCli(int port, String target) throws IOException, ExecutionException, InterruptedException {
        client = AsynchronousSocketChannel.open();
        Future<Void> connectionStatus = client.connect(new InetSocketAddress(target, port));
        connectionStatus.get();
    }

    public void stop() throws IOException {
        client.close();
    }

    public String sendMessage(String message) throws ExecutionException, InterruptedException {
        byte[] asBytes = message.getBytes();
        ByteBuffer buffer = ByteBuffer.wrap(asBytes);
        Future<Integer> writeResult = client.write(buffer);

        writeResult.get();
        buffer.flip();
        Future<Integer> readResult = client.read(buffer);
        readResult.get();

        String fromBuffer = new String(buffer.array()).trim();
        buffer.clear();
        return fromBuffer;
    }
}
