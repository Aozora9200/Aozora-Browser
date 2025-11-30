package com.aozora.aozora;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BootingActivity extends Activity {

    private static final long ICON_ANIM_MS = 600L;

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
    private ImageView Background;

    private static final String POPUP_PREFS_NAME = "popup_prefs";
    private static final String KEY_POPUP = "selected_popupbutton";
    private static final int POPUPBUTTON_LIGHT = 0;
    private static final int POPUPBUTTON_DARK = 1;
    private static final int POPUPBUTTON_SYSTEM = 2;

    private MediaPlayer mediaPlayer;
    private long splashStartTime;
    private ExecutorService ioExecutor;
    private Handler mainHandler;
    private ProgressBar progressBar;

    private View splash;
    private ImageView splashIcon;
    private ProgressBar splashSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        applySavedTheme();
        SharedPreferences setupprefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean bootScreen = setupprefs.getBoolean("bootScreen", true);
        if (!bootScreen) {
            Intent intent = new Intent(this, MainActivity.class);
            finish(); // ← 今のアクティビティを閉じたい場合
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            return;
        }
        setContentView(R.layout.booting);
        Background = findViewById(R.id.background);
        applySavedBackground();
        applyBackTheme();
        initializeViews();
        applySavedPopup();
        initializeThreading();
        onProgressChanged(40);
        ioExecutor.execute(this::loadSplashIconAndPlaySound);
        splashStartTime = System.currentTimeMillis();
        playStartupAnimation();
        new Handler().postDelayed(() -> {
            onProgressChanged(100);
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish(); // ← 今のアクティビティを閉じたい場合
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, 2000); // 2秒 (2000ミリ秒)
    }

    private void applySavedPopup() {
        SharedPreferences prefs = getSharedPreferences(POPUP_PREFS_NAME, MODE_PRIVATE);
        int popup = prefs.getInt(KEY_POPUP, POPUPBUTTON_SYSTEM);
        SharedPreferences themeprefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int theme = themeprefs.getInt(KEY_THEME, THEME_SYSTEM);

        switch (popup) {
            case POPUPBUTTON_LIGHT:
                splashIcon.setImageResource(R.mipmap.aozora1);
                break;
            case POPUPBUTTON_DARK:
                splashIcon.setImageResource(R.mipmap.aozora);
                break;
            case POPUPBUTTON_SYSTEM:
            default:
                switch (theme) {
                    case THEME_LIGHT:
                        splashIcon.setImageResource(R.mipmap.aozora1);
                        break;
                    case THEME_DARK:
                        splashIcon.setImageResource(R.mipmap.aozora);
                        break;
                    case THEME_SYSTEM:
                    default:
                        // OS 側の設定に従う
                        int nightModeFlags = getResources().getConfiguration().uiMode
                                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;

                        if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                            splashIcon.setImageResource(R.mipmap.aozora);
                        } else {
                            splashIcon.setImageResource(R.mipmap.aozora1);
                        }
                        break;
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
                setTheme(android.R.style.Theme_Holo_Light_NoActionBar_Fullscreen);
                break;
            case THEME_DARK:
                setTheme(android.R.style.Theme_Holo_NoActionBar_Fullscreen);
                break;
            case THEME_SYSTEM:
            default:
                break;
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

    private void onProgressChanged(int percentage){
        Animator animation = ObjectAnimator.ofInt(progressBar,"progress",percentage);
        animation.setDuration(500); // 0.5秒間でアニメーションする
        animation.setInterpolator(new DecelerateInterpolator());
        animation.start();
    }

    private void loadSplashIconAndPlaySound() {
        Bitmap bitmap = null;
        InputStream is = null;
        try {
            is = getAssets().open("aozora.png");
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, opt);
            closeQuietly(is);

            opt.inSampleSize = calculateInSampleSize(opt.outWidth, opt.outHeight, 512, 512);
            opt.inJustDecodeBounds = false;
            opt.inPreferredConfig = Bitmap.Config.RGB_565;
            opt.inDither = false;
            opt.inMutable = false;
            is = getAssets().open("aozora.png");
            bitmap = BitmapFactory.decodeStream(is, null, opt);
        } catch (IOException e) {

        } finally {
            closeQuietly(is);
        }

        final Bitmap finalBitmap = bitmap;
        mainHandler.post(() -> {
            //if (splashIcon != null && finalBitmap != null) splashIcon.setImageBitmap(finalBitmap);
            SharedPreferences setupprefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
            boolean bootSound = setupprefs.getBoolean("bootSound", true);
            if (bootSound) {
                playBootSound();
            }
        });
    }

    private static int calculateInSampleSize(int width, int height, int reqWidth, int reqHeight) {
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height >> 1;
            int halfWidth = width >> 1;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize <<= 1;
            }
        }
        return inSampleSize;
    }

    private void initializeViews() {
        splash = findViewById(R.id.splash);
        splashIcon = findViewById(R.id.splashIcon);
        splashSpinner = findViewById(R.id.splashSpinner);
        progressBar = findViewById(R.id.booting_progress);
    }

    private void initializeThreading() {
        mainHandler = new Handler(Looper.getMainLooper());
        ioExecutor = Executors.newSingleThreadExecutor();
    }

    private static void closeQuietly(InputStream is) {
        if (is != null) try { is.close(); } catch (IOException ignored) {}
    }

    private void playBootSound() {
        releaseMediaPlayer();
        try {
            MediaPlayer mp = new MediaPlayer();
            mediaPlayer = mp;
            android.content.res.AssetFileDescriptor afd = getAssets().openFd("boot.mp3");
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mp.setLooping(false);
            mp.setVolume(1.0f, 1.0f);
            mp.setOnPreparedListener(MediaPlayer::start);
            mp.setOnCompletionListener(m -> releaseMediaPlayer());
            mp.setOnErrorListener((m, what, extra) -> { releaseMediaPlayer(); return true; });
            mp.prepareAsync();
        } catch (IOException e) {

            releaseMediaPlayer();
        }
    }
    private void releaseMediaPlayer() {
        final MediaPlayer mp = mediaPlayer;
        mediaPlayer = null;
        if (mp != null) {
            try { mp.stop(); } catch (Throwable ignored) {}
            mp.release();
        }
    }

    private void playStartupAnimation() {
        PropertyValuesHolder sx = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.8f, 1f);
        PropertyValuesHolder sy = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.8f, 1f);
        ObjectAnimator scale = ObjectAnimator.ofPropertyValuesHolder(splashIcon, sx, sy);
        AnimatorSet set = new AnimatorSet();
        set.play(scale);
        set.setDuration(ICON_ANIM_MS);
        set.setInterpolator(new BounceInterpolator());
        set.start();
    }
}
