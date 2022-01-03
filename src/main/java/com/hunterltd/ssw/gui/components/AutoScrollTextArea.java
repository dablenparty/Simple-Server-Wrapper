package com.hunterltd.ssw.gui.components;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.TextArea;

public class AutoScrollTextArea extends TextArea {
    private final BooleanProperty autoScroll;

    public AutoScrollTextArea() {
        autoScroll = new SimpleBooleanProperty(true);
    }

    @Override
    public void appendText(String s) {
        if (autoScroll.get())
            super.appendText(s);
        else {
            double scrollTop = getScrollTop();
            setText(getText() + s);
            setScrollTop(scrollTop);
        }
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
