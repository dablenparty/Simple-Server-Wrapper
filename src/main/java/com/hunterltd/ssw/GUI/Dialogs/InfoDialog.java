package com.hunterltd.ssw.GUI.Dialogs;

import javax.swing.*;
import java.awt.event.*;

public class InfoDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JLabel infoLabel;

    public InfoDialog(String title, String infoText) {
        this(title, infoText, "OK");
    }

    public InfoDialog(String title, String infoText, String okButtonText) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        setTitle(title);

        infoLabel.setText(infoText);

        buttonOK.addActionListener(e -> onOK());
        buttonOK.setText(okButtonText);

        buttonCancel.addActionListener(e -> onCancel());

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> onCancel(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        );
    }

    protected void onOK() {
        dispose();
    }

    protected void onCancel() {
        dispose();
    }
}
