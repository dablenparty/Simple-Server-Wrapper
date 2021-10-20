package com.hunterltd.ssw.cli;

import com.hunterltd.ssw.server.MinecraftServer;
import com.hunterltd.ssw.utilities.MavenUtils;
import com.hunterltd.ssw.utilities.MinecraftServerSettings;
import com.hunterltd.ssw.utilities.ThreadUtils;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

public class SswServerCli {
    private final int port;
    private final MinecraftServer minecraftServer;
    private final List<ExecutorService> serviceList = new ArrayList<>();
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    SswServerCli(int port, File serverFile) {
        this.port = port;
        minecraftServer = new MinecraftServer(serverFile, MinecraftServerSettings.getSettingsFromDefaultPath(serverFile));
        minecraftServer.on("data", objects -> printlnToServerAndClientRaw((String) objects[0]));
        minecraftServer.on("exiting", objects -> printlnToServerAndClient("Stopping server..."));
        minecraftServer.on("exit", objects -> printlnToServerAndClient("Server successfully stopped!"));
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

    private void printfToServerAndClient(String formatString, Object... args) {
        // prevents formatting twice
        String message = ThreadUtils.threadStampString(String.format(formatString, args));
        System.out.print(message);
        this.out.print(message);
    }

    private void printlnToServerAndClient(String string) {
        String message = ThreadUtils.threadStampString(string);
        printlnToServerAndClientRaw(message);
    }

    private void printlnToServerAndClientRaw(String message) {
        System.out.println(message);
        this.out.println(message);
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port, 0, InetAddress.getLoopbackAddress());
        clientSocket = serverSocket.accept();
        System.out.printf("Connection accepted from %s on port %d%n",
                clientSocket.getInetAddress(), clientSocket.getPort());
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        out = new PrintWriter(clientSocket.getOutputStream(), true);

        String message;
        while ((message = in.readLine()) != null) {
            System.out.printf("[Client %s:%s] %s%n",
                    clientSocket.getInetAddress(), clientSocket.getPort(), message);
            switch (message) {
                case "start" -> {
                    printlnToServerAndClient("Starting server...");
                    // running is handled in AliveStateCheckTask
                    minecraftServer.setShouldBeRunning(true);
                }
                case "stop" ->
                        // this requires a special case so a separate thread can stop the server
                        printlnToServerAndClient("Stopping server...");
                case "close" -> printlnToServerAndClient("Closing client connection");
                default -> {
                    if (minecraftServer.isRunning())
                        minecraftServer.sendCommand(message.trim());
                    else
                        printfToServerAndClient("Unknown command: %s%n", message);
                }
            }
            if (message.equals("close")) {
                out.println("Closing SSW server...");
                break;
            }
//            out.println(message);
        }
        stop();
    }

    public void stop() throws IOException {
        in.close();
        out.close();
        clientSocket.close();
        serverSocket.close();
    }
}
