package com.aozora.aozora;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.TextureView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.VideoView;

public class BootingMOV extends Activity {
    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME = "selected_theme";
    private static final int THEME_LIGHT = 0;
    private static final int THEME_DARK = 1;
    private static final int THEME_SYSTEM = 2;
    private VideoView tutorialvideo;
    private ImageView background;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applySavedTheme();
        setContentView(R.layout.az_mov);
        TouchEffectView.attach(getWindow());
        background = findViewById(R.id.background);
        tutorialvideo = findViewById(R.id.azmov);
        applySavedMov();
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, 3000);
    }

    private void tutorialSetting(String videouri) {
        tutorialvideo.setVideoURI(Uri.parse(videouri));
        tutorialvideo.setOnPreparedListener(mp -> {
            mp.setLooping(false);  // ループ再生OFF
            mp.setVolume(0f, 0f); // ミュート
            tutorialvideo.start();
        });
    }

    private void applySavedTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int theme = prefs.getInt(KEY_THEME, THEME_SYSTEM);
        int black = getResources().getColor(R. color. backgroundBlack);
        int white = getResources().getColor(R. color. backgroundWhite);

        switch (theme) {
            case THEME_LIGHT:
            case THEME_DARK:
            case THEME_SYSTEM:
            default:
                setTheme(android.R.style.Theme_Holo_Light_NoActionBar);
                break;
        }
    }

    private void applySavedMov() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int theme = prefs.getInt(KEY_THEME, THEME_SYSTEM);
        int black = getResources().getColor(R.color.black);
        int white = getResources().getColor(R.color.white_mov);

        switch (theme) {
            case THEME_LIGHT:
            case THEME_DARK:
            case THEME_SYSTEM:
            default:
                String animation_path_white = "android.resource://" + getPackageName() + "/" + R.raw.az_white;
                tutorialSetting(animation_path_white);
                background.setBackgroundColor(white);
                break;
        }
    }

}
