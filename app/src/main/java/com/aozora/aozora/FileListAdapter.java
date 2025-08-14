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
import java.util.List;

public class FileListAdapter extends ArrayAdapter<FileItem> {
    private LayoutInflater inflater;

    public FileListAdapter(Context context, List<FileItem> items) {
        super(context, 0, items);
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

        text.setText(item.getDisplayName());

        if (item.isDirectory) {
            icon.setImageResource(android.R.drawable.ic_menu_agenda);
        } else {
            String name = item.file.getName().toLowerCase();
            if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")) {
                icon.setImageResource(android.R.drawable.ic_menu_gallery);
            } else if (name.endsWith(".mp3")) {
                icon.setImageResource(android.R.drawable.ic_media_play);
            } else if (name.endsWith(".pdf")) {
                icon.setImageResource(android.R.drawable.ic_menu_view);
            } else {
                icon.setImageResource(android.R.drawable.ic_menu_help);
            }
        }

        // 背景色変更（明るめの青）
        if (item.isSelected) {
            root.setBackgroundColor(Color.parseColor("#CCE5FF"));
        } else {
            root.setBackgroundColor(Color.TRANSPARENT);
        }

        return convertView;
    }
}