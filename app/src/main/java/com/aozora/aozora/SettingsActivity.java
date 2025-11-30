package com.aozora.aozora;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.Collections;

public class SettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        // Action Bar が表示されているか確認
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(false);
        }

        // Fragment を追加
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.settings_fragment, new SettingsFragment())
                    .commit();
        }
        // Action Bar が表示されているか確認
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences setupprefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean rebootApp = setupprefs.getBoolean("rebootApp", false);
        if (rebootApp) {
            finish();
            overridePendingTransition(0, 0);
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent result = new Intent();
            result.putExtra("action", "recreate");
            setResult(RESULT_OK, result);
            finish(); // 前の画面に戻る
            overridePendingTransition(R.anim.no_animation,  R.anim.slide_out_down_low);
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
        overridePendingTransition(R.anim.no_animation,  R.anim.slide_out_down_low);
    }
}