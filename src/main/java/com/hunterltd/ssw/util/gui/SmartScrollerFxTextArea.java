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
        System.out.println(scrollTop);
        double maximum = calculateMaximumScroll(scrollTop);
        if (scrollTop == previousMaximumScrollValue) {
            previousMaximumScrollValue = maximum;
            previousScrollValue = scrollTop;
            textArea.selectPositionCaret(textArea.getLength());
            textArea.deselect();
            // auto scroll
        } else {
            // do nothing
        }
    }

    private double calculateMaximumScroll(double oldPosition) {
        // scroll to bottom
        textArea.selectPositionCaret(textArea.getLength());
        textArea.deselect();
        double maximum = textArea.getScrollTop();
        textArea.setScrollTop(oldPosition);
        return maximum;
    }
}
