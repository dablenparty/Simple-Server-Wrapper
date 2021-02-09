package com.hunterltd.ServerWrapper.GUI;

import com.hunterltd.ServerWrapper.Server.MinecraftServer;
import com.hunterltd.ServerWrapper.Server.StreamGobbler;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class WrapperGUI extends JFrame {
    private FileDialog serverInfo;
    private JButton openDialogButton;
    private JButton runButton;
    private JButton sendButton;
    private JPanel rootPanel;
    private JScrollPane consoleScrollPane;
    private JTabbedPane consolePane;
    private JTextField commandTextField;
    private JTextArea consoleTextArea;
    private JTextField serverPathTextField;
    private MinecraftServer server;

    public WrapperGUI() {
        add(rootPanel);
        ((DefaultCaret) consoleTextArea.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE); // Automatic scrolling

        sendButton.addActionListener(e -> sendCommand(consoleTextArea.getText()));
        openDialogButton.addActionListener(e -> selectNewFile());
        runButton.addActionListener(e -> runButtonAction());

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

    private void startServer() {
        consoleTextArea.setText(""); // Essentially flushes the console output window
        runButton.setText("Stop");
        try {
            server = new MinecraftServer(serverInfo.getDirectory(), serverInfo.getFile(), 4096, 4096).run();
        } catch (IOException e) {
            //TODO: dialog that the server couldn't be opened
            e.printStackTrace();
            return;
        }
        Consumer<String> addText = text -> consoleTextArea.setText(consoleTextArea.getText() + "\n" + text);
        StreamGobbler gobbler = new StreamGobbler(server.getServerProcess().getInputStream(), addText);
        Executors.newSingleThreadExecutor().submit(gobbler);
    }

    private void stopServer() {
        runButton.setText("Run");
        try {
            server.stop();
        } catch (IOException ignored) {
            // this usually happens when the stream is already closed but you try to send the stop command anyways
        }
    }

    private void runButtonAction() {
        // Server is null on initial startup
        if (server == null || !server.isRunning()) {
            startServer();
        } else {
            stopServer();
        }
        server.setRunning(!server.isRunning());
        System.out.println(server.isRunning());
    }

    private void selectNewFile() {
        FileDialog fd = new FileDialog(this, "Select your server.jar", FileDialog.LOAD);
        fd.setFilenameFilter((dir, name) -> name.endsWith(".jar"));
        fd.setVisible(true);
        serverPathTextField.setText(Paths.get(fd.getDirectory(), fd.getFile()).toString());
        serverInfo = fd;
    }

    public MinecraftServer getServer() {
        return server;
    }
}
