package com.hunterltd.ssw.gui.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class SimpleServerWrapperController {
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
    private Label welcomeText;

    @FXML
    protected void onRunButtonClick() {
        runButton.setText(runButton.getText().equals("Run") ? "Stop" : "Run");
    }

    @FXML
    protected void onSendButtonClick() {
        serverOutputTextArea.appendText("You clicked the button!\n");
    }
}
