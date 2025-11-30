package com.aozora.aozora;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.app.Activity;
import android.widget.Toast;

public class Ior1orl extends Activity {
    private EditText editText;
    private TextView textViewResult;
    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME = "selected_theme";
    private static final int THEME_LIGHT = 0;
    private static final int THEME_DARK = 1;
    private static final int THEME_SYSTEM = 2;
    private ImageView Background;

    private static final String KEY_BACKGROUND = "selected_background";
    private static final int BACKGROUND1 = 0;
    private static final int BACKGROUND2 = 1;
    private static final int BACKGROUND3 = 2;
    private static final int BACKGROUND4 = 3;
    private static final int BACKGROUND_CUSTOM = 4;
    private static final String KEY_IMAGE_URI = "image_uri";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applySavedTheme();
        setContentView(R.layout.ior1orl);
        Background = findViewById(R.id.background);
        applySavedBackground();
        applyBackTheme();

        editText = findViewById(R.id.editIor1orl);
        Button buttonCheck = findViewById(R.id.buttonCheck);
        textViewResult = findViewById(R.id.textViewResult);

        buttonCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkCharacter();
            }
        });
        // Action Bar が表示されているか確認
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void applySavedBackground() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int theme = prefs.getInt(KEY_BACKGROUND, BACKGROUND1);
        ImageView BackgroundImage = findViewById(R.id.backgroundImage);
        switch (theme) {
            case BACKGROUND1:
                BackgroundImage.setVisibility(View.VISIBLE);
                Background.setVisibility(View.VISIBLE);
                BackgroundImage.setImageResource(R.drawable.setupback);
                break;
            case BACKGROUND2:
                BackgroundImage.setVisibility(View.VISIBLE);
                Background.setVisibility(View.VISIBLE);
                BackgroundImage.setImageResource(R.drawable.background2);
                break;
            case BACKGROUND3:
                BackgroundImage.setVisibility(View.VISIBLE);
                Background.setVisibility(View.VISIBLE);
                BackgroundImage.setImageResource(R.drawable.background3);
                break;
            case BACKGROUND4:
                BackgroundImage.setVisibility(View.GONE);
                Background.setVisibility(View.GONE);
                break;
            case BACKGROUND_CUSTOM:
            default:
                String uriString = prefs.getString(KEY_IMAGE_URI, null);
                if (uriString != null) {
                    Uri savedUri = Uri.parse(uriString);
                    BackgroundImage.setVisibility(View.VISIBLE);
                    Background.setVisibility(View.VISIBLE);
                    BackgroundImage.setImageURI(savedUri);
                } else {
                    BackgroundImage.setVisibility(View.VISIBLE);
                    Background.setVisibility(View.VISIBLE);
                    BackgroundImage.setImageResource(R.drawable.setupback);
                }
                break;
        }
    }

    private void applySavedTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int theme = prefs.getInt(KEY_THEME, THEME_SYSTEM);
        int black = getResources().getColor(R. color. backgroundBlack);
        int white = getResources().getColor(R. color. backgroundWhite);

        switch (theme) {
            case THEME_LIGHT:
                setTheme(android.R.style.Theme_Holo_Light);
                break;
            case THEME_DARK:
                setTheme(android.R.style.Theme_Holo);
                break;
            case THEME_SYSTEM:
            default:
                break;
        }
    }

    private void applyBackTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int theme = prefs.getInt(KEY_THEME, THEME_SYSTEM);
        int black = getResources().getColor(R. color. backgroundBlack);
        int white = getResources().getColor(R. color. backgroundWhite);

        switch (theme) {
            case THEME_LIGHT:
                Background.setBackgroundColor(white);
                break;
            case THEME_DARK:
                Background.setBackgroundColor(black);
                break;
            case THEME_SYSTEM:
            default:
                // OS 側の設定に従う
                int nightModeFlags = getResources().getConfiguration().uiMode
                        & android.content.res.Configuration.UI_MODE_NIGHT_MASK;

                if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                    Background.setBackgroundColor(black);
                } else {
                    Background.setBackgroundColor(white);
                }
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
            finish();
            overridePendingTransition(R.anim.no_animation,  R.anim.slide_out_down_low);
        }
        if (item.getItemId() == android.R.id.home) {
            finish(); // 前の画面に戻る
            overridePendingTransition(R.anim.no_animation,  R.anim.slide_out_down_low);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        finish(); // 前の画面に戻る
        overridePendingTransition(R.anim.no_animation,  R.anim.slide_out_down_low);
    }

    private void checkCharacter() {
        String input = editText.getText().toString().trim();
        if (input.length() != 1) {
            textViewResult.setText("1文字を入力してください");
            return;
        }

        char c = input.charAt(0);
        String result;

        switch (c) {
            case 'I':
                result = "アルファベットの大文字 I(ｱｲ) です";
                break;
            case 'l':
                result = "アルファベットの小文字 l(ｴﾙ) です";
                break;
            case '1':
                result = "数字の 1(ｲﾁ) です";
                break;
            default:
                result = "I, l, 1 のいずれかを入力してください";
        }

        textViewResult.setText(result);
    }
}
