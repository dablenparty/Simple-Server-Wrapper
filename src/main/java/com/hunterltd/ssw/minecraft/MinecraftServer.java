package com.hunterltd.ssw.minecraft;

import com.google.gson.*;
import com.hunterltd.ssw.cli.tasks.AliveStateCheckTask;
import com.hunterltd.ssw.cli.tasks.ServerPingTask;
import com.hunterltd.ssw.util.FixedSizeStack;
import com.hunterltd.ssw.util.concurrency.NamedExecutorService;
import com.hunterltd.ssw.util.concurrency.StreamGobbler;
import com.hunterltd.ssw.util.concurrency.ThreadUtils;
import com.hunterltd.ssw.util.events.EventEmitter;
import com.hunterltd.ssw.util.serial.GsonExclude;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static com.hunterltd.ssw.util.concurrency.ThreadUtils.printfWithTimeAndThread;
import static com.hunterltd.ssw.util.concurrency.ThreadUtils.printlnWithTimeAndThread;
import static com.hunterltd.ssw.util.serial.GsonUtils.GSON_EXCLUDE_STRATEGY;

/**
 * Minecraft server wrapper class
 */
public class MinecraftServer extends EventEmitter {
    private static final int MAX_COMMAND_HISTORY_SIZE = 10;
    private static final int CRASHES_BEFORE_SHUTDOWN = 3;

    private final ProcessBuilder pB;
    private final ServerSettings serverSettings;
    private final FixedSizeStack<String> commandHistory;
    private final Path serverPath;
    private volatile boolean shouldBeRunning = false;
    private volatile boolean shouldRestart = false;
    private volatile boolean shuttingDown = false;
    private volatile NamedExecutorService namedInputService, namedErrorService;
    private Process serverProcess;
    private List<String> serverArgs;
    private ServerProperties properties = null;
    private boolean eulaAgreedTo;
    private boolean propsExists;
    private int port = 25565;
    private int crashCount = 0;

    /**
     * Creates a class from a server file and automatically creates the settings file
     *
     * @param serverFile Server archive
     */
    public MinecraftServer(File serverFile) {
        this(serverFile, ServerSettings.getSettingsFromDefaultPath(serverFile));
    }

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
        // TODO auto detect the server version and apply appropriate Log4J fixes (1.14 and later)
        pB = new ProcessBuilder();
        File pBDirectory = new File(serverFolder);
        try {
            pBDirectory = pBDirectory.getCanonicalFile();
        } catch (IOException ignored) {
        }
        pB.directory(pBDirectory);
        serverSettings = minecraftServerSettings;

        Path serverPath1 = Path.of(serverFolder, serverFilename);
        try {
            serverPath1 = Path.of(Path.of(serverFolder, serverFilename).toFile().getCanonicalPath());
        } catch (IOException ignored) {
        }

        serverPath = serverPath1;
        propsExists = updateProperties();

        parseEula();
        updateExtraArgs();

        pB.command(serverArgs);

        commandHistory = new FixedSizeStack<>(MAX_COMMAND_HISTORY_SIZE);
    }

    private void parseEula() {
        Path eulaPath = Path.of(serverPath.getParent().toString(), "eula.txt");
        if (Files.exists(eulaPath)) {
            try {
                List<String> lines = Files.readAllLines(eulaPath);
                lines.forEach(s -> {
                    // commented lines
                    if (s.startsWith("#"))
                        return;
                    if (s.startsWith("eula")) {
                        String[] tokens = s.split("=");
                        eulaAgreedTo = Boolean.parseBoolean(tokens[1]);
                    }
                });
            } catch (IOException e) {
                printfWithTimeAndThread(System.err, "An error occurred parsing %s%n", e.getLocalizedMessage());
            }
        }
    }

    /**
     * @return {@code true} if the EULA has been agreed to. {@code false} otherwise
     */
    public boolean isEulaAgreedTo() {
        return eulaAgreedTo;
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
        commandHistory.push(cmd.trim());
    }

    /**
     * Updates the classes properties and starts the server
     *
     * @throws IOException if an I/O error occurs starting the server process
     */
    public void run() throws IOException {
        propsExists = updateProperties();
        parseEula();
        serverProcess = pB.start();
        serverProcess.onExit().thenApply(process -> {
            // if the server should be running, but it isn't, it crashed
            if (shouldBeRunning) {
                crashCount++;
                // prevents crash/launch loops
                boolean restart = false;
                String message;
                if (!eulaAgreedTo)
                    message = "EULA is not agreed to, the server will not restart";
                else if (crashCount == CRASHES_BEFORE_SHUTDOWN)
                    message = "Server has crashed " + CRASHES_BEFORE_SHUTDOWN + " times, the server will not restart";
                else {
                    restart = true;
                    message = "Server crashed, restarting...";
                }
                printlnWithTimeAndThread(System.out, message);
                if (!restart) {
                    shouldBeRunning = false;
                    shouldRestart = false;
                    crashCount = 0;
                }
            }
            shutdownReadServices();
            emit("exit", process);
            return null;
        });
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

    /**
     * Sends the stop command to the server
     *
     * @param timeout  timeout before killing the process
     * @param timeUnit unit of time for {@code timeout}
     */
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
     * Starts all background services that this server instance requires and returns an unmodifiable list of them.
     *
     * @return Unmodifiable list of all {@link NamedExecutorService}'s that were started by this server instance
     */
    public List<NamedExecutorService> startAllBackgroundServices() {
        List<NamedExecutorService> serviceList = new ArrayList<>(2);
        ScheduledExecutorService aliveScheduledService = Executors.newSingleThreadScheduledExecutor(ThreadUtils.newNamedThreadFactory("Alive State Check"));
        NamedExecutorService aliveNamedService = new NamedExecutorService("Alive State Check", aliveScheduledService);
        serviceList.add(aliveNamedService);
        AliveStateCheckTask stateCheckTask = new AliveStateCheckTask(this);
        aliveScheduledService.scheduleWithFixedDelay(stateCheckTask, 1L, 1L, TimeUnit.SECONDS);
        stateCheckTask.getChildServices().forEach(aliveNamedService::addChildService);
        if (serverSettings.getShutdown()) {
            printlnWithTimeAndThread(System.out, "Auto startup/shutdown is enabled");
            // make a new thread
            ScheduledExecutorService pingScheduledService = Executors.newSingleThreadScheduledExecutor(ThreadUtils.newNamedThreadFactory("Server Ping Service"));
            ServerPingTask pingTask = new ServerPingTask(this);
            // make the named service and add the ping tasks' child service
            NamedExecutorService serverPingService = new NamedExecutorService("Server Ping Service", pingScheduledService);
            pingTask.getChildServices().forEach(serverPingService::addChildService);
            serviceList.add(serverPingService);
            // lastly, schedule the task
            pingScheduledService.scheduleWithFixedDelay(pingTask, 2L, 2L, TimeUnit.SECONDS);
        }
        return Collections.unmodifiableList(serviceList);
    }

    /**
     * Generates a launch script for the server with the corresponding file extension
     *
     * @param ext File extension
     */
    public void generateBatch(String ext) {
        updateExtraArgs();
        String command = String.join(" ", serverArgs);
        File launchBatch = Path.of(String.valueOf(serverPath.getParent()), String.format("launch.%s", ext)).toFile();

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
    public Optional<ServerProperties> getProperties() {
        return Optional.ofNullable(properties);
    }

    /**
     * The properties don't always exist (e.g. on a servers first run) so this tries to read the properties file if it
     * exists
     *
     * @return Boolean on whether the properties were updated or not
     */
    public boolean updateProperties() {
        try {
            properties = new ServerProperties(Path.of(String.valueOf(serverPath.getParent()), "server.properties").toFile());
            propsExists = true;
            port = Integer.parseInt(properties.get("server-port"));
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
        String javaHome = System.getProperty("java.home");
        File executableFile = Path.of(javaHome, "bin", "java.exe").toFile();
        String javaCommand = executableFile.exists() ? executableFile.toString() : "java";

        int settingsMemoryMb = (int) (serverSettings.getMemory() * 1024);
        serverArgs = new ArrayList<>(Arrays.asList(javaCommand,
                String.format("-Xmx%dM", settingsMemoryMb),
                String.format("-Xms%dM", settingsMemoryMb),
                "-jar",
                serverPath.toString(),
                "nogui"));

        if (serverSettings.hasExtraArgs())
            serverArgs.addAll(3, serverSettings.getExtraArgs());
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
     * Returns the command history as a queue
     *
     * @return Command history queue
     */
    public FixedSizeStack<String> getCommandHistory() {
        return commandHistory;
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
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
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
        @GsonExclude
        private final Path settingsPath;
        private double memory;
        private List<String> extraArgs;
        private boolean autoRestart;
        private int restartInterval;
        private boolean autoShutdown;
        private int shutdownInterval;
        private MinecraftVersion version;

        /**
         * Creates an instance of this class with default values
         *
         * @param settingsPath {@code Path} to the JSON file this class is written to & read from
         */
        public ServerSettings(Path settingsPath) {
            memory = 1.0;
            extraArgs = new ArrayList<>();
            autoRestart = false;
            restartInterval = 6;
            autoShutdown = false;
            shutdownInterval = 15;
            version = null;
            this.settingsPath = settingsPath;
        }

        /**
         * Creates a new settings file in the default path ({serverFile.parent}/ssw/wrapperSettings.json)
         *
         * @param serverFile Server {@link File} object
         * @return {@code ServerSettings} object
         */
        public static ServerSettings getSettingsFromDefaultPath(File serverFile) {
            Path settingsPath = Path.of(serverFile.getParent(), "ssw", "wrapperSettings.json");
            if (Files.exists(settingsPath)) {
                Gson gson = new GsonBuilder()
                        .setExclusionStrategies(GSON_EXCLUDE_STRATEGY)
                        .registerTypeAdapter(
                                MinecraftVersion.class,
                                (JsonDeserializer<MinecraftVersion>)
                                        (jsonElement, type, jsonDeserializationContext) -> MinecraftVersion.of(jsonElement.getAsString())
                        )
                        .registerTypeAdapter(
                                ServerSettings.class,
                                new ServerSettingsInstanceCreator(settingsPath)
                        )
                        .create();
                String jsonString;
                try {
                    jsonString = Files.readString(settingsPath);
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
                ServerSettings settings = gson.fromJson(jsonString, ServerSettings.class);
                try {
                    if (settings.getVersion().isEmpty()) {
                        String versionFromJar = tryReadVersionFromJar(settingsPath.getParent().getParent());
                        settings.setVersion(MinecraftVersion.of(versionFromJar));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return settings;
            }
            try {
                Path parent = settingsPath.getParent();
                Files.createDirectories(parent);
                Files.createFile(settingsPath);
                ServerSettings settings = new ServerSettings(settingsPath);
                String versionFromJar = tryReadVersionFromJar(parent.getParent());
                settings.setVersion(MinecraftVersion.of(versionFromJar));
                settings.writeData();
                return settings;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        /**
         * Tries to read the Minecraft version from any existing JAR files in the server folder. If the version can't be
         * found or the version is too early (prior to 1.14), this method will return {@code null}.
         * <p>
         * JAR files for any Minecraft server on version 1.14 or later contain a {@code version.json} file. The
         * {@code name} property in this file is the Minecraft version string. This string is what gets returned.
         * <p>
         * All JAR files must be checked because the selected server file might not be the Minecraft server JAR. For
         * example, if a server is run using Fabric, the user might select {@code fabric-server-launch.jar} as the
         * server file; however, despite this being the correct file to launch the server, this is not the server JAR
         * containing {@code version.json}.
         *
         * @param serverFolder the server folder
         * @return Minecraft version string
         * @throws IOException if an I/O error occurs reading files from {@code serverFolder} (e.g. it is not a
         *                     directory)
         */
        @SuppressWarnings("unchecked")
        private static String tryReadVersionFromJar(Path serverFolder) throws IOException {
            File[] jarFiles = serverFolder.toFile().listFiles((dir, name) -> name.endsWith("jar"));
            if (jarFiles == null)
                throw new IOException(String.format("An error occurred listing files in %s", serverFolder));
            for (File file : jarFiles) {
                JarFile jarFile = new JarFile(file);
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().equals("version.json")) {
                        String jsonString = new String(jarFile.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8);
                        Map<String, ?> map = new Gson().fromJson(jsonString, Map.class);
                        return (String) map.get("name");
                    }
                }
            }
            return null;
        }

        /**
         * Write this object to the JSON file
         *
         * @throws IOException if an I/O error occurs writing to or creating the file, or the text cannot be encoded
         *                     using the specified charset (from {@link Files#writeString(Path, CharSequence, OpenOption...)}
         */
        public void writeData() throws IOException {
            Gson gson = new GsonBuilder()
                    .setExclusionStrategies(GSON_EXCLUDE_STRATEGY)
                    .registerTypeAdapter(
                            MinecraftVersion.class,
                            (JsonSerializer<MinecraftVersion>)
                                    (minecraftVersion, type, jsonSerializationContext) -> new JsonPrimitive(minecraftVersion.getId())
                    )
                    .create();
            String writeString = gson.toJson(this);
            Files.writeString(settingsPath, writeString);
        }

        /**
         * @return memory allocated to the server in GB
         */
        public double getMemory() {
            return memory;
        }

        /**
         * Set memory allocated to the server
         *
         * @param memory memory in GB
         */
        public void setMemory(double memory) {
            this.memory = memory;
        }

        /**
         * @return list of extra JVM arguments added by the user
         */
        public List<String> getExtraArgs() {
            return extraArgs;
        }

        /**
         * Sets the list of extra JVM arguments
         *
         * @param extraArgs new list
         */
        public void setExtraArgs(Collection<String> extraArgs) {
            this.extraArgs = new ArrayList<>(extraArgs);
        }

        /**
         * @return if there are more than 0 extra JVM arguments
         */
        public boolean hasExtraArgs() {
            return extraArgs.size() != 0;
        }

        /**
         * @return {@code true} if auto-restarting is enabled. {@code false otherwise}
         */
        public boolean getRestart() {
            return autoRestart;
        }

        /**
         * Enables/disables automatic restarting
         *
         * @param autoRestart new value
         */
        public void setRestart(boolean autoRestart) {
            this.autoRestart = autoRestart;
        }

        /**
         * @return the interval after which the server will be automatically restarted (if this is enabled), in hours
         */
        public int getRestartInterval() {
            return restartInterval;
        }

        /**
         * Sets the interval, in hours, before the server is automatically restarted (if this feature is enabled)
         *
         * @param restartInterval new interval in minutes
         */
        public void setRestartInterval(int restartInterval) {
            this.restartInterval = restartInterval;
        }

        /**
         * @return {@code true} if a proxy server is inserted between the client and server to provide automatic
         * shutdown/startup capabilities. {@code false} otherwise
         */
        public boolean getShutdown() {
            return autoShutdown;
        }

        /**
         * Sets whether a proxy server should be inserted between the client and server for automatic shutdown/startup
         *
         * @param autoShutdown new value
         */
        public void setShutdown(boolean autoShutdown) {
            this.autoShutdown = autoShutdown;
        }

        /**
         * @return the interval (in minutes) the server should remain with 0 players before shutting down
         */
        public int getShutdownInterval() {
            return shutdownInterval;
        }

        /**
         * Sets the interval, in minutes, the server should remain with 0 players before shutting down
         *
         * @param shutdownInterval new value
         */
        public void setShutdownInterval(int shutdownInterval) {
            this.shutdownInterval = shutdownInterval;
        }

        /**
         * Gets the version string wrapped in an {@link Optional}. If the server is too old (prior to 1.14), this will
         * be empty
         *
         * @return Minecraft version string
         */
        public Optional<MinecraftVersion> getVersion() {
            return Optional.ofNullable(version);
        }

        /**
         * Sets the version string for the server
         *
         * @param version new value
         */
        public void setVersion(MinecraftVersion version) {
            this.version = version;
        }

        private record ServerSettingsInstanceCreator(Path path) implements InstanceCreator<ServerSettings> {
            @Override
            public ServerSettings createInstance(Type type) {
                return new ServerSettings(path);
            }
        }
    }

    public static class ServerProperties extends HashMap<String, String> {
        private final File propsFile;
        private final List<String> comments = new ArrayList<>();

        public ServerProperties(File propertiesFile) throws IOException {
            propsFile = propertiesFile;
            read();
        }

        public File getPropsFile() {
            return propsFile;
        }

        /**
         * Reads and parses the {@code server.properties} file and puts the data into a {@link HashMap}
         *
         * @throws IOException if the file does not exist or cannot be read due to other I/O errors
         */
        public void read() throws IOException {
            try (BufferedReader reader = new BufferedReader(new FileReader(propsFile))) {
                reader.lines().forEach(line -> {
                    if (line.startsWith("#")) {
                        comments.add(line);
                        return; // Ignores commented lines
                    }
                    String[] split = line.split("=");
                    // No value for key
                    String value = split.length == 1 ? null : split[1];
                    this.put(split[0], value);
                });
            }
        }

        /**
         * Writes the data stored within the {@link HashMap} to the {@code server.properties} file
         *
         * @throws FileNotFoundException if the file does not exist
         */
        public void write() throws FileNotFoundException {
            PrintWriter writer = new PrintWriter(propsFile);
            writer.write(String.join("\n", comments)); // writes the comments back at the start
            this.forEach((key, value) -> {
                String line = value == null ?
                        key + "=" : String.join("=", key, value);
                writer.append("\n").append(line);
            });
            writer.flush();
            writer.close();
        }
    }
}
