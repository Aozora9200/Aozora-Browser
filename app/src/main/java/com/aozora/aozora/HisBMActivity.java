package com.aozora.aozora;

import android.app.Activity;
import android.app.TabActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.widget.TabHost;

public class HisBMActivity extends TabActivity {

    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME = "selected_theme";
    private static final int THEME_LIGHT = 0;
    private static final int THEME_DARK = 1;
    private static final int THEME_SYSTEM = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applySavedTheme();
        setContentView(R.layout.bmhistab);
        initTabs();
        // Action Bar が表示されているか確認
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void applySavedTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int theme = prefs.getInt(KEY_THEME, THEME_SYSTEM);
        int black = getResources().getColor(R. color. backgroundBlack);
        int white = getResources().getColor(R. color. backgroundWhite);

        switch (theme) {
            case THEME_LIGHT:
                setTheme(android.R.style.Theme_Holo_Light);
                break;
            case THEME_DARK:
                setTheme(android.R.style.Theme_Holo);
                break;
            case THEME_SYSTEM:
            default:
                break;
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // 前の画面に戻る
            overridePendingTransition(R.anim.no_animation,  R.anim.slide_out_down_low);
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
        tabHost.setCurrentTab(1);

    }
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            // 子アクティビティを先に終了する（あれば）
            Activity current = getLocalActivityManager().getCurrentActivity();
            if (current != null) {
                current.finish();
            }
            // 親タブ全体を終了
            finish();
            overridePendingTransition(R.anim.no_animation, R.anim.slide_out_down_low);
            return true;
        }
        return super.dispatchKeyEvent(event);
    }
}