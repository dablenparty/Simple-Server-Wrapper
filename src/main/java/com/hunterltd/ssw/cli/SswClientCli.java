package com.hunterltd.ssw.cli;

import com.hunterltd.ssw.utilities.MavenUtils;
import com.hunterltd.ssw.utilities.StreamGobbler;
import com.hunterltd.ssw.utilities.ThreadUtils;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;

public class SswClientCli {
    private Socket clientSocket;
//    private BufferedReader in;
    private PrintWriter out;
    private ExecutorService readService;

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

    public static void main(String[] args) throws IOException {
        Namespace namespace = parseArgs(args);
        SswClientCli clientCli = new SswClientCli();
        int port = namespace.getInt("port");
        String target = namespace.getString("target");

        try {
            clientCli.connect(target, port);
            System.out.printf("Connected to %s:%d%n", target, port);
        } catch (ConnectException connectException) {
            System.err.println(connectException.getLocalizedMessage());
            System.err.println("It is most likely that the SSW server is not running");
            return;
        }
        String message;
        Scanner userInputScanner = new Scanner(System.in);
        while (!(message = userInputScanner.nextLine()).equals("close")) {
            clientCli.sendToServer(message);
            if (message.equals("logout")) break;
        }
        clientCli.sendToServer(message);
        userInputScanner.close();
        clientCli.closeConnection();
    }

    public void connect(String targetIp, int port) throws IOException {
        clientSocket = new Socket(targetIp, port);
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        InputStream socketInputStream = clientSocket.getInputStream();
//        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        readService = StreamGobbler.execute(socketInputStream, System.out::println, "Socket Read Service");
    }

    public void closeConnection() throws IOException {
        ThreadUtils.tryShutdownExecutorService(readService);
//        in.close();
        out.close();
        clientSocket.close();
    }

    public void sendToServer(String message) {
        out.println(message);
        // previously returned the response. that will now be handled by a separate thread
//        return in.readLine();
    }

}
