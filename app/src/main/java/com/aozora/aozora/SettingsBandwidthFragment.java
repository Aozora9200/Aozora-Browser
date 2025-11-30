package com.aozora.aozora;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.ListView;

public class SettingsBandwidthFragment extends PreferenceFragment {

    private static final String KEY_IMG_BLOCK_ENABLED = "img_block_enabled";
    private static final String PREF_NAME = "AdvancedBrowserPrefs";

    private CheckBoxPreference Blockimg;
    private CheckBoxPreference intentJump;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.bandwidth);
        String packageName = getActivity().getPackageName();
        // ✅ Preferenceを取得
        Preference openActivityDefault = findPreference("default");
        if (openActivityDefault != null) {
            openActivityDefault.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
                startActivity(intent);
                return true;
            });
        }
        // CheckBoxPreferenceの取得
        Blockimg = (CheckBoxPreference) findPreference("block");
        intentJump = (CheckBoxPreference) findPreference("intentJump");
        // 現在の状態を取得
        SharedPreferences prefs = getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean Block = prefs.getBoolean(KEY_IMG_BLOCK_ENABLED, false);

        // 初期表示の制御
        Blockimg.setChecked(Block);

        SharedPreferences setupprefs = getActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean IntentJump = setupprefs.getBoolean("intentJump", true);
        if (IntentJump) {
            intentJump.setChecked(true);
        } else {
            intentJump.setChecked(false);
        }

        intentJump.setOnPreferenceChangeListener((preference, newValue) -> {
            // チェックステータス取得
            boolean check = (Boolean) newValue;
            if (check) {
                SharedPreferences prefAni = getActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefAni.edit();
                editor.putBoolean("intentJump", true);
                editor.apply();
            } else {
                SharedPreferences prefAni = getActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefAni.edit();
                editor.putBoolean("intentJump", false);
                editor.apply();
            }
            return true;
        });

        if (Block) {
            Blockimg.setChecked(true);
        } else {
            Blockimg.setChecked(false);
        }

        Blockimg.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean isChecked = (Boolean) newValue;
            if (isChecked) {

                getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                        .edit().putBoolean(KEY_IMG_BLOCK_ENABLED, true).apply();
            } else {
                getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                        .edit().putBoolean(KEY_IMG_BLOCK_ENABLED, false).apply();
            }
            return true;
        });
    }
}
