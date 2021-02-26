package com.hunterltd.ServerWrapper.GUI.Dialogs;

import com.hunterltd.ServerWrapper.Utilities.Settings;

import javax.swing.*;
import java.io.FileNotFoundException;
import java.io.IOException;

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
    private JButton batchFileButton;
    private boolean directChange = true;
    private final Settings settings;

    public SettingsDialog(Settings settingsObj, String filename) {
        settings = settingsObj;
        setContentPane(rootPanel);
        setModal(true);
        getRootPane().setDefaultButton(buttonSave);
        setTitle("Server Settings - " + filename);

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
        memoryComboBox.setSelectedIndex(((settings.getMemory() * 2) / 1024) - 1);
        automaticRestartCheckBox.setSelected(settings.getRestart());
        restartIntervalComboBox.setSelectedIndex(settings.getInterval() - 1);
        extraArgsTextField.setText(String.join(" ", settings.getExtraArgs()));
    }

    private void onSave() {
        settings.setMemory((int) ((memorySlider.getValue() / 2.0) * 1024));
        if (automaticRestartCheckBox.isSelected() &&
                !settings.getRestart()) {
            int result = JOptionPane.showConfirmDialog(
                    this,
                    "If a server is running, it will need to be restarted for these changes to take effect. " +
                            "Would you still like to save now?",
                    "Unsaved changes",
                    JOptionPane.YES_NO_OPTION
            );
            if (result == JOptionPane.YES_OPTION) {
                settings.setRestart(automaticRestartCheckBox.isSelected());
                settings.setInterval(restartIntervalSlider.getValue());
            }
        } else {
            settings.setRestart(automaticRestartCheckBox.isSelected());
            settings.setInterval(restartIntervalSlider.getValue());
        }

        settings.setExtraArgs(extraArgsTextField.getText().split(" "));

        try {
            settings.writeData();
        } catch (FileNotFoundException e) {
            InfoDialog errorDialog = new InfoDialog("Settings not found",
                    "The settings file could not be found. Ensure that it has not been moved or deleted");
        }
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

    public static void main(String[] args) throws IOException {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        SettingsDialog dialog = new SettingsDialog(new Settings("test"), "testfile.jar");
        dialog.pack();
        dialog.setVisible(true);
    }
}
