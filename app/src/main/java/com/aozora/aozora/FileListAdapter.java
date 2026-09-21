package com.aozora.aozora;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileListAdapter extends ArrayAdapter<FileItem> {

    private static final int SELECTED_COLOR = 0xFFCCE5FF; // 明るめの青
    private static final ExecutorService BG = Executors.newFixedThreadPool(2);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    // 拡張子 → アイコン。getView のたびに endsWith を何十回も回さないためのテーブル
    private static final Map<String, Integer> ICONS = new HashMap<>();

    static {
        put(R.drawable.picture, "jpg", "jpeg", "png", "webp", "svg");
        put(R.drawable.music, "mp3", "wav", "aiff", "aif", "aifc", "afc", "aac", "m4a",
                "ogg", "oga", "wma", "flac", "alac", "midi");
        put(R.drawable.pdf, "pdf", "pptx", "ppsx", "pptm", "ppt", "key");
        put(R.drawable.zip, "zip");
        put(R.drawable.text, "txt", "rtf", "wps", "xml", "xps", "js", "xlsx", "xls", "xlsb",
                "json", "css", "dot", "dotm", "dotx", "odt", "docx", "doc", "docm");
        put(R.drawable.unknownfile, "xz", "gz", "tar", "7z");
        put(R.drawable.html, "html", "mht", "mhtml", "htm");
        put(R.drawable.iso, "iso", "img", "vhd", "dmg", "vmdk");
        put(R.drawable.movie, "mp4", "avi", "mov", "wmv", "flv", "webm", "mpg", "mkv", "asf", "gif", "vob");
    }

    private static void put(int res, String... exts) {
        for (String e : exts) ICONS.put(e, res);
    }

    private static class Holder {
        LinearLayout root;
        ImageView icon;
        TextView name;
        TextView day;
        TextView size;
        FileItem bound; // 非同期結果を反映してよいか判定するため
    }

    private final LayoutInflater inflater;
    private final FileManager activity;
    private final LruCache<String, Drawable> apkIcons = new LruCache<>(64);

    public FileListAdapter(FileManager context, List<FileItem> items) {
        super(context, 0, items);
        this.activity = context;
        this.inflater = LayoutInflater.from(context);
    }

    /** アダプタを作り直さずに中身だけ差し替える（通知は1回だけ） */
    public void setItems(List<FileItem> items) {
        setNotifyOnChange(false);
        clear();
        addAll(items);
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder h;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.file_list_item, parent, false);
            h = new Holder();
            h.root = convertView.findViewById(R.id.itemRoot);
            h.icon = convertView.findViewById(R.id.imageIcon);
            h.name = convertView.findViewById(R.id.textFileName);
            h.day = convertView.findViewById(R.id.textFileDay);
            h.size = convertView.findViewById(R.id.textFileSize);
            convertView.setTag(h);
        } else {
            h = (Holder) convertView.getTag();
        }

        FileItem item = getItem(position);
        h.bound = item;

        h.name.setText(item.getDisplayName());
        bindIcon(h, item);
        bindInfo(h, item);

        String date = item.getDateText();
        h.day.setText(date.isEmpty() ? "" : "　作成日: " + date);

        h.root.setBackgroundColor(activity.isSelected(item.file) ? SELECTED_COLOR : 0x00000000);
        return convertView;
    }

    private void bindIcon(final Holder h, final FileItem item) {
        if (item.isDirectory) {
            h.icon.setImageResource(R.drawable.folder);
            return;
        }

        String name = item.file.getName().toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        String ext = dot >= 0 ? name.substring(dot + 1) : "";

        if (!item.isZipEntry() && (ext.equals("apk") || ext.equals("apkm"))) {
            bindApkIcon(h, item);
            return;
        }

        Integer res = ICONS.get(ext);
        h.icon.setImageResource(res != null ? res : R.drawable.file);
    }

    private void bindApkIcon(final Holder h, final FileItem item) {
        final String path = item.file.getAbsolutePath();
        final String key = path + "@" + item.time;

        Drawable cached = apkIcons.get(key);
        if (cached != null) {
            h.icon.setImageDrawable(cached);
            return;
        }

        h.icon.setImageResource(R.drawable.apk);
        BG.execute(() -> {
            final Drawable d = loadApkIcon(path);
            if (d == null) return;
            apkIcons.put(key, d);
            MAIN.post(() -> {
                if (h.bound == item) h.icon.setImageDrawable(d);
            });
        });
    }

    private Drawable loadApkIcon(String path) {
        try {
            PackageManager pm = activity.getPackageManager();
            PackageInfo pi = pm.getPackageArchiveInfo(path, 0);
            if (pi == null || pi.applicationInfo == null) return null;
            pi.applicationInfo.sourceDir = path;
            pi.applicationInfo.publicSourceDir = path;
            return pm.getApplicationIcon(pi.applicationInfo);
        } catch (Exception e) {
            return null;
        }
    }

    private void bindInfo(final Holder h, final FileItem item) {
        if (!item.isDirectory) {
            h.size.setText(FileItem.formatSize(item.size));
            return;
        }
        if (item.isParentLink) {
            h.size.setText("");
            return;
        }

        int count = item.childCount;
        if (count >= 0) {
            h.size.setText("項目数: " + count);
            return;
        }

        // 項目数は listFiles が重いので、表示された行だけバックグラウンドで数える
        h.size.setText("項目数: -");
        if (item.childCountRequested) return;
        item.childCountRequested = true;
        final File dir = item.file;
        BG.execute(() -> {
            String[] list = dir.list();
            final int c = list != null ? list.length : 0;
            item.childCount = c;
            MAIN.post(() -> {
                if (h.bound == item) h.size.setText("項目数: " + c);
            });
        });
    }
}