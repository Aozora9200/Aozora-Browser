package com.aozora.aozora;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileItem {
    public File file;
    public boolean isDirectory;
    public boolean isSelected = false;
    public long size = 0; // ✅ ファイルサイズ（ZIP内も含む）
    public long time = 0; // ✅ 作成/更新日時（ZIP対応）

    // 通常ファイル用
    public FileItem(File file) {
        this.file = file;
        this.isDirectory = file.isDirectory();
        this.size = file.isFile() ? file.length() : 0;
        this.time = file.lastModified();
    }

    // ZIP用
    public FileItem(File file, boolean isDirectory, long size, long time) {
        this.file = file;
        this.isDirectory = isDirectory;
        this.size = size;
        this.time = time;
    }

    public String getDisplayName() {
        return file.getName() + (isDirectory ? "/" : "");
    }

    public String getInfoText() {
        if (isDirectory) {
            return "フォルダ";
        } else {
            return formatFileSize(size);
        }
    }

    public String getDateText() {
        if (time <= 0) return "";
        return new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(new Date(time));
    }

    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format(Locale.getDefault(), "%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}