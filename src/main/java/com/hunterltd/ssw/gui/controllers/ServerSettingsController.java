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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class ServerSettingsController extends FxController {
    private final MinecraftServer minecraftServer;
    private final HashMap<String, String> oldProperties;
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
    @FXML
    private TextField newValueTextField;
    @FXML
    private TextField newKeyTextField;
    @FXML
    private Button addPropertyButton;
    private boolean dirty = false;

    public ServerSettingsController(SimpleServerWrapperModel model, MinecraftServer minecraftServer) {
        super(model);
        this.minecraftServer = minecraftServer;
        Optional<MinecraftServer.ServerProperties> propertiesOptional = minecraftServer.getProperties();

        oldProperties = propertiesOptional.map(HashMap::new).orElse(null);
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

        extraArgsTextField.setOnKeyTyped(keyEvent -> dirty = true);

        // Automation tab
        restartCheckbox.setSelected(serverSettings.getRestart());
        proxyCheckbox.setSelected(serverSettings.getShutdown());

        restartIntervalSlider.disableProperty().bind(restartCheckbox.selectedProperty().not());
        restartIntervalSlider.setOnMouseReleased(mouseEvent -> dirty = true);
        restartIntervalSlider.setValue(serverSettings.getRestartInterval());

        proxyShutdownIntervalSlider.disableProperty().bind(proxyCheckbox.selectedProperty().not());
        proxyShutdownIntervalSlider.setOnMouseReleased(mouseEvent -> dirty = true);
        proxyShutdownIntervalSlider.setValue(serverSettings.getShutdownInterval());

        // Properties tab
        propertyTableColumn.setCellValueFactory(dataFeatures -> new SimpleStringProperty(dataFeatures.getValue().getKey()));

        valueTableColumn.setCellValueFactory(dataFeatures -> new SimpleStringProperty(dataFeatures.getValue().getValue()));
        valueTableColumn.setCellFactory(TextFieldTableCell.forTableColumn());

        Optional<MinecraftServer.ServerProperties> propertiesOptional = minecraftServer.getProperties();
        propertiesOptional.ifPresent(serverProperties -> {
            ObservableList<Map.Entry<String, String>> items = FXCollections.observableArrayList(serverProperties.entrySet());
            propertyTableView.setItems(items);
            //noinspection unchecked
            propertyTableView.getColumns().setAll(propertyTableColumn, valueTableColumn);
        });
        boolean empty = propertiesOptional.isEmpty();
        addPropertyButton.setDisable(empty);
        newKeyTextField.setDisable(empty);
        newValueTextField.setDisable(empty);
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
        minecraftServer.getProperties().ifPresent(properties -> {
            if (oldProperties != null) {
                properties.clear();
                properties.putAll(oldProperties);
            }
        });
        ((Stage) cancelButton.getScene().getWindow()).close();
    }

    @FXML
    protected void onSaveClicked() throws IOException {
        MinecraftServer.ServerSettings serverSettings = minecraftServer.getServerSettings();
        serverSettings.setMemory(memoryComboBox.getValue());
        String argsFieldText = extraArgsTextField.getText();
        List<String> extraArgs = argsFieldText.isBlank() ? new ArrayList<>() : List.of(argsFieldText.split(" "));
        serverSettings.setExtraArgs(extraArgs);
        serverSettings.setRestartInterval((int) restartIntervalSlider.getValue());
        serverSettings.setShutdownInterval((int) proxyShutdownIntervalSlider.getValue());
        serverSettings.setRestart(restartCheckbox.isSelected());
        serverSettings.setShutdown(proxyCheckbox.isSelected());
        serverSettings.writeData();
        minecraftServer.getProperties().ifPresent(properties -> {
            try {
                properties.write();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    protected void onValueEdited(TableColumn.CellEditEvent<Map.Entry<String, String>, String> editEvent) {
        dirty = true;
        editEvent.getTableView().getItems().get(editEvent.getTablePosition().getRow()).setValue(editEvent.getNewValue());
    }

    @FXML
    protected void onAddPropertyClicked() {
        dirty = true;
        minecraftServer.getProperties().ifPresent(properties -> properties.put(newKeyTextField.getText(), newValueTextField.getText()));
    }
}
