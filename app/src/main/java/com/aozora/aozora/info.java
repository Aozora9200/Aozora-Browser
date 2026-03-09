package com.aozora.aozora;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;
import android.widget.FrameLayout;

public class info extends Activity {

    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME = "selected_theme";
    private static final int THEME_LIGHT = 0;
    private static final int THEME_DARK = 1;
    private static final int THEME_SYSTEM = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applySavedTheme();
        setContentView(R.layout.info);

        // Action Bar が表示されているか確認
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Fragment を追加
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.info_fragment, new infoFragment())
                    .commit();
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
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}