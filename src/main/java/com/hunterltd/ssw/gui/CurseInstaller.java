package com.hunterltd.ssw.gui;

import com.hunterltd.ssw.curse.CurseAddon;
import com.hunterltd.ssw.curse.CurseModpack;
import com.hunterltd.ssw.curse.data.CurseManifestFileEntry;
import com.hunterltd.ssw.gui.dialogs.InternalErrorDialog;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Objects;

public class CurseInstaller extends JFrame {
    private JTextField zipPathTextField;
    private JButton modpackFileButton;
    private JProgressBar installProgressBar;
    private JPanel rootPanel;
    private JTextField serverPathTextField;
    private JButton serverPathButton;
    private JButton installButton;
    private CurseModpack curseModpack;
    private File serverPath;
    private SwingWorker<Void, Void> worker;
    private boolean installing = false;

    public CurseInstaller() {
        add(rootPanel);

        modpackFileButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.getName().endsWith(".zip") || f.isDirectory();
                }

                @Override
                public String getDescription() {
                    return "ZIP Archive (*.zip)";
                }
            });
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                curseModpack = new CurseModpack(fileChooser.getSelectedFile());
                zipPathTextField.setText(fileChooser.getSelectedFile().toString());
            }
        });

        serverPathButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                serverPath = fileChooser.getSelectedFile();
                serverPathTextField.setText(serverPath.toString());
            }
        });

        installButton.addActionListener(e -> {
            JComponent[] components = {modpackFileButton, serverPathButton, zipPathTextField, serverPathTextField};
            for (JComponent comp :
                    components) {
                comp.setEnabled(false);
            }
            if (installing) {
                int result = JOptionPane.showConfirmDialog(this,
                        "Are you sure you want to cancel?",
                        "Cancel pack installation",
                        JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    worker.cancel(true);
                }
                return;
            }
            installButton.setText("Cancel");
            installProgressBar.setValue(0);
            installProgressBar.setString("");
            installing = true;

            // Delegates the downloading/installing of mod files to a separate worker thread to avoid blocking the EDT
            worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws IOException, ParseException {
                    firePropertyChange("status", "", "Extracting...");
                    if (curseModpack.isExtracted()) {
                        int result = JOptionPane.showConfirmDialog(null,
                                String.format("%s has already been extracted. Would you like to extract again?",
                                        Paths.get(curseModpack.getExtractPath()).getParent()),
                                "ZIP already extracted",
                                JOptionPane.YES_NO_OPTION);
                        if (result == JOptionPane.YES_OPTION) {
                            FileUtils.deleteDirectory(new File(curseModpack.getExtractPath()));
                            curseModpack.extractAll();
                        } else {
                            curseModpack.getManifest().load();
                        }
                    } else {
                        curseModpack.extractAll();
                    }

                    firePropertyChange("status", "Extracting...", "Getting files...");
                    CurseManifestFileEntry[] files = curseModpack.getManifest().getFiles();

                    File modsFolder = Paths.get(serverPath.toString(), "mods").toFile();
                    try {
                        if (Objects.requireNonNull(modsFolder.listFiles()).length != 0) {
                            int result = JOptionPane.showConfirmDialog(null,
                                    "The mods folder is not empty. Would you like to overwrite it?",
                                    "Mods folder not empty",
                                    JOptionPane.YES_NO_OPTION);
                            if (result == JOptionPane.NO_OPTION) return null;
                            FileUtils.deleteDirectory(modsFolder);
                        }
                    } catch (NullPointerException ignored) {}

                    Client client = ClientBuilder.newClient();
                    int filesLength = files.length;
                    int maxNameLength = 20;
                    for (int i = 0; i < filesLength; i++) {
                        // Mimics the CurseModpack's download method, but restructured to fire property changes along
                        // the way to update the EDT
                        CurseManifestFileEntry manifestEntry = files[i];
                        Response response = client.target(
                                String.format("https://addons-ecs.forgesvc.net/api/v2/addon/%d/file/%d",
                                        manifestEntry.getProjectID(),
                                        manifestEntry.getFileID())
                        ).request(MediaType.APPLICATION_JSON).get();
                        try {
                            CurseAddon addon = new CurseAddon((JSONObject) new JSONParser().parse(response.readEntity(String.class)));
                            String addonName = addon.toString().length() > maxNameLength ?
                                    addon.toString().substring(0, maxNameLength - 4) + "..." :
                                    addon.toString();
                            firePropertyChange("status",
                                    "Getting files...",
                                    String.format("Mod %d of %d: %20s", i + 1, filesLength, addonName));
                            addon.download(serverPath.toString());
                        } catch (ParseException | IOException e) {
                            e.printStackTrace();
                        } finally {
                            setProgress((int) Math.round((i / (double) filesLength) * 100));
                        }
                        if (isCancelled()) {
                            // Checks if the cancel button was clicked
                            return null;
                        }
                    }
                    firePropertyChange("status", "Installing mods...", "Copying overrides...");
                    File overrideFolder = Paths.get(curseModpack.getExtractPath(), "overrides").toFile();
                    File[] overrides = overrideFolder.listFiles();
                    if (overrides != null) {
                        for (File file :
                                overrides) {
                            try {
                                if (file.isDirectory()) {
                                    FileUtils.copyDirectory(file, Paths.get(serverPath.toString(), file.getName()).toFile());
                                } else {
                                    FileUtils.copyFileToDirectory(file, serverPath);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    try {
                        FileUtils.deleteDirectory(new File(curseModpack.getExtractPath()));
                    } catch (IOException e) {
                        new InternalErrorDialog(e);
                    }
                    return null;
                }

                @Override
                protected void done() {
                    installProgressBar.setString("Done!");
                    installProgressBar.setValue(100);
                    JComponent[] components = {modpackFileButton, serverPathButton, zipPathTextField, serverPathTextField};
                    for (JComponent comp :
                            components) {
                        comp.setEnabled(true);
                    }
                    installing = false;
                    installButton.setText("Install");
                }
            };

            worker.addPropertyChangeListener(evt -> {
                if (evt.getPropertyName().equals("progress")) {
                    int progress = (Integer) evt.getNewValue();
                    installProgressBar.setValue(progress);

                } else if (evt.getPropertyName().equals("status")) {
                    installProgressBar.setString((String) evt.getNewValue());
                }
            });

            worker.execute();
        });

        setTitle("CurseForge Modpack Installer");
    }

    public SwingWorker<Void, Void> getWorker() {
        return worker;
    }

    public static void main(String[] args) {
        CurseInstaller installer = new CurseInstaller();
        installer.pack();
        installer.setVisible(true);
    }
}
