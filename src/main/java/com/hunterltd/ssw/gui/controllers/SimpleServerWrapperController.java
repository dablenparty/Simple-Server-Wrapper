package com.hunterltd.ssw.gui.controllers;

import com.hunterltd.ssw.gui.SimpleServerWrapperGui;
import com.hunterltd.ssw.gui.model.SimpleServerWrapperModel;
import com.hunterltd.ssw.minecraft.MinecraftServer;
import com.hunterltd.ssw.util.concurrency.NamedExecutorService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static com.hunterltd.ssw.util.concurrency.ThreadUtils.runOnFxThread;

public class SimpleServerWrapperController extends FxController {
    @FXML
    private Menu serverMenu;
    @FXML
    private Button selectFileButton;
    @FXML
    private TextField commandTextField;
    @FXML
    private Button sendCommandButton;
    @FXML
    private TextArea serverOutputTextArea;
    @FXML
    private Button runButton;
    @FXML
    private TextField serverPathTextField;
    private MinecraftServer minecraftServer = null;
    private List<NamedExecutorService> serviceList = null;

    public SimpleServerWrapperController(SimpleServerWrapperModel model) {
        super(model);
    }

    public void initialize() {
        SimpleServerWrapperModel model = getInternalModel();

        serverOutputTextArea.textProperty().bind(model.outputtedTextProperty());
        serverPathTextField.textProperty().bind(model.serverPathProperty());
        sendCommandButton.disableProperty().bind(model.serverRunningProperty().not());
        commandTextField.disableProperty().bind(model.serverRunningProperty().not());
        selectFileButton.disableProperty().bind(model.serverRunningProperty());
        model.outputtedTextProperty().addListener((observableValue, oldValue, newValue) -> {
            // works... but not very well
            // serverOutputTextArea.setScrollTop(Double.MAX_VALUE);
            // TODO add a checkbox for auto-scrolling
            serverOutputTextArea.selectEnd();
            serverOutputTextArea.deselect();
        });
    }

    @FXML
    protected void onRunButtonClick() {
        minecraftServer.setShouldBeRunning(!minecraftServer.isRunning());
    }

    @FXML
    protected void onSendButtonClick() {
        String command = commandTextField.getText();
        // starting/stopping is handled by a separate thread
        try {
            if (command.equals("stop"))
                minecraftServer.setShouldBeRunning(false);
            else
                minecraftServer.sendCommand(command);
        } catch (IOException e) {
            e.printStackTrace();
            serverOutputTextArea.appendText("Error: %s\n".formatted(e.getMessage()));
        } finally {
            commandTextField.clear();
        }
    }

    @FXML
    protected void onSelectFileButtonClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select server JAR");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JAR archives", "*.jar"));
        File chosen = fileChooser.showOpenDialog(selectFileButton.getScene().getWindow());
        if (chosen == null)
            return;
        // remove all listeners before reassigning the variable to prevent anything from accidentally firing
        if (minecraftServer != null) {
            minecraftServer.removeAllListeners();
            serviceList.forEach(NamedExecutorService::shutdown);
            Iterator<NamedExecutorService> iterator = serviceList.listIterator();
            while (iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        } else {
            selectFileButton.getScene().getWindow()
                    .setOnCloseRequest(windowEvent -> {
                        if (minecraftServer.isRunning()) {
                            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                            alert.setTitle("Quit");
                            alert.setHeaderText("A server is still running!");
                            alert.setContentText("Are you sure you want to quit? This will shut down the server.");
                            Optional<ButtonType> result = alert.showAndWait();
                            if (result.isEmpty() || result.get() != ButtonType.OK) {
                                windowEvent.consume();
                                return;
                            }
                            minecraftServer.stop();
                        }
                        serviceList.forEach(NamedExecutorService::shutdown);
                    });
        }
        SimpleServerWrapperModel model = getInternalModel();

        minecraftServer = new MinecraftServer(chosen);
        serviceList = minecraftServer.startAllBackgroundServices();
        minecraftServer.on("start", args -> runOnFxThread(() -> {
                    runButton.setText("Stop");
                    model.setServerRunning(true);
                }))
                .on("exiting", args -> runOnFxThread(() -> runButton.setText("Stopping...")))
                .on("exit", args -> runOnFxThread(() -> {
                    runButton.setText("Run");
                    model.setServerRunning(false);
                }))
                .on("data", args -> {
                    String text = (String) args[0];
                    if (!text.endsWith("\n"))
                        text += '\n';
                    String finalText = text;
                    runOnFxThread(() -> model.appendToOutputtedText(finalText));
                });
        model.setServerPath(minecraftServer.getServerPath().toString());
        MinecraftServer.ServerSettings serverSettings = minecraftServer.getServerSettings();
        model.setExtraArgs(String.join(" ", serverSettings.getExtraArgs()));
        model.setRestart(serverSettings.getRestart());
        model.setRestartInterval(serverSettings.getRestartInterval());
        model.setProxy(serverSettings.getShutdown());
        model.setProxyShutdownInterval(serverSettings.getShutdownInterval());
        runButton.setDisable(false);
        serverMenu.setDisable(false);
    }

    @FXML
    protected void onSettingsMenuClick(ActionEvent event) throws IOException {
        Stage stage = new Stage();
        SimpleServerWrapperModel model = getInternalModel();
        URL viewResource = SimpleServerWrapperGui.class.getResource("server-settings-view.fxml");
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(viewResource);
        loader.setControllerFactory(aClass -> new ServerSettingsController(model, minecraftServer));
        Parent root = loader.load();
        stage.setScene(new Scene(root));
        stage.setTitle("Server Settings");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(((MenuItem) event.getSource()).getStyleableNode().getScene().getWindow());
        stage.show();
    }

    @FXML
    protected void onOpenInFolderMenuClick() {
        Path serverPath = minecraftServer.getServerPath();
        while (!Files.isDirectory(serverPath))
            serverPath = serverPath.getParent();
        try {
            Desktop.getDesktop().browse(serverPath.toUri());
        } catch (IOException e) {
            // TODO make alerts on errors
            e.printStackTrace();
        }
    }
}
