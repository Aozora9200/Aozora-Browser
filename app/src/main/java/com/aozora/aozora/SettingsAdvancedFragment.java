package com.aozora.aozora;

import static android.content.Context.MODE_PRIVATE;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.ListView;
import android.widget.Toast;

public class SettingsAdvancedFragment extends PreferenceFragment {

    private CheckBoxPreference javaScript, PopupBlock, geoLocation;
    private static final String KEY_IMG_BLOCK_ENABLED = "img_block_enabled";
    private static final String KEY_UA_ENABLED = "ua_enabled";
    private static final String KEY_DESKUA_ENABLED = "deskua_enabled";
    private static final String KEY_CT3UA_ENABLED = "ct3ua_enabled";
    private static final String PREF_NAME = "AdvancedBrowserPrefs";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.advanced);
        String packageName = getActivity().getPackageName();
        // ✅ Preferenceを取得
        Preference openActivitygeoSettings = findPreference("geoSettings");
        if (openActivitygeoSettings != null) {
            openActivitygeoSettings.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(getActivity(), geoSettingsActivity.class);
                startActivity(intent);
                return true;
            });
        }
        // CheckBoxPreferenceの取得
        javaScript = (CheckBoxPreference) findPreference("javascript");
        PopupBlock = (CheckBoxPreference) findPreference("popupblock");
        // 現在の状態を取得
        SharedPreferences setupprefs = getActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isjavaScript = setupprefs.getBoolean("javaScript", true);
        if (isjavaScript) {
            javaScript.setChecked(true);
        } else {
            javaScript.setChecked(false);
        }
        javaScript.setOnPreferenceChangeListener((preference, newValue) -> {
            // チェックステータス取得
            boolean check = (Boolean) newValue;
            if (check) {
                SharedPreferences prefs = getActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("javaScript", true);
                editor.apply();
            } else {
                SharedPreferences prefs = getActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("javaScript", false);
                editor.apply();
            }
            return true;
        });

        boolean ispopupBlock = setupprefs.getBoolean("popupBlock", false);
        if (ispopupBlock) {
            PopupBlock.setChecked(false);
        } else {
            PopupBlock.setChecked(true);
        }
        PopupBlock.setOnPreferenceChangeListener((preference, newValue) -> {
            // チェックステータス取得
            boolean check = (Boolean) newValue;
            if (check) {
                SharedPreferences prefs = getActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("popupBlock", false);
                editor.apply();
            } else {
                SharedPreferences prefs = getActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("popupBlock", true);
                editor.apply();
            }
            return true;
        });

        Preference openActivityreset = findPreference("reset");
        if (openActivityreset != null) {
            openActivityreset.setOnPreferenceClickListener(preference -> {
                new AlertDialog.Builder(getActivity())
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("初期設定にリセット")
                        .setMessage("設定をデフォルト値に戻しますか？")
                        .setPositiveButton("OK", (dialog, which) -> {
                            SharedPreferences prefs = getActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putBoolean("javaScript", true);
                            editor.putBoolean("popupBlock", false);
                            editor.putBoolean("intentJump", true);
                            editor.putBoolean("ProgressBarAnimation", true);
                            editor.putBoolean("hidebottom", false);
                            editor.putBoolean("batteryalert", true);
                            editor.putBoolean("zoomButton", true);
                            editor.putBoolean("bootScreen", true);
                            editor.putBoolean("bootSound", true);
                            editor.putBoolean("touchEffect", true);
                            editor.putBoolean("securityAlert", true);
                            editor.putBoolean("acceptCookies", true);
                            editor.putBoolean("geoLocation", true);
                            editor.putBoolean("isSwipeReload", true);
                            editor.apply();

                            getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                                    .edit().putBoolean(KEY_IMG_BLOCK_ENABLED, false).apply();
                            getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                                    .edit().putBoolean(KEY_DESKUA_ENABLED, false).apply();
                            getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                                    .edit().putBoolean(KEY_UA_ENABLED, false).apply();
                            getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                                    .edit().putBoolean(KEY_CT3UA_ENABLED, false).apply();

                            PopupBlock.setChecked(true);
                            javaScript.setChecked(true);
                        })
                        .setNegativeButton("キャンセル", null)
                        .show();
                return true;
            });
        }
    }
}
