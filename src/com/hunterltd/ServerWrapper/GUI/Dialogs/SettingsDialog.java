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
    private JCheckBox automaticRestartCheckBox;
    private JComboBox<Integer> restartIntervalComboBox;
    private JLabel intervalLabel;
    private JSlider restartIntervalSlider;
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
        for (double i = 0.5; i <= 16; i+=0.5) memoryComboBox.addItem(i);
        for (int i = 1; i <= 24; i++) restartIntervalComboBox.addItem(i);

        memorySlider.addChangeListener(e -> updateComboBox(memoryComboBox, (JSlider) e.getSource()));
        memoryComboBox.addActionListener(e -> updateSlider((JComboBox<?>) e.getSource(), memorySlider));

        restartIntervalSlider.addChangeListener(e -> updateComboBox(restartIntervalComboBox, (JSlider) e.getSource()));
        restartIntervalComboBox.addActionListener(e -> updateSlider((JComboBox<?>) e.getSource(), restartIntervalSlider));

        automaticRestartCheckBox.addActionListener(e -> {
            boolean isSelected = automaticRestartCheckBox.isSelected();
            intervalLabel.setEnabled(isSelected);
            restartIntervalComboBox.setEnabled(isSelected);
            restartIntervalSlider.setEnabled(isSelected);
        });
    }

    private void onSave() {
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    private void updateSlider(JComboBox<?> comboBox, JSlider slider) {
        if (directChange) {
            slider.setValue(comboBox.getSelectedIndex() + 1);
        }
    }

    private void updateComboBox(JComboBox<?> comboBox, JSlider slider) {
        directChange = false;
        comboBox.setSelectedIndex(slider.getValue() - 1);
        directChange = true;
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
