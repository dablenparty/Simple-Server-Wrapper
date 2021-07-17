package com.hunterltd.ssw.cli;

import com.hunterltd.ssw.utilities.MavenUtils;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

public class SswServerCli {
    private final int port;
    private final File serverFile;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    SswServerCli(int port, File serverFile) {
        this.port = port;
        this.serverFile = serverFile;
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

    public static void main(String[] args) throws IOException {
        Namespace namespace = parseArgs(args);
        File server = new File(namespace.getString("server")).getAbsoluteFile();
        int port = namespace.getInt("port");
        SswServerCli serverCli = new SswServerCli(port, server);
        serverCli.start();
        serverCli.stop();
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        clientSocket = serverSocket.accept();
        System.out.println("Connection accepted");
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        String message;
        while ((message = in.readLine()) != null) {
            if (message.equals("close")) {
                out.println("Closing SSW server...");
                break;
            }
            out.println(message);
        }
    }

    public void stop() throws IOException {
        in.close();
        out.close();
        clientSocket.close();
        serverSocket.close();
    }
}
