package com.aozora.aozora;

import android.app.Activity;
import android.content.Context;
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
import android.view.inputmethod.InputMethodManager;

import com.airbnb.lottie.LottieAnimationView;

import app.rive.runtime.kotlin.RiveAnimationView;
import app.rive.runtime.kotlin.core.Rive;
import app.rive.runtime.kotlin.core.Loop;
import app.rive.runtime.kotlin.core.Direction;

public class PasswordSettingActivity extends Activity {

    private static final String STARTUP_PASSWORD = "startup_password";
    private boolean startup = true;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private ImageButton nextButton, saveButton;
    private TextView setpassword_msg;
    private LinearLayout newpassword_box, checkpassword_box;
    private CheckBox usePassWordonStartup;

    private PasswordManager passwordManager;

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

    private SharedPreferences appPrefs;
    private static final String PREF_USE_PIN = "use_pin";

    private ImageView Background;
    private LottieAnimationView lottieAnimationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        applySavedTheme();

        setContentView(
                R.layout.activity_password_setting
        );
        TouchEffectView.attach(getWindow());
        Background = findViewById(R.id.background);
        applySavedBackground();
        applyBackTheme();

        lottieAnimationView = findViewById(R.id.lottieAnimationView);

        usePassWordonStartup = findViewById(R.id.usePassword);

        setpassword_msg =
                findViewById(R.id.setpassword_msg);

        passwordEditText =
                findViewById(R.id.passwordEditText);

        confirmPasswordEditText =
                findViewById(R.id.confirmPasswordEditText);

        saveButton =
                findViewById(R.id.saveButton);

        nextButton =
                findViewById(R.id.nextButton);

        newpassword_box =
                findViewById(R.id.new_password);

        checkpassword_box =
                findViewById(R.id.new_password_check);

        lottieAnimationView.setAnimation(
                "animation/Password_Failure.json"
        );

        Animation animin =
                AnimationUtils.loadAnimation(
                        PasswordSettingActivity.this,
                        R.anim.fade
                );

        Animation animout =
                AnimationUtils.loadAnimation(
                        PasswordSettingActivity.this,
                        R.anim.fadeout
                );

        new Handler().postDelayed(() -> {
            setpassword_msg.startAnimation(animout);
            setpassword_msg.setVisibility(View.INVISIBLE);
            setpassword_msg.setText("パスワード を設定");
            setpassword_msg.startAnimation(animin);
            setpassword_msg.setVisibility(View.VISIBLE);
            }, 1300);

        new Handler().postDelayed(() -> {
            lottieAnimationView.playAnimation();
        }, 2000);

        new Handler().postDelayed(() -> {
            Animation anim =
                    AnimationUtils.loadAnimation(
                            PasswordSettingActivity.this,
                            R.anim.fade
                    );

            newpassword_box.startAnimation(anim);
            newpassword_box.setVisibility(View.VISIBLE);
        }, 500);

        passwordManager =
                new PasswordManager(this);

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

        nextButton.setOnClickListener(v -> {
            String password =
                    passwordEditText.getText().toString();

            if (password.isEmpty()) {

                setpassword_msg.startAnimation(animout);
                setpassword_msg.setVisibility(View.INVISIBLE);
                setpassword_msg.setText("パスワード を入力してください");
                setpassword_msg.startAnimation(animin);
                setpassword_msg.setVisibility(View.VISIBLE);

                return;
            }

            new Handler().postDelayed(() -> {
                InputMethodManager imm =
                        (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

                imm.hideSoftInputFromWindow(
                        getWindow().getDecorView().getWindowToken(),
                        0
                );
                Animation anim =
                        AnimationUtils.loadAnimation(
                                PasswordSettingActivity.this,
                                R.anim.fadeout
                        );

                newpassword_box.startAnimation(anim);
                newpassword_box.setVisibility(View.GONE);
                anim =
                        AnimationUtils.loadAnimation(
                                PasswordSettingActivity.this,
                                R.anim.fade
                        );
                checkpassword_box.startAnimation(anim);
                checkpassword_box.setVisibility(View.VISIBLE);
            }, 500);

        });

        saveButton.setOnClickListener(v -> {

            String password =
                    passwordEditText.getText().toString();

            String confirmPassword =
                    confirmPasswordEditText
                            .getText()
                            .toString();

            if (confirmPassword.isEmpty()) {

                setpassword_msg.startAnimation(animout);
                setpassword_msg.setVisibility(View.INVISIBLE);
                setpassword_msg.setText("もう一度、同じパスワード を入力してください");
                setpassword_msg.startAnimation(animin);
                setpassword_msg.setVisibility(View.VISIBLE);

                return;
            }

            if (!password.equals(confirmPassword)) {

                InputMethodManager imm =
                        (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

                imm.hideSoftInputFromWindow(
                        getWindow().getDecorView().getWindowToken(),
                        0
                );

                setpassword_msg.startAnimation(animout);
                setpassword_msg.setVisibility(View.INVISIBLE);
                setpassword_msg.setText("パスワード が一致しません");
                setpassword_msg.startAnimation(animin);
                setpassword_msg.setVisibility(View.VISIBLE);


                passwordEditText.setText("");
                confirmPasswordEditText.setText("");

                Animation anim =
                        AnimationUtils.loadAnimation(
                                PasswordSettingActivity.this,
                                R.anim.fadeout
                        );

                checkpassword_box.startAnimation(anim);
                checkpassword_box.setVisibility(View.GONE);
                anim =
                        AnimationUtils.loadAnimation(
                                PasswordSettingActivity.this,
                                R.anim.fade
                        );
                newpassword_box.startAnimation(anim);
                newpassword_box.setVisibility(View.VISIBLE);

                return;
            }

            boolean success =
                    passwordManager.setPassword(password);

            if (success) {

                appPrefs =
                        getSharedPreferences("AppPrefs", MODE_PRIVATE);

                boolean usePin =
                        appPrefs.getBoolean(PREF_USE_PIN, false);
                if (usePin) {
                    appPrefs.edit()
                            .putBoolean(PREF_USE_PIN, false)
                            .apply();
                }

                InputMethodManager imm =
                        (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

                imm.hideSoftInputFromWindow(
                        getWindow().getDecorView().getWindowToken(),
                        0
                );

                Animation anim =
                        AnimationUtils.loadAnimation(
                                PasswordSettingActivity.this,
                                R.anim.fadeout
                        );

                lottieAnimationView.startAnimation(anim);
                lottieAnimationView.setVisibility(
                        View.INVISIBLE
                );

                new Handler().postDelayed(() -> {
                    lottieAnimationView.setVisibility(
                            View.VISIBLE
                    );
                    lottieAnimationView.setAnimation(
                            "animation/Password_Verify.json"
                    );
                    lottieAnimationView.playAnimation();
                    setpassword_msg.startAnimation(animout);
                    setpassword_msg.setVisibility(View.INVISIBLE);
                    setpassword_msg.setText("パスワード を設定しました");
                    setpassword_msg.startAnimation(animin);
                    setpassword_msg.setVisibility(View.VISIBLE);
                }, 500);
                new Handler().postDelayed(() -> {
                    overridePendingTransition(
                            R.anim.fade,
                            R.anim.no_animation
                    );
                    finish();
                }, 2000);

            } else {

                setpassword_msg.startAnimation(animout);
                setpassword_msg.setVisibility(View.INVISIBLE);
                setpassword_msg.setText("パスワード の保存に失敗しました");
                setpassword_msg.startAnimation(animin);
                setpassword_msg.setVisibility(View.VISIBLE);
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