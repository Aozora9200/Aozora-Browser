package com.aozora.aozora;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.TextureView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.app.Activity;
import android.widget.Toast;
import android.widget.VideoView;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.view.Surface;

import java.util.ArrayList;

public class NewFeatures extends Activity {
    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME = "selected_theme";
    private static final int THEME_LIGHT = 0;
    private static final int THEME_DARK = 1;
    private static final int THEME_SYSTEM = 2;
    private ImageView Background, tutorialimage;

    private static final String KEY_BACKGROUND = "selected_background";
    private static final int BACKGROUND1 = 0;
    private static final int BACKGROUND2 = 1;
    private static final int BACKGROUND3 = 2;
    private static final int BACKGROUND4 = 3;
    private static final int BACKGROUND_CUSTOM = 4;
    private static final String KEY_IMAGE_URI = "image_uri";
    private TextView help, title, detail;
    private LinearLayout controlScreen, tutorial;

    private FrameLayout list1, list2, list3, list4, list5;

    private ImageButton control_close, tutorial_close;
    private ImageButton canttouch;
    private boolean control_opening, tutorial_opening, maintutorial_opening;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applySavedTheme();
        setContentView(R.layout.activity_newfeatures);
        Background = findViewById(R.id.background);
        applySavedBackground();
        applyBackTheme();

        canttouch = findViewById(R.id.canttouch);
        control_close = findViewById(R.id.control_close);

        controlScreen = findViewById(R.id.controlScreen);

        tutorial =  findViewById(R.id.tutorial);
        tutorialimage = findViewById(R.id.tutorialimage);
        tutorial_close = findViewById(R.id.tutorial_close);

        title = findViewById(R.id.title);
        detail = findViewById(R.id.detail);

        list1 = findViewById(R.id.list1);
        list2 = findViewById(R.id.list2);
        list3 = findViewById(R.id.list3);
        list4 = findViewById(R.id.list4);
        list5 = findViewById(R.id.list5);

        list1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                control_opening = false;
                tutorial_opening = true;
                Animation animout = AnimationUtils.loadAnimation(NewFeatures.this, R.anim.fadeout);
                controlScreen.startAnimation(animout);
                controlScreen.setVisibility(View.GONE);
                Animation anim = AnimationUtils.loadAnimation(NewFeatures.this, R.anim.slide_in_up_low);
                tutorial.startAnimation(anim);
                tutorial.setVisibility(View.VISIBLE);
                tutorialimage.setImageResource(R.drawable.screenshot_topdark);
                String title = "モダンかつ懐かしいデザイン";
                String detail = "Coara Browser をベースに2011年頃のAndroid端末で主流だった立体的な Holo デザイン を使用しつつ モダンUI を取り込み、アニメーションや細部のデザインまでこだわりました。";
                tutorialSetting(title, detail);
            }
        });

        list2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                control_opening = false;
                tutorial_opening = true;
                Animation animout = AnimationUtils.loadAnimation(NewFeatures.this, R.anim.fadeout);
                controlScreen.startAnimation(animout);
                controlScreen.setVisibility(View.GONE);
                Animation anim = AnimationUtils.loadAnimation(NewFeatures.this, R.anim.slide_in_up_low);
                tutorial.startAnimation(anim);
                tutorial.setVisibility(View.VISIBLE);
                tutorialimage.setImageResource(R.drawable.helptools);
                String title = "多彩で便利なツール";
                String detail = "Aozora では、Coara Browser に搭載していたアプリを始め、様々なアプリや機能が搭載されています。ブラウジングをしながら、便利で多彩なツールを無料でご使用いただけます。";
                tutorialSetting(title, detail);
            }
        });

        list3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                control_opening = false;
                tutorial_opening = true;
                Animation animout = AnimationUtils.loadAnimation(NewFeatures.this, R.anim.fadeout);
                controlScreen.startAnimation(animout);
                controlScreen.setVisibility(View.GONE);
                Animation anim = AnimationUtils.loadAnimation(NewFeatures.this, R.anim.slide_in_up_low);
                tutorial.startAnimation(anim);
                tutorial.setVisibility(View.VISIBLE);
                tutorialimage.setImageResource(R.drawable.screenshot_topwhite);
                String title = "幅広いカスタマイズ性";
                String detail = "今まで固定デザインだった背景や下部バーを自由にカスタマイズできるようになりました。また、白デザインにも対応し、明るくモダンなデザインを使えるようになりました。";
                tutorialSetting(title, detail);
            }
        });

        list4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                control_opening = false;
                tutorial_opening = true;
                Animation animout = AnimationUtils.loadAnimation(NewFeatures.this, R.anim.fadeout);
                controlScreen.startAnimation(animout);
                controlScreen.setVisibility(View.GONE);
                Animation anim = AnimationUtils.loadAnimation(NewFeatures.this, R.anim.slide_in_up_low);
                tutorial.startAnimation(anim);
                tutorial.setVisibility(View.VISIBLE);
                tutorialimage.setImageResource(R.drawable.screenshot_settings);
                String title = "まとめられた設定";
                String detail = "今まで2つ存在していた設定を1つにまとめ、簡単に設定ができるようになりました。また、ツールと設定を分けることにより、ダイレクトにツールを開けるようになり、目的の項目が見つけやすくなりました。";
                tutorialSetting(title, detail);
            }
        });

        list5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                control_opening = false;
                tutorial_opening = true;
                Animation animout = AnimationUtils.loadAnimation(NewFeatures.this, R.anim.fadeout);
                controlScreen.startAnimation(animout);
                controlScreen.setVisibility(View.GONE);
                Animation anim = AnimationUtils.loadAnimation(NewFeatures.this, R.anim.slide_in_up_low);
                tutorial.startAnimation(anim);
                tutorial.setVisibility(View.VISIBLE);
                tutorialimage.setImageResource(R.drawable.screenshot_tabs);
                String title = "使いやすくなったタブ";
                String detail = "リスト式のタブから全画面式のタブへ変更し、更に使いやすく、洗練されたデザインになりました。";
                tutorialSetting(title, detail);
            }
        });

        tutorial_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                canttouch.setVisibility(View.VISIBLE);
                tutorial_opening = false;
                control_opening = true;
                Animation animout = AnimationUtils.loadAnimation(NewFeatures.this, R.anim.slide_out_down_low);
                tutorial.startAnimation(animout);
                tutorial.setVisibility(View.GONE);
                Animation anim = AnimationUtils.loadAnimation(NewFeatures.this, R.anim.fade);
                controlScreen.startAnimation(anim);
                controlScreen.setVisibility(View.VISIBLE);
                new Handler().postDelayed(() -> {
                    canttouch.setVisibility(View.GONE);
                }, 650);
            }
        });

        control_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // 前の画面に戻る
                overridePendingTransition(R.anim.no_animation,  R.anim.slide_out_down_low);
            }
        });
    }

    private void tutorialSetting(String titleText, String detailText) {
        title.setText(titleText);
        detail.setText(detailText);
    }

    private void help(String helpText) {
        Animation anim = AnimationUtils.loadAnimation(NewFeatures.this, R.anim.fade);
        Animation animout = AnimationUtils.loadAnimation(NewFeatures.this, R.anim.fadeout);
        help.startAnimation(animout);
        help.setVisibility(View.GONE);
        help.setText(helpText);
        help.startAnimation(anim);
        help.setVisibility(View.VISIBLE);
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
                setTheme(android.R.style.Theme_Holo_Light_NoActionBar);
                break;
            case THEME_DARK:
                setTheme(android.R.style.Theme_Holo_NoActionBar);
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
    public void onBackPressed() {
        if (tutorial_opening) {
            canttouch.setVisibility(View.VISIBLE);
            tutorial_opening = false;
            control_opening = true;
            Animation animout = AnimationUtils.loadAnimation(NewFeatures.this, R.anim.slide_out_down_low);
            tutorial.startAnimation(animout);
            tutorial.setVisibility(View.GONE);
            Animation anim = AnimationUtils.loadAnimation(NewFeatures.this, R.anim.fade);
            controlScreen.startAnimation(anim);
            controlScreen.setVisibility(View.VISIBLE);
            new Handler().postDelayed(() -> {
                canttouch.setVisibility(View.GONE);
            }, 650);
        } else {
            finish(); // 前の画面に戻る
            overridePendingTransition(R.anim.no_animation, R.anim.slide_out_down_low);
        }
    }

}
