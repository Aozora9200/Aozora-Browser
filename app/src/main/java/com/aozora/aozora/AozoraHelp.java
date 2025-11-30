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

public class AozoraHelp extends Activity {
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
    private TextView help, title, detail;
    private VideoView animation_video;
    private TextureView textureView;
    private MediaPlayer mediaPlayer;
    private LinearLayout helpActivity, controlScreen, tutorial;
    private FrameLayout helpswitchtab, helptools, helpbookmark, helpsavedpage, whatsnew;
    private VideoView tutorialvideo;

    private ImageButton control_close, tutorial_close;
    private ImageButton canttouch;
    private boolean control_opening, tutorial_opening, maintutorial_opening;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applySavedTheme();
        setContentView(R.layout.aozora_help);
        Background = findViewById(R.id.background);
        applySavedBackground();
        applyBackTheme();

        canttouch = findViewById(R.id.canttouch);
        whatsnew = findViewById(R.id.whatsnew);

        Button boot = findViewById(R.id.helpAozoraBoot);
        Button settings = findViewById(R.id.helpAozoraSettings);
        Button UA = findViewById(R.id.helpAozoraUA);
        Button contact = findViewById(R.id.helpAozoraContact);
        Button control = findViewById(R.id.helpAozoraControl);
        help = findViewById(R.id.help_explanation);
        ImageButton close = findViewById(R.id.help_close);
        control_close = findViewById(R.id.control_close);

        helpActivity = findViewById(R.id.helpActivity);
        controlScreen = findViewById(R.id.controlScreen);

        helpswitchtab = findViewById(R.id.helpswitchtab);
        helptools = findViewById(R.id.helptools);
        helpbookmark = findViewById(R.id.helpbookmark);
        helpsavedpage = findViewById(R.id.helpsavedpage);
        tutorial =  findViewById(R.id.tutorial);
        tutorial_close = findViewById(R.id.tutorial_close);

        tutorialvideo = findViewById(R.id.tutorialvideo);
        title = findViewById(R.id.title);
        detail = findViewById(R.id.detail);

        textureView = findViewById(R.id.logo_animation);
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                playVideo(surface, width, height);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                if (mediaPlayer != null) {
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
        });

        helpsavedpage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tutorialvideo.setVisibility(View.VISIBLE);
                control_opening = false;
                tutorial_opening = true;
                Animation animout = AnimationUtils.loadAnimation(AozoraHelp.this, R.anim.fadeout);
                controlScreen.startAnimation(animout);
                controlScreen.setVisibility(View.GONE);
                Animation anim = AnimationUtils.loadAnimation(AozoraHelp.this, R.anim.slide_in_up_low);
                tutorial.startAnimation(anim);
                tutorial.setVisibility(View.VISIBLE);
                String animation_path = "android.resource://" + getPackageName() + "/" + R.raw.savedpage;
                String title = getString(R.string.help_savedpage_title);
                String detail = getString(R.string.help_savedpage_detail);
                tutorialSetting(animation_path, title, detail);
            }
        });

        helpbookmark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tutorialvideo.setVisibility(View.VISIBLE);
                control_opening = false;
                tutorial_opening = true;
                Animation animout = AnimationUtils.loadAnimation(AozoraHelp.this, R.anim.fadeout);
                controlScreen.startAnimation(animout);
                controlScreen.setVisibility(View.GONE);
                Animation anim = AnimationUtils.loadAnimation(AozoraHelp.this, R.anim.slide_in_up_low);
                tutorial.startAnimation(anim);
                tutorial.setVisibility(View.VISIBLE);
                String animation_path = "android.resource://" + getPackageName() + "/" + R.raw.bookmark;
                String title = getString(R.string.help_bookmark_title);
                String detail = getString(R.string.help_bookmark_detail);
                tutorialSetting(animation_path, title, detail);
            }
        });

        whatsnew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AozoraHelp.this, NewFeatures.class));
                overridePendingTransition(R.anim.slide_in_up_low, R.anim.no_animation);
            }
        });

        helptools.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tutorialvideo.setVisibility(View.VISIBLE);
                control_opening = false;
                tutorial_opening = true;
                Animation animout = AnimationUtils.loadAnimation(AozoraHelp.this, R.anim.fadeout);
                controlScreen.startAnimation(animout);
                controlScreen.setVisibility(View.GONE);
                Animation anim = AnimationUtils.loadAnimation(AozoraHelp.this, R.anim.slide_in_up_low);
                tutorial.startAnimation(anim);
                tutorial.setVisibility(View.VISIBLE);
                String animation_path = "android.resource://" + getPackageName() + "/" + R.raw.tools;
                String title = getString(R.string.help_tools_title);
                String detail = getString(R.string.help_tools_detail);
                tutorialSetting(animation_path, title, detail);
            }
        });

        helpswitchtab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tutorialvideo.setVisibility(View.VISIBLE);
                control_opening = false;
                tutorial_opening = true;
                Animation animout = AnimationUtils.loadAnimation(AozoraHelp.this, R.anim.fadeout);
                controlScreen.startAnimation(animout);
                controlScreen.setVisibility(View.GONE);
                Animation anim = AnimationUtils.loadAnimation(AozoraHelp.this, R.anim.slide_in_up_low);
                tutorial.startAnimation(anim);
                tutorial.setVisibility(View.VISIBLE);
                String animation_path = "android.resource://" + getPackageName() + "/" + R.raw.tablist;
                String title = getString(R.string.help_switchtab_title);
                String detail = getString(R.string.help_switchtab_detail);
                tutorialSetting(animation_path, title, detail);
            }
        });

        tutorial_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (maintutorial_opening) {
                    canttouch.setVisibility(View.VISIBLE);
                    maintutorial_opening = false;
                    Animation animout = AnimationUtils.loadAnimation(AozoraHelp.this, R.anim.slide_out_down_low);
                    tutorial.startAnimation(animout);
                    tutorial.setVisibility(View.GONE);
                    Animation anim = AnimationUtils.loadAnimation(AozoraHelp.this, R.anim.fade);
                    helpActivity.startAnimation(anim);
                    helpActivity.setVisibility(View.VISIBLE);
                    new Handler().postDelayed(() -> {
                        canttouch.setVisibility(View.GONE);
                    }, 650);
                } else {
                    canttouch.setVisibility(View.VISIBLE);
                    tutorial_opening = false;
                    control_opening = true;
                    Animation animout = AnimationUtils.loadAnimation(AozoraHelp.this, R.anim.slide_out_down_low);
                    tutorial.startAnimation(animout);
                    tutorial.setVisibility(View.GONE);
                    Animation anim = AnimationUtils.loadAnimation(AozoraHelp.this, R.anim.fade);
                    controlScreen.startAnimation(anim);
                    controlScreen.setVisibility(View.VISIBLE);
                    new Handler().postDelayed(() -> {
                        canttouch.setVisibility(View.GONE);
                    }, 650);
                }
            }
        });

        control_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                canttouch.setVisibility(View.VISIBLE);
                control_opening = false;
                Animation animout = AnimationUtils.loadAnimation(AozoraHelp.this, R.anim.slide_out_down_low);
                controlScreen.startAnimation(animout);
                controlScreen.setVisibility(View.GONE);
                Animation anim = AnimationUtils.loadAnimation(AozoraHelp.this, R.anim.fade);
                helpActivity.startAnimation(anim);
                helpActivity.setVisibility(View.VISIBLE);
                new Handler().postDelayed(() -> {
                    canttouch.setVisibility(View.GONE);
                }, 650);
            }
        });

        control.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                control_opening = true;
                Animation animout = AnimationUtils.loadAnimation(AozoraHelp.this, R.anim.fadeout);
                helpActivity.startAnimation(animout);
                helpActivity.setVisibility(View.GONE);
                Animation anim = AnimationUtils.loadAnimation(AozoraHelp.this, R.anim.slide_in_up_low);
                controlScreen.startAnimation(anim);
                controlScreen.setVisibility(View.VISIBLE);
            }
        });

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // 前の画面に戻る
                overridePendingTransition(R.anim.no_animation,  R.anim.slide_out_down_low);
            }
        });

        boot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tutorialvideo.setVisibility(View.VISIBLE);
                maintutorial_opening = true;
                Animation animout = AnimationUtils.loadAnimation(AozoraHelp.this, R.anim.fadeout);
                helpActivity.startAnimation(animout);
                helpActivity.setVisibility(View.GONE);
                Animation anim = AnimationUtils.loadAnimation(AozoraHelp.this, R.anim.slide_in_up_low);
                tutorial.startAnimation(anim);
                tutorial.setVisibility(View.VISIBLE);
                String animation_path = "android.resource://" + getPackageName() + "/" + R.raw.boot;
                String title = getString(R.string.help_boot_title);
                String detail = getString(R.string.help_boot_detail);
                tutorialSetting(animation_path, title, detail);
                help(getString(R.string.help_boot_detail));
            }
        });
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tutorialvideo.setVisibility(View.VISIBLE);
                maintutorial_opening = true;
                Animation animout = AnimationUtils.loadAnimation(AozoraHelp.this, R.anim.fadeout);
                helpActivity.startAnimation(animout);
                helpActivity.setVisibility(View.GONE);
                Animation anim = AnimationUtils.loadAnimation(AozoraHelp.this, R.anim.slide_in_up_low);
                tutorial.startAnimation(anim);
                tutorial.setVisibility(View.VISIBLE);
                String animation_path = "android.resource://" + getPackageName() + "/" + R.raw.settings;
                String title = getString(R.string.help_settings_title);
                String detail = getString(R.string.help_settings_detail);
                tutorialSetting(animation_path, title, detail);
                help(getString(R.string.help_settings_detail));
            }
        });
        UA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                maintutorial_opening = true;
                Animation animout = AnimationUtils.loadAnimation(AozoraHelp.this, R.anim.fadeout);
                helpActivity.startAnimation(animout);
                helpActivity.setVisibility(View.GONE);
                Animation anim = AnimationUtils.loadAnimation(AozoraHelp.this, R.anim.slide_in_up_low);
                tutorial.startAnimation(anim);
                tutorial.setVisibility(View.VISIBLE);
                String animation_path = "android.resource://" + getPackageName() + "/" + R.raw.tools;
                String title = getString(R.string.help_ua_title);
                String detail = getString(R.string.help_ua_detail);
                tutorialvideo.setVisibility(View.GONE);
                tutorialSetting(animation_path, title, detail);
                help(getString(R.string.help_ua_detail));
            }
        });
        contact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                help(getString(R.string.help_contact_wait));
                new Handler().postDelayed(() -> {
                    Intent result = new Intent();
                    result.putExtra("action", "contact");
                    setResult(RESULT_OK, result);
                    finish();
                    overridePendingTransition(R.anim.no_animation,  R.anim.slide_out_down_low);
                }, 1000);
            }
        });
    }

    private void tutorialSetting(String videouri, String titleText, String detailText) {
        tutorialvideo.setVideoURI(Uri.parse(videouri));
        tutorialvideo.setOnPreparedListener(mp -> {
            mp.setLooping(true);  // ループ再生ON
            mp.setVolume(0f, 0f); // ミュート
            tutorialvideo.start();
        });
        title.setText(titleText);
        detail.setText(detailText);
    }

    private void playVideo(SurfaceTexture surface, int viewWidth, int viewHeight) {
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.aozoralogoanimation);
            mediaPlayer.setSurface(new Surface(surface));
            mediaPlayer.setLooping(false);

            mediaPlayer.setOnPreparedListener(mp -> {
                // centerCrop風に拡大
                adjustAspectRatio(textureView, mp.getVideoWidth(), mp.getVideoHeight());
                mp.start();
            });

            // ✅ 再生完了イベント（メソッド呼び出しも可能）
            mediaPlayer.setOnCompletionListener(mp -> {
                onVideoFinished(); // 任意のメソッドを呼び出し
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e("Video", "エラー: " + what + ", " + extra);
                return false;
            });

            mediaPlayer.prepareAsync();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ✅ centerCropを実現するメソッド
    private void adjustAspectRatio(TextureView textureView, int videoWidth, int videoHeight) {
        float viewWidth = textureView.getWidth();
        float viewHeight = textureView.getHeight();
        float aspectRatio = (float) videoHeight / videoWidth;

        int newWidth, newHeight;
        if (viewHeight > viewWidth * aspectRatio) {
            newWidth = (int) (viewHeight / aspectRatio);
            newHeight = (int) viewHeight;
        } else {
            newWidth = (int) viewWidth;
            newHeight = (int) (viewWidth * aspectRatio);
        }

        float xOff = (viewWidth - newWidth) / 2f;
        float yOff = (viewHeight - newHeight) / 2f;

        Matrix txform = new Matrix();
        textureView.getTransform(txform);
        txform.setScale((float) newWidth / viewWidth, (float) newHeight / viewHeight);
        txform.postTranslate(xOff, yOff);
        textureView.setTransform(txform);
    }

    // ✅ 再生終了時に呼び出す処理
    private void onVideoFinished() {
        // 例：動画再生後に画像を表示
        findViewById(R.id.logoversion).setVisibility(View.VISIBLE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mediaPlayer != null) {
            mediaPlayer.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void help(String helpText) {
        Animation anim = AnimationUtils.loadAnimation(AozoraHelp.this, R.anim.fade);
        Animation animout = AnimationUtils.loadAnimation(AozoraHelp.this, R.anim.fadeout);
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
        if (control_opening) {
            canttouch.setVisibility(View.VISIBLE);
            control_opening = false;
            Animation animout = AnimationUtils.loadAnimation(AozoraHelp.this, R.anim.slide_out_down_low);
            controlScreen.startAnimation(animout);
            controlScreen.setVisibility(View.GONE);
            Animation anim = AnimationUtils.loadAnimation(AozoraHelp.this, R.anim.fade);
            helpActivity.startAnimation(anim);
            helpActivity.setVisibility(View.VISIBLE);
            new Handler().postDelayed(() -> {
                canttouch.setVisibility(View.GONE);
            }, 650);
        } else if (tutorial_opening) {
            canttouch.setVisibility(View.VISIBLE);
            tutorial_opening = false;
            control_opening = true;
            Animation animout = AnimationUtils.loadAnimation(AozoraHelp.this, R.anim.slide_out_down_low);
            tutorial.startAnimation(animout);
            tutorial.setVisibility(View.GONE);
            Animation anim = AnimationUtils.loadAnimation(AozoraHelp.this, R.anim.fade);
            controlScreen.startAnimation(anim);
            controlScreen.setVisibility(View.VISIBLE);
            new Handler().postDelayed(() -> {
                canttouch.setVisibility(View.GONE);
            }, 650);
        } else if (maintutorial_opening) {
            canttouch.setVisibility(View.VISIBLE);
            maintutorial_opening = false;
            Animation animout = AnimationUtils.loadAnimation(AozoraHelp.this, R.anim.slide_out_down_low);
            tutorial.startAnimation(animout);
            tutorial.setVisibility(View.GONE);
            Animation anim = AnimationUtils.loadAnimation(AozoraHelp.this, R.anim.fade);
            helpActivity.startAnimation(anim);
            helpActivity.setVisibility(View.VISIBLE);
            new Handler().postDelayed(() -> {
                canttouch.setVisibility(View.GONE);
            }, 650);
        } else {
            finish(); // 前の画面に戻る
            overridePendingTransition(R.anim.no_animation, R.anim.slide_out_down_low);
        }
    }

}
