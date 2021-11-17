package com.hunterltd.ssw.gui.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

import java.io.File;

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

    @FXML
    protected void onRunButtonClick() {
        runButton.setText(runButton.getText().equals("Run") ? "Stop" : "Run");
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
        serverPathTextField.setText(chosen.toString());
        runButton.setDisable(false);
        sendCommandButton.setDisable(false);
        commandTextField.setDisable(false);
    }
}
