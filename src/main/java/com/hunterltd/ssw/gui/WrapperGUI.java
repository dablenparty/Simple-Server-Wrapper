package com.hunterltd.ssw.gui;

import com.hunterltd.ssw.gui.dialogs.InfoDialog;
import com.hunterltd.ssw.gui.dialogs.InternalErrorDialog;
import com.hunterltd.ssw.gui.dialogs.SettingsDialog;
import com.hunterltd.ssw.server.MinecraftServer;
import com.hunterltd.ssw.utilities.MinecraftServerSettings;
import com.hunterltd.ssw.utilities.SmartScroller;
import com.hunterltd.ssw.utilities.network.PortListener;
import com.hunterltd.ssw.utilities.network.ServerListPing;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class WrapperGUI extends JFrame {
    @Serial
    private static final long serialVersionUID = 1L;
    private final ActionListener noServerSelected = e -> {
        InfoDialog dialog = new InfoDialog("No server selected",
                "A server must be selected to do this!");
        dialog.pack();
        dialog.setVisible(true);
    };
    private final Timer serverPingTimer;
    private PortListener portListener;
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
    private ActionListener settingsOpen, openInFolder;
    private MinecraftServerSettings serverSettings;
    private ServerListPing serverPinger;
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

        {
            // Menu Bar
            MenuBar menuBar = new MenuBar();
            Menu fileMenu = new Menu("File"),
                    serverMenu = new Menu("Server");
            MenuItem settingsItem = new MenuItem("Server settings"),
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
        }

        // keeps track of how many half minutes pass
        final int[] halfMinutesPassed = new int[]{0};

        // every 30 seconds
        serverPingTimer = new Timer(30000, e -> {
            if (!server.isRunning() || server.isShuttingDown()) return;

            ServerListPing.StatusResponse response;
            try {
                response = serverPinger.fetchData();
            } catch (IOException ioException) {
                ioException.printStackTrace();
                new InternalErrorDialog(ioException);
                return;
            }
            int onlinePlayers = response.getPlayers().getOnline();
            if (onlinePlayers > 0) halfMinutesPassed[0] = 0;
            else if (halfMinutesPassed[0] == serverSettings.getShutdownInterval() << 1) stopServer();
            else halfMinutesPassed[0] += 1;
        });


        new SmartScroller(consoleScrollPane);
        String baseTitle = "Simple Server Wrapper";
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
            portListener.stop();
        } catch (NullPointerException e) {
            //ignored
        }
        server.updateProperties();
        try {
            server.run();
            server.setShouldBeRunning(true);
        } catch (IOException e) {
            e.printStackTrace();
            new InternalErrorDialog(e);
            if (server != null && server.isRunning()) {
                server.getServerProcess().destroy();
            }
        }
    }

    public void stopServer() {
        server.setShuttingDown(true);
        server.setShouldBeRunning(false);

        final SwingWorker<Void, Void> shutdownServerWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    server.stop(10L, TimeUnit.SECONDS);
                } catch (IOException e) {
                    e.printStackTrace();
                    firePropertyChange("error", null, e);
                }
                return null;
            }

            @Override
            protected void done() {
                super.done();
            }
        };
        shutdownServerWorker.addPropertyChangeListener(evt -> {
            if (evt.getPropertyName().equals("error")) new InternalErrorDialog((Exception) evt.getNewValue());
        });
        shutdownServerWorker.execute();
    }

    private void runButtonAction() {
        // Server is null on initial startup
        try {
            if (!server.isRunning()) startServer();
            else stopServer();
        } catch (NullPointerException e) {
            // ignored
        }
    }


    private void selectNewFile() {
        JFileChooser serverFileInfo = new JFileChooser();
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
            serverSettings = new MinecraftServerSettings(Paths.get(serverFileInfo.getSelectedFile().getParent(),
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

        registerServerListeners();
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

    private void registerServerListeners() {
        // sets up the listeners for the minecraft server
        server.on("start", args -> {
            runButton.setText("Stop");
            consoleTextArea.setText("Starting server...\n");
            serverPinger = new ServerListPing();
            serverPinger.setAddress(new InetSocketAddress(server.getPort()));
            if (serverSettings.getShutdown()) serverPingTimer.start();
        });
        server.on("exiting", args -> runButton.setText("Stopping..."));
        server.on("exit", args -> {
            runButton.setText("Start");
            consoleTextArea.append("Server stopped\n");
            if (serverSettings.getShutdown()) {
                serverPingTimer.stop();
                portListener = new PortListener(server.getPort());
                try {
                    // starts the port listener on the server port
                    portListener.start();
                    portListener.on("connection", objs -> startServer());
                    portListener.on("error", objects -> new InternalErrorDialog((Exception) objects[0]));
                    portListener.on("close", objects -> System.out.println("Listener closed"));
                    portListener.on("stop", objects -> System.out.println("Listener stopped"));
                } catch (BindException bindException) {
                    InfoDialog infoDialog = new InfoDialog("Bind Exception",
                            String.format("An error occurred binding the port listener to port %d. Make sure no other" +
                                    " servers are running and try again.", server.getPort()));
                    infoDialog.pack();
                    infoDialog.setVisible(true);
                } catch (IOException e) {
                    new InternalErrorDialog(e);
                }
            }
        });
        server.on("data", args -> consoleTextArea.append((String) args[0] + '\n'));
        server.on("error", args -> new InternalErrorDialog((Exception) args[0]));
    }

    public MinecraftServer getServer() {
        return server;
    }
}
