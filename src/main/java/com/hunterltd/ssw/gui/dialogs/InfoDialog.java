package com.hunterltd.ssw.gui.dialogs;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serial;

public class InfoDialog extends JDialog {
    @Serial
    private static final long serialVersionUID = 1L;
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JLabel infoLabel;

    public InfoDialog(String title, String infoText) {
        this(title, infoText, "OK");
    }

    public InfoDialog(String title, String infoText, String okButtonText) {
        add(contentPane);
        setContentPane(contentPane);
        setModalityType(ModalityType.MODELESS);
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
