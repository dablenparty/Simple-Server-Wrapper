package com.hunterltd.ssw.gui.controllers;

import com.hunterltd.ssw.gui.model.SimpleServerWrapperModel;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;

public class CurseInstallerController extends FxController {
    @FXML
    private Label installProgressLabel;
    @FXML
    private ProgressBar installProgressBar;
    @FXML
    private TextField serverFolderTextField;
    @FXML
    private TextField modpackTextField;

    public CurseInstallerController(SimpleServerWrapperModel model) {
        super(model);
    }

    @Override
    public void initialize() {

    }

    @FXML
    protected void onInstallButtonClicked() {

    }
}
