package com.hunterltd.ssw.gui;

import javax.swing.*;
import java.io.File;

public class CurseInstaller extends JFrame {
    private JTextField zipPathTextField;
    private JButton button1;
    private JProgressBar installProgressBar;
    private JPanel rootPanel;
    private File modpackZip;

    public CurseInstaller(File zipFile) {
        modpackZip = zipFile;

        setTitle("CurseForge Modpack Installer");
        add(rootPanel);
    }
}
