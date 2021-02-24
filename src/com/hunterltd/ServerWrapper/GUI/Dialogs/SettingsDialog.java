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
    private JCheckBox automaticRestartCheckBox;
    private JComboBox<Integer> restartIntervalComboBox;
    private JLabel memoryLabel;
    private JLabel intervalLabel;
    private JSlider restartIntervalSlider;
    private JTextField extraArgsTextField;
    private boolean directChange = true;

    public SettingsDialog() {
        setContentPane(rootPanel);
        setModal(true);
        getRootPane().setDefaultButton(buttonSave);
        setTitle("Server Settings");

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

        // Set components based on UserSettings
        memoryComboBox.setSelectedIndex(((UserSettings.getMemory() * 2) / 1024) - 1);
        automaticRestartCheckBox.setSelected(UserSettings.getRestart());
        restartIntervalComboBox.setSelectedIndex(UserSettings.getInterval() - 1);
        extraArgsTextField.setText(String.join(" ", UserSettings.getExtraArgs()));
    }

    private void onSave() {
        UserSettings.setMemory((int) ((memorySlider.getValue() / 2.0) * 1024));
        if (automaticRestartCheckBox.isSelected() &&
                !UserSettings.getRestart()) {
            int result = JOptionPane.showConfirmDialog(
                    this,
                    "The server needs to be restarted to enable the auto-restart feature. Would you like to save anyways?",
                    "Unsaved changes",
                    JOptionPane.YES_NO_OPTION
            );
            if (result == JOptionPane.YES_OPTION) {
                UserSettings.setRestart(automaticRestartCheckBox.isSelected());
                UserSettings.setInterval(restartIntervalSlider.getValue());
            }
        } else {
            UserSettings.setRestart(automaticRestartCheckBox.isSelected());
            UserSettings.setInterval(restartIntervalSlider.getValue());
        }

        UserSettings.setExtraArgs(extraArgsTextField.getText().split(" "));

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
