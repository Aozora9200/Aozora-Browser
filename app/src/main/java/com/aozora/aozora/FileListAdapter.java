package com.aozora.aozora;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FileListAdapter extends ArrayAdapter<FileItem> {
    private LayoutInflater inflater;
    private final FileManager activity;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());

    public FileListAdapter(FileManager context, List<FileItem> items) {
        super(context, 0, items);
        this.activity = context;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        FileItem item = getItem(position);
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.file_list_item, parent, false);
        }

        LinearLayout root = convertView.findViewById(R.id.itemRoot);
        ImageView icon = convertView.findViewById(R.id.imageIcon);
        TextView text = convertView.findViewById(R.id.textFileName);
        TextView textDay = convertView.findViewById(R.id.textFileDay);
        TextView textSize = convertView.findViewById(R.id.textFileSize);

        File file = item.file;
        text.setText(item.getDisplayName());

        if (item.isDirectory) {
            icon.setImageResource(R.drawable.folder);
        } else {
            String name = item.file.getName().toLowerCase();
            if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp") || name.endsWith(".svg")) {
                icon.setImageResource(R.drawable.picture);
            } else if (
                    name.endsWith(".mp3") ||
                            name.endsWith(".wav") ||
                            name.endsWith(".aiff") ||
                            name.endsWith(".aif") ||
                            name.endsWith(".aifc") ||
                            name.endsWith(".afc") ||
                            name.endsWith(".aac") ||
                            name.endsWith(".m4a") ||
                            name.endsWith(".ogg") ||
                            name.endsWith(".oga") ||
                            name.endsWith(".wma") ||
                            name.endsWith(".flac") ||
                            name.endsWith(".alac") ||
                            name.endsWith(".midi")) {
                icon.setImageResource(R.drawable.music);
            } else if (name.endsWith(".pdf") ||
                    name.endsWith(".pptx") ||
                    name.endsWith(".ppsx") ||
                    name.endsWith(".pptm") ||
                    name.endsWith(".ppt") ||
                    name.endsWith(".key")) {
                icon.setImageResource(R.drawable.pdf);
            } else if (name.endsWith(".zip")) {
                icon.setImageResource(R.drawable.zip);
            } else if (
                    name.endsWith(".txt") ||
                            name.endsWith(".rtf") ||
                            name.endsWith(".wps") ||
                            name.endsWith(".xml") ||
                            name.endsWith(".xps") ||
                            name.endsWith(".js") ||
                            name.endsWith(".xlsx") ||
                            name.endsWith(".xls") ||
                            name.endsWith(".xlsb") ||
                            name.endsWith(".json") ||
                            name.endsWith(".css") ||
                            name.endsWith(".dot") ||
                            name.endsWith(".dotm") ||
                            name.endsWith(".dotx") ||
                            name.endsWith(".odt") ||
                            name.endsWith(".docx") ||
                            name.endsWith(".doc") ||
                            name.endsWith(".docm")) {
                icon.setImageResource(R.drawable.text);
            } else if (
                    name.endsWith(".xz") ||
                    name.endsWith(".gz") ||
                    name.endsWith(".tar") ||
                    name.endsWith(".7z")) {
                icon.setImageResource(R.drawable.unknownfile);
            } else if (name.endsWith(".html") ||
                    name.endsWith(".mht") ||
                    name.endsWith(".mhtml") ||
                    name.endsWith(".htm")) {
                icon.setImageResource(R.drawable.html);
            } else if (name.endsWith(".iso")
                    || name.endsWith(".img")
                    || name.endsWith(".vhd")
                    || name.endsWith(".dmg")
                    || name.endsWith(".vmdk")) {
                icon.setImageResource(R.drawable.iso);
            } else if (
                    name.endsWith(".mp4") ||
                    name.endsWith(".avi") ||
                    name.endsWith(".mov") ||
                    name.endsWith(".wmv") ||
                    name.endsWith(".flv") ||
                    name.endsWith(".webm") ||
                    name.endsWith(".mpg") ||
                    name.endsWith(".mkv") ||
                    name.endsWith(".asf") ||
                    name.endsWith(".gif") ||
                    name.endsWith(".vob")) {
                icon.setImageResource(R.drawable.movie);
            } else if (name.endsWith(".apk") || name.endsWith(".apkm")) {
                try {
                    android.content.pm.PackageManager pm = activity.getPackageManager();
                    android.content.pm.PackageInfo pi = pm.getPackageArchiveInfo(file.getAbsolutePath(), 0);

                    if (pi != null) {
                        pi.applicationInfo.sourceDir = file.getAbsolutePath();
                        pi.applicationInfo.publicSourceDir = file.getAbsolutePath();
                        icon.setImageDrawable(pm.getApplicationIcon(pi.applicationInfo));
                    } else {
                        icon.setImageResource(R.drawable.apk);
                    }
                } catch (Exception e) {
                    icon.setImageResource(R.drawable.apk);
                }
            } else {
                icon.setImageResource(R.drawable.file);
            }
        }

        // 📅 情報表示
        String info;
        String day;
        String dateStr = dateFormat.format(new Date(file.lastModified()));

        if (file.isDirectory()) {
            File[] children = file.listFiles();
            int count = (children != null) ? children.length : 0;
            info = "項目数: " + count;
            day = "　作成日: " + dateStr;

        } else {
            info = readableFileSize(file.length());
            day = "　作成日: " + dateStr;
        }
        textSize.setText(info);
        textDay.setText(day);

        // 背景色変更（明るめの青）
        if (activity.selectedFiles.contains(item.file)) {
            root.setBackgroundColor(Color.parseColor("#CCE5FF"));
        } else {
            root.setBackgroundColor(Color.TRANSPARENT);
        }

        return convertView;
    }

    private String readableFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format(Locale.getDefault(), "%.1f %s",
                size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

}