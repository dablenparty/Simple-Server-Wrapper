package com.hunterltd.ssw.gui.components;

import javafx.scene.control.TextArea;

public class SmartScrollTextArea extends TextArea {
    private double previousMaximum = 0.0;

    @Override
    public void appendText(String s) {
        double currentScrollTop = getScrollTop();
        if (currentScrollTop < previousMaximum - 10) {
            setText(getText() + s);
            setScrollTop(Double.MAX_VALUE);
            // doesn't always go all the way down, but good enough
            previousMaximum = getScrollTop();
            setScrollTop(currentScrollTop);
        } else {
            super.appendText(s);
            previousMaximum = getScrollTop();
        }
    }
}
