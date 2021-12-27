package com.hunterltd.ssw.gui.controllers;

import com.hunterltd.ssw.gui.model.SimpleServerWrapperModel;
import com.hunterltd.ssw.minecraft.MinecraftServer;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ServerSettingsController {
    private final SimpleServerWrapperModel model;
    private final MinecraftServer minecraftServer;
    @FXML
    private TextField extraArgsTextField;
    @FXML
    private ComboBox<Double> memoryComboBox;
    @FXML
    private ProgressBar memoryProgressBar;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;
    @FXML
    private CheckBox restartCheckbox;
    @FXML
    private Slider restartIntervalSlider;

    public ServerSettingsController(SimpleServerWrapperModel model, MinecraftServer minecraftServer) {
        this.model = model;
        this.minecraftServer = minecraftServer;
    }

    public void initialize() {
        // General tab
        ObjectProperty<ObservableList<Double>> memoryObjectProperty = new SimpleObjectProperty<>(model.getServerMemory());
        memoryComboBox.itemsProperty().bind(memoryObjectProperty);
        memoryComboBox.setOnAction(actionEvent -> {
            double comboBoxValue = memoryComboBox.getValue();
            memoryProgressBar.setProgress(comboBoxValue / model.getMaxMemory());
        });
        MinecraftServer.ServerSettings serverSettings = minecraftServer.getServerSettings();
        memoryComboBox.setValue(serverSettings.getMemory());
        memoryProgressBar.setProgress(serverSettings.getMemory() / model.getMaxMemory());
        extraArgsTextField.textProperty().bind(model.extraArgsProperty());

        // Automation tab
        restartIntervalSlider.disableProperty().bind(restartCheckbox.selectedProperty().not());
        model.restartIntervalProperty().bind(restartIntervalSlider.valueProperty());
    }

    @FXML
    protected void onCancelClicked() {
        // TODO check for unsaved changes
        ((Stage) cancelButton.getScene().getWindow()).close();
    }

    @FXML
    protected void onSaveClicked() throws IOException {
        MinecraftServer.ServerSettings serverSettings = minecraftServer.getServerSettings();
        serverSettings.setMemory(memoryComboBox.getValue());
        String argsFieldText = model.getExtraArgs();
        List<String> extraArgs = argsFieldText.isBlank() ? new ArrayList<>() : List.of(argsFieldText.split(" "));
        serverSettings.setExtraArgs(extraArgs);
        serverSettings.setRestartInterval(model.getRestartInterval());
        serverSettings.writeData();
    }
}
