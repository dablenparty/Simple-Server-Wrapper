package com.hunterltd.ServerWrapper.GUI.Dialogs;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentListener;
import java.util.Arrays;

public class SettingsDialog extends JDialog implements ActionListener, ChangeListener {
    private JPanel rootPanel;
    private JButton buttonSave;
    private JButton buttonCancel;
    private JPanel buttonPanel;
    private JPanel contentPanel;
    private JTabbedPane tabbedPane1;
    private JComboBox<Double> memoryComboBox;
    private JSlider memorySlider;
    private JCheckBox separateErrorTabCheckBox;
    private boolean unsavedChanges = false;

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

        memorySlider.addChangeListener(e -> memoryComboBox.setSelectedIndex(memorySlider.getValue() - 1));
        memorySlider.addChangeListener(this);
        memoryComboBox.addActionListener(this);
        separateErrorTabCheckBox.addActionListener(this);
    }

    private void onSave() {
        // add your code here
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    public static void main(String[] args) {
        SettingsDialog dialog = new SettingsDialog();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("ACTION: " + e.getSource().toString());
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        System.out.println("CHANGE: " + e.getSource().toString());
    }
}
