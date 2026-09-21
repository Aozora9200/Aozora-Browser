package com.aozora.aozora;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileItem {
    public final File file;            // 実ファイル。ZIP内項目の場合はZIP内パスを表す仮の File
    public final boolean isDirectory;
    public boolean isSelected = false;
    public final long size;            // ファイルサイズ（ZIP内は展開後サイズ）
    public final long time;            // 更新日時（ZIP内はエントリの日時）

    /** 「..」（上の階層へ）の項目 */
    public final boolean isParentLink;

    /** ZIP内の項目ならZIP内のフルパス（フォルダは末尾 "/"）。通常ファイルは null */
    public final String zipEntryPath;

    /** フォルダの項目数。-1 は未計算（アダプタがバックグラウンドで数えてここにキャッシュする） */
    public volatile int childCount = -1;
    public volatile boolean childCountRequested = false;

    private String displayName;
    private String dateText;

    // 通常ファイル用（stat をまとめて1回ずつだけ呼ぶ）
    public FileItem(File file) {
        this.file = file;
        boolean dir = file.isDirectory();
        this.isDirectory = dir;
        this.size = dir ? 0 : file.length();
        this.time = file.lastModified();
        this.isParentLink = false;
        this.zipEntryPath = null;
    }

    // 互換用
    public FileItem(File file, boolean isDirectory, long size, long time) {
        this(file, isDirectory, size, time, false, null, -1);
    }

    private FileItem(File file, boolean isDirectory, long size, long time,
                     boolean isParentLink, String zipEntryPath, int childCount) {
        this.file = file;
        this.isDirectory = isDirectory;
        this.size = size;
        this.time = time;
        this.isParentLink = isParentLink;
        this.zipEntryPath = zipEntryPath;
        this.childCount = childCount;
    }

    /** 「..」項目。通常ビューでは parentDir が移動先、ZIPビューでは null を渡す */
    public static FileItem parentLink(File parentDir) {
        return new FileItem(parentDir != null ? parentDir : new File(".."), true, 0, 0, true, null, -1);
    }

    /** ZIP内の項目 */
    public static FileItem zipEntry(String fullPath, boolean isDirectory, long size, long time, int childCount) {
        return new FileItem(new File(fullPath), isDirectory, size, time, false, fullPath, childCount);
    }

    public boolean isZipEntry() {
        return zipEntryPath != null;
    }

    public String getDisplayName() {
        if (displayName == null) {
            displayName = isParentLink ? ".." : file.getName() + (isDirectory ? "/" : "");
        }
        return displayName;
    }

    public String getInfoText() {
        return isDirectory ? "フォルダ" : formatSize(size);
    }

    public String getDateText() {
        if (dateText == null) {
            dateText = time <= 0 ? ""
                    : new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(new Date(time));
        }
        return dateText;
    }

    public static String formatSize(long bytes) {
        if (bytes < 1024) return Math.max(bytes, 0) + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        exp = Math.min(exp, 6);
        String pre = String.valueOf("KMGTPE".charAt(exp - 1));
        return String.format(Locale.getDefault(), "%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}