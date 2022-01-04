package com.hunterltd.ssw.gui.controllers;

import com.hunterltd.ssw.curse.CurseModpack;
import com.hunterltd.ssw.curse.api.CurseAddon;
import com.hunterltd.ssw.gui.model.SimpleServerWrapperModel;
import com.hunterltd.ssw.util.concurrency.NamedExecutorService;
import com.hunterltd.ssw.util.concurrency.ThreadUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import net.lingala.zip4j.ZipFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CurseInstallerController extends FxController {
    @FXML
    private Label installProgressLabel;
    @FXML
    private ProgressBar installProgressBar;
    @FXML
    private TextField serverFolderTextField;
    @FXML
    private TextField modpackTextField;
    private ZipFile modpackZipFile = null;

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
        SimpleServerWrapperModel model = getInternalModel();

        try (CurseModpack modpack = CurseModpack.createCurseModpack(modpackZipFile)) {
            ExecutorService service = Executors.newSingleThreadExecutor();
            service.submit(() -> modpack.install(Path.of(model.getServerPath()).getParent()));
            NamedExecutorService namedExecutorService = new NamedExecutorService("Modpack Install Service", service);
            modpack.on("download", args -> {
                CurseAddon mod = (CurseAddon) args[0];
                int modNumber = (int) args[1];
                int filesLength = (int) args[2];
                ThreadUtils.runOnFxThread(() -> {
                    installProgressLabel.setText(String.format("Mod %d/%d: %s", modNumber, filesLength, mod));
                    installProgressBar.setProgress((double) modNumber / filesLength);
                });
            });
            modpack.on("done", args -> {
                ThreadUtils.runOnFxThread(() -> installProgressLabel.setText("Done!"));
                ThreadUtils.tryShutdownNamedExecutorService(namedExecutorService);
            });
            modpack.on("error", args -> {
                Exception exception = (Exception) args[0];
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(exception.toString());
                alert.setContentText(exception.getLocalizedMessage());
                alert.show();
            });
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(e.toString());
            alert.setContentText(e.getLocalizedMessage());
            alert.show();
            e.printStackTrace();
        }
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
        modpackZipFile = new ZipFile(chosen);
    }
}
