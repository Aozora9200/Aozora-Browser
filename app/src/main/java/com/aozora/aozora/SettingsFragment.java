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
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ✅ 見切れ対策のためにパディングを追加
        View rootView = getView();
        if (rootView != null) {
            ListView listView = (ListView) rootView.findViewById(android.R.id.list);
            if (listView != null) {
                int statusBarHeight = getStatusBarHeight();
                listView.setPadding(0, statusBarHeight, 0, 0);
                listView.setClipToPadding(false);
            }
        }
    }

    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
}
