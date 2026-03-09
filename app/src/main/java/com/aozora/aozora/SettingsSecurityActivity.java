package com.aozora.aozora;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;
import android.widget.FrameLayout;

public class SettingsSecurityActivity extends Activity {

    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME = "selected_theme";
    private static final int THEME_LIGHT = 0;
    private static final int THEME_DARK = 1;
    private static final int THEME_SYSTEM = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applySavedTheme();
        setContentView(R.layout.settings_security);

        // Action Bar が表示されているか確認
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(false);
        }

        // Fragment を追加
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.settings_browser_fragment, new SettingsSecurityFragment())
                    .commit();
        }
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
            Intent result = new Intent(this, MainActivity.class);
            result.putExtra("action", "recreate");
            setResult(RESULT_OK, result);
            finish(); // 前の画面に戻る
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Intent result = new Intent();
        result.putExtra("action", "recreate");
        setResult(RESULT_OK, result);
        finish(); // 前の画面に戻る
    }

}