package com.hunterltd.ssw.cli;

import com.hunterltd.ssw.curse.CurseModpack;
import org.apache.commons.io.FileUtils;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class CurseCli {
    private final CurseModpack curseModpack;
    private final File serverFolder;

    public CurseCli(File modpackZipFile, File serverFile) {
        serverFolder = serverFile.isDirectory() ? serverFile : serverFile.getParentFile();
        curseModpack = new CurseModpack(modpackZipFile);
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);
        try {
            extractModpack(scanner);
        } catch (IOException | ParseException exception) {
            exception.printStackTrace();
        }
    }

    private void extractModpack(Scanner inputScanner) throws IOException, ParseException {
        if (curseModpack.isExtracted()) {
            System.out.print("%s has already been extracted, overwrite? (y/N): ");
            if (!inputScanner.next().startsWith("y")) {
                curseModpack.getManifest().load();
                return;
            }
        }
        FileUtils.deleteDirectory(curseModpack.getExtractFolder());
        curseModpack.extractAll();
    }
}
