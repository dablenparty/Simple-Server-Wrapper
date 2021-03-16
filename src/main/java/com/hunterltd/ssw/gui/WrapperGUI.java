package com.hunterltd.ssw.gui;

import com.hunterltd.ssw.gui.dialogs.InfoDialog;
import com.hunterltd.ssw.gui.dialogs.InternalErrorDialog;
import com.hunterltd.ssw.gui.dialogs.SettingsDialog;
import com.hunterltd.ssw.server.MinecraftServer;
import com.hunterltd.ssw.server.StreamGobbler;
import com.hunterltd.ssw.utilities.Settings;
import com.hunterltd.ssw.utilities.SmartScroller;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.function.Consumer;

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
    private MinecraftServer server;
    private Timer aliveTimer, restartTimer;
    private int[] timeCounter = new int[]{0, 0, 0}; // H:M:S
    private final String restartCommandTemplate = "me %sis restarting in %d %s!"; // color code, time integer, time unit
    private final String baseTitle = "Simple Server Wrapper";
    private final ActionListener noServerSelected = e -> {
        InfoDialog dialog = new InfoDialog("No server selected",
                "A server must be selected to do this!");
        dialog.pack();
        dialog.setVisible(true);
    };
    private ActionListener settingsOpen, openInFolder;
    private Settings serverSettings;

    public WrapperGUI() {
        new SmartScroller(consoleScrollPane);

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
        Menu fileMenu = new Menu("File"),
                serverMenu = new Menu("Server");
        MenuItem settingsItem = new MenuItem("Server Settings"),
                openInFolderItem = new MenuItem("Open in folder"),
                curseInstallItem = new MenuItem("Install CurseForge Modpack");
        fileMenu.add(curseInstallItem);
        serverMenu.add(settingsItem);
        serverMenu.add(openInFolderItem);
        menuBar.add(fileMenu);
        menuBar.add(serverMenu);
        serverMenu.addActionListener(noServerSelected);
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
            new InternalErrorDialog(ioException);
        }
    }

    public void startServer() {
        consoleTextArea.setText("");
        // Essentially flushes the output windows
        server.updateProperties();
        try {
            server.run();
        } catch (IOException e) {
            e.printStackTrace();
            new InternalErrorDialog(e);
            if (server != null && server.isRunning()) {
                server.getServerProcess().destroy();
            }
            return;
        }

        Consumer<String> addConsoleText = text -> consoleTextArea.append(text + '\n');

        // Pipes the server outputs into the GUI using the pre-defined consumers
        StreamGobbler.execute(server.getServerProcess().getInputStream(), addConsoleText);
        StreamGobbler.execute(server.getServerProcess().getErrorStream(), addConsoleText);

        sendServerStatus(true);
    }

    public void stopServer() {
        server.setShouldBeRunning(false);
    }

    private void runButtonAction() {
        // Server is null on initial startup
        if (server.getServerProcess() == null || !server.isRunning()) {
            startServer();
        } else {
            stopServer();
        }
    }

    private void sendServerStatus(boolean start) {
        server.setShouldBeRunning(start);
        if (server.isRunning() && server.shouldBeRunning()) {
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
            title = baseTitle + " - " + serverFileInfo.getSelectedFile();
        } else {
            runText = "Run";
            title = baseTitle;

        }
        runButton.setText(runText);
        setTitle(title);
    }

    private void selectNewFile() {
        serverFileInfo = new JFileChooser();
        serverFileInfo.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.getName().toLowerCase().endsWith(".jar") || f.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Java Archive (*.jar)";
            }
        });
        if (serverFileInfo.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        serverFileInfo.setVisible(true);
        MenuItem settingsItem = this.getMenuBar().getMenu(1).getItem(0),
                openInFolderItem = this.getMenuBar().getMenu(1).getItem(1);
        settingsItem.removeActionListener(settingsOpen);
        openInFolderItem.removeActionListener(openInFolder);

        settingsOpen = e -> {
            SettingsDialog settingsDialog = new SettingsDialog(server);
            settingsDialog.pack();
            settingsDialog.setVisible(true);
        };
        try {
            serverSettings = new Settings(Paths.get(serverFileInfo.getSelectedFile().getParent(),
                    "ssw",
                    "wrapperSettings.json"));
        } catch (IOException e) {
            e.printStackTrace();
            new InternalErrorDialog(e);
        }
        settingsItem.addActionListener(settingsOpen);

        serverPathTextField.setText(serverFileInfo.getSelectedFile().toString());
        this.getMenuBar().getMenu(1).removeActionListener(noServerSelected);
        server = new MinecraftServer(serverFileInfo.getSelectedFile(),
                serverSettings);
        openInFolder = e -> {
            try {
                // Opens the enclosing folder in File Explorer, Finder, etc.
                Desktop.getDesktop().open(server.getServerPath().getParent().toFile());
            } catch (IOException ioException) {
                new InternalErrorDialog(ioException);
            }
        };
        openInFolderItem.addActionListener(openInFolder);

        aliveTimer = new Timer(100, e -> {
            if (!server.shouldBeRunning() && server.isRunning()) {
                aliveTimer.stop();
                if (restartTimer != null) restartTimer.stop();
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        try {
                            server.stop();
                        } catch (IOException ioException) {
                            firePropertyChange("error", null, ioException);
                        }
                        return null;
                    }

                    @Override
                    protected void done() {
                        super.done();
                        consoleTextArea.append("Server has been stopped.\n");
                    }
                };
                worker.addPropertyChangeListener(evt -> {
                    if (evt.getPropertyName().equalsIgnoreCase("error")) {
                        new InternalErrorDialog((Exception) evt.getNewValue());
                        server.getServerProcess().destroy();
                    }
                });
                worker.execute();
                sendServerStatus(false);
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
