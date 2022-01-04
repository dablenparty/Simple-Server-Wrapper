package com.hunterltd.ssw.gui.components;

import javafx.scene.control.Alert;

public class ErrorAlert {
    public static void showNewDialog(Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(e.toString());
        alert.setContentText(e.getLocalizedMessage());
        alert.show();
    }
}
