package com.hunterltd.ssw.cli;

import com.hunterltd.ssw.utilities.MavenUtils;
import com.hunterltd.ssw.utilities.ThreadUtils;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SswClientCli {
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private ExecutorService readService;
    private boolean cancel = false;

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
            return;
        }
        String message;
        Scanner userInputScanner = new Scanner(System.in);
        while (!(message = userInputScanner.nextLine()).equals("close"))
            clientCli.sendToServer(message);
        clientCli.sendToServer(message);
        userInputScanner.close();
        clientCli.closeConnection();
    }

    public void connect(String targetIp, int port) throws IOException {
        clientSocket = new Socket(targetIp, port);
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        // this is awful, please change this at some point to use a custom Runnable
        readService = Executors.newSingleThreadExecutor();
        readService.submit(() -> {
            String line = "\n";
            while (!cancel && line != null) {
                try {
                    line = in.readLine();
                    System.out.println(line);
                } catch (IOException e) {
                    System.err.println(e.getLocalizedMessage());
                    break;
                }
            }
        });
    }

    public void closeConnection() throws IOException {
        cancel = true;
        ThreadUtils.tryShutdownExecutorService(readService);
        in.close();
        out.close();
        clientSocket.close();
    }

    public void sendToServer(String message) {
        out.println(message);
        // previously returned the response. that will now be handled by a separate thread
//        return in.readLine();
    }

}
