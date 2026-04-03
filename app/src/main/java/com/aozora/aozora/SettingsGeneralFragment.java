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

public class SettingsGeneralFragment extends PreferenceFragment {

    private static final String KEY_UA_ENABLED = "ua_enabled";
    private static final String KEY_DESKUA_ENABLED = "deskua_enabled";
    private static final String KEY_CT3UA_ENABLED = "ct3ua_enabled";
    private static final String KEY_IMG_BLOCK_ENABLED = "img_block_enabled";
    private static final String PREF_NAME = "AdvancedBrowserPrefs";

    private CheckBoxPreference reloadPref;
    private CheckBoxPreference progressBarAnimation;
    private CheckBoxPreference zoomPref;
    private CheckBoxPreference vibPref;
    private CheckBoxPreference bootPref;
    private CheckBoxPreference bootSoundPref;
    private CheckBoxPreference hidebottomPref;
    private CheckBoxPreference batteryalertPref;
    private CheckBoxPreference TouchEffect;
    private CheckBoxPreference Update;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.general);
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
        reloadPref = (CheckBoxPreference) findPreference("reload");
        progressBarAnimation = (CheckBoxPreference) findPreference("progress");
        zoomPref = (CheckBoxPreference) findPreference("zoom");
        vibPref = (CheckBoxPreference) findPreference("vibrate");
        bootPref = (CheckBoxPreference) findPreference("boot");
        bootSoundPref = (CheckBoxPreference) findPreference("bootsound");
        hidebottomPref = (CheckBoxPreference) findPreference("hidebottom");
        batteryalertPref = (CheckBoxPreference) findPreference("batteryalert");
        TouchEffect = (CheckBoxPreference) findPreference("touchEffect");
        Update = (CheckBoxPreference) findPreference("update");

        SharedPreferences setupprefs = getActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean reload = setupprefs.getBoolean("isSwipeReload", true);
        boolean ProgressBarAnimation = setupprefs.getBoolean("ProgressBarAnimation", true);
        boolean hidebottom = setupprefs.getBoolean("hidebottom", false);
        boolean batteryalert = setupprefs.getBoolean("batteryalert", true);
        boolean zoombutton = setupprefs.getBoolean("zoomButton", true);
        boolean Vibrate = setupprefs.getBoolean("Vibrate", true);
        boolean bootScreen = setupprefs.getBoolean("bootScreen", false);
        boolean bootSound = setupprefs.getBoolean("bootSound", false);
        if (reload) {
            reloadPref.setChecked(true);
        } else {
            reloadPref.setChecked(false);
        }
        if (hidebottom) {
            hidebottomPref.setChecked(true);
        } else {
            hidebottomPref.setChecked(false);
        }
        if (batteryalert) {
            batteryalertPref.setChecked(true);
        } else {
            batteryalertPref.setChecked(false);
        }
        if (Vibrate) {
            vibPref.setChecked(true);
        } else {
            vibPref.setChecked(false);
        }
        if (bootScreen) {
            bootPref.setChecked(true);
            bootSoundPref.setEnabled(true);
        } else {
            bootPref.setChecked(false);
            bootSoundPref.setEnabled(false);
        }
        if (bootSound) {
            bootSoundPref.setChecked(true);
        } else {
            bootSoundPref.setChecked(false);
        }
        if (zoombutton) {
            zoomPref.setChecked(true);
        } else {
            zoomPref.setChecked(false);
        }
        if (ProgressBarAnimation) {
            progressBarAnimation.setChecked(true);
        } else {
            progressBarAnimation.setChecked(false);
        }
        reloadPref.setOnPreferenceChangeListener((preference, newValue) -> {
            // チェックステータス取得
            boolean check = (Boolean) newValue;
            if (check) {
                SharedPreferences prefAni = getActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefAni.edit();
                editor.putBoolean("isSwipeReload", true);
                editor.apply();
            } else {
                SharedPreferences prefAni = getActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefAni.edit();
                editor.putBoolean("isSwipeReload", false);
                editor.apply();
            }
            return true;
        });
        progressBarAnimation.setOnPreferenceChangeListener((preference, newValue) -> {
            // チェックステータス取得
            boolean check = (Boolean) newValue;
            if (check) {
                SharedPreferences prefAni = getActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefAni.edit();
                editor.putBoolean("ProgressBarAnimation", true);
                editor.apply();
            } else {
                SharedPreferences prefAni = getActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefAni.edit();
                editor.putBoolean("ProgressBarAnimation", false);
                editor.apply();
            }
            return true;
        });

        hidebottomPref.setOnPreferenceChangeListener((preference, newValue) -> {
            // チェックステータス取得
            boolean check = (Boolean) newValue;
            if (check) {
                SharedPreferences prefBottom = getActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefBottom.edit();
                editor.putBoolean("hidebottom", true);
                editor.apply();
            } else {
                SharedPreferences prefAni = getActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefAni.edit();
                editor.putBoolean("hidebottom", false);
                editor.apply();
            }
            return true;
        });

        batteryalertPref.setOnPreferenceChangeListener((preference, newValue) -> {
            // チェックステータス取得
            boolean check = (Boolean) newValue;
            if (check) {
                SharedPreferences prefBottom = getActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefBottom.edit();
                editor.putBoolean("batteryalert", true);
                editor.apply();
            } else {
                SharedPreferences prefAni = getActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefAni.edit();
                editor.putBoolean("batteryalert", false);
                editor.apply();
            }
            return true;
        });

        zoomPref.setOnPreferenceChangeListener((preference, newValue) -> {
            // チェックステータス取得
            boolean check = (Boolean) newValue;
            if (check) {
                SharedPreferences prefAni = getActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefAni.edit();
                editor.putBoolean("zoomButton", true);
                editor.apply();
            } else {
                SharedPreferences prefAni = getActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefAni.edit();
                editor.putBoolean("zoomButton", false);
                editor.apply();
            }
            return true;
        });

        vibPref.setOnPreferenceChangeListener((preference, newValue) -> {
            // チェックステータス取得
            boolean check = (Boolean) newValue;
            if (check) {
                SharedPreferences prefAni = getActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefAni.edit();
                editor.putBoolean("Vibrate", true);
                editor.apply();
            } else {
                SharedPreferences prefAni = getActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefAni.edit();
                editor.putBoolean("Vibrate", false);
                editor.apply();
            }
            return true;
        });

        bootPref.setOnPreferenceChangeListener((preference, newValue) -> {
            // チェックステータス取得
            boolean check = (Boolean) newValue;
            if (check) {
                SharedPreferences prefAni = getActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefAni.edit();
                editor.putBoolean("bootScreen", true);
                editor.apply();
                bootSoundPref.setEnabled(true);
            } else {
                SharedPreferences prefAni = getActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefAni.edit();
                editor.putBoolean("bootScreen", false);
                editor.apply();
                bootSoundPref.setEnabled(false);
            }
            return true;
        });

        bootSoundPref.setOnPreferenceChangeListener((preference, newValue) -> {
            // チェックステータス取得
            boolean check = (Boolean) newValue;
            if (check) {
                SharedPreferences prefAni = getActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefAni.edit();
                editor.putBoolean("bootSound", true);
                editor.apply();
            } else {
                SharedPreferences prefAni = getActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefAni.edit();
                editor.putBoolean("bootSound", false);
                editor.apply();
            }
            return true;
        });

        boolean istouchEffect = setupprefs.getBoolean("touchEffect", true);
        if (istouchEffect) {
            TouchEffect.setChecked(true);
        } else {
            TouchEffect.setChecked(false);
        }
        TouchEffect.setOnPreferenceChangeListener((preference, newValue) -> {
            // チェックステータス取得
            boolean check = (Boolean) newValue;
            if (check) {
                SharedPreferences prefTouch = getActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefTouch.edit();
                editor.putBoolean("touchEffect", true);
                editor.apply();
            } else {
                SharedPreferences prefTouch = getActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefTouch.edit();
                editor.putBoolean("touchEffect", false);
                editor.apply();
            }
            return true;
        });

        boolean isUpdate = setupprefs.getBoolean("updateDialog", true);
        if (isUpdate) {
            Update.setChecked(true);
        } else {
            Update.setChecked(false);
        }
        Update.setOnPreferenceChangeListener((preference, newValue) -> {
            // チェックステータス取得
            boolean check = (Boolean) newValue;
            if (check) {
                SharedPreferences prefTouch = getActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefTouch.edit();
                editor.putBoolean("updateDialog", true);
                editor.apply();
            } else {
                SharedPreferences prefTouch = getActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefTouch.edit();
                editor.putBoolean("updateDialog", false);
                editor.apply();
            }
            return true;
        });
    }
}
