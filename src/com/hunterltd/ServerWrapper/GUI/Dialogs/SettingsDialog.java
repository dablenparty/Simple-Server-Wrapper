package com.hunterltd.ServerWrapper.GUI.Dialogs;

import javax.swing.*;

public class SettingsDialog extends JDialog {
    private JPanel rootPanel;
    private JButton buttonSave;
    private JButton buttonCancel;
    private JPanel buttonPanel;
    private JPanel contentPanel;
    private JTabbedPane settingsTabs;
    private JComboBox<Double> memoryComboBox;
    private JSlider memorySlider;
    private JCheckBox separateErrorTabCheckBox;
    private JLabel memoryLabel;
    private boolean directChange = true;

    public SettingsDialog() {
        setContentPane(rootPanel);
        setModal(true);
        getRootPane().setDefaultButton(buttonSave);

        buttonSave.addActionListener(e -> onSave());
        buttonCancel.addActionListener(e -> onCancel());

        /* TODO:
            - default server
            - automatic server detection
            - auto-restart on interval
            - custom memory allocation
            - split error pane */
        for (double i = 0.5; i <= 16; i+=0.5) {
            memoryComboBox.addItem(i);
        }

        memorySlider.addChangeListener(e -> {
            directChange = false;
            memoryComboBox.setSelectedIndex(memorySlider.getValue() - 1);
            directChange = true;
        });

        memoryComboBox.addActionListener(e -> {
            if (directChange) {
                memorySlider.setValue(memoryComboBox.getSelectedIndex() + 1);
            }
        });
    }

    private void onSave() {
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        SettingsDialog dialog = new SettingsDialog();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
