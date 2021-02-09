package com.hunterltd.ServerWrapper.GUI;

import com.hunterltd.ServerWrapper.Server.MinecraftServer;
import com.hunterltd.ServerWrapper.Server.StreamGobbler;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class WrapperGUI extends JFrame {
    private JTabbedPane consolePane;
    private JPanel rootPanel;
    private JTextArea consoleTextArea;
    private JTextField commandTextField;
    private JButton sendButton;
    private JScrollPane consoleScrollPane;
    private MinecraftServer server;

    public WrapperGUI() {
        add(rootPanel);
        consoleTextArea.setEditable(false);
        consoleTextArea.setLineWrap(true);
        ((DefaultCaret) consoleTextArea.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE); // Automatic scrolling

        FileDialog fd = new FileDialog(this, "Select your server.jar", FileDialog.LOAD);
        fd.setFilenameFilter((dir, name) -> name.endsWith(".jar"));
        fd.setVisible(true);
        String filename = fd.getFile();
        if (filename == null) {
            System.exit(0);
        }
        try {
            server = new MinecraftServer(fd.getDirectory(), filename, 4096, 4096);
        } catch (IOException e) {
            //TODO: dialog that the server couldn't be opened
            e.printStackTrace();
            System.exit(1);
        }
        Consumer<String> addText = text -> consoleTextArea.setText(consoleTextArea.getText() + "\n" + text);
        StreamGobbler gobbler = new StreamGobbler(server.getServerProcess().getInputStream(), addText);
        Executors.newSingleThreadExecutor().submit(gobbler);

        sendButton.addActionListener(e -> sendCommand(consoleTextArea.getText()));
        consolePane.registerKeyboardAction(e -> {
                sendCommand(commandTextField.getText());
                commandTextField.setText("");
            },
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }
    private void sendCommand(String cmd) {
        try {
            server.sendCommand(cmd);
        } catch (IOException ioException) {
            //TODO: dialog that the command couldn't be sent
            ioException.printStackTrace();
        }
    }

    public MinecraftServer getServer() {
        return server;
    }
}
