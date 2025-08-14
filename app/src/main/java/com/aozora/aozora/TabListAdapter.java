package com.aozora.aozora;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import java.util.List;

public class TabListAdapter extends BaseAdapter {
    private Context context;
    private List<TabInfo> tabList;
    private int currentTabIndex; // 🔹 現在のタブ
    private TabActionListener listener;

    public interface TabActionListener {
        void onTabClose(int position);
    }

    public TabListAdapter(Context context, List<TabInfo> tabList, int currentTabIndex) {
        this.context = context;
        this.tabList = tabList;
        this.currentTabIndex = currentTabIndex;
    }

    @Override
    public int getCount() {
        return tabList.size();
    }

    @Override
    public Object getItem(int position) {
        return tabList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.tab_list_item, parent, false);
        }

        ImageView iconView = convertView.findViewById(R.id.tabIcon);
        TextView titleView = convertView.findViewById(R.id.tabTitle);
        // TextView tabUrl = convertView.findViewById(R.id.tabUrl);
        ImageButton tabCloseButton = convertView.findViewById(R.id.tabCloseButton);

        TabInfo tab = tabList.get(position);
        titleView.setText(tab.getTitle() != null ? tab.getTitle() : tab.getUrl());
        // tabUrl.setText(tab.getUrl());

        Bitmap icon = tab.getIcon();
        if (icon != null) {
            iconView.setImageBitmap(icon);
        } else {
            iconView.setImageResource(R.drawable.default_icon);
        }

        // 🔹 現在のタブなら、タイトルを強調
        if (position == currentTabIndex) {
            titleView.setText("✔ " + titleView.getText());
        }
        // タブを閉じるボタンの処理
        tabCloseButton.setOnClickListener(v -> {
            if (context instanceof MainActivity) {
                ((MainActivity) context).onTabClose(position);
                notifyDataSetChanged();
            }
        });

        return convertView;
    }
}