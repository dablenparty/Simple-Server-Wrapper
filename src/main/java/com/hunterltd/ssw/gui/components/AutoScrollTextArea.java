package com.hunterltd.ssw.gui.components;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.TextArea;

public class AutoScrollTextArea extends TextArea {
    private final BooleanProperty autoScroll;

    public AutoScrollTextArea() {
        autoScroll = new SimpleBooleanProperty(true);
    }

    public boolean isAutoScroll() {
        return autoScroll.get();
    }

    public void setAutoScroll(boolean autoScroll) {
        this.autoScroll.set(autoScroll);
    }

    public BooleanProperty autoScrollProperty() {
        return autoScroll;
    }
}
