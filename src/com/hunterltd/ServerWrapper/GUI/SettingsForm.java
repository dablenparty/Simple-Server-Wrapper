package com.hunterltd.ServerWrapper.GUI;

import javax.swing.*;

public class SettingsForm {
    private JTabbedPane settingsTabbedPane;
    private JPanel rootPanel;
    private JPanel generalSettingsPanel;
    private JComboBox<Integer> memoryComboBox;

    public SettingsForm() {
        /* TODO:
            - default server
            - automatic server detection
            - auto-restart on interval
            - custom memory allocation
            - split error pane
        */
        for (int i = 1; i <= 16; i++) {
            memoryComboBox.addItem(i/2);
            memoryComboBox.addItem(i);
        }
    }
}
