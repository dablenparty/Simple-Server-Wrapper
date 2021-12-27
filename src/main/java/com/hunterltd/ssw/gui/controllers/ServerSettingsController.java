package com.hunterltd.ssw.gui.controllers;

import com.hunterltd.ssw.gui.model.SimpleServerWrapperModel;
import com.hunterltd.ssw.minecraft.MinecraftServer;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;

public class ServerSettingsController {
    @FXML
    public ComboBox<Double> memoryComboBox;
    private final SimpleServerWrapperModel model;
    private final MinecraftServer minecraftServer;

    public ServerSettingsController(SimpleServerWrapperModel model, MinecraftServer minecraftServer) {
        this.model = model;
        this.minecraftServer = minecraftServer;
    }

    public void initialize() {
        ObjectProperty<ObservableList<Double>> memoryObjectProperty = new SimpleObjectProperty<>(model.getServerMemory());
        memoryComboBox.itemsProperty().bind(memoryObjectProperty);
        memoryComboBox.setValue(minecraftServer.getServerSettings().getMemory());
    }
}
