package com.hunterltd.ServerWrapper.Server;

import com.hunterltd.ServerWrapper.GUI.Dialogs.InfoDialog;
import com.hunterltd.ServerWrapper.GUI.Dialogs.InternalErrorDialog;
import com.hunterltd.ServerWrapper.Utilities.Settings;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MinecraftServer {
    private Process serverProcess;
    private final ProcessBuilder pB;
    private final Settings serverSettings;
    private final String[] serverArgs;
    private final Path serverPath;

    public MinecraftServer(String serverFolder, String serverFilename, Settings settings) {
        pB = new ProcessBuilder();
        pB.directory(new File(serverFolder));
        serverSettings = settings;
        serverPath = Paths.get(serverFolder, serverFilename);

        List<String> argsList = new ArrayList<>(Arrays.asList("java",
                String.format("-Xmx%dM", serverSettings.getMemory()),
                String.format("-Xms%dM", serverSettings.getMemory()),
                "-jar",
                serverPath.toString(),
                "nogui"));

        if (serverSettings.hasExtraArgs()) argsList.addAll(3, settings.getExtraArgs());

        serverArgs = argsList.toArray(new String[0]);
        pB.command(serverArgs);
    }

    public Process getServerProcess() {
        return serverProcess;
    }

    public void sendCommand(String cmd) throws IOException {
        if (!cmd.endsWith("\n")) {
            cmd += "\n";
        }
        OutputStream out = serverProcess.getOutputStream();
        out.write(cmd.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    public void run() throws IOException {
        serverProcess = pB.start();
    }

    public void stop() throws IOException {
        sendCommand("stop");

        try {
            serverProcess.getOutputStream().close();
            serverProcess.getInputStream().close();
            serverProcess.getErrorStream().close();
        } catch (IOException ignored){
            InfoDialog dialog = new InfoDialog("Internal Error",
                    "An unknown internal error occurred while closing the server. " +
                            "Check task manager to make sure it was properly closed.");
            dialog.pack();
            dialog.setVisible(true);
        }
    }

    public void generateBatch(String ext) {
        String command = String.join(" ", serverArgs);
        File launchBatch = Paths.get(String.valueOf(serverPath.getParent()), String.format("launch.%s", ext)).toFile();

        try {
            if (!launchBatch.createNewFile()) {
                int result = JOptionPane.showConfirmDialog(null,
                        "The launch file already exists! Would you like to overwrite it?",
                        "Overwrite launch file",
                        JOptionPane.YES_NO_OPTION
                );
                if (result == JOptionPane.NO_OPTION) {
                    return;
                }
            }
            PrintWriter writer = new PrintWriter(launchBatch);
            if (ext.equals("bat")) {
                command += "\npause";
            }
            writer.write(command);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            InternalErrorDialog errorDialog = new InternalErrorDialog();
            errorDialog.pack();
            errorDialog.setVisible(true);
        }
    }

    public boolean isRunning() {
        return serverProcess.isAlive();
    }

    public Settings getServerSettings() {
        return serverSettings;
    }

    public Path getServerPath() {
        return serverPath;
    }
}
