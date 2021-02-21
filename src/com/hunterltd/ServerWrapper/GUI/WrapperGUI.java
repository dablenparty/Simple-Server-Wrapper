package com.hunterltd.ServerWrapper.GUI;

import com.hunterltd.ServerWrapper.GUI.Dialogs.InternalErrorDialog;
import com.hunterltd.ServerWrapper.GUI.Dialogs.SettingsDialog;
import com.hunterltd.ServerWrapper.Server.MinecraftServer;
import com.hunterltd.ServerWrapper.Server.StreamGobbler;
import com.hunterltd.ServerWrapper.Utilities.UserSettings;

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
    private Timer aliveTimer, restartTimer;

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

        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File");
        MenuItem settingsItem = new MenuItem("Settings");
        fileMenu.add(settingsItem);
        menuBar.add(fileMenu);
        settingsItem.addActionListener(e -> {
            SettingsDialog settings = new SettingsDialog();
            settings.pack();
            settings.setVisible(true);
        });
        this.setMenuBar(menuBar);

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
            server = new MinecraftServer(serverFileInfo.getDirectory(),
                    serverFileInfo.getFile(),
                    UserSettings.getMemory(),
                    UserSettings.getMemory()
            ).run();
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
        aliveTimer = new Timer(100, e -> {
            if (!server.isRunning()) {
                aliveTimer.stop();
                runButton.setText("Run");
                if (restartTimer != null) restartTimer.stop();
            }
        });

        // A new action listener is created for each new restartTimer in case the user changes the restart interval
        // and wants to enact it by simply restarting the server rather than the entire wrapper.
        // Currently set to 10's of seconds (aka 3 in settings means 30 seconds)
        restartTimer = UserSettings.getRestart() ? new Timer(UserSettings.getInterval() * 10000, e -> {
            restartTimer.stop();
            stopServer();
            startServer();
        }) : null;

        aliveTimer.start();
        restartTimer.start();
        serverPathTextField.setEnabled(false);
        openDialogButton.setEnabled(false);
        commandTextField.setEnabled(true);
        sendButton.setEnabled(true);
        runButton.setText("Stop");
    }

    private void stopServer() {
        try {
            server.stop();
        } catch (IOException e) {
            server.getServerProcess().destroy();
        }
        serverPathTextField.setEnabled(true);
        commandTextField.setEnabled(false);
        openDialogButton.setEnabled(true);
        sendButton.setEnabled(false);

        aliveTimer.stop();
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
        try {
            serverPathTextField.setText(Paths.get(fd.getDirectory(), fd.getFile()).toString());
            serverFileInfo = fd;
        } catch (NullPointerException ignored) {}
    }

    public MinecraftServer getServer() {
        return server;
    }
}
