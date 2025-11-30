package com.aozora.aozora;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.View;
import android.widget.ListView;

public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        String packageName = getActivity().getPackageName();
        // ✅ Preferenceを取得
        Preference openActivityPreference = findPreference("Information");
        if (openActivityPreference != null) {
            openActivityPreference.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(getActivity(), info.class);
                startActivity(intent);
                return true;
            });
        }
        Preference backup = findPreference("Storage");
        if (backup != null) {
            backup.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
                return true;
            });
        }
        Preference openActivityTheme = findPreference("Theme");
        if (openActivityTheme != null) {
            openActivityTheme.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(getActivity(), themeSettings.class);
                startActivityForResult(intent, 1001);
                return true;
            });
        }

        Preference openActivityWallpaper = findPreference("Wallpaper");
        if (openActivityWallpaper != null) {
            openActivityWallpaper.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(getActivity(), wallpaperSettings.class);
                startActivity(intent);
                return true;
            });
        }

        Preference openActivityBrowser = findPreference("Browser");
        if (openActivityBrowser != null) {
            openActivityBrowser.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(getActivity(), SettingsBrowserActivity.class);
                startActivity(intent);
                return true;
            });
        }

        Preference openActivityDataUsage = findPreference("network");
        if (openActivityDataUsage != null) {
            openActivityDataUsage.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(getActivity(), DataUsage.class);
                startActivity(intent);
                return true;
            });
        }
    }
}
