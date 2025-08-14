package com.aozora.aozora;

import java.io.File;

public class FileItem {
    public File file;
    public boolean isDirectory;
    public boolean isSelected = false;

    public FileItem(File file) {
        this.file = file;
        this.isDirectory = file.isDirectory();
    }

    public String getDisplayName() {
        return file.getName() + (isDirectory ? "/" : "");
    }
}