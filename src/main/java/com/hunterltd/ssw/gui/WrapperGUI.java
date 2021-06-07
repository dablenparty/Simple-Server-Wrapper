package com.hunterltd.ssw.gui;

import javax.swing.*;

public class WrapperGUI extends JFrame {
    private static final long serialVersionUID = 1L;
    private JFileChooser serverFileInfo;
    private JButton openDialogButton;
    private JButton runButton;
    private JButton sendButton;
    private JPanel consolePanel;
    private JPanel rootPanel;
    private JScrollPane consoleScrollPane;
    private JTabbedPane tabbedPane;
    private JTextArea consoleTextArea;
    private JTextField commandTextField;
    private JTextField serverPathTextField;

    public WrapperGUI() {
        add(rootPanel);
    }
}
