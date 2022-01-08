package com.hunterltd.ssw.gui.controllers;

import com.hunterltd.ssw.gui.model.SimpleServerWrapperModel;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;

import java.util.Locale;

public class SelectVersionController extends FxController {
    @FXML
    private CheckBox snapshotsCheckBox;
    @FXML
    private ComboBox<String> versionsComboBox;

    public SelectVersionController(SimpleServerWrapperModel model) {
        super(model);
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
}
