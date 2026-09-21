package com.aozora.aozora;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import app.rive.runtime.kotlin.RiveAnimationView;
import app.rive.runtime.kotlin.core.Rive;
import app.rive.runtime.kotlin.core.Loop;
import app.rive.runtime.kotlin.core.Direction;

public class LockSettingsActivity extends Activity {

    private static final String STARTUP_PASSWORD = "startup_password";
    private boolean startup = true;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private ImageButton nextButton, saveButton;
    private TextView setpassword_msg;
    private LinearLayout setpin_box, setpassword_box, none_box;
    private CheckBox usePassWordonStartup;
    private CheckBox passwordSkipCheckBox;
    private Spinner passwordSkipSpinner;

    private static final String PREF_SKIP_MINUTES =
            "password_skip_minutes";

    private static final String PREF_SKIP_MINUTES_BACKUP =
            "password_skip_minutes_backup";

    private PasswordManager passwordManager;
    private SharedPreferences appPrefs;

    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME = "selected_theme";
    private static final int THEME_LIGHT = 0;
    private static final int THEME_DARK = 1;
    private static final int THEME_SYSTEM = 2;

    private static final String KEY_BACKGROUND = "selected_background";
    private static final int BACKGROUND1 = 0;
    private static final int BACKGROUND2 = 1;
    private static final int BACKGROUND3 = 2;
    private static final int BACKGROUND4 = 3;
    private static final int BACKGROUND_CUSTOM = 4;
    private static final String KEY_IMAGE_URI = "image_uri";
    private static final String PREF_USE_PIN = "use_pin";

    private ImageView Background;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        applySavedTheme();

        setContentView(
                R.layout.activity_lock_setting
        );
        TouchEffectView.attach(getWindow());
        Background = findViewById(R.id.background);
        applySavedBackground();
        applyBackTheme();

        usePassWordonStartup = findViewById(R.id.usePassword);

        passwordSkipCheckBox =
                findViewById(R.id.passwordSkipCheckBox);

        passwordSkipSpinner =
                findViewById(R.id.passwordSkipSpinner);

        setpassword_msg =
                findViewById(R.id.setpassword_msg);

        none_box =
                findViewById(R.id.none);

        setpassword_box =
                findViewById(R.id.set_password);

        setpin_box =
                findViewById(R.id.set_pin);

        new Handler().postDelayed(() -> {
            Animation anim =
                    AnimationUtils.loadAnimation(
                            LockSettingsActivity.this,
                            R.anim.fade
                    );

            setpassword_box.startAnimation(anim);
            setpassword_box.setVisibility(View.VISIBLE);

            none_box.startAnimation(anim);
            none_box.setVisibility(View.VISIBLE);

            setpin_box.startAnimation(anim);
            setpin_box.setVisibility(View.VISIBLE);

            usePassWordonStartup.startAnimation(anim);
            usePassWordonStartup.setVisibility(View.VISIBLE);

            setpassword_msg.startAnimation(anim);
            setpassword_msg.setVisibility(View.VISIBLE);

            passwordSkipCheckBox.startAnimation(anim);
            passwordSkipCheckBox.setVisibility(View.VISIBLE);

            passwordSkipSpinner.startAnimation(anim);
            passwordSkipSpinner.setVisibility(View.VISIBLE);

        }, 500);

        passwordManager =
                new PasswordManager(this);

        appPrefs =
                getSharedPreferences(
                        "AppPrefs",
                        MODE_PRIVATE
                );

// 初回起動時のデフォルト値
        if (!appPrefs.contains(PREF_SKIP_MINUTES)) {

            appPrefs.edit()
                    .putInt(PREF_SKIP_MINUTES, 1)
                    .putInt(PREF_SKIP_MINUTES_BACKUP, 1)
                    .apply();
        }

// プルダウンの選択肢
        String[] skipTimeLabels = {
                "1分",
                "5分",
                "10分",
                "15分",
                "30分",
                "60分"
        };

        int[] skipTimeValues = {
                1,
                5,
                10,
                15,
                30,
                60
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        skipTimeLabels
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        passwordSkipSpinner.setAdapter(adapter);

// 保存されている時間
        int savedSkipMinutes =
                appPrefs.getInt(
                        PREF_SKIP_MINUTES,
                        1
                );

// 現在の設定が0ならOFF
        passwordSkipCheckBox.setChecked(
                savedSkipMinutes > 0
        );

// プルダウンの選択位置
        int selectedIndex = 0;

        for (int i = 0; i < skipTimeValues.length; i++) {

            if (skipTimeValues[i] == savedSkipMinutes) {
                selectedIndex = i;
                break;
            }
        }

// 0の場合はバックアップ値を使用
        if (savedSkipMinutes == 0) {

            int backup =
                    appPrefs.getInt(
                            PREF_SKIP_MINUTES_BACKUP,
                            1
                    );

            for (int i = 0; i < skipTimeValues.length; i++) {

                if (skipTimeValues[i] == backup) {
                    selectedIndex = i;
                    break;
                }
            }
        }

        passwordSkipSpinner.setSelection(selectedIndex);

// 初期状態
        passwordSkipSpinner.setEnabled(
                passwordSkipCheckBox.isChecked()
        );

// プルダウン変更
        passwordSkipSpinner.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {

                        if (!passwordSkipCheckBox.isChecked()) {
                            return;
                        }

                        int minutes =
                                skipTimeValues[position];

                        appPrefs.edit()
                                .putInt(
                                        PREF_SKIP_MINUTES,
                                        minutes
                                )
                                .putInt(
                                        PREF_SKIP_MINUTES_BACKUP,
                                        minutes
                                )
                                .apply();
                    }

                    @Override
                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent) {
                    }
                });

// チェックON/OFF
        passwordSkipCheckBox.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {

                    if (isChecked) {

                        // OFFにする前の値を復元
                        int backup =
                                appPrefs.getInt(
                                        PREF_SKIP_MINUTES_BACKUP,
                                        1
                                );

                        if (backup <= 0) {
                            backup = 1;
                        }

                        appPrefs.edit()
                                .putInt(
                                        PREF_SKIP_MINUTES,
                                        backup
                                )
                                .apply();

                        // Spinnerも復元
                        for (int i = 0; i < skipTimeValues.length; i++) {

                            if (skipTimeValues[i] == backup) {

                                passwordSkipSpinner
                                        .setSelection(i);

                                break;
                            }
                        }

                        passwordSkipSpinner.setEnabled(true);

                    } else {

                        // OFFならスキップ時間を0にする
                        int current =
                                appPrefs.getInt(
                                        PREF_SKIP_MINUTES,
                                        1
                                );

                        if (current > 0) {

                            appPrefs.edit()
                                    .putInt(
                                            PREF_SKIP_MINUTES_BACKUP,
                                            current
                                    )
                                    .apply();
                        }

                        appPrefs.edit()
                                .putInt(
                                        PREF_SKIP_MINUTES,
                                        0
                                )
                                .apply();

                        passwordSkipSpinner.setEnabled(false);
                    }
                });

        SharedPreferences setupprefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        startup = setupprefs.getBoolean(STARTUP_PASSWORD, true);
        if (startup) {
            usePassWordonStartup.setChecked(true);
        } else {
            usePassWordonStartup.setChecked(false);
        }

        usePassWordonStartup.setOnCheckedChangeListener((buttonView, isChecked) -> {
            getSharedPreferences("AppPrefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean(STARTUP_PASSWORD, isChecked)
                    .apply();
        });

        none_box.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(LockSettingsActivity.this)
                        .setTitle("パスワード保護機能を無効にしますか？")
                        .setMessage("パスワードがないと、パスワード保護機能は利用できません。")
                        .setNegativeButton("キャンセル", (dialog, which) -> {
                            return;
                        })
                        .setPositiveButton("無効にする", (dialog, which) -> {
                            passwordManager.clearAll();
                            finish();
                        })
                        .show();
            }
        });

        setpassword_box.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                passwordManager = new PasswordManager(LockSettingsActivity.this);
                if (passwordManager.isPasswordSet()) {
                    Intent intent = new Intent(LockSettingsActivity.this, PasswordActivity.class);

                    intent.putExtra(
                            "destination_activity",
                            "PasswordSettingActivity"
                    );

                    intent.putExtra(
                            "requirePasswordOnFirstLaunch",
                            false
                    );

                    intent.putExtra("usePasswordSkip", false);

                    startActivity(intent);
                    return;
                } else {
                    Intent intent = new Intent(LockSettingsActivity.this, PasswordSettingActivity.class);
                    startActivity(intent);
                    return;
                }
            }
        });

        setpin_box.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                passwordManager = new PasswordManager(LockSettingsActivity.this);
                if (passwordManager.isPasswordSet()) {
                    Intent intent = new Intent(LockSettingsActivity.this, PasswordActivity.class);

                    intent.putExtra(
                            "destination_activity",
                            "PinSettingActivity"
                    );

                    intent.putExtra(
                            "requirePasswordOnFirstLaunch",
                            false
                    );

                    intent.putExtra("usePasswordSkip", false);

                    startActivity(intent);
                    return;
                } else {
                    Intent intent = new Intent(LockSettingsActivity.this, PinSettingActivity.class);
                    startActivity(intent);
                    return;
                }
            }
        });

    }

    private void applyBackTheme() {

        SharedPreferences prefs =
                getSharedPreferences(
                        PREFS_NAME,
                        MODE_PRIVATE
                );

        int theme =
                prefs.getInt(
                        KEY_THEME,
                        THEME_SYSTEM
                );

        int black =
                getResources().getColor(
                        R.color.backgroundBlack
                );

        int white =
                getResources().getColor(
                        R.color.backgroundWhite
                );

        switch (theme) {

            case THEME_LIGHT:
                Background.setBackgroundColor(white);
                break;

            case THEME_DARK:
                Background.setBackgroundColor(black);
                break;

            case THEME_SYSTEM:
            default:

                int nightModeFlags =
                        getResources()
                                .getConfiguration()
                                .uiMode
                                & android.content.res.Configuration
                                .UI_MODE_NIGHT_MASK;

                if (nightModeFlags ==
                        android.content.res.Configuration
                                .UI_MODE_NIGHT_YES) {

                    Background.setBackgroundColor(black);

                } else {

                    Background.setBackgroundColor(white);
                }

                break;
        }
    }

    private void applySavedTheme() {

        SharedPreferences prefs =
                getSharedPreferences(
                        PREFS_NAME,
                        MODE_PRIVATE
                );

        int theme =
                prefs.getInt(
                        KEY_THEME,
                        THEME_SYSTEM
                );

        switch (theme) {

            case THEME_LIGHT:

                setTheme(
                        android.R.style
                                .Theme_Holo_Light_NoActionBar_Fullscreen
                );

                break;

            case THEME_DARK:

                setTheme(
                        android.R.style
                                .Theme_Holo_NoActionBar_Fullscreen
                );

                break;

            case THEME_SYSTEM:
            default:
                break;
        }
    }

    private void applySavedBackground() {

        SharedPreferences prefs =
                getSharedPreferences(
                        PREFS_NAME,
                        MODE_PRIVATE
                );

        int theme =
                prefs.getInt(
                        KEY_BACKGROUND,
                        BACKGROUND1
                );

        ImageView BackgroundImage =
                findViewById(
                        R.id.backgroundImage
                );

        switch (theme) {

            case BACKGROUND1:

                BackgroundImage.setVisibility(
                        View.VISIBLE
                );

                Background.setVisibility(
                        View.VISIBLE
                );

                BackgroundImage.setImageResource(
                        R.drawable.setupback
                );

                break;

            case BACKGROUND2:

                BackgroundImage.setVisibility(
                        View.VISIBLE
                );

                Background.setVisibility(
                        View.VISIBLE
                );

                BackgroundImage.setImageResource(
                        R.drawable.background2
                );

                break;

            case BACKGROUND3:

                BackgroundImage.setVisibility(
                        View.VISIBLE
                );

                Background.setVisibility(
                        View.VISIBLE
                );

                BackgroundImage.setImageResource(
                        R.drawable.background3
                );

                break;

            case BACKGROUND4:

                BackgroundImage.setVisibility(
                        View.GONE
                );

                Background.setVisibility(
                        View.GONE
                );

                break;

            case BACKGROUND_CUSTOM:
            default:

                String uriString =
                        prefs.getString(
                                KEY_IMAGE_URI,
                                null
                        );

                if (uriString != null) {

                    Uri savedUri =
                            Uri.parse(uriString);

                    BackgroundImage.setVisibility(
                            View.VISIBLE
                    );

                    Background.setVisibility(
                            View.VISIBLE
                    );

                    BackgroundImage.setImageURI(
                            savedUri
                    );

                } else {

                    BackgroundImage.setVisibility(
                            View.VISIBLE
                    );

                    Background.setVisibility(
                            View.VISIBLE
                    );

                    BackgroundImage.setImageResource(
                            R.drawable.setupback
                    );
                }

                break;
        }
    }

}