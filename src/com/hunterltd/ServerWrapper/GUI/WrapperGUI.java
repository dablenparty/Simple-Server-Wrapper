package com.hunterltd.ServerWrapper.GUI;

import com.hunterltd.ServerWrapper.GUI.Dialogs.InternalErrorDialog;
import com.hunterltd.ServerWrapper.GUI.Dialogs.SettingsDialog;
import com.hunterltd.ServerWrapper.Server.MinecraftServer;
import com.hunterltd.ServerWrapper.Server.StreamGobbler;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.function.Consumer;

public class WrapperGUI extends JFrame {
    private static final long serialVersionUID = 1L;
    private FileDialog serverFileInfo;
    private JButton openDialogButton;
    private JButton runButton;
    private JButton sendButton;
    private JPanel consolePanel;
    private JPanel rootPanel;
    private JScrollPane consoleScrollPane;
    private JScrollPane errorScrollPane;
    private JTabbedPane tabbedPane;
    private JTextArea consoleTextArea;
    private JTextArea errorTextArea;
    private JTextField commandTextField;
    private JTextField serverPathTextField;
    private JPanel errorPanel;
    private MinecraftServer server;
    private Timer timer;

    private final ActionListener timerListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (!server.isRunning()) {
                timer.stop();
                runButton.setText("Run");
            }
        }
    };

    public WrapperGUI() {
        ((DefaultCaret) consoleTextArea.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE); // Automatic scrolling

        sendButton.addActionListener(e -> sendCommand(commandTextField.getText()));
        openDialogButton.addActionListener(e -> selectNewFile());
        runButton.addActionListener(e -> runButtonAction());

        consolePanel.registerKeyboardAction(e -> {
                sendCommand(commandTextField.getText());
                commandTextField.setText("");
            },
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        //TODO: add menu bar for settings & other stuff
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File");
        MenuItem settingsItem = new MenuItem("Settings");
        fileMenu.add(settingsItem);
        menuBar.add(fileMenu);
        this.setMenuBar(menuBar);
        settingsItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SettingsDialog settings = new SettingsDialog();
                settings.pack();
                settings.setVisible(true);
            }
        });


        add(rootPanel);
    }

    private void sendCommand(String cmd) {
        try {
            server.sendCommand(cmd);
        } catch (IOException ioException) {
            ioException.printStackTrace();
            InternalErrorDialog errDialog = new InternalErrorDialog();
            errDialog.pack();
            errDialog.setVisible(true);
        }
    }

    private void startServer() {
        // Essentially flushes the output windows
        consoleTextArea.setText("");
        errorTextArea.setText("");
        try {
            server = new MinecraftServer(serverFileInfo.getDirectory(), serverFileInfo.getFile(), 4096, 4096).run();
        } catch (IOException e) {
            e.printStackTrace();
            InternalErrorDialog errDialog = new InternalErrorDialog();
            errDialog.pack();
            errDialog.setVisible(true);
            if (server != null && server.isRunning()) {
                server.getServerProcess().destroy();
            }
            return;
        }

        Consumer<String> addConsoleText = text -> consoleTextArea.setText(consoleTextArea.getText() + "\n" + text);
        Consumer<String> addErrorText = text -> errorTextArea.setText(errorTextArea.getText() + "\n" + text);

        // Pipes the server outputs into the GUI using the pre-defined consumers
        StreamGobbler.execute(server.getServerProcess().getInputStream(), addConsoleText);
        StreamGobbler.execute(server.getServerProcess().getErrorStream(), addErrorText);
        timer = new Timer(100, timerListener);
        timer.start();
        serverPathTextField.setEnabled(false);
        openDialogButton.setEnabled(false);
        commandTextField.setEnabled(true);
        sendButton.setEnabled(true);
        runButton.setText("Stop");
    }

    private void stopServer() {
        try {
            serverPathTextField.setEnabled(true);
            commandTextField.setEnabled(false);
            openDialogButton.setEnabled(true);
            sendButton.setEnabled(false);
            server.stop();
            timer.stop();
        } catch (IOException ignored) {
            // this usually happens when the stream is already closed but you try to send the stop command anyways
        }
        runButton.setText("Run");
    }

    private void runButtonAction() {
        // Server is null on initial startup
        if (server == null || !server.isRunning()) {
            startServer();
        } else {
            stopServer();
        }
    }

    private void selectNewFile() {
        FileDialog fd = new FileDialog(this, "Select your server.jar", FileDialog.LOAD);
        fd.setFilenameFilter((dir, name) -> name.endsWith(".jar"));
        fd.setVisible(true);
        serverPathTextField.setText(Paths.get(fd.getDirectory(), fd.getFile()).toString());
        serverFileInfo = fd;
    }

    public MinecraftServer getServer() {
        return server;
    }
}