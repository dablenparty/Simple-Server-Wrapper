package com.hunterltd.ssw.gui.dialogs;

public class InternalErrorDialog extends InfoDialog {
    public InternalErrorDialog(Exception exception) {
        super(exception.getClass().getSimpleName(), "An internal error occurred: " + exception.getLocalizedMessage());
        this.pack();
        this.setVisible(true);
    }
}
