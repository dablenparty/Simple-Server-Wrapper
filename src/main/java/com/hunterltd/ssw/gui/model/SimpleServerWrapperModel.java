package com.hunterltd.ssw.gui.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class SimpleServerWrapperModel {
    private final StringProperty outputtedText;

    public SimpleServerWrapperModel() {
        outputtedText = new SimpleStringProperty();
    }

    public String getOutputtedText() {
        return outputtedText.get();
    }

    public StringProperty outputtedTextProperty() {
        return outputtedText;
    }

    public void setOutputtedText(String outputtedText) {
        this.outputtedText.set(outputtedText);
    }

    public void appendToOutputtedText(String text) {
        this.outputtedText.set(getOutputtedText() + text);
    }
}
