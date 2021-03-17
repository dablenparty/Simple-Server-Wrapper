package com.hunterltd.ssw.gui;

import com.hunterltd.ssw.curse.CurseAddon;
import com.hunterltd.ssw.curse.CurseModpack;
import com.hunterltd.ssw.curse.data.CurseManifestFileEntry;
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
import java.util.HashMap;

public class CurseInstaller extends JFrame {
    private JTextField zipPathTextField;
    private JButton newFileButton;
    private JProgressBar installProgressBar;
    private JPanel rootPanel;
    private JTextField serverPathTextField;
    private JButton serverPathButton;
    private JButton installButton;
    private JButton cancelButton;
    private CurseModpack curseModpack;
    private File serverPath;
    private SwingWorker<Void, Void> worker;

    public CurseInstaller() {
        add(rootPanel);

        newFileButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.getName().endsWith(".zip") || f.isDirectory();
                }

                @Override
                public String getDescription() {
                    return "ZIP Archive";
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

        cancelButton.addActionListener(evt -> {
            int result = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to cancel?",
                    "Cancel pack installation",
                    JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                worker.cancel(true);
            }
        });

        installButton.addActionListener(e -> {
            JComponent[] components = {newFileButton, serverPathButton, zipPathTextField, serverPathTextField, installButton};
            for (JComponent comp :
                    components) {
                comp.setEnabled(false);
            }
            cancelButton.setEnabled(true);
            installProgressBar.setValue(0);
            installProgressBar.setString("");

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
                            curseModpack.extractAll();
                        } else {
                            curseModpack.getManifest().load();
                        }
                    } else {
                        curseModpack.extractAll();
                    }

                    firePropertyChange("status", "Extracting...", "Getting files...");
                    CurseManifestFileEntry[] files = curseModpack.getManifest().getFiles();

                    Client client = ClientBuilder.newClient();
                    int filesLength = files.length;
                    for (int i = 0; i < filesLength; i++) {
                        CurseManifestFileEntry manifestEntry = files[i];
                        Response response = client.target(
                                String.format("https://addons-ecs.forgesvc.net/api/v2/addon/%d/file/%d",
                                        manifestEntry.getProjectID(),
                                        manifestEntry.getFileID())
                        ).request(MediaType.APPLICATION_JSON).get();
                        try {
                            CurseAddon addon = new CurseAddon((JSONObject) new JSONParser().parse(response.readEntity(String.class)));
                            firePropertyChange("status",
                                    "Getting files...",
                                    String.format("Mod %d of %d: %s", i + 1, filesLength, addon.toString()));
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
                    if (!new File(curseModpack.getExtractPath()).delete()) System.out.println("An error occurred removing the folder");
                    return null;
                }

                @Override
                protected void done() {
                    installProgressBar.setString("Done!");
                    installProgressBar.setValue(100);
                    JComponent[] components = {newFileButton, serverPathButton, zipPathTextField, serverPathTextField, installButton};
                    for (JComponent comp :
                            components) {
                        comp.setEnabled(true);
                    }
                    cancelButton.setEnabled(false);
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
