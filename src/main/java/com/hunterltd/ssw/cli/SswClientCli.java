package com.hunterltd.ssw.cli;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Properties;

public class SswClientCli {
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;

    public static void main(String[] args) throws IOException {
        SswClientCli clientCli = new SswClientCli();
        Namespace namespace = clientCli.parseArgs(args);
        int port = namespace.getInt("port");
        String target = namespace.getString("target");

        try {
            clientCli.connect(target, port);
        } catch (ConnectException connectException) {
            System.err.println(connectException.getLocalizedMessage());
            return;
        }
        String response = clientCli.sendToServer("hello friend");
        System.out.println(response);
    }

    public Namespace parseArgs(String[] args) {
        Properties mavenProperties = getMavenProperties();
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

    private Properties getMavenProperties() {
        Properties mavenProperties = new Properties();
        try (InputStream resourceStream = SswClientCli.class.getClassLoader().getResourceAsStream("project.properties")) {
            mavenProperties.load(resourceStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mavenProperties;
    }

    public void connect(String targetIp, int port) throws IOException {
        clientSocket = new Socket(targetIp, port);
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }

    public void closeConnection() throws IOException {
        in.close();
        out.close();
        clientSocket.close();
    }

    public String sendToServer(String message) throws IOException {
        out.println(message);
        return in.readLine();
    }

}
