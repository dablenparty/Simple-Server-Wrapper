package com.hunterltd.ssw.gui;

import com.hunterltd.ssw.gui.controllers.SimpleServerWrapperController;
import com.hunterltd.ssw.gui.model.SimpleServerWrapperModel;
import com.hunterltd.ssw.minecraft.VersionManifestV2;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SimpleServerWrapperGui extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        VersionManifestV2.load();
        final SimpleServerWrapperModel model = new SimpleServerWrapperModel();
        FXMLLoader fxmlLoader = new FXMLLoader(SimpleServerWrapperGui.class.getResource("simple-server-wrapper-view.fxml"));
        fxmlLoader.setControllerFactory(aClass -> new SimpleServerWrapperController(model));
        primaryStage.setTitle("Simple Server Wrapper");
        primaryStage.setScene(new Scene(fxmlLoader.load(), 320, 240));
        primaryStage.show();
    }
}
