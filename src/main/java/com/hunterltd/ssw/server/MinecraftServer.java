package com.hunterltd.ssw.server;

import com.hunterltd.ssw.gui.dialogs.InfoDialog;
import com.hunterltd.ssw.gui.dialogs.InternalErrorDialog;
import com.hunterltd.ssw.server.properties.ServerProperties;
import com.hunterltd.ssw.utilities.Settings;

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
    private List<String> serverArgs;
    private final Path serverPath;
    private ServerProperties properties = null;
    private boolean propsExists;
    private boolean shouldBeRunning;
    private boolean shouldRestart = false;

    public MinecraftServer(File serverFile, Settings serverSettings) {
        this(serverFile.getParent(), serverFile.getName(), serverSettings);
    }

    public MinecraftServer(String serverFolder, String serverFilename, Settings settings) {
        pB = new ProcessBuilder();
        pB.directory(new File(serverFolder));
        serverSettings = settings;
        serverPath = Paths.get(serverFolder, serverFilename);

        propsExists = updateProperties();

        serverArgs = new ArrayList<>(Arrays.asList("java",
                String.format("-Xmx%dM", serverSettings.getMemory()),
                String.format("-Xms%dM", serverSettings.getMemory()),
                "-jar",
                serverPath.toString(),
                "nogui"));

        if (serverSettings.hasExtraArgs()) serverArgs.addAll(3, settings.getExtraArgs());
        updateExtraArgs();

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
    }

    public void generateBatch(String ext) {
        updateExtraArgs();
        String command = String.join(" ", serverArgs);
        File launchBatch = Paths.get(String.valueOf(serverPath.getParent()), String.format("launch.%s", ext)).toFile();

        try (PrintWriter writer = new PrintWriter(launchBatch)) {
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

            if (ext.equals("bat")) {
                command += "\npause";
            }
            writer.write(command);
            writer.flush();
        } catch (IOException e) {
            new InternalErrorDialog(e);
        } finally {
            InfoDialog dialog = new InfoDialog("File created",
                    String.format("\"%s\" was successfully created in the server folder", launchBatch.getName()));
            dialog.pack();
            dialog.setVisible(true);
        }
    }

    public boolean shouldBeRunning() {
        return shouldBeRunning;
    }

    public void setShouldBeRunning(boolean shouldBeRunning) {
        this.shouldBeRunning = shouldBeRunning;
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

    public ServerProperties getProperties() {
        return properties;
    }

    public boolean updateProperties() {
        try {
            properties = new ServerProperties(Paths.get(String.valueOf(serverPath.getParent()), "server.properties").toFile());
            propsExists = true;
            return true;
        } catch (FileNotFoundException e) {
            propsExists = false;
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            new InternalErrorDialog(e);
            return false;
        }
    }

    public void updateExtraArgs() {
        serverArgs = new ArrayList<>(Arrays.asList("java",
                String.format("-Xmx%dM", serverSettings.getMemory()),
                String.format("-Xms%dM", serverSettings.getMemory()),
                "-jar",
                serverPath.toString(),
                "nogui"));

        if (serverSettings.hasExtraArgs()) serverArgs.addAll(3, serverSettings.getExtraArgs());
    }

    public boolean propertiesExists() {
        return propsExists;
    }

    public boolean shouldRestart() {
        return shouldRestart;
    }

    public void setShouldRestart(boolean shouldRestart) {
        this.shouldRestart = shouldRestart;
    }
}
