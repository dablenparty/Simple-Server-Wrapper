package com.hunterltd.ssw.gui;

import com.hunterltd.ssw.gui.dialogs.InfoDialog;
import com.hunterltd.ssw.gui.dialogs.InternalErrorDialog;
import com.hunterltd.ssw.gui.dialogs.SettingsDialog;
import com.hunterltd.ssw.server.MinecraftServer;
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
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

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

        {
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
        }

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
        server.updateProperties();
        try {
            server.run();
        } catch (IOException e) {
            e.printStackTrace();
            new InternalErrorDialog(e);
            if (server != null && server.isRunning()) {
                server.getServerProcess().destroy();
            }
        }
    }

    public void stopServer() {
        stopServer(false);
    }

    public void stopServer(boolean restart) {
        server.setShuttingDown(true);
        server.setShouldRestart(restart);
        server.setShouldBeRunning(false);

        SwingWorker<Void, Void> shutdownServerWorker = new SwingWorker<>() {
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
        if (!server.isRunning()) startServer();
        else stopServer();
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
        server.on("start", args -> {
            runButton.setText("Stop");
            consoleTextArea.setText("Starting server...\n");
        });
        server.on("exiting", args -> runButton.setText("Stopping..."));
        server.on("exit", args -> {
            runButton.setText("Start");
            consoleTextArea.append("Server stopped\n");
        });
        server.on("data", args -> consoleTextArea.append((String) args[0] + '\n'));
        server.on("error", args -> new InternalErrorDialog((Exception) args[0]));
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
