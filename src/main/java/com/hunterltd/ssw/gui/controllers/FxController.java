package com.hunterltd.ssw.gui.controllers;

import com.hunterltd.ssw.gui.model.SimpleServerWrapperModel;

public abstract class FxController {
    private final SimpleServerWrapperModel model;

    public FxController(SimpleServerWrapperModel model) {
        this.model = model;
    }

    public abstract void initialize();

    protected final SimpleServerWrapperModel getInternalModel() {
        return model;
    }
}
