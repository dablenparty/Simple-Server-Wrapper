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
    private TableView<Map.Entry<String, ?>> propertyTableView;
    @FXML
    private TableColumn<Map.Entry<String, ?>, String> propertyTableColumn;
    @FXML
    private TableColumn<Map.Entry<String, ?>, String> valueTableColumn;

    public ServerSettingsController(SimpleServerWrapperModel model, MinecraftServer minecraftServer) {
        super(model);
        this.minecraftServer = minecraftServer;
    }

    @SuppressWarnings("unchecked")
    public void initialize() {
        SimpleServerWrapperModel model = getInternalModel();

        // General tab
        ObjectProperty<ObservableList<Double>> memoryObjectProperty = new SimpleObjectProperty<>(model.getServerMemoryOptions());
        memoryComboBox.itemsProperty().bind(memoryObjectProperty);
        memoryComboBox.setOnAction(actionEvent -> {
            double comboBoxValue = memoryComboBox.getValue();
            memoryProgressBar.setProgress(comboBoxValue / model.getMaxMemory());
        });
        MinecraftServer.ServerSettings serverSettings = minecraftServer.getServerSettings();
        memoryComboBox.setValue(serverSettings.getMemory());
        memoryProgressBar.setProgress(serverSettings.getMemory() / model.getMaxMemory());
        extraArgsTextField.textProperty().bindBidirectional(model.extraArgsProperty());

        // Automation tab
        restartIntervalSlider.disableProperty().bind(restartCheckbox.selectedProperty().not());
        restartIntervalSlider.valueProperty().bindBidirectional(model.restartIntervalProperty());
        restartCheckbox.selectedProperty().bindBidirectional(model.restartProperty());
        proxyShutdownIntervalSlider.disableProperty().bind(proxyCheckbox.selectedProperty().not());
        proxyShutdownIntervalSlider.valueProperty().bindBidirectional(model.proxyShutdownIntervalProperty());
        proxyCheckbox.selectedProperty().bindBidirectional(model.proxyProperty());

        // Properties tab
        propertyTableColumn.setCellValueFactory(dataFeatures -> new SimpleStringProperty(dataFeatures.getValue().getKey()));
        valueTableColumn.setCellValueFactory(dataFeatures -> new SimpleStringProperty(String.valueOf(dataFeatures.getValue().getValue())));
        ObservableList<Map.Entry<String, ?>> items = FXCollections.observableArrayList(minecraftServer.getProperties().entrySet());
        propertyTableView.setItems(items);
        propertyTableView.getColumns().setAll(propertyTableColumn, valueTableColumn);
    }

    // TODO set a dirty flag whenever something changes, it's a lot faster than this
    private boolean settingsChanged(SimpleServerWrapperModel model, MinecraftServer.ServerSettings serverSettings) {
        return memoryComboBox.getValue() != serverSettings.getMemory()
                || model.isProxy() != serverSettings.getShutdown()
                || model.isRestart() != serverSettings.getRestart()
                || model.getRestartInterval() != serverSettings.getRestartInterval()
                || model.getProxyShutdownInterval() != serverSettings.getShutdownInterval()
                || !model.getExtraArgs().equals(String.join(" ", serverSettings.getExtraArgs()));
    }

    @FXML
    protected void onCancelClicked() {
        if (settingsChanged(getInternalModel(), minecraftServer.getServerSettings())) {
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
    }
}
