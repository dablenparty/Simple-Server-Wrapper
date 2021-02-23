package com.hunterltd.ServerWrapper.GUI;

import com.hunterltd.ServerWrapper.GUI.Dialogs.InternalErrorDialog;
import com.hunterltd.ServerWrapper.GUI.Dialogs.SettingsDialog;
import com.hunterltd.ServerWrapper.Server.MinecraftServer;
import com.hunterltd.ServerWrapper.Server.StreamGobbler;
import com.hunterltd.ServerWrapper.Utilities.UserSettings;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
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
    private int[] timeCounter = new int[]{0, 0, 0}; // H:M:S
    private final String restartCommandTemplate = "me %sis restarting in %d %s!"; // color code, time integer, time unit
    private final String baseTitle = "Simple Server Wrapper";

    public WrapperGUI() {
        ((DefaultCaret) consoleTextArea.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE); // Automatic scrolling

        // Action Listeners
        sendButton.addActionListener(e -> sendCommand(commandTextField.getText()));
        openDialogButton.addActionListener(e -> selectNewFile());
        runButton.addActionListener(e -> runButtonAction());

        // Keyboard Registers
        consolePanel.registerKeyboardAction(e -> {
                sendCommand(commandTextField.getText());
                commandTextField.setText("");
            },
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        // Menu Bar
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

        setTitle(baseTitle);

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
                setTitle(baseTitle);
                if (restartTimer != null) restartTimer.stop();
            }
        });

        // Keeps track of every second in a 3 element array
        restartTimer = UserSettings.getRestart() ? new Timer(1000, e -> {
            final int interval = UserSettings.getInterval();
            int hours = timeCounter[0], minutes = timeCounter[1], seconds = timeCounter[2];

            seconds++;

            if (seconds != 60) {
                timeCounter[2] = seconds;
            } else {
                minutes++;
                timeCounter[1] = minutes;
                timeCounter[2] = 0; // resets "seconds" counter
            }

            if (minutes == 60) {
                hours++;
                timeCounter[0] = hours;
                timeCounter[1] = 0; // resets "minutes" counter
                sendCommand(String.format(restartCommandTemplate, "§7", interval - hours, "hours")); // gray
            }

            if (hours == interval) {
                restartTimer.stop();
                this.stopServer();
                this.startServer();
                timeCounter = new int[]{0, 0, 0};
            } else if (hours == interval - 1) {
                // the final hour
                // 60 is actually the edge case because it doesn't get reset to 0 until the next iteration
                switch (minutes) {
                    case 30:
                    case 45:
                    case 50:
                    case 55:
                        if (seconds == 60) {
                            sendCommand(String.format(restartCommandTemplate, "§e", 60 - minutes, "minutes")); // yellow
                        }
                        break;
                    case 59:
                        // the final minute
                        switch (seconds) {
                            case 30:
                            case 50:
                                sendCommand(String.format(restartCommandTemplate, "§c", 60 - seconds, "seconds")); // red
                                break;
                            case 60:
                                sendCommand(String.format(restartCommandTemplate, "§c", 1, "minute")); // red
                                break;
                        }
                        break;
                }
            }
        }) : null;

        aliveTimer.start();
        if (restartTimer != null) restartTimer.start();
        serverPathTextField.setEnabled(false);
        openDialogButton.setEnabled(false);
        commandTextField.setEnabled(true);
        sendButton.setEnabled(true);
        runButton.setText("Stop");
        setTitle(baseTitle + " - " + serverFileInfo.getFile());
    }

    private void stopServer() {
        try {
            server.stop();
            consoleTextArea.setText(consoleTextArea.getText() + "\nServer has been stopped.");
        } catch (IOException e) {
            server.getServerProcess().destroy();
        }
        serverPathTextField.setEnabled(true);
        commandTextField.setEnabled(false);
        openDialogButton.setEnabled(true);
        sendButton.setEnabled(false);

        runButton.setText("Run");
        setTitle(baseTitle);
        aliveTimer.stop();
        if (restartTimer != null) restartTimer.stop();
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
        } catch (NullPointerException ignored) {
            // Thrown when the user clicks "Cancel" in the dialog. Can be ignored
        }
    }

    public MinecraftServer getServer() {
        return server;
    }
}
