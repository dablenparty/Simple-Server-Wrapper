package com.hunterltd.ssw.gui.controllers;

import com.hunterltd.ssw.minecraft.MinecraftServer;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;

import static com.hunterltd.ssw.util.concurrency.ThreadUtils.runOnFxThread;

public class SimpleServerWrapperController {
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

    @FXML
    protected void onRunButtonClick() {
        if (!minecraftServer.isRunning()) {
            try {
                minecraftServer.run();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            runButton.setText("Stopping...");
            minecraftServer.stop();
        }
    }

    @FXML
    protected void onSendButtonClick() {
        String command = commandTextField.getText();
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
        if (minecraftServer != null)
            minecraftServer.removeAllListeners();
        minecraftServer = new MinecraftServer(chosen);
        minecraftServer.on("start", args -> runOnFxThread(this::enableServerBasedComponents))
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
        runButton.setDisable(true);
        runButton.setText("Run");
        sendCommandButton.setDisable(true);
        commandTextField.setDisable(true);
    }

    private void enableServerBasedComponents() {
        runButton.setDisable(false);
        runButton.setText("Stop");
        sendCommandButton.setDisable(false);
        commandTextField.setDisable(false);
    }
}
