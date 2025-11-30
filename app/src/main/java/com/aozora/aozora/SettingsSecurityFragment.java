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

public class SettingsSecurityFragment extends PreferenceFragment {

    private CheckBoxPreference securityAlert, acceptCookies, geoLocation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.security);
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
        securityAlert = (CheckBoxPreference) findPreference("securityAlert");
        acceptCookies = (CheckBoxPreference) findPreference("acceptCookies");
        geoLocation = (CheckBoxPreference) findPreference("geolocation");
        // 現在の状態を取得
        SharedPreferences setupprefs = getActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean issecurityAlert = setupprefs.getBoolean("securityAlert", true);

        if (issecurityAlert) {
            securityAlert.setChecked(true);
        } else {
            securityAlert.setChecked(false);
        }

        securityAlert.setOnPreferenceChangeListener((preference, newValue) -> {
            // チェックステータス取得
            boolean check = (Boolean) newValue;
            if (check) {
                SharedPreferences prefs = getActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("securityAlert", true);
                editor.apply();
            } else {
                SharedPreferences prefs = getActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("securityAlert", false);
                editor.apply();
            }
            return true;
        });

        boolean isacceptCookies = setupprefs.getBoolean("acceptCookies", true);
        if (isacceptCookies) {
            acceptCookies.setChecked(true);
        } else {
            acceptCookies.setChecked(false);
        }

        acceptCookies.setOnPreferenceChangeListener((preference, newValue) -> {
            // チェックステータス取得
            boolean check = (Boolean) newValue;
            if (check) {
                SharedPreferences prefs = getActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("acceptCookies", true);
                editor.apply();
            } else {
                SharedPreferences prefs = getActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("acceptCookies", false);
                editor.apply();
            }
            return true;
        });

        boolean isgeoLocation = setupprefs.getBoolean("geoLocation", true);
        if (isgeoLocation) {
            geoLocation.setChecked(true);
        } else {
            geoLocation.setChecked(false);
        }

        geoLocation.setOnPreferenceChangeListener((preference, newValue) -> {
            // チェックステータス取得
            boolean check = (Boolean) newValue;
            if (check) {
                SharedPreferences prefs = getActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("geoLocation", true);
                editor.apply();
            } else {
                SharedPreferences prefs = getActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("geoLocation", false);
                editor.apply();
            }
            return true;
        });

        Preference openActivityDelCookie = findPreference("delcache");
        if (openActivityDelCookie != null) {
            openActivityDelCookie.setOnPreferenceClickListener(preference -> {
                new AlertDialog.Builder(getActivity())
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Cookieをすべて消去")
                        .setMessage("すべてのCookieを削除しますか？")
                        .setPositiveButton("OK", (dialog, which) -> {
                            CookieManager cm = CookieManager.getInstance();
                            cm.removeAllCookie(); // API 19 では removeAllCookie を使用
                            CookieSyncManager.getInstance().sync(); // flush の代わりに CookieSyncManager を使用
                        })
                        .setNegativeButton("キャンセル", null)
                        .show();
                return true;
            });
        }

        Preference openActivityDelCache = findPreference("delcache");
        if (openActivityDelCache != null) {
            openActivityDelCache.setOnPreferenceClickListener(preference -> {
                WebViewUtils.clearWebViewCache(getActivity());
                Toast.makeText(getActivity(), "キャッシュを削除しました", Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        Preference openActivityDelgeolocation = findPreference("delgeolocation");
        if (openActivityDelgeolocation != null) {
            openActivityDelgeolocation.setOnPreferenceClickListener(preference -> {
                new AlertDialog.Builder(getActivity())
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("位置情報アクセスをクリア")
                        .setMessage("ウェブサイトの現在地情報へのアクセスをクリアしますか？")
                        .setPositiveButton("OK", (dialog, which) -> {
                            SharedPreferences geoPrefs = getActivity().getSharedPreferences("GeoPermissionStore", MODE_PRIVATE);
                            geoPrefs.edit().clear().apply();
                        })
                        .setNegativeButton("キャンセル", null)
                        .show();
                return true;
            });
        }
    }
}
