package com.hunterltd.ssw.gui.dialogs;

import com.hunterltd.ssw.server.MinecraftServer;
import com.hunterltd.ssw.server.properties.PropertiesTableModel;
import com.hunterltd.ssw.utilities.Settings;

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
    private JLabel restartIntervalLabel;
    private JSlider restartIntervalSlider;
    private JTextField extraArgsTextField;
    private JButton batchFileButton;
    private JScrollPane propsScrollPane;
    private JTable propsTable;
    private JCheckBox automaticShutdownCheckBox;
    private JComboBox<Integer> shutdownIntervalComboBox;
    private JLabel shutdownIntervalLabel;
    private JSlider shutdownIntervalSlider;
    private JPanel propsPanel;
    private JButton openPropsInEditorButton;
    private boolean directChange = true;
    private final Settings settings;
    private final MinecraftServer server;

    public SettingsDialog(MinecraftServer minecraftServer) {
        server = minecraftServer;

        add(rootPanel);

        settings = server.getServerSettings();
        setContentPane(rootPanel);
        setModal(true);
        getRootPane().setDefaultButton(buttonSave);
        setTitle("Server Settings - " + server.getServerPath().getFileName());

        buttonSave.addActionListener(e -> onSave());
        buttonCancel.addActionListener(e -> onCancel());

        for (double i = 0.5; i <= 16; i+=0.5) memoryComboBox.addItem(i);
        for (int i = 1; i <= 24; i++) restartIntervalComboBox.addItem(i);
        for (int i = 5; i <= 60; i+=5) shutdownIntervalComboBox.addItem(i);

        final String ext = System.getProperty("os.name").toLowerCase().contains("win") ? "bat" : "txt";
        batchFileButton.addActionListener(e -> server.generateBatch(ext));
        openPropsInEditorButton.addActionListener(e -> {
            try {
                java.awt.Desktop.getDesktop().edit(server.getProperties().getPropsFile());
            } catch (IOException ioException) {
                new InternalErrorDialog(ioException);
            }
        });

        memorySlider.addChangeListener(e -> updateComboBox(memoryComboBox, (JSlider) e.getSource()));
        memoryComboBox.addActionListener(e -> updateSlider((JComboBox<?>) e.getSource(), memorySlider));

        restartIntervalSlider.addChangeListener(e -> updateComboBox(restartIntervalComboBox, (JSlider) e.getSource()));
        restartIntervalComboBox.addActionListener(e -> updateSlider((JComboBox<?>) e.getSource(), restartIntervalSlider));

        shutdownIntervalSlider.addChangeListener(e -> updateComboBox(shutdownIntervalComboBox, (JSlider) e.getSource()));
        shutdownIntervalComboBox.addActionListener(e -> updateSlider((JComboBox<?>) e.getSource(), shutdownIntervalSlider));

        automaticRestartCheckBox.addChangeListener(e -> setPanelEnabled((JCheckBox) e.getSource(),
                new JComponent[]{restartIntervalLabel, restartIntervalSlider, restartIntervalComboBox}));
        automaticShutdownCheckBox.addChangeListener(e -> setPanelEnabled((JCheckBox) e.getSource(),
                new JComponent[]{shutdownIntervalLabel, shutdownIntervalSlider, shutdownIntervalComboBox}));

        // Set components based on UserSettings
        memoryComboBox.setSelectedIndex(((settings.getMemory() * 2) / 1024) - 1);
        automaticRestartCheckBox.setSelected(settings.getRestart());
        restartIntervalComboBox.setSelectedIndex(settings.getRestartInterval() - 1);
        automaticShutdownCheckBox.setSelected(settings.getShutdown());
        shutdownIntervalComboBox.setSelectedIndex((settings.getShutdownInterval() / 5) - 1);
        extraArgsTextField.setText(String.join(" ", settings.getExtraArgs()));
        if (server.propertiesExists() || server.updateProperties()) {
            propsTable.setModel(new PropertiesTableModel(server.getProperties()));
        } else {
            settingsTabs.removeTabAt(2); // remove properties tab
        }
    }

    @SuppressWarnings("unchecked")
    private void onSave() {
        settings.setMemory((int) ((memorySlider.getValue() / 2.0) * 1024));
        if ((automaticRestartCheckBox.isSelected() && !settings.getRestart())
                || (automaticShutdownCheckBox.isSelected() && !settings.getShutdown())) {
            InfoDialog dialog = new InfoDialog("Restart",
                    "The server must be restarted if it is running in order for these changes to take effect");
            dialog.pack();
            dialog.setVisible(true);
        }
        settings.setRestart(automaticRestartCheckBox.isSelected());
        settings.setRestartInterval(restartIntervalSlider.getValue());
        settings.setShutdown(automaticShutdownCheckBox.isSelected());
        settings.setShutdownInterval(shutdownIntervalSlider.getValue() * 5);

        settings.setExtraArgs(extraArgsTextField.getText().split(" "));

        try {
            settings.writeData();
        } catch (FileNotFoundException e) {
            new InternalErrorDialog(e);
        }

        // Saves properties from table
        if (server.propertiesExists()) {
            for (int i = 0; i < propsTable.getRowCount(); i++)
                server.getProperties().replace(propsTable.getValueAt(i, 0), propsTable.getValueAt(i, 1));

            try {
                server.getProperties().write();
            } catch (FileNotFoundException e) {
                new InternalErrorDialog(e);
            }
        }
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
}
