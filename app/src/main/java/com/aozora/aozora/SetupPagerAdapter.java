package com.aozora.aozora;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;


import androidx.viewpager.widget.PagerAdapter;

public class SetupPagerAdapter extends PagerAdapter {
    private Activity activity;
    private int[] layouts;
    private SparseArray<View> views = new SparseArray<>();
    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME = "selected_theme";
    private static final String KEY_UA_ENABLED = "ua_enabled";
    private static final String KEY_DESKUA_ENABLED = "deskua_enabled";
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String KEY_IMAGE_URI = "image_uri";
    private static final String PREF_NAME = "AdvancedBrowserPrefs";

    private boolean deskuaEnabled = false;
    private boolean ua_Enabled = false;

    private static final int THEME_LIGHT = 0;
    private static final int THEME_DARK = 1;
    private static final int THEME_SYSTEM = 2;

    private static final String KEY_BACKGROUND = "selected_background";
    private static final int BACKGROUND1 = 0;
    private static final int BACKGROUND2 = 1;
    private static final int BACKGROUND3 = 2;
    private static final int BACKGROUND4 = 3;
    private static final int BACKGROUND_CUSTOM = 4;

    private ImageButton themeButton, backgroundButton;

    public SetupPagerAdapter(Activity activity, int[] layouts) {
        this.activity = activity;
        this.layouts = layouts;
    }

    @Override
    public int getCount() {
        return layouts.length;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        LayoutInflater inflater = LayoutInflater.from(activity);
        View view = inflater.inflate(layouts[position], container, false);
        if (position == 0) {
            applySavedBackground();
        }
        if (position == 1) {
            CheckBox progressBarAnimation = view.findViewById(R.id.setup_checkbox_progressBar);
            themeButton = view.findViewById(R.id.theme_example_image);
            themeButton.setImageResource(R.drawable.aozora_default);
            SharedPreferences setupprefs = activity.getSharedPreferences("AppPrefs", MODE_PRIVATE);
            boolean ProgressBarAnimation = setupprefs.getBoolean("ProgressBarAnimation", true);
            if (ProgressBarAnimation) {
                progressBarAnimation.setChecked(true);
            } else {
                progressBarAnimation.setChecked(false);
            }
            progressBarAnimation.setOnClickListener(v -> {
                // チェックステータス取得
                boolean check = progressBarAnimation.isChecked();
                if (check) {
                    SharedPreferences prefs = activity.getSharedPreferences("AppPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("ProgressBarAnimation", true);
                    editor.apply();
                } else {
                    SharedPreferences prefs = activity.getSharedPreferences("AppPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("ProgressBarAnimation", false);
                    editor.apply();
                }
            });
            // 選択済みテーマを先に適用
            Spinner spinner = view.findViewById(R.id.setup_styleList);
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                    activity,
                    R.array.theme_options,
                    android.R.layout.simple_spinner_item
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            // 現在の選択を反映
            applySavedTheme();
            SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            int savedTheme = prefs.getInt(KEY_THEME, THEME_SYSTEM);
            spinner.setSelection(savedTheme);

            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                    if (position != savedTheme) {
                        saveTheme(position);
                        activity.recreate(); // Activity 再起動でテーマ反映
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
            Button helpProgressButton = view.findViewById(R.id.setup_help_progress);
            helpProgressButton.setOnClickListener(v -> {
                Intent intent = new Intent(activity, HelpProgress.class);
                activity.startActivity(intent);
                activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }
        if (position == 2) {
            backgroundButton = view.findViewById(R.id.background_example_image);
            Spinner spinner = view.findViewById(R.id.setup_backgroundList);
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                    activity,
                    R.array.background_options,
                    android.R.layout.simple_spinner_item
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            // 現在の選択を反映
            applySavedBackgroundImage();
            applySavedBackground();
            SharedPreferences Backgroundprefs = activity.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            int savedBackground = Backgroundprefs.getInt(KEY_BACKGROUND, BACKGROUND1);
            spinner.setSelection(savedBackground);

            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                    if (position != savedBackground) {
                        saveBackground(position);
                        activity.recreate(); // Activity 再起動でテーマ反映
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
            CheckBox desktopcheck = view.findViewById(R.id.setup_checkbox_desktop);
            CheckBox uacheck = view.findViewById(R.id.setup_checkbox_ua);
            CheckBox bottomcheck = view.findViewById(R.id.setup_checkbox_bottom);
            SharedPreferences setupprefs = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            SharedPreferences prefs = activity.getSharedPreferences("AppPrefs", MODE_PRIVATE);
            boolean hidebottom = prefs.getBoolean("hidebottom", false);
            deskuaEnabled = setupprefs.getBoolean(KEY_DESKUA_ENABLED, false);
            ua_Enabled = setupprefs.getBoolean(KEY_UA_ENABLED, false);
            if (deskuaEnabled) {
                desktopcheck.setChecked(true);
                uacheck.setChecked(false);
            } else {
                desktopcheck.setChecked(false);
            }
            if (ua_Enabled) {
                uacheck.setChecked(true);
                desktopcheck.setChecked(false);
            } else {
                uacheck.setChecked(false);
            }
            if (hidebottom) {
                bottomcheck.setChecked(true);
            } else {
                bottomcheck.setChecked(false);
            }
            bottomcheck.setOnClickListener(v -> {
                // チェックステータス取得
                boolean check = bottomcheck.isChecked();
                if (check) {
                    SharedPreferences bottomprefs = activity.getSharedPreferences("AppPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = bottomprefs.edit();
                    editor.putBoolean("hidebottom", true);
                    editor.apply();
                } else {
                    SharedPreferences bottomprefs = activity.getSharedPreferences("AppPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = bottomprefs.edit();
                    editor.putBoolean("hidebottom", false);
                    editor.apply();
                }
            });
            desktopcheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    uacheck.setChecked(false);

                    activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                            .edit().putBoolean(KEY_DESKUA_ENABLED, true).apply();
                } else {
                    activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                            .edit().putBoolean(KEY_DESKUA_ENABLED, false).apply();
                }
            });

            uacheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    desktopcheck.setChecked(false);

                    activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                            .edit().putBoolean(KEY_UA_ENABLED, true).apply();
                } else {
                    activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                            .edit().putBoolean(KEY_UA_ENABLED, false).apply();
                }
            });
            Button helpButton = view.findViewById(R.id.setup_help_ua);
            helpButton.setOnClickListener(v -> {
                Intent intent = new Intent(activity, HelpActivity.class);
                activity.startActivity(intent);
                activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }
        if (position == 3) {
            Button default_button = view.findViewById(R.id.setup_default);
            default_button.setOnClickListener(v -> {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
                activity.startActivity(intent);
                activity.overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            });
        }
        container.addView(view);
        views.put(position, view);
        return view;
    }

    private void saveTheme(int themeMode) {
        SharedPreferences.Editor editor = activity.getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putInt(KEY_THEME, themeMode);
        editor.apply();
        applySavedTheme();
    }

    private void applySavedTheme() {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int theme = prefs.getInt(KEY_THEME, THEME_SYSTEM);

        switch (theme) {
            case THEME_LIGHT:
                themeButton.setImageResource(R.drawable.aozora_white);
                themeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(activity, "デバイス側でのダークモードの有無に関わらず、ライトモードにします.", Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            case THEME_DARK:
                themeButton.setImageResource(R.drawable.aozora_dark);
                themeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(activity, "デバイス側でのダークモードの有無に関わらず、ダークモードにします.", Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            case THEME_SYSTEM:
            default:
                themeButton.setImageResource(R.drawable.aozora_default);
                themeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(activity, "デバイス側でのダークモードの設定に合わせて、自動的に変更されます.", Toast.LENGTH_SHORT).show();
                    }
                });
                break;
        }
    }

    private void saveBackground(int themeMode) {
        SharedPreferences.Editor editor = activity.getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putInt(KEY_BACKGROUND, themeMode);
        editor.apply();
        applySavedBackgroundImage();
    }

    private void applySavedBackground() {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int theme = prefs.getInt(KEY_BACKGROUND, BACKGROUND1);
        ImageView BackgroundImage = activity.findViewById(R.id.backgroundImage);
        ImageView Background = activity.findViewById(R.id.background);
        switch (theme) {
            case BACKGROUND1:
                prefs.edit().remove(KEY_IMAGE_URI).apply();
                BackgroundImage.setVisibility(View.VISIBLE);
                BackgroundImage.setImageResource(R.drawable.setupback);
                Background.setVisibility(View.GONE);
                break;
            case BACKGROUND2:
                prefs.edit().remove(KEY_IMAGE_URI).apply();
                BackgroundImage.setVisibility(View.VISIBLE);
                BackgroundImage.setImageResource(R.drawable.background2);
                Background.setVisibility(View.GONE);
                break;
            case BACKGROUND3:
                prefs.edit().remove(KEY_IMAGE_URI).apply();
                BackgroundImage.setVisibility(View.VISIBLE);
                BackgroundImage.setImageResource(R.drawable.background3);
                Background.setVisibility(View.VISIBLE);
                break;
            case BACKGROUND4:
                prefs.edit().remove(KEY_IMAGE_URI).apply();
                BackgroundImage.setVisibility(View.GONE);
                Background.setVisibility(View.GONE);
                break;
            case BACKGROUND_CUSTOM:
            default:
                String uriString = prefs.getString(KEY_IMAGE_URI, null);
                if (uriString != null) {
                    Uri savedUri = Uri.parse(uriString);
                    BackgroundImage.setVisibility(View.VISIBLE);
                    BackgroundImage.setImageURI(savedUri);
                    Background.setVisibility(View.VISIBLE);
                } else {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.setType("image/*");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    activity.startActivityForResult(intent, PICK_IMAGE_REQUEST);
                }
                break;
        }
    }

    private void applySavedBackgroundImage() {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int theme = prefs.getInt(KEY_BACKGROUND, BACKGROUND1);
        switch (theme) {
            case BACKGROUND1:
                backgroundButton.setVisibility(View.VISIBLE);
                backgroundButton.setImageResource(R.drawable.setupback);
                break;
            case BACKGROUND2:
                backgroundButton.setVisibility(View.VISIBLE);
                backgroundButton.setImageResource(R.drawable.background2);
                break;
            case BACKGROUND3:
                backgroundButton.setVisibility(View.VISIBLE);
                backgroundButton.setImageResource(R.drawable.background3);
                break;
            case BACKGROUND4:
                backgroundButton.setVisibility(View.INVISIBLE);
                break;
            case BACKGROUND_CUSTOM:
            default:
                String uriString = prefs.getString(KEY_IMAGE_URI, null);
                if (uriString != null) {
                    Uri savedUri = Uri.parse(uriString);
                    backgroundButton.setVisibility(View.VISIBLE);
                    backgroundButton.setImageURI(savedUri);
                } else {
                    backgroundButton.setVisibility(View.VISIBLE);
                    backgroundButton.setImageResource(R.drawable.setupback);
                }
        }
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
        views.remove(position);
    }
    // Activity からページのViewを取得できる
    public View getViewAt(int position) {
        return views.get(position);
    }
}