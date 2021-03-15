package com.hunterltd.ssw.gui;

import com.hunterltd.ssw.gui.dialogs.InfoDialog;
import com.hunterltd.ssw.gui.dialogs.InternalErrorDialog;
import com.hunterltd.ssw.gui.dialogs.SettingsDialog;
import com.hunterltd.ssw.server.MinecraftServer;
import com.hunterltd.ssw.server.StreamGobbler;
import com.hunterltd.ssw.utilities.Settings;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
    private JTabbedPane tabbedPane;
    private JTextArea consoleTextArea;
    private JTextField commandTextField;
    private JTextField serverPathTextField;
    private MinecraftServer server;
    private Timer aliveTimer, restartTimer;
    private int[] timeCounter = new int[]{0, 0, 0}; // H:M:S
    private final String restartCommandTemplate = "me %sis restarting in %d %s!"; // color code, time integer, time unit
    private final String baseTitle = "Simple Server Wrapper";
    private final ActionListener settingsWarn = e -> {
        InfoDialog dialog = new InfoDialog("No server selected",
                "A server must be selected to change its settings!");
        dialog.pack();
        dialog.setVisible(true);
    };
    private ActionListener settingsOpen;
    private Settings serverSettings;
    private SwingWorker<Void, Void> serverWorker;

    public WrapperGUI() {
        ((DefaultCaret) consoleTextArea.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE); // Automatic scrolling

        // Action Listeners
        sendButton.addActionListener(e -> sendCommand(commandTextField.getText()));
        openDialogButton.addActionListener(e -> selectNewFile());
        runButton.addActionListener(e -> runButtonAction());

        // Keyboard Registers
        rootPanel.registerKeyboardAction(e -> {
                sendCommand(commandTextField.getText());
                commandTextField.setText("");
            },
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        // Menu Bar
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File");
        MenuItem settingsItem = new MenuItem("Server Settings"),
                curseInstallItem = new MenuItem("Install CurseForge Modpack");
        fileMenu.add(settingsItem);
        fileMenu.add(curseInstallItem);
        menuBar.add(fileMenu);
        settingsItem.addActionListener(settingsWarn);
        curseInstallItem.addActionListener(e -> {
            CurseInstaller installer = new CurseInstaller();
            installer.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            installer.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    SwingWorker<Void, Void> worker = installer.getWorker();
                    if (worker != null) {
                        worker.cancel(true);
                    }
                    installer.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
                }
            });
            installer.pack();
            installer.setVisible(true);
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

    public void startServer() {

        serverWorker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                // Essentially flushes the output windows
                consoleTextArea.setText("");
                server.updateProperties();
                try {
                    server.run();
                } catch (IOException e) {
                    e.printStackTrace();
                    InternalErrorDialog errDialog = new InternalErrorDialog();
                    errDialog.pack();
                    errDialog.setVisible(true);
                    if (server != null && server.isRunning()) {
                        server.getServerProcess().destroy();
                    }
                    return null;
                }

                Consumer<String> addConsoleText = text -> firePropertyChange("newLine", "", text);

                // Pipes the server outputs into the GUI using the pre-defined consumers
                StreamGobbler.execute(server.getServerProcess().getInputStream(), addConsoleText);
                StreamGobbler.execute(server.getServerProcess().getErrorStream(), addConsoleText);

                firePropertyChange("start", false, true);
                return null;
            }
        };

        serverWorker.addPropertyChangeListener(evt -> {
            switch (evt.getPropertyName()) {
                case "newLine":
                    consoleTextArea.setText(consoleTextArea.getText() + '\n' + evt.getNewValue());
                    break;
                case "start":
                    boolean start = (boolean) evt.getNewValue();
                    if (server.isRunning()) {
                        aliveTimer.start();
                        if (restartTimer != null) restartTimer.start();
                    }
                    serverPathTextField.setEnabled(!start);
                    openDialogButton.setEnabled(!start);
                    commandTextField.setEnabled(start);
                    sendButton.setEnabled(start);
                    String runText, title;
                    if (start) {
                        runText = "Stop";
                        title = baseTitle + " - " + serverFileInfo.getFile();
                    } else {
                        runText = "Run";
                        title = baseTitle;

                        try {
                            server.stop();
                            consoleTextArea.setText(consoleTextArea.getText() + "\nServer has been stopped.");
                        } catch (IOException e) {
                            server.getServerProcess().destroy();
                            InfoDialog dialog = new InfoDialog("Force stop server",
                                    "An internal IO error occurred, and the server had to be forcibly shut down");
                            dialog.pack();
                            dialog.setVisible(true);
                        }
                    }
                    runButton.setText(runText);
                    setTitle(title);
                    break;
            }
        });
        serverWorker.execute();
    }

    public void stopServer() {
        serverWorker.firePropertyChange("start", true, false);
    }

    private void runButtonAction() {
        // Server is null on initial startup
        if (server.getServerProcess() == null || !server.isRunning()) {
            startServer();
        } else {
            stopServer();
        }
    }

    private void selectNewFile() {
        serverFileInfo = new FileDialog(this, "Select your server.jar", FileDialog.LOAD);
        serverFileInfo.setFilenameFilter((dir, name) -> name.endsWith(".jar"));
        serverFileInfo.setVisible(true);
        MenuItem settingsItem = this.getMenuBar().getMenu(0).getItem(0);
        settingsItem.removeActionListener(settingsOpen);
        settingsOpen = e -> {
            SettingsDialog settingsDialog = new SettingsDialog(server);
            settingsDialog.pack();
            settingsDialog.setVisible(true);
        };
        try {
            serverPathTextField.setText(Paths.get(serverFileInfo.getDirectory(), serverFileInfo.getFile()).toString());
            serverSettings = new Settings(Paths.get(serverFileInfo.getDirectory(), "ssw", "wrapperSettings.json"));
            settingsItem.removeActionListener(settingsWarn);
            settingsItem.addActionListener(settingsOpen);
            server = new MinecraftServer(serverFileInfo.getDirectory(),
                    serverFileInfo.getFile(),
                    serverSettings);
        } catch (NullPointerException ignored) {
        } catch (IOException e) {
            InternalErrorDialog errorDialog = new InternalErrorDialog();
            errorDialog.pack();
            errorDialog.setVisible(true);
        }

        aliveTimer = new Timer(100, e -> {
            if (!server.isRunning()) {
                aliveTimer.stop();
                serverPathTextField.setEnabled(true);
                commandTextField.setEnabled(false);
                openDialogButton.setEnabled(true);
                sendButton.setEnabled(false);
                runButton.setText("Run");
                setTitle(baseTitle);
                if (restartTimer != null) restartTimer.stop();
            }
        });

        // Keeps track of every second in a 3 element array
        restartTimer = serverSettings.getRestart() ? new Timer(1000, e -> {
            final int interval = serverSettings.getInterval();
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
                stopServer();
                startServer();
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

    }

    public MinecraftServer getServer() {
        return server;
    }
}
