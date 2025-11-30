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

public class SettingsBrowserFragment extends PreferenceFragment {

    private static final String KEY_UA_ENABLED = "ua_enabled";
    private static final String KEY_DESKUA_ENABLED = "deskua_enabled";
    private static final String KEY_CT3UA_ENABLED = "ct3ua_enabled";
    private static final String PREF_NAME = "AdvancedBrowserPrefs";

    private CheckBoxPreference uaCheckPref;
    private CheckBoxPreference CT3UAPref;
    private CheckBoxPreference desktopCheckPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.browser);
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
        uaCheckPref = (CheckBoxPreference) findPreference("UA");
        CT3UAPref = (CheckBoxPreference) findPreference("CT3UA");
        desktopCheckPref = (CheckBoxPreference) findPreference("desktop");
        // 現在の状態を取得
        SharedPreferences prefs = getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean deskuaEnabled = prefs.getBoolean(KEY_DESKUA_ENABLED, false);
        boolean uaEnabled = prefs.getBoolean(KEY_UA_ENABLED, false);
        boolean CT3UA = prefs.getBoolean(KEY_CT3UA_ENABLED, false);

        desktopCheckPref.setChecked(deskuaEnabled);
        uaCheckPref.setChecked(uaEnabled);
        CT3UAPref.setChecked(CT3UA);

        if (deskuaEnabled) {
            desktopCheckPref.setChecked(true);
            uaCheckPref.setChecked(false);
            CT3UAPref.setChecked(false);
        } else {
            desktopCheckPref.setChecked(false);
        }
        if (uaEnabled) {
            uaCheckPref.setChecked(true);
            desktopCheckPref.setChecked(false);
            CT3UAPref.setChecked(false);
        } else {
            uaCheckPref.setChecked(false);
        }
        if (CT3UA) {
            CT3UAPref.setChecked(true);
            uaCheckPref.setChecked(false);
            desktopCheckPref.setChecked(false);
        } else {
            CT3UAPref.setChecked(false);
        }

        // 排他制御用リスナー
        desktopCheckPref.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean isChecked = (Boolean) newValue;
            if (isChecked) {
                uaCheckPref.setChecked(false);
                CT3UAPref.setChecked(false);

                getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                        .edit().putBoolean(KEY_DESKUA_ENABLED, true).apply();
                getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                        .edit().putBoolean(KEY_UA_ENABLED, false).apply();
                getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                        .edit().putBoolean(KEY_CT3UA_ENABLED, false).apply();
            } else {
                getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                        .edit().putBoolean(KEY_DESKUA_ENABLED, false).apply();
            }
            return true;
        });

        uaCheckPref.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean isChecked = (Boolean) newValue;
            if (isChecked) {
                desktopCheckPref.setChecked(false);
                CT3UAPref.setChecked(false);

                getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                        .edit().putBoolean(KEY_UA_ENABLED, true).apply();
                getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                        .edit().putBoolean(KEY_DESKUA_ENABLED, false).apply();
                getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                        .edit().putBoolean(KEY_CT3UA_ENABLED, false).apply();
            } else {
                getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                        .edit().putBoolean(KEY_UA_ENABLED, false).apply();
            }
            return true;
        });

        CT3UAPref.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean isChecked = (Boolean) newValue;
            if (isChecked) {
                uaCheckPref.setChecked(false);
                desktopCheckPref.setChecked(false);

                getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                        .edit().putBoolean(KEY_CT3UA_ENABLED, true).apply();
                getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                        .edit().putBoolean(KEY_DESKUA_ENABLED, false).apply();
                getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                        .edit().putBoolean(KEY_UA_ENABLED, false).apply();
            } else {
                getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                        .edit().putBoolean(KEY_CT3UA_ENABLED, false).apply();
            }
            return true;
        });

        Preference openActivityGeneral = findPreference("general");
        if (openActivityGeneral != null) {
            openActivityGeneral.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(getActivity(), SettingsGeneralActivity.class);
                startActivity(intent);
                return true;
            });
        }
        Preference openActivitySecurity = findPreference("security");
        if (openActivitySecurity != null) {
            openActivitySecurity.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(getActivity(), SettingsSecurityActivity.class);
                startActivity(intent);
                return true;
            });
        }
        Preference openActivitydetail = findPreference("detail");
        if (openActivitydetail != null) {
            openActivitydetail.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(getActivity(), SettingsAdvancedActivity.class);
                startActivity(intent);
                return true;
            });
        }
        Preference openActivitybandwidth = findPreference("bandwidth");
        if (openActivitybandwidth != null) {
            openActivitybandwidth.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(getActivity(), SettingsBandwidthActivity.class);
                startActivity(intent);
                return true;
            });
        }
    }
}
