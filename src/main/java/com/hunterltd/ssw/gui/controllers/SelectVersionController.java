package com.hunterltd.ssw.gui.controllers;

import com.hunterltd.ssw.gui.components.ErrorAlert;
import com.hunterltd.ssw.gui.model.SimpleServerWrapperModel;
import com.hunterltd.ssw.minecraft.MinecraftServer;
import com.hunterltd.ssw.minecraft.MinecraftVersion;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Locale;

public class SelectVersionController extends FxController {
    private final MinecraftServer.ServerSettings serverSettings;
    @FXML
    private CheckBox snapshotsCheckBox;
    @FXML
    private ComboBox<String> versionsComboBox;

    public SelectVersionController(SimpleServerWrapperModel model, MinecraftServer.ServerSettings serverSettings) {
        super(model);
        this.serverSettings = serverSettings;
    }

    @Override
    public void initialize() {
        SimpleServerWrapperModel model = getInternalModel();
        ObjectProperty<ObservableList<String>> minecraftVersionOptions = new SimpleObjectProperty<>(model.getMinecraftVersionOptions());
        versionsComboBox.itemsProperty().bind(minecraftVersionOptions);
    }

    @FXML
    protected void onSnapshotsCheckBoxAction() {
        SimpleServerWrapperModel model = getInternalModel();
        FilteredList<String> minecraftVersionOptions = model.getMinecraftVersionOptions();
        minecraftVersionOptions.setPredicate(snapshotsCheckBox.isSelected() ? (s -> true) : (s -> !s.toLowerCase(Locale.ROOT).startsWith("snapshot")));
    }

    @FXML
    protected void onSaveButtonClicked() {
        String[] tokens = versionsComboBox.getValue().split(" ");
        MinecraftVersion minecraftVersion = MinecraftVersion.of(tokens[1]);
        serverSettings.setVersion(minecraftVersion);
        try {
            serverSettings.writeData();
        } catch (IOException e) {
            e.printStackTrace();
            ErrorAlert.showNewDialog(e);
        }
        ((Stage) versionsComboBox.getScene().getWindow()).close();
    }
}
