package com.aozora.aozora;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TabHost;

public class BmHisActivity extends TabActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bmhistab);
        initTabs();
        // Action Bar が表示されているか確認
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // 前の画面に戻る
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void initTabs(){

        Resources res = getResources();
        TabHost tabHost = getTabHost();
        TabHost.TabSpec spec;
        Intent intent;

        // ブックマーク
        intent = new Intent().setClass(this, SavedBookMarkActivity.class);
        spec = tabHost.newTabSpec("BM").setIndicator(
                        "ブックマーク")
                .setContent(intent);
        tabHost.addTab(spec);

        // 保存したページ
        intent = new Intent().setClass(this, SavedPagesActivity.class);
        spec = tabHost.newTabSpec("SavedPage").setIndicator(
                        "保存したページ")
                .setContent(intent);
        tabHost.addTab(spec);

        // Set Default Tab - zero based index
        tabHost.setCurrentTab(0);

    }
}
