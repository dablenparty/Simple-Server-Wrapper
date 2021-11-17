package com.hunterltd.ssw.gui.controllers;

import com.hunterltd.ssw.minecraft.MinecraftServer;
import javafx.application.Platform;
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
                runButton.setText("Stop");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            runButton.setText("Stopping...");
            minecraftServer.stop();
        }


        minecraftServer.on("exit", args -> Platform.runLater(() -> {
            flipEnabledComponents();
            runButton.setText("Run");
        }));
    }

    @FXML
    protected void onSendButtonClick() {
        serverOutputTextArea.appendText("You clicked the button!\n");
    }

    @FXML
    protected void onSelectFileButtonClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select server JAR");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JAR archives", "*.jar"));
        File chosen = fileChooser.showOpenDialog(selectFileButton.getScene().getWindow());
        if (chosen == null)
            return;
        minecraftServer = new MinecraftServer(chosen);
        minecraftServer.on("data", args -> {
            String text = (String) args[0];
            if (!text.endsWith("\n"))
                text += '\n';
            String finalText = text;
            runOnFxThread(() -> serverOutputTextArea.appendText(finalText));
        });
        serverPathTextField.setText(chosen.toString());
        flipEnabledComponents();
    }

    private void flipEnabledComponents() {
        runButton.setDisable(!runButton.isDisable());
        sendCommandButton.setDisable(!sendCommandButton.isDisable());
        commandTextField.setDisable(!commandTextField.isDisable());
    }
}
