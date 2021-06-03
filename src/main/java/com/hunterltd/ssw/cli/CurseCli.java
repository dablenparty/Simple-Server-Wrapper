package com.hunterltd.ssw.cli;

import com.hunterltd.ssw.curse.CurseModpack;

import java.io.File;

public class CurseCli {
    private final CurseModpack curseModpack;
    private final File serverFolder;

    public CurseCli(File modpackZipFile, File serverFile) {
        serverFolder = serverFile.isDirectory() ? serverFile : serverFile.getParentFile();
        curseModpack = new CurseModpack(modpackZipFile);
    }
}
