package com.hunterltd.ssw.curse;

import net.lingala.zip4j.ZipFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static com.hunterltd.ssw.utilities.concurrency.ThreadUtils.printlnWithTimeAndThread;

public class CurseCli {
    private final ZipFile modpackZip;
    private final Path serverFolder;

    public CurseCli(ZipFile modpackZip, File serverFile) {
        this.modpackZip = modpackZip;
        this.serverFolder = serverFile.isDirectory() ? serverFile.toPath() : serverFile.getParentFile().toPath();
    }

    public void run() {
        printlnWithTimeAndThread(System.out, "Extracting modpack...");
        try (CurseModpack modpack = CurseModpack.createCurseModpack(modpackZip)) {
            if (!modpack.install(serverFolder)) {
                System.out.println("Install aborted");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
