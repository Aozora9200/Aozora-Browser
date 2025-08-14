package com.aozora.aozora;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;
import java.util.HashMap;
import java.util.List;

public class HistoryExpandableListAdapter extends BaseExpandableListAdapter {
    private Context context;
    private List<String> categoryList;
    private HashMap<String, List<String>> historyMap;

    public HistoryExpandableListAdapter(Context context, List<String> categoryList, HashMap<String, List<String>> historyMap) {
        this.context = context;
        this.categoryList = categoryList;
        this.historyMap = historyMap;
    }

    @Override
    public int getGroupCount() {
        return categoryList.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return historyMap.get(categoryList.get(groupPosition)).size();
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        List<String> historyList = historyMap.get(categoryList.get(groupPosition));

        // 🔹 もし `childPosition` が `historyList` のサイズを超えたら `null` を返す
        if (historyList == null || childPosition >= historyList.size()) {
            return null;
        }

        return historyList.get(childPosition);
    }

    @Override
    public Object getGroup(int groupPosition) {
        // 🔹 `groupPosition` の範囲をチェック
        if (groupPosition < 0 || groupPosition >= categoryList.size()) {
            return null;
        }
        return categoryList.get(groupPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_expandable_list_item_1, parent, false);
        }
        TextView textView = (TextView) convertView.findViewById(android.R.id.text1);
        textView.setText(categoryList.get(groupPosition));
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false);
        }
        TextView textView = (TextView) convertView.findViewById(android.R.id.text1);
        textView.setText(historyMap.get(categoryList.get(groupPosition)).get(childPosition));
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}