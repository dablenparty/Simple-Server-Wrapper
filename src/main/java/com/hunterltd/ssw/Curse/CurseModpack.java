package com.hunterltd.ssw.Curse;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CurseModpack extends ZipFile {
    private final Path extractPath;

    public CurseModpack(File zipFile) {
        super(zipFile);
        extractPath = Paths.get(zipFile.getParent(), FilenameUtils.getBaseName(zipFile.getName()));
    }

    public void extractAll() throws ZipException {
        this.extractAll(String.valueOf(extractPath));
    }

    public boolean isExtracted() {
        return extractPath.toFile().exists();
    }

    public Path getExtractPath() {
        return extractPath;
    }

    private void readCurseData() {
        //read manifest.json
    }

    public static void main(String[] args) {
        CurseModpack pack = new CurseModpack(new File("/Volumes/SANDISK32/Minecraft Enhanced-v1.3.zip"));
        System.out.println(pack.getExtractPath());
    }
}
