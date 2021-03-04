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
    private CurseModpack curseModpack;
    private File serverPath;

    public CurseInstaller() {
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
        installButton.addActionListener(e -> {
            JComponent[] components = {newFileButton, serverPathButton, zipPathTextField, serverPathTextField, installButton};
            for (JComponent comp :
                    components) {
                comp.setEnabled(false);
            }
            worker.addPropertyChangeListener(evt -> {
                if (evt.getPropertyName().equals("progress")) {
                    int progress = (Integer) evt.getNewValue();
                    installProgressBar.setValue(progress);
                    installProgressBar.setString(String.format("Downloading mods: %d", progress) + "%");
                }
            });
            worker.execute();
            for (JComponent comp :
                    components) {
                comp.setEnabled(true);
            }
        });

        setTitle("CurseForge Modpack Installer");
        add(rootPanel);
    }

    SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
        @Override
        protected Void doInBackground() throws IOException, ParseException {
            curseModpack.extractAll();
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
//                    installProgressBar.setString(String.format("Mod %d of %d: %s", i + 1, filesLength, addon.toString()));
                    addon.download(serverPath.toString());
                } catch (ParseException | IOException e) {
                    e.printStackTrace();
                } finally {
                    setProgress((int) Math.round((i / (double) filesLength) * 100));
                }
            }
            File overrides = Paths.get(curseModpack.getExtractPath(), "overrides", "mods").toFile();
            try {
                FileUtils.copyDirectory(overrides, Paths.get(serverPath.toString(), "mods").toFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void done() {
            installProgressBar.setString("Done!");
            installProgressBar.setValue(100);
        }
    };

//    public boolean install(String folder) throws IOException, ParseException {
//        boolean error = false;
//        installProgressBar.setString("Extracting...");
//        curseModpack.extractAll();
//        installProgressBar.setString("Extracted");
//        Client client = ClientBuilder.newClient();
//        CurseManifestFileEntry[] files = curseModpack.getManifest().getFiles();
//        int filesLength = files.length;
//        installProgressBar.setMaximum(filesLength);
//        for (int i = 0; i < filesLength; i++) {
//            CurseManifestFileEntry manifestEntry = files[i];
//            Response response = client.target(
//                    String.format("https://addons-ecs.forgesvc.net/api/v2/addon/%d/file/%d",
//                            manifestEntry.getProjectID(),
//                            manifestEntry.getFileID())
//            ).request(MediaType.APPLICATION_JSON).get();
//            try {
//                CurseAddon addon = new CurseAddon((JSONObject) new JSONParser().parse(response.readEntity(String.class)));
//                installProgressBar.setString(String.format("Mod %d of %d: %s", i + 1, filesLength, addon.toString()));
//                addon.download(folder);
//            } catch (ParseException | IOException e) {
//                e.printStackTrace();
//                error = true;
//            } finally {
//                installProgressBar.setValue(i + 1);
//            }
//        }
//        File overrides = Paths.get(curseModpack.getExtractPath(), "overrides", "mods").toFile();
//        try {
//            FileUtils.copyDirectory(overrides, Paths.get(folder, "mods").toFile());
//        } catch (IOException e) {
//            e.printStackTrace();
//            error = true;
//        }
//
//        return error;
//    }

    public static void main(String[] args) {
        CurseInstaller installer = new CurseInstaller();
        installer.pack();
        installer.setVisible(true);
    }
}