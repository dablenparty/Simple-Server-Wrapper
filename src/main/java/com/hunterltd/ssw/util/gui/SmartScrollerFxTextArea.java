package com.hunterltd.ssw.util.gui;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextArea;

public class SmartScrollerFxTextArea implements ChangeListener<String> {
    private final TextArea textArea;
    private boolean hasScrolled = false;
    private double previousScrollValue = -1;
    private double previousMaximumScrollValue = -1;

    public SmartScrollerFxTextArea(TextArea textArea) {
        this.textArea = textArea;
    }

    @Override
    public void changed(ObservableValue<? extends String> observableValue, String oldValue, String newValue) {
        double scrollTop = textArea.getScrollTop();
        double maximum = calculateMaximumScroll(scrollTop);
        System.out.println(maximum);
        if (scrollTop == previousMaximumScrollValue) {
            previousMaximumScrollValue = maximum;
            previousScrollValue = scrollTop;
            textArea.selectEnd();
            textArea.deselect();
            // auto scroll
        } else {
            // do nothing
        }
    }

    private double calculateMaximumScroll(double oldPosition) {
        // scroll to bottom
        textArea.setScrollTop(Double.MAX_VALUE);
        double maximum = textArea.getScrollTop();
        textArea.setScrollTop(oldPosition);
        return maximum;
    }
}
