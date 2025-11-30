package com.aozora.aozora;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class HistoryActivity extends Activity {
    private ExpandableListView expandableListView;
    private HistoryExpandableListAdapter adapter;
    private List<String> categoryList;
    private HashMap<String, List<String>> historyMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        expandableListView = findViewById(R.id.expandableListView);
        DBHistory dbHelper = new DBHistory(this);
        historyMap = dbHelper.getHistoryByCategory();
        categoryList = new ArrayList<>(historyMap.keySet());

        // 🔹 データのサイズをチェック
        boolean hasHistory = false;
        for (List<String> list : historyMap.values()) {
            if (list != null && !list.isEmpty()) {
                hasHistory = true;
                break;
            }
        }

        if (hasHistory) {
            adapter = new HistoryExpandableListAdapter(this, categoryList, historyMap);
            expandableListView.setAdapter(adapter);
        } else {
            expandableListView.setVisibility(View.GONE);
        }
    }
}