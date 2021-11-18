package com.hunterltd.ssw.gui.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class ConfirmationDialogController {
    @FXML
    private Button cancelButton;
    @FXML
    private Button okButton;
    @FXML
    private Label messageLabel;

    private static boolean RESULT;

    @FXML
    protected void onOkButtonClicked() {
        RESULT = true;
    }

    @FXML
    protected void onCancelButtonClicked() {
        RESULT = false;
    }
}
