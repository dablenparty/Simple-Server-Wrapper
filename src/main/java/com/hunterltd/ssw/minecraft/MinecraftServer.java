package com.hunterltd.ssw.minecraft;

import com.google.gson.Gson;
import com.hunterltd.ssw.util.concurrency.NamedExecutorService;
import com.hunterltd.ssw.util.concurrency.StreamGobbler;
import com.hunterltd.ssw.util.concurrency.ThreadUtils;
import com.hunterltd.ssw.util.events.EventEmitter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Minecraft server wrapper class
 */
public class MinecraftServer extends EventEmitter {
    private final ProcessBuilder pB;
    private final ServerSettings serverSettings;
    private final List<String> commandHistory;
    private final Path serverPath;
    private Process serverProcess;
    private List<String> serverArgs;
    private ServerProperties properties = null;
    private boolean propsExists;
    private int port = 25565;
    private volatile boolean shouldBeRunning = false;
    private volatile boolean shouldRestart = false;
    private volatile boolean shuttingDown = false;
    private volatile NamedExecutorService namedInputService, namedErrorService;

    /**
     * Creates a class from an archive file and settings class
     *
     * @param serverFile     Server .jar file
     * @param serverSettings Server settings
     */
    public MinecraftServer(File serverFile, ServerSettings serverSettings) {
        this(serverFile.getParent(), serverFile.getName(), serverSettings);
    }

    public MinecraftServer(String serverFolder, String serverFilename, ServerSettings minecraftServerSettings) {
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
        serverProcess.onExit().thenApply(process -> {
            shutdownReadServices();
            emit("exit", process);
            return null;
        }); // emits "exit" when the process exits
        emit("start", serverProcess);
        Consumer<String> gobblerConsumer = text -> emit("data", text);
        ExecutorService inputExecutorService = StreamGobbler.execute(
                serverProcess.getInputStream(),
                gobblerConsumer,
                "Server Input Stream"
        );
        namedInputService = new NamedExecutorService("MinecraftServer Input Service", inputExecutorService);
        ExecutorService errorExecutorService = StreamGobbler.execute(
                serverProcess.getErrorStream(),
                gobblerConsumer,
                "Server Error Stream"
        );
        namedErrorService = new NamedExecutorService("MinecraftServer Error Service", errorExecutorService);
    }

    /**
     * Sends the stop command to the server
     */
    public void stop() {
        stop(5L, TimeUnit.SECONDS);
    }

    public void stop(long timeout, TimeUnit timeUnit) {
        shuttingDown = true;
        emit("exiting");
        try {
            sendCommand("stop");
            if (!serverProcess.waitFor(timeout, timeUnit)) serverProcess.destroy();
        } catch (InterruptedException | IOException e) {
            serverProcess.destroy();
        } finally {
//            shutdownReadServices();
            shuttingDown = false;
        }

    }

    private void shutdownReadServices() {
        ThreadUtils.tryShutdownNamedExecutorService(namedInputService);
        ThreadUtils.tryShutdownNamedExecutorService(namedErrorService);
        for (Closeable stream :
                new Closeable[]{
                        serverProcess.getInputStream(),
                        serverProcess.getErrorStream(),
                        serverProcess.getOutputStream()
                }) {
            try {
                stream.close();
            } catch (IOException e) {
                System.err.println(e.getLocalizedMessage());
            }
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
            //noinspection ResultOfMethodCallIgnored
            launchBatch.createNewFile();
            if (ext.equals("bat"))
                command += "\npause";
            writer.write(command);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
            emit("error", e);
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
    public ServerSettings getServerSettings() {
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
            emit("error", e);
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
     * @param idx Index to retrieve
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

    public static class ServerSettings {
        private final Path settingsPath;
        private int memory;
        private List<String> extraArgs;
        private boolean autoRestart;
        private int restartInterval;
        private boolean autoShutdown;
        private int shutdownInterval;

        public ServerSettings(Path settingsPath) throws UnsupportedOperationException {
            memory = 1024;
            extraArgs = new ArrayList<>();
            autoRestart = false;
            restartInterval = 6;
            autoShutdown = false;
            shutdownInterval = 15;
            this.settingsPath = settingsPath;
        }

        /**
         * Creates a new settings file in the default path ({serverFile.parent}/ssw/wrapperSettings.json)
         *
         * @param serverFile Server file object
         * @return MinecraftServerSettings object
         */
        public static ServerSettings getSettingsFromDefaultPath(File serverFile) {
            Path settingsPath = Paths.get(serverFile.getParent(), "ssw", "wrapperSettings.json");
            if (Files.exists(settingsPath)) {
                Gson gson = new Gson();
                String jsonString;
                try {
                    jsonString = Files.readString(settingsPath);
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
                return gson.fromJson(jsonString, ServerSettings.class);
            }
            try {
                ServerSettings settings = new ServerSettings(settingsPath);
                settings.writeData();
                return settings;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        public void writeData() throws IOException {
            Gson gson = new Gson();
            String writeString = gson.toJson(this);
            Files.writeString(settingsPath, writeString);
        }

        public int getMemory() {
            return memory;
        }

        public void setMemory(int memory) {
            this.memory = memory;
        }

        public List<String> getExtraArgs() {
            return extraArgs;
        }

        public void setExtraArgs(Collection<String> extraArgs) {
            this.extraArgs = new ArrayList<>(extraArgs);
        }

        public boolean hasExtraArgs() {
            return extraArgs.size() != 0;
        }

        public boolean getRestart() {
            return autoRestart;
        }

        public void setRestart(boolean autoRestart) {
            this.autoRestart = autoRestart;
        }

        public int getRestartInterval() {
            return restartInterval;
        }

        public void setRestartInterval(int restartInterval) {
            this.restartInterval = restartInterval;
        }

        public boolean getShutdown() {
            return autoShutdown;
        }

        public void setShutdown(boolean autoShutdown) {
            this.autoShutdown = autoShutdown;
        }

        public int getShutdownInterval() {
            return shutdownInterval;
        }

        public void setShutdownInterval(int shutdownInterval) {
            this.shutdownInterval = shutdownInterval;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static class ServerProperties extends HashMap {
        private final File propsFile;
        private final List<String> comments = new ArrayList<>();

        public ServerProperties(File propertiesFile) throws IOException {
            propsFile = propertiesFile;
            read();
        }

        public File getPropsFile() {
            return propsFile;
        }

        public void read() throws IOException {
            try (BufferedReader reader = new BufferedReader(new FileReader(propsFile))) {
                reader.lines().forEach(line -> {
                    if (line.startsWith("#")) {
                        comments.add(line);
                        return; // Ignores commented lines
                    }
                    String[] split = line.split("=");
                    if (split.length == 1) {
                        // No value for key
                        this.put(split[0], null);
                    } else {
                        String key = split[0], value = split[1];
                        try {
                            this.put(key, Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            switch (value) {
                                case "true" -> this.put(key, true);
                                case "false" -> this.put(key, false);
                                default ->
                                        // regular string
                                        this.put(key, value);
                            }
                        }
                    }
                });
            }
        }

        public void write() throws FileNotFoundException {
            PrintWriter writer = new PrintWriter(propsFile);
            writer.write(String.join("\n", comments)); // writes the comments back at the start
            this.forEach((key, value) -> {
                String line = value == null ?
                        key + "=" : String.join("=", (String) key, String.valueOf(value));
                writer.append("\n").append(line);
            });
            writer.flush();
            writer.close();
        }
    }
}