package com.hunterltd.ssw.cli;

import com.hunterltd.ssw.utilities.MavenUtils;
import com.hunterltd.ssw.utilities.ThreadUtils;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public class SswServerCli {
    private final int port;
    private final File serverFile;
    private ServerSocket serverSocket;
    private final List<ExecutorService> executorServices = new ArrayList<>(1);

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

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.printf("Connection accepted from %s on port %s%n",
                        clientSocket.getInetAddress(), clientSocket.getPort());
                SswClientHandler clientHandler = new SswClientHandler(clientSocket);
                ExecutorService currentService = Executors.newSingleThreadExecutor(
                        ThreadUtils.newNamedThreadFactory(
                                String.format("Client %s:%d",
                        clientSocket.getRemoteSocketAddress(), clientSocket.getPort()
                                )
                        )
                );
                currentService.submit(clientHandler);
                executorServices.add(currentService);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            executorServices.forEach(ThreadUtils::tryShutdownExecutorService);
            try {
                stop();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() throws IOException {
        serverSocket.close();
    }

    private static class SswClientHandler implements Runnable {
        private final Socket socket;

        SswClientHandler(Socket clientSocket) {
            socket = clientSocket;
        }

        @Override
        public void run() {
            try {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String message;
                while (!socket.isClosed() && (message = in.readLine()) != null) {
                    System.out.printf("[Client @%s] %s%n", socket.getInetAddress(), message);
                    if (message.equals("close")) {
                        out.println("Closing ssw server...");
                        break;
                    }
                    out.printf(message);
                }
                in.close();
                out.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
