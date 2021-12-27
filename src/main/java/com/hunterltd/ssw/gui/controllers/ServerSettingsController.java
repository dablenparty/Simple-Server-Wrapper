package com.hunterltd.ssw.gui.controllers;

import com.hunterltd.ssw.gui.model.SimpleServerWrapperModel;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;

public class ServerSettingsController {
    @FXML
    public ComboBox<Double> memoryComboBox;
    private final SimpleServerWrapperModel model;

    public ServerSettingsController(SimpleServerWrapperModel model) {
        this.model = model;
    }

    public void initialize() {
        ObjectProperty<ObservableList<Double>> memoryObjectProperty = new SimpleObjectProperty<>(model.getServerMemory());
        memoryComboBox.itemsProperty().bind(memoryObjectProperty);
    }
}
