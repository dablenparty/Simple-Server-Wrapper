package com.hunterltd.ssw.server;

import com.dablenparty.jsevents.EventEmitter;
import com.hunterltd.ssw.gui.dialogs.InfoDialog;
import com.hunterltd.ssw.gui.dialogs.InternalErrorDialog;
import com.hunterltd.ssw.server.properties.ServerProperties;
import com.hunterltd.ssw.utilities.MinecraftServerSettings;
import com.hunterltd.ssw.utilities.StreamGobbler;
import com.hunterltd.ssw.utilities.ThreadUtils;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Minecraft server wrapper class
 */
public class MinecraftServer extends EventEmitter {
    private final ProcessBuilder pB;
    private final MinecraftServerSettings serverSettings;
    private final List<String> commandHistory;
    private final Path serverPath;
    private Process serverProcess;
    private List<String> serverArgs;
    private ServerProperties properties = null;
    private boolean propsExists;
    private int port = 25565;
    private volatile boolean shouldBeRunning;
    private volatile boolean shouldRestart = false;
    private volatile boolean shuttingDown = false;
    private volatile ExecutorService inputService, errorService;

    /**
     * Creates a class from an archive file and settings class
     *
     * @param serverFile     Server .jar file
     * @param serverSettings Server settings
     */
    public MinecraftServer(File serverFile, MinecraftServerSettings serverSettings) {
        this(serverFile.getParent(), serverFile.getName(), serverSettings);
    }

    public MinecraftServer(String serverFolder, String serverFilename, MinecraftServerSettings minecraftServerSettings) {
        pB = new ProcessBuilder();
        File pBDirectory = new File(serverFolder);
        try {
            pBDirectory = pBDirectory.getCanonicalFile();
        } catch (IOException ignored) {
        }
        pB.directory(pBDirectory);
        serverSettings = minecraftServerSettings;

        Path serverPath1 = Paths.get(serverFolder, serverFilename);
        try {
            serverPath1 = Path.of(Paths.get(serverFolder, serverFilename).toFile().getCanonicalPath());
        } catch (IOException ignored) {
        }

        serverPath = serverPath1;
        propsExists = updateProperties();

        int settingsMemory = serverSettings.getMemory();
        serverArgs = new ArrayList<>(Arrays.asList("java",
                String.format("-Xmx%dM", settingsMemory),
                String.format("-Xms%dM", settingsMemory),
                "-jar",
                serverPath.toString(),
                "nogui"));

        if (serverSettings.hasExtraArgs()) serverArgs.addAll(3, minecraftServerSettings.getExtraArgs());
        updateExtraArgs();

        pB.command(serverArgs);

        commandHistory = new ArrayList<>();
        commandHistory.add(""); // Not entirely sure why this is needed, but the command history won't work without it
    }

    /**
     * @return Server process
     */
    public Process getServerProcess() {
        return serverProcess;
    }

    /**
     * Sends a command to tge server process
     *
     * @param cmd Command
     * @throws IOException if an I/O error occurs writing to the server process
     */
    public void sendCommand(String cmd) throws IOException {
        if (!cmd.endsWith("\n")) {
            cmd += "\n";
        }
        OutputStream out = serverProcess.getOutputStream();
        out.write(cmd.getBytes(StandardCharsets.UTF_8));
        out.flush(); // sends the command
        commandHistory.add(cmd.trim());
    }

    /**
     * Updates the classes properties and starts the server
     *
     * @throws IOException if an I/O error occurs starting the server process
     */
    public void run() throws IOException {
        propsExists = updateProperties();
        serverProcess = pB.start();
        emit("start", serverProcess);
        Consumer<String> gobblerConsumer = text -> emit("data", text);
        inputService = StreamGobbler.execute(
                serverProcess.getInputStream(),
                gobblerConsumer,
                "Server Input Stream"
        );
        errorService = StreamGobbler.execute(
                serverProcess.getErrorStream(),
                gobblerConsumer,
                "Server Error Stream"
        );
    }

    /**
     * Sends the stop command to the server
     *
     * @throws IOException if an I/O error occurs writing to the server process
     */
    public void stop() throws IOException {
        stop(5L, TimeUnit.SECONDS);
    }

    public void stop(long timeout, TimeUnit timeUnit) throws IOException {
        emit("exiting");
        try {
            sendCommand("stop");
            if (!serverProcess.waitFor(timeout, timeUnit)) serverProcess.destroy();
        } catch (InterruptedException | IOException e) {
            serverProcess.destroy();
        } finally {
            ThreadUtils.tryShutdownExecutorService(inputService);
            ThreadUtils.tryShutdownExecutorService(errorService);
            serverProcess.getInputStream().close();
            serverProcess.getErrorStream().close();
            serverProcess.getOutputStream().close();
            emit("exit");
        }

    }

    /**
     * Generates a launch script for the server with the corresponding file extension
     *
     * @param ext File extension
     */
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
                if (result == JOptionPane.NO_OPTION) return;
            }

            if (ext.equals("bat")) command += "\npause";
            writer.write(command);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
            new InternalErrorDialog(e);
        } finally {
            InfoDialog dialog = new InfoDialog("File created",
                    String.format("\"%s\" was successfully created in the server folder", launchBatch.getName()));
            dialog.pack();
            dialog.setVisible(true);
        }
    }

    /**
     * @return Boolean on whether the server should be running
     */
    public boolean shouldBeRunning() {
        return shouldBeRunning;
    }

    /**
     * Sets whether the server should be running or not
     *
     * @param shouldBeRunning Value to assign to
     */
    public void setShouldBeRunning(boolean shouldBeRunning) {
        this.shouldBeRunning = shouldBeRunning;
    }

    /**
     * @return Boolean on whether the server is running
     */
    public boolean isRunning() {
        boolean result;
        try {
            result = serverProcess.isAlive();
        } catch (NullPointerException ignored) {
            result = false;
        }
        return result;
    }

    /**
     * @return The servers settings
     */
    public MinecraftServerSettings getServerSettings() {
        return serverSettings;
    }

    /**
     * @return The server path as a {@link Path} object
     */
    public Path getServerPath() {
        return serverPath;
    }

    /**
     * @return The servers properties
     */
    public ServerProperties getProperties() {
        return properties;
    }

    /**
     * The properties don't always exist (e.g. on a servers first run) so this tries to read the properties file if it
     * exists
     *
     * @return Boolean on whether the properties were updated or not
     */
    public boolean updateProperties() {
        try {
            properties = new ServerProperties(Paths.get(String.valueOf(serverPath.getParent()), "server.properties").toFile());
            propsExists = true;
            port = (int) properties.get("server-port");
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

    /**
     * Updates the extra args for the server
     */
    public void updateExtraArgs() {
        serverArgs = new ArrayList<>(Arrays.asList("java",
                String.format("-Xmx%dM", serverSettings.getMemory()),
                String.format("-Xms%dM", serverSettings.getMemory()),
                "-jar",
                serverPath.toString(),
                "nogui"));

        if (serverSettings.hasExtraArgs()) serverArgs.addAll(3, serverSettings.getExtraArgs());
    }

    /**
     * @return Boolean on whether the properties file exists
     */
    public boolean propertiesExists() {
        return propsExists;
    }

    /**
     * @return Boolean on whether the server should restart
     */
    public boolean shouldRestart() {
        return shouldRestart;
    }

    /**
     * Sets whether the server should restart or not
     *
     * @param shouldRestart Value to set
     */
    public void setShouldRestart(boolean shouldRestart) {
        this.shouldRestart = shouldRestart;
    }

    /**
     * @param idx Index ti retrieve
     * @return Command from history
     */
    public String getCommandFromHistory(int idx) {
        return commandHistory.get(idx);
    }

    /**
     * @return Number of commands in the history
     */
    public int getHistorySize() {
        return commandHistory.size();
    }

    /**
     * @return Server port
     */
    public int getPort() {
        return port;
    }

    /**
     * @return Boolean on if the server is shutting down
     */
    public boolean isShuttingDown() {
        return shuttingDown;
    }

    /**
     * Sets if the server is shutting down
     *
     * @param shuttingDown Value
     */
    public void setShuttingDown(boolean shuttingDown) {
        this.shuttingDown = shuttingDown;
    }
}
