package com.hunterltd.ssw.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ConfirmationDialog extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ConfirmationDialog.class.getResource("confirmation-dialog.fxml"));
        stage.setTitle("Simple Server Wrapper");
        stage.setScene(new Scene(fxmlLoader.load(), 320, 240));
        stage.show();
    }
}
