package com.hunterltd.ServerWrapper.GUI.Dialogs;

import com.hunterltd.ServerWrapper.Utilities.UserSettings;

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
    private JCheckBox automaticRestartCheckBox;
    private JComboBox<Integer> restartIntervalComboBox;
    private JLabel memoryLabel;
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

        automaticRestartCheckBox.addChangeListener(e -> setPanelEnabled((JCheckBox) e.getSource(),
                new JComponent[]{intervalLabel, restartIntervalSlider, restartIntervalComboBox}));

        memoryComboBox.setSelectedIndex(((UserSettings.getMemory() * 2) / 1024) - 1);
        separateErrorTabCheckBox.setSelected(UserSettings.getErrorTab());
        automaticRestartCheckBox.setSelected(UserSettings.getRestart());
        restartIntervalComboBox.setSelectedIndex(UserSettings.getInterval() - 1);
    }

    private void onSave() {
        UserSettings.setMemory((int) ((memorySlider.getValue() / 2.0) * 1024));
        UserSettings.setErrorTab(separateErrorTabCheckBox.isSelected());
        UserSettings.setRestart(automaticRestartCheckBox.isSelected());
        UserSettings.setInterval(restartIntervalSlider.getValue());

        UserSettings.save();
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

    private void setPanelEnabled(JCheckBox checkBox, JComponent[] components) {
        boolean checked = checkBox.isSelected();
        for (JComponent comp :
                components) {
            comp.setEnabled(checked);
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
    }
}
