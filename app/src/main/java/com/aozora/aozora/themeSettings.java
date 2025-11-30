package com.aozora.aozora;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.app.Activity;
import android.widget.Toast;

public class themeSettings extends Activity {

    private static final String PREFS_NAME = "theme_prefs";
    private static final String POPUP_PREFS_NAME = "popup_prefs";
    private static final String ARROW_PREFS_NAME = "arrow_prefs";
    private static final String KEY_THEME = "selected_theme";
    private static final String KEY_POPUP = "selected_popupbutton";
    private static final String KEY_ARROW = "selected_arrowbutton";

    private static final int THEME_LIGHT = 0;
    private static final int THEME_DARK = 1;
    private static final int THEME_SYSTEM = 2;

    private static final int POPUPBUTTON_LIGHT = 0;
    private static final int POPUPBUTTON_DARK = 1;
    private static final int POPUPBUTTON_SYSTEM = 2;

    private static final int ARROWBUTTON_OLD = 0;
    private static final int ARROWBUTTON_NEW = 1;
    private static final int ARROWBUTTON_IOS = 2;

    private ImageButton themeButton, forwardButton, backButton, popupButton, reloadButton, hideButton;
    private TextView tabCount;
    private LinearLayout bottombar;
    private boolean reboot = false;
    private ColorFilter invertFilter, grayFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.themesettings);

        themeButton = findViewById(R.id.theme_example_image);
        themeButton.setImageResource(R.drawable.aozora_default);
        backButton = findViewById(R.id.backButton);
        forwardButton = findViewById(R.id.forwardButton);
        popupButton = findViewById(R.id.action_popup);
        reloadButton = findViewById(R.id.action_reload);
        tabCount = findViewById(R.id.tabCountTextView);
        hideButton = findViewById(R.id.hidebottom);
        bottombar = findViewById(R.id.bottomBarSample);

        // 色反転フィルタを準備
        ColorMatrix colorMatrix_Invert = new ColorMatrix(new float[] {
                -1,  0,  0,  0, 255, // R
                0, -1,  0,  0, 255, // G
                0,  0, -1,  0, 255, // B
                0,  0,  0,  1,   0  // A
        });
        ColorMatrix colorMatrix_Gray75 = new ColorMatrix(new float[] {
                0, 0, 0, 0, 107, // R
                0, 0, 0, 0, 107, // G
                0, 0, 0, 0, 107, // B
                0, 0, 0, 1,   0  // A
        });
        invertFilter = new ColorMatrixColorFilter(colorMatrix_Invert);
        grayFilter = new ColorMatrixColorFilter(colorMatrix_Gray75);

        // 選択済みテーマを先に適用
        Spinner stylespinner = findViewById(R.id.setup_styleList);
        Spinner popupspinner = findViewById(R.id.popupbutton_stylelist);
        Spinner arrowspinner = findViewById(R.id.arrowbutton_stylelist);
        ArrayAdapter<CharSequence> popupadapter = ArrayAdapter.createFromResource(
                themeSettings.this,
                R.array.popupbutton_options,
                android.R.layout.simple_spinner_item
        );
        ArrayAdapter<CharSequence> arrowadapter = ArrayAdapter.createFromResource(
                themeSettings.this,
                R.array.arrowbutton_options,
                android.R.layout.simple_spinner_item
        );
        ArrayAdapter<CharSequence> styleadapter = ArrayAdapter.createFromResource(
                themeSettings.this,
                R.array.theme_options,
                android.R.layout.simple_spinner_item
        );
        styleadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        popupadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        arrowadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        stylespinner.setAdapter(styleadapter);
        popupspinner.setAdapter(popupadapter);
        arrowspinner.setAdapter(arrowadapter);
        // 現在の選択を反映
        applySavedTheme();
        applySavedPopup();
        applySavedArrow();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int savedTheme = prefs.getInt(KEY_THEME, THEME_SYSTEM);
        stylespinner.setSelection(savedTheme);
        SharedPreferences popupprefs = getSharedPreferences(POPUP_PREFS_NAME, MODE_PRIVATE);
        int savedPopup = popupprefs.getInt(KEY_POPUP, POPUPBUTTON_SYSTEM);
        popupspinner.setSelection(savedPopup);
        SharedPreferences arrowprefs = getSharedPreferences(ARROW_PREFS_NAME, MODE_PRIVATE);
        int savedArrow = arrowprefs.getInt(KEY_ARROW, ARROWBUTTON_NEW);
        arrowspinner.setSelection(savedArrow);

        stylespinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                if (position != savedTheme) {
                    saveTheme(position);
                    recreate(); // Activity 再起動でテーマ反映
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        popupspinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                if (position != savedPopup) {
                    savePopup(position);
                    recreate(); // Activity 再起動でテーマ反映
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        arrowspinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                if (position != savedArrow) {
                    saveArrow(position);
                    recreate(); // Activity 再起動でテーマ反映
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Action Bar が表示されているか確認
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void saveTheme(int themeMode) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putInt(KEY_THEME, themeMode);
        editor.apply();
        applySavedTheme();
    }

    private void savePopup(int popupMode) {
        SharedPreferences.Editor editor = getSharedPreferences(POPUP_PREFS_NAME, MODE_PRIVATE).edit();
        editor.putInt(KEY_POPUP, popupMode);
        editor.apply();
        applySavedPopup();
    }

    private void saveArrow(int arrowMode) {
        SharedPreferences.Editor editor = getSharedPreferences(ARROW_PREFS_NAME, MODE_PRIVATE).edit();
        editor.putInt(KEY_ARROW, arrowMode);
        editor.apply();
        applySavedArrow();
    }

    private void applySavedPopup() {
        SharedPreferences prefs = getSharedPreferences(POPUP_PREFS_NAME, MODE_PRIVATE);
        int theme = prefs.getInt(KEY_POPUP, POPUPBUTTON_SYSTEM);

        switch (theme) {
            case POPUPBUTTON_LIGHT:
                popupButton.setImageResource(R.mipmap.aozora1);
                popupButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(themeSettings.this, "デバイス側でのダークモードの有無に関わらず、ライトスタイルにします.", Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            case POPUPBUTTON_DARK:
                popupButton.setImageResource(R.mipmap.aozora);
                popupButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(themeSettings.this, "デバイス側でのダークモードの有無に関わらず、ダークスタイルにします.", Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            case POPUPBUTTON_SYSTEM:
            default:
                popupButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(themeSettings.this, "デバイス側でのダークモードの設定に合わせて、自動的に変更されます.", Toast.LENGTH_SHORT).show();
                    }
                });
                break;
        }
    }

    private void applySavedArrow() {
        SharedPreferences prefs = getSharedPreferences(ARROW_PREFS_NAME, MODE_PRIVATE);
        int theme = prefs.getInt(KEY_ARROW, ARROWBUTTON_NEW);

        switch (theme) {
            case ARROWBUTTON_OLD:
                backButton.setImageResource(R.drawable.back);
                forwardButton.setImageResource(R.drawable.forward);
                break;
            case ARROWBUTTON_IOS:
                backButton.setImageResource(R.drawable.ios_back);
                forwardButton.setImageResource(R.drawable.ios_forward);
                break;
            case ARROWBUTTON_NEW:
            default:
                backButton.setImageResource(R.drawable.ic_sysbar_back);
                forwardButton.setImageResource(R.drawable.ic_sysbar_forward);
                break;
        }
    }

    private void applySavedTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int theme = prefs.getInt(KEY_THEME, THEME_SYSTEM);
        SharedPreferences popupprefs = getSharedPreferences(POPUP_PREFS_NAME, MODE_PRIVATE);
        int popuptheme = popupprefs.getInt(KEY_POPUP, POPUPBUTTON_SYSTEM);

        switch (theme) {
            case THEME_LIGHT:
                themeButton.setImageResource(R.drawable.aozora_white);
                themeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(themeSettings.this, "デバイス側でのダークモードの有無に関わらず、ライトモードにします.", Toast.LENGTH_SHORT).show();
                    }
                });
                int whitecolor = getResources().getColor(R.color.white);
                int darktext = getResources().getColor(R.color.textdark);
                bottombar.setBackgroundColor(whitecolor);
                backButton.setColorFilter(grayFilter);
                forwardButton.setColorFilter(grayFilter);
                reloadButton.setColorFilter(grayFilter);
                hideButton.setColorFilter(grayFilter);
                tabCount.setTextColor(darktext);
                switch (popuptheme) {
                    case POPUPBUTTON_SYSTEM:
                        popupButton.setImageResource(R.mipmap.aozora1);
                        break;
                }
                break;
            case THEME_DARK:
                themeButton.setImageResource(R.drawable.aozora_dark);
                themeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(themeSettings.this, "デバイス側でのダークモードの有無に関わらず、ダークモードにします.", Toast.LENGTH_SHORT).show();
                    }
                });
                int darkcolor = getResources().getColor(R.color.black);
                int whitetext = getResources().getColor(R.color.white);
                bottombar.setBackgroundColor(darkcolor);
                backButton.clearColorFilter();
                forwardButton.clearColorFilter();
                reloadButton.clearColorFilter();
                hideButton.clearColorFilter();
                tabCount.setTextColor(whitetext);
                break;
            case THEME_SYSTEM:
            default:
                themeButton.setImageResource(R.drawable.aozora_default);
                themeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(themeSettings.this, "デバイス側でのダークモードの設定に合わせて、自動的に変更されます.", Toast.LENGTH_SHORT).show();
                    }
                });
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_ior1orl, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.ior1orl_close) {
            rebootDialog();
        }
        if (item.getItemId() == android.R.id.home) {
            rebootDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void rebootDialog() {
        new AlertDialog.Builder(this)
                .setMessage("テーマの変更を反映するには再起動が必要です")
                .setPositiveButton("キャンセル", (dialog, which) -> {
                    
                })
                .setNegativeButton("再起動", (dialog, which) -> {
                    //Intent intent = new Intent(this, BootingActivity.class);
                    SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("rebootApp", true);
                    editor.apply();
                    finish();
                    overridePendingTransition(0, 0);
                    //startActivity(intent);
                })
                .setNeutralButton("後で", (dialog, which) -> {
                    finish();
                })
                .show();
    }

    @Override
    public void onBackPressed() {
        rebootDialog();
    }

}
