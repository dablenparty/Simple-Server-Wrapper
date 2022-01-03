package com.hunterltd.ssw.gui.controllers;

import com.hunterltd.ssw.gui.model.SimpleServerWrapperModel;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import net.lingala.zip4j.ZipFile;

import java.io.File;

public class CurseInstallerController extends FxController {
    @FXML
    private Label installProgressLabel;
    @FXML
    private ProgressBar installProgressBar;
    @FXML
    private TextField serverFolderTextField;
    @FXML
    private TextField modpackTextField;
    private ZipFile modpack = null;

    public CurseInstallerController(SimpleServerWrapperModel model) {
        super(model);
    }

    @Override
    public void initialize() {
        SimpleServerWrapperModel model = getInternalModel();
        serverFolderTextField.textProperty().bind(model.serverPathProperty());
    }

    @FXML
    protected void onInstallButtonClicked() {

    }

    @FXML
    protected void onSelectModpackButtonClicked() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select modpack ZIP");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP archive", "*.zip", "*.ZIP"));
        File chosen = fileChooser.showOpenDialog(modpackTextField.getScene().getWindow());
        if (chosen == null)
            return;
        modpackTextField.setText(chosen.getAbsolutePath());
        modpack = new ZipFile(chosen);
    }
}
