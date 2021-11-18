package com.hunterltd.ssw.gui.controllers;

import com.hunterltd.ssw.cli.tasks.AliveStateCheckTask;
import com.hunterltd.ssw.cli.tasks.ServerPingTask;
import com.hunterltd.ssw.minecraft.MinecraftServer;
import com.hunterltd.ssw.util.concurrency.NamedExecutorService;
import com.hunterltd.ssw.util.concurrency.ThreadUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.hunterltd.ssw.util.concurrency.ThreadUtils.printlnWithTimeAndThread;
import static com.hunterltd.ssw.util.concurrency.ThreadUtils.runOnFxThread;

public class SimpleServerWrapperController {
    private final List<NamedExecutorService> serviceList;
    @FXML
    private Button selectFileButton;
    @FXML
    private TextField commandTextField;
    @FXML
    private Button sendCommandButton;
    @FXML
    private TextArea serverOutputTextArea;
    @FXML
    private Button runButton;
    @FXML
    private TextField serverPathTextField;
    private MinecraftServer minecraftServer = null;

    public SimpleServerWrapperController() {
        serviceList = new ArrayList<>();
    }

    @FXML
    protected void onRunButtonClick() {
        minecraftServer.setShouldBeRunning(!minecraftServer.isRunning());
    }

    @FXML
    protected void onSendButtonClick() {
        String command = commandTextField.getText();
        // starting/stopping is handled by a separate thread
        if (command.equals("stop")) {
            minecraftServer.setShouldBeRunning(false);
            return;
        }
        try {
            minecraftServer.sendCommand(command);
        } catch (IOException e) {
            e.printStackTrace();
            commandTextField.appendText("Error: %s\n".formatted(e.getMessage()));
        } finally {
            commandTextField.clear();
        }
    }

    @FXML
    protected void onSelectFileButtonClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select server JAR");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JAR archives", "*.jar"));
        File chosen = fileChooser.showOpenDialog(selectFileButton.getScene().getWindow());
        if (chosen == null)
            return;
        // remove all listeners before reassigning the variable to prevent anything from accidentally firing
        if (minecraftServer != null) {
            minecraftServer.removeAllListeners();
            serviceList.forEach(NamedExecutorService::shutdown);
            Iterator<NamedExecutorService> iterator = serviceList.listIterator();
            while (iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        } else {
            selectFileButton.getScene().getWindow()
                    .setOnCloseRequest(windowEvent -> {
                        // TODO show warning when server is still running
                        if (minecraftServer.isRunning())
                            minecraftServer.stop();
                        serviceList.forEach(NamedExecutorService::shutdown);
                    });
        }

        minecraftServer = new MinecraftServer(chosen);
        startAllServices();
        minecraftServer.on("start", args -> runOnFxThread(this::enableServerBasedComponents))
                .on("exiting", args -> runOnFxThread(() -> runButton.setText("Stopping...")))
                .on("exit", args -> runOnFxThread(this::disabledServerBasedComponents))
                .on("data", args -> {
                    String text = (String) args[0];
                    if (!text.endsWith("\n"))
                        text += '\n';
                    String finalText = text;
                    runOnFxThread(() -> serverOutputTextArea.appendText(finalText));
                });
        serverPathTextField.setText(chosen.toString());
        runButton.setDisable(false);
    }

    private void disabledServerBasedComponents() {
        runButton.setText("Run");
        sendCommandButton.setDisable(true);
        commandTextField.setDisable(true);
    }

    private void enableServerBasedComponents() {
        runButton.setText("Stop");
        sendCommandButton.setDisable(false);
        commandTextField.setDisable(false);
    }

    // TODO move this to util, copy paste was quick and dirty
    private void startAllServices() {
        ScheduledExecutorService aliveScheduledService = Executors.newSingleThreadScheduledExecutor(ThreadUtils.newNamedThreadFactory("Alive State Check"));
        NamedExecutorService aliveNamedService = new NamedExecutorService("Alive State Check", aliveScheduledService);
        serviceList.add(aliveNamedService);
        AliveStateCheckTask stateCheckTask = new AliveStateCheckTask(minecraftServer);
        aliveScheduledService.scheduleWithFixedDelay(stateCheckTask, 1L, 1L, TimeUnit.SECONDS);
        stateCheckTask.getChildServices().forEach(aliveNamedService::addChildService);
        MinecraftServer.ServerSettings serverSettings = minecraftServer.getServerSettings();
        if (serverSettings.getShutdown()) {
            printlnWithTimeAndThread(System.out, "Auto startup/shutdown is enabled");
            // make a new thread
            ScheduledExecutorService pingScheduledService = Executors.newSingleThreadScheduledExecutor(ThreadUtils.newNamedThreadFactory("Server Ping Service"));
            ServerPingTask pingTask = new ServerPingTask(minecraftServer);
            // make the named service and add the ping tasks' child service
            NamedExecutorService serverPingService = new NamedExecutorService("Server Ping Service", pingScheduledService);
            pingTask.getChildServices().forEach(serverPingService::addChildService);
            serviceList.add(serverPingService);
            // lastly, schedule the task
            pingScheduledService.scheduleWithFixedDelay(pingTask, 2L, 2L, TimeUnit.SECONDS);
        }
    }
}
