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

public class CurseInstaller extends JFrame {
    private JTextField zipPathTextField;
    private JButton newFileButton;
    private JProgressBar installProgressBar;
    private JPanel rootPanel;
    private JTextField serverPathTextField;
    private JButton serverPathButton;
    private CurseModpack curseModpack;
    private File serverPath;

    public CurseInstaller() {
        newFileButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.getName().endsWith(".zip");
                }

                @Override
                public String getDescription() {
                    return "ZIP Archive";
                }
            });
            curseModpack = new CurseModpack(fileChooser.getSelectedFile());
            zipPathTextField.setText(fileChooser.getSelectedFile().toString());
        });
        serverPathButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            serverPath = fileChooser.getSelectedFile();
            serverPathTextField.setText(serverPath.toString());
        });
        setTitle("CurseForge Modpack Installer");
        add(rootPanel);
    }

    public boolean install(String folder) {
        boolean error = false;
        Client client = ClientBuilder.newClient();
        for (CurseManifestFileEntry manifestEntry :
                curseModpack.getManifest().getFiles()) {
            Response response = client.target(
                    String.format("https://addons-ecs.forgesvc.net/api/v2/addon/%d/file/%d",
                            manifestEntry.getProjectID(),
                            manifestEntry.getFileID())
            ).request(MediaType.APPLICATION_JSON).get();
            try {
                CurseAddon addon = new CurseAddon((JSONObject) new JSONParser().parse(response.readEntity(String.class)));
                addon.download(folder);
            } catch (ParseException | IOException e) {
                e.printStackTrace();
                error = true;
            }
        }
        File overrides = Paths.get(serverPath.toString(), "overrides", "mods").toFile();
        try {
            FileUtils.copyDirectory(overrides, Paths.get(folder, "mods").toFile());
        } catch (IOException e) {
            e.printStackTrace();
            error = true;
        }

        return error;

    }
}
