package com.hunterltd.ssw.cli;

import com.hunterltd.ssw.utilities.MavenUtils;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class SswServerCli {
    private final int port;
    private final File serverFile;
    private final AsynchronousServerSocketChannel serverSocketChannel;
    private final Future<AsynchronousSocketChannel> acceptConnection;

    SswServerCli(int port, File serverFile) throws IOException {
        this.port = port;
        this.serverFile = serverFile;
        serverSocketChannel = AsynchronousServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress("127.0.0.1", this.port));
        acceptConnection = serverSocketChannel.accept();
    }

    public void run() throws ExecutionException, InterruptedException, IOException {
        AsynchronousSocketChannel clientSocketChannel = acceptConnection.get();
        if (clientSocketChannel != null) {
            while (clientSocketChannel.isOpen()) {
                ByteBuffer buffer = ByteBuffer.allocate(32);
                Future<Integer> readFromClient = clientSocketChannel.read(buffer);
                readFromClient.get();
                buffer.flip();
                String message = new String(buffer.array()).trim();
                if (message.equals("close")) break;
                Future<Integer> writeToClient = clientSocketChannel.write(buffer);
                writeToClient.get();
                buffer.clear();
            }
            clientSocketChannel.close();
            serverSocketChannel.close();
        }
    }

    public static Namespace parseArgs(String[] args) {
        Properties mavenProperties = MavenUtils.getMavenProperties();
        ArgumentParser parser = ArgumentParsers.newFor("Simple Server Wrapper Server CLI").build()
                .defaultHelp(true)
                .version(String.format("${prog} v%s", mavenProperties.getProperty("version")))
                .description("Server-side commandline interface for running and interacting with a Minecraft server." +
                        " This also allows for remote or local connections from the client-side counterpart to this program");
        parser.addArgument("-v", "--version")
                .action(Arguments.version());

        parser.addArgument("server")
                .action(Arguments.store())
                .metavar("SERVER_PATH")
                .help("path to server jar file");

        parser.addArgument("-m", "--modpack")
                .action(Arguments.store())
                .required(false)
                .help("CurseForge modpack zip file to install to server");

        parser.addArgument("-p", "--port")
                .action(Arguments.store())
                .setDefault(9609)
                .help("port number to connect on");

        return parser.parseArgsOrFail(args);
    }

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        Namespace namespace = parseArgs(args);
        File server = new File(namespace.getString("server")).getAbsoluteFile();
        int port = namespace.getInt("port");
        SswServerCli serverCli = new SswServerCli(port, server);
        serverCli.run();
//        serverCli.start();
//        serverCli.stop();
    }
}
