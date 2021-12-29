package com.hunterltd.ssw.gui.controllers;

import com.hunterltd.ssw.gui.model.SimpleServerWrapperModel;
import com.hunterltd.ssw.minecraft.MinecraftServer;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ServerSettingsController extends FxController {
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
    @FXML
    private CheckBox proxyCheckbox;
    @FXML
    private Slider proxyShutdownIntervalSlider;
    @FXML
    private TableView<Map.Entry<String, String>> propertyTableView;
    @FXML
    private TableColumn<Map.Entry<String, String>, String> propertyTableColumn;
    @FXML
    private TableColumn<Map.Entry<String, String>, String> valueTableColumn;
    private boolean dirty = false;

    public ServerSettingsController(SimpleServerWrapperModel model, MinecraftServer minecraftServer) {
        super(model);
        this.minecraftServer = minecraftServer;
    }

    public void initialize() {
        SimpleServerWrapperModel model = getInternalModel();

        // General tab
        ObjectProperty<ObservableList<Double>> memoryObjectProperty = new SimpleObjectProperty<>(model.getServerMemoryOptions());
        memoryComboBox.itemsProperty().bind(memoryObjectProperty);
        memoryComboBox.setOnAction(actionEvent -> {
            double comboBoxValue = memoryComboBox.getValue();
            memoryProgressBar.setProgress(comboBoxValue / model.getMaxMemory());
            dirty = true;
        });
        MinecraftServer.ServerSettings serverSettings = minecraftServer.getServerSettings();
        memoryComboBox.setValue(serverSettings.getMemory());
        memoryProgressBar.setProgress(serverSettings.getMemory() / model.getMaxMemory());
        extraArgsTextField.textProperty().bindBidirectional(model.extraArgsProperty());
        extraArgsTextField.setOnKeyTyped(keyEvent -> dirty = true);

        // Automation tab
        restartIntervalSlider.disableProperty().bind(restartCheckbox.selectedProperty().not());
        restartIntervalSlider.valueProperty().bindBidirectional(model.restartIntervalProperty());
        restartIntervalSlider.setOnMouseReleased(mouseEvent -> dirty = true);
        restartCheckbox.selectedProperty().bindBidirectional(model.restartProperty());
        proxyShutdownIntervalSlider.disableProperty().bind(proxyCheckbox.selectedProperty().not());
        proxyShutdownIntervalSlider.valueProperty().bindBidirectional(model.proxyShutdownIntervalProperty());
        proxyShutdownIntervalSlider.setOnMouseReleased(mouseEvent -> dirty = true);
        proxyCheckbox.selectedProperty().bindBidirectional(model.proxyProperty());

        // Properties tab
        propertyTableColumn.setCellValueFactory(dataFeatures -> new SimpleStringProperty(dataFeatures.getValue().getKey()));
        valueTableColumn.setCellValueFactory(dataFeatures -> new SimpleStringProperty(dataFeatures.getValue().getValue()));
        valueTableColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        ObservableList<Map.Entry<String, String>> items = FXCollections.observableArrayList(minecraftServer.getProperties().entrySet());
        propertyTableView.setItems(items);
        //noinspection unchecked
        propertyTableView.getColumns().setAll(propertyTableColumn, valueTableColumn);
    }

    @FXML
    protected void onCancelClicked() {
        if (dirty) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Unsaved Changes");
            alert.setHeaderText("You have unsaved changes!");
            alert.setContentText("Would you like to exit without saving?");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK)
                return;
        }
        ((Stage) cancelButton.getScene().getWindow()).close();
    }

    @FXML
    protected void onSaveClicked() throws IOException {
        SimpleServerWrapperModel model = getInternalModel();

        MinecraftServer.ServerSettings serverSettings = minecraftServer.getServerSettings();
        serverSettings.setMemory(memoryComboBox.getValue());
        String argsFieldText = model.getExtraArgs();
        List<String> extraArgs = argsFieldText.isBlank() ? new ArrayList<>() : List.of(argsFieldText.split(" "));
        serverSettings.setExtraArgs(extraArgs);
        serverSettings.setRestartInterval(model.getRestartInterval());
        serverSettings.setShutdownInterval(model.getProxyShutdownInterval());
        serverSettings.setRestart(model.isRestart());
        serverSettings.setShutdown(model.isProxy());
        serverSettings.writeData();
        minecraftServer.getProperties().write();
    }

    @FXML
    protected void onValueEdited(TableColumn.CellEditEvent<Map.Entry<String, String>, String> editEvent) {
        dirty = true;
        editEvent.getTableView().getItems().get(editEvent.getTablePosition().getRow()).setValue(editEvent.getNewValue());
    }
}
