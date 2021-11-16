package com.hunterltd.ssw.curse;

public final class CurseMod {
    private int projectID;
    private int fileID;
    private boolean required;

    public int getProjectID() {
        return projectID;
    }

    public int getFileID() {
        return fileID;
    }

    public boolean isRequired() {
        return required;
    }

    @Override
    public String toString() {
        return "CurseMod{" +
                "projectID=" + projectID +
                ", fileID=" + fileID +
                ", required=" + required +
                '}';
    }
}
