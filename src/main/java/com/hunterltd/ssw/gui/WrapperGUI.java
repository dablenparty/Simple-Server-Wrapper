package com.hunterltd.ssw.gui;

import com.hunterltd.ssw.gui.dialogs.InfoDialog;
import com.hunterltd.ssw.gui.dialogs.InternalErrorDialog;
import com.hunterltd.ssw.gui.dialogs.SettingsDialog;
import com.hunterltd.ssw.server.ConnectionListener;
import com.hunterltd.ssw.server.MinecraftServer;
import com.hunterltd.ssw.server.StreamGobbler;
import com.hunterltd.ssw.utilities.ServerListPing;
import com.hunterltd.ssw.utilities.Settings;
import com.hunterltd.ssw.utilities.SmartScroller;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
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
    private Timer aliveTimer, restartTimer, connectionListenerTimer, shutdownTimer, playerCountListenerTimer;
    private int[] restartCounter = new int[]{0, 0, 0}, // H:M:S
            shutdownCounter = new int[]{0, 0}; // M:S
    private final String restartCommandTemplate = "me %s is restarting in %d %s!"; // color code, time integer, time unit
    private final String baseTitle = "Simple Server Wrapper";
    private final ActionListener noServerSelected = e -> {
        InfoDialog dialog = new InfoDialog("No server selected",
                "A server must be selected to do this!");
        dialog.pack();
        dialog.setVisible(true);
    };
    private ActionListener settingsOpen, openInFolder;
    private Settings serverSettings;
    private SwingWorker<Void, Void> serverPingWorker;
    private int historyLocation = 0;

    public WrapperGUI() {
        add(rootPanel);

        // Action Listeners
        sendButton.addActionListener(e -> sendCommand(commandTextField.getText()));
        openDialogButton.addActionListener(e -> selectNewFile());
        runButton.addActionListener(e -> runButtonAction());

        // Keyboard Registers
        // Pressing Enter sends the typed command
        rootPanel.registerKeyboardAction(e -> {
                historyLocation = 0;
                sendCommand(commandTextField.getText());
                commandTextField.setText("");
            },
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        // Up and down arrows cycle through the command history
        rootPanel.registerKeyboardAction(e -> {
            if (server != null && server.getHistorySize() > 1) {
                if (historyLocation < server.getHistorySize() - 1 && historyLocation >= 0) {
                    historyLocation += 1;
                    commandTextField.setText(
                            server.getCommandFromHistory(server.getHistorySize() - historyLocation));
                }
            }
        },
                KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        rootPanel.registerKeyboardAction(e -> {
                    if (server != null && server.getHistorySize() > 0) {
                        if (historyLocation < server.getHistorySize() && historyLocation > 1) {
                            historyLocation -= 1;
                            commandTextField.setText(
                                    server.getCommandFromHistory(server.getHistorySize() - historyLocation));
                        }
                    }
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        // Menu Bar
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File"),
                serverMenu = new Menu("Server");
        MenuItem settingsItem = new MenuItem("Server Settings"),
                openInFolderItem = new MenuItem("Open in folder"),
                curseInstallItem = new MenuItem("Install CurseForge Modpack");
        // Adds the modpack installer, settings, and open in folder items
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

        new SmartScroller(consoleScrollPane);
        setTitle(baseTitle);
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
        try {
            // the listener will be null if the server hasn't started before or auto shutdown is disabled
            ConnectionListener.stop();
        } catch (NullPointerException ignored) {
        }
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
        stopServer(false);
    }

    public void stopServer(boolean restart) {
        server.setShuttingDown(true);
        server.setShouldRestart(restart);
        server.setShouldBeRunning(false);
    }

    private void runButtonAction() {
        // Server is null on initial startup
        if (!server.isRunning()) startServer();
        else stopServer();
    }

    private void sendServerStatus(boolean start) {
        server.setShouldBeRunning(start);
        if (server.isRunning() && server.shouldBeRunning()) {
            aliveTimer = new Timer(100, e -> {
                if ((!server.shouldBeRunning() && server.isRunning()) || !server.isRunning()) {
                    resetTimersAndCounters();
                    SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() {
                            try {
                                server.stop();
                            } catch (IOException | InterruptedException ioException) {
                                firePropertyChange("error", null, ioException);
                            }

                            // Prevents the method from completing until the server is fully shut down
                            while (true) if (!server.isRunning()) break;

                            return null;
                        }

                        @Override
                        protected void done() {
                            super.done();
                            String time = new SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis());
                            consoleTextArea.append(String.format("[%s] [SSW thread] Server has been stopped.\n", time));
                            try {
                                // closes server streams
                                server.getServerProcess().getOutputStream().close();
                                server.getServerProcess().getInputStream().close();
                                server.getServerProcess().getErrorStream().close();
                            } catch (IOException e) {
                                e.printStackTrace();
                                new InternalErrorDialog(e);
                            } finally {
                                if (server.shouldRestart()) startServer();
                                else sendServerStatus(false);
                                server.setShuttingDown(false);
                            }
                        }
                    };
                    worker.addPropertyChangeListener(evt -> {
                        if (evt.getPropertyName().equalsIgnoreCase("error")) {
                            new InternalErrorDialog((Exception) evt.getNewValue());
                            server.getServerProcess().destroy();
                        }
                    });
                    worker.execute();
                }
            });

            // Keeps track of every second
            restartTimer = serverSettings.getRestart() ? new Timer(1000, e -> {
                final int interval = serverSettings.getRestartInterval();
                int hours = restartCounter[0], minutes = restartCounter[1], seconds = restartCounter[2];

                seconds++;

                minutes = incrementCounter(minutes, seconds, restartCounter, 2, 1);

                if (minutes == 60) {
                    hours++;
                    restartCounter[0] = hours;
                    restartCounter[1] = 0; // resets "minutes" counter
                    sendCommand(String.format(restartCommandTemplate, "§7", interval - hours, "hours")); // gray
                }

                if (hours == interval) {
                    restartTimer.stop();
                    stopServer(true);
                    restartCounter = new int[]{0, 0, 0};
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
        }
        serverPathTextField.setEnabled(!start);
        openDialogButton.setEnabled(!start);
        commandTextField.setEnabled(start);
        sendButton.setEnabled(start);
        String runText, title;
        if (start) {
            runText = "Stop";
            title = baseTitle + " - " + serverFileInfo.getSelectedFile();
//            ConnectionListener.stop(); // this should be stopped before the sever is launched to avoid binding issues

            if (serverSettings.getShutdown()) {
                // This is very similar to the restart timer in structure
                shutdownTimer = new Timer(1000, e -> {
                    final int interval = serverSettings.getShutdownInterval();
                    int minutes = shutdownCounter[0], seconds = shutdownCounter[1];

                    seconds++;

                    minutes = incrementCounter(minutes, seconds, shutdownCounter, 1, 0);

                    if (minutes == interval) {
                        System.out.printf("[%s] No players have joined in a while, closing the server%n",
                                new SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis()));
                        stopServer();
                    }
                });

                serverPingWorker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws InterruptedException {
                        ServerListPing pinger = new ServerListPing();
                        pinger.setAddress(new InetSocketAddress(server.getPort()));
                        do {
                            try {
                                // putting this in the try block lets me still sleep the thread from the if statement without
                                // calling sleep again
                                if (!server.isRunning() || server.isShuttingDown()) continue;

                                ServerListPing.StatusResponse response = pinger.fetchData();
                                if (server.isRunning() && !server.isShuttingDown()) {
                                    if (response.getPlayers().getOnline() != 0) {
                                        firePropertyChange("playersOnline", 0, response.getPlayers().getOnline());
                                    } else {
                                        firePropertyChange("shutdown", false, true);
                                    }
                                }
                            } catch (IOException | NullPointerException exception) {
//                                exception.printStackTrace();
                            } finally {
                                Thread.sleep(2000);
                            }
                        } while (true);
                    }

                    @Override
                    protected void done() {
                        System.out.println("Pinger is done pinging");
                    }
                };

                serverPingWorker.addPropertyChangeListener(e -> {
                    switch (e.getPropertyName()) {
                        case "playersOnline":
                            if ((int) e.getNewValue() > 0 && shutdownTimer != null && shutdownTimer.isRunning()) {
                                shutdownTimer.stop();
                                shutdownCounter = new int[]{0, 0};
                            }
                            break;
                        case "shutdown":
                            if (shutdownTimer != null && !shutdownTimer.isRunning()) {
                                shutdownTimer.start();
                            }
                            break;
                        default:
                            break;
                    }
                });

                serverPingWorker.execute();
            }
        } else {
            runText = "Run";
            title = baseTitle;

            if (serverSettings.getShutdown()) {
                try {
                    ConnectionListener.start(server.getPort());
                    System.out.printf("[%s] Listener successfully started%n",
                            new SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis()));
                } catch (IOException e) {
                    if (e instanceof BindException) {
                        InfoDialog dialog = new InfoDialog("Bind Exception",
                                "A bind exception occurred when starting the port listener. This usually means that " +
                                        "there's already a server running. Check task manager for a java process using a " +
                                        "lot of memory");
                        dialog.pack();
                        dialog.setVisible(true);
                    }
                    e.printStackTrace();
                }

                connectionListenerTimer = new Timer(1000, e -> {
                    if (!ConnectionListener.isConnectionAttempted()) {
                        return;
                    }
                    // SocketException
                    if (!server.isRunning() && !server.isShuttingDown()) {
                        connectionListenerTimer.stop();
                        if (serverSettings.getShutdown()) startServer();
                    }
                });
                connectionListenerTimer.start();
            }
        }
        runButton.setText(runText);
        setTitle(title);
    }

    private int incrementCounter(int minutes, int seconds, int[] restartCounter, int secondsIndex, int minutesIndex) {
        if (seconds != 60) {
            restartCounter[secondsIndex] = seconds;
        } else {
            minutes++;
            restartCounter[minutesIndex] = minutes;
            restartCounter[secondsIndex] = 0; // resets "seconds" counter
        }
        return minutes;
    }

    private void resetTimersAndCounters() {
        runButton.setText("Stopping...");
        aliveTimer.stop();
        if (restartTimer != null) restartTimer.stop();
        restartCounter = new int[]{0, 0, 0};
        if (shutdownTimer != null) shutdownTimer.stop();
        shutdownCounter = new int[]{0, 0};
        if (playerCountListenerTimer != null) playerCountListenerTimer.stop();
    }

    private void selectNewFile() {
        try {
            // this will be null if the auto-shutdown feature is disabled or the wrapper was just launched
            serverPingWorker.cancel(true);
        } catch (NullPointerException ignored) {
        }
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

        // lambda for an ActionListener
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
    }

    public MinecraftServer getServer() {
        return server;
    }
}
