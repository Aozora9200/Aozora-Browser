package com.aozora.aozora;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.animation.LayoutTransition;

import com.airbnb.lottie.LottieAnimationView;

public class PinSettingActivity extends Activity {

    private static final String STARTUP_PASSWORD = "startup_password";

    private boolean startup = true;

    private TextView setpassword_msg;
    private ImageButton enter;
    private LinearLayout newpassword_box;

    private PasswordManager passwordManager;

    // PIN入力
    private LinearLayout pinDisplay;
    private StringBuilder pinInput = new StringBuilder();

    // 1回目に入力したPIN
    private String firstPin = null;

    // 確認PIN入力中か
    private boolean confirmingPin = false;

    // 最後に入力した数字をマスクする処理
    private Runnable maskPinRunnable;

    private final Handler handler = new Handler();

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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        applySavedTheme();

        setContentView(R.layout.activity_pin_setting);
        TouchEffectView.attach(getWindow());
        Background = findViewById(R.id.background);

        applySavedBackground();
        applyBackTheme();

        setpassword_msg =
                findViewById(R.id.setpassword_msg);

        newpassword_box =
                findViewById(R.id.new_password);

        enter =
                findViewById(R.id.pinEnter);

        pinDisplay =
                findViewById(R.id.pinDisplay);
        passwordManager =
                new PasswordManager(this);

        // PIN表示部分のレイアウトアニメーション
        LayoutTransition layoutTransition =
                new LayoutTransition();

        layoutTransition.setDuration(
                LayoutTransition.CHANGE_APPEARING,
                180
        );

        layoutTransition.setDuration(
                LayoutTransition.CHANGE_DISAPPEARING,
                180
        );

        layoutTransition.setDuration(
                LayoutTransition.APPEARING,
                140
        );

        layoutTransition.setDuration(
                LayoutTransition.DISAPPEARING,
                140
        );

        pinDisplay.setLayoutTransition(
                layoutTransition
        );

        Animation animin =
                AnimationUtils.loadAnimation(
                        PinSettingActivity.this,
                        R.anim.fade
                );

        Animation animout =
                AnimationUtils.loadAnimation(
                        PinSettingActivity.this,
                        R.anim.fadeout
                );

        new Handler().postDelayed(() -> {
            setpassword_msg.startAnimation(animout);
            setpassword_msg.setVisibility(View.INVISIBLE);
            setpassword_msg.setText("PIN を設定");
            setpassword_msg.startAnimation(animin);
            setpassword_msg.setVisibility(View.VISIBLE);
        }, 1300);

        // 初期表示アニメーション
        new Handler().postDelayed(() -> {

            Animation anim =
                    AnimationUtils.loadAnimation(
                            PinSettingActivity.this,
                            R.anim.fade
                    );

            newpassword_box.startAnimation(anim);
            newpassword_box.setVisibility(View.VISIBLE);

            setpassword_msg.startAnimation(anim);
            setpassword_msg.setVisibility(View.VISIBLE);

        }, 500);


        /*
         * 数字ボタン
         */
        int[] buttonIds = {
                R.id.pin0,
                R.id.pin1,
                R.id.pin2,
                R.id.pin3,
                R.id.pin4,
                R.id.pin5,
                R.id.pin6,
                R.id.pin7,
                R.id.pin8,
                R.id.pin9
        };

        for (int id : buttonIds) {

            Button button = findViewById(id);

            button.setOnClickListener(v -> {

                String number =
                        ((Button) v).getText().toString();

                // 最大8桁
                if (pinInput.length() >= 8) {
                    return;
                }

                pinInput.append(number);

                updatePinDisplay();
            });
        }


        /*
         * バックスペース
         */
        TextView backspace =
                findViewById(R.id.pinBackspace);

        backspace.setOnClickListener(v -> {

            if (pinInput.length() == 0) {
                return;
            }

            if (maskPinRunnable != null) {
                handler.removeCallbacks(maskPinRunnable);
                maskPinRunnable = null;
            }

            pinInput.deleteCharAt(
                    pinInput.length() - 1
            );

            // 最後のViewを削除
            if (pinDisplay.getChildCount() > 0) {

                pinDisplay.removeViewAt(
                        pinDisplay.getChildCount() - 1
                );
            }

            // 残っているものをすべて●にする
            for (int i = 0;
                 i < pinDisplay.getChildCount();
                 i++) {

                TextView view =
                        (TextView) pinDisplay.getChildAt(i);

                view.setText("●");
            }
        });


        /*
         * ✓ボタン
         */
        enter.setOnClickListener(v -> {

            if (pinInput.length() == 0) {

                if (confirmingPin) {

                    setpassword_msg.startAnimation(animout);
                    setpassword_msg.setVisibility(View.INVISIBLE);
                    setpassword_msg.setText("確認PIN を入力してください");
                    setpassword_msg.startAnimation(animin);
                    setpassword_msg.setVisibility(View.VISIBLE);

                } else {

                    setpassword_msg.startAnimation(animout);
                    setpassword_msg.setVisibility(View.INVISIBLE);
                    setpassword_msg.setText("PINを入力してください");
                    setpassword_msg.startAnimation(animin);
                    setpassword_msg.setVisibility(View.VISIBLE);
                }

                return;
            }


            /*
             * 1回目のPIN入力
             */
            if (!confirmingPin) {

                // pinDisplayではなく、実際の数字を保存
                firstPin =
                        pinInput.toString();

                // 1回目の入力をClear
                clearPinInput();

                // 確認モードへ
                confirmingPin = true;

                setpassword_msg.startAnimation(animout);
                setpassword_msg.setVisibility(View.INVISIBLE);

                setpassword_msg.setText(
                        "PINの再入力"
                );

                setpassword_msg.startAnimation(animin);
                setpassword_msg.setVisibility(View.VISIBLE);

                return;
            }


            /*
             * 2回目のPIN入力
             */
            String confirmPin =
                    pinInput.toString();

            /*
             * 最初のPINと比較
             */
            if (firstPin != null
                    && firstPin.equals(confirmPin)) {

                /*
                 * PIN一致
                 * → PasswordManagerへ保存
                 */
                enter.setEnabled(false);
                enter.setAlpha(0.4f);

                boolean success =
                        passwordManager.setPassword(firstPin);

                if (success) {

                    appPrefs =
                            getSharedPreferences(
                                    "AppPrefs",
                                    MODE_PRIVATE
                            );

                    // PINを使用する設定にする
                    appPrefs.edit()
                            .putBoolean(
                                    PREF_USE_PIN,
                                    true
                            )
                            .apply();

                    InputMethodManager imm =
                            (InputMethodManager)
                                    getSystemService(
                                            Context.INPUT_METHOD_SERVICE
                                    );

                    if (imm != null) {
                        imm.hideSoftInputFromWindow(
                                getWindow()
                                        .getDecorView()
                                        .getWindowToken(),
                                0
                        );
                    }

                    Animation anim =
                            AnimationUtils.loadAnimation(
                                    PinSettingActivity.this,
                                    R.anim.fadeout
                            );
                    appPrefs =
                            getSharedPreferences("AppPrefs", MODE_PRIVATE);

                    boolean usePin =
                            appPrefs.getBoolean(PREF_USE_PIN, false);
                    if (!usePin) {
                        appPrefs.edit()
                                .putBoolean(PREF_USE_PIN, true)
                                .apply();
                    }
                    new Handler().postDelayed(() -> {

                        setpassword_msg.startAnimation(animout);
                        setpassword_msg.setVisibility(View.INVISIBLE);
                        setpassword_msg.setText("PIN を設定しました");
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

                    enter.setEnabled(true);
                    enter.setAlpha(1.0f);

                    setpassword_msg.startAnimation(animout);
                    setpassword_msg.setVisibility(View.INVISIBLE);
                    setpassword_msg.setText("PIN の保存に失敗しました");
                    setpassword_msg.startAnimation(animin);
                    setpassword_msg.setVisibility(View.VISIBLE);
                }

            } else {

                /*
                 * PIN不一致
                 *
                 * 表示をClear
                 * ↓
                 * 最初のPIN設定からやり直し
                 */
                enter.setEnabled(false);
                enter.setAlpha(0.4f);

                setpassword_msg.startAnimation(animout);
                setpassword_msg.setVisibility(View.INVISIBLE);
                setpassword_msg.setText("PIN が一致しません");
                setpassword_msg.startAnimation(animin);
                setpassword_msg.setVisibility(View.VISIBLE);

                animatePinClear();

                handler.postDelayed(() -> {

                    setpassword_msg.startAnimation(animout);
                    setpassword_msg.setVisibility(View.INVISIBLE);

                    firstPin = null;
                    confirmingPin = false;

                    setpassword_msg.startAnimation(animin);
                    setpassword_msg.setVisibility(View.VISIBLE);

                    setpassword_msg.setText(
                            "新しいPINを入力してください"
                    );

                    enter.setEnabled(true);
                    enter.setAlpha(1.0f);

                }, 2000);
            }
        });
    }


    /**
     * PIN表示を更新
     */
    private void updatePinDisplay() {

        // 前回のマスク処理をキャンセル
        if (maskPinRunnable != null) {
            handler.removeCallbacks(maskPinRunnable);
            maskPinRunnable = null;
        }

        int length =
                pinInput.length();

        if (length == 0) {

            pinDisplay.removeAllViews();

            return;
        }


        /*
         * すでに表示されている最後の数字を
         * ●に変更する
         */
        if (pinDisplay.getChildCount()
                == length - 1) {

            if (pinDisplay.getChildCount() > 0) {

                TextView previousView =
                        (TextView)
                                pinDisplay.getChildAt(
                                        pinDisplay.getChildCount() - 1
                                );

                previousView.setText("●");
            }


            // 新しく入力された数字
            TextView lastDigitView =
                    createPinTextView(
                            String.valueOf(
                                    pinInput.charAt(
                                            length - 1
                                    )
                            )
                    );

            pinDisplay.addView(
                    lastDigitView
            );


            // 追加された数字だけアニメーション
            Animation anim =
                    AnimationUtils.loadAnimation(
                            PinSettingActivity.this,
                            R.anim.pin_digit_in
                    );

            lastDigitView.startAnimation(anim);

        } else {

            /*
             * 想定外の状態なら再構築
             */
            pinDisplay.removeAllViews();

            for (int i = 0;
                 i < length - 1;
                 i++) {

                TextView dot =
                        createPinTextView("●");

                pinDisplay.addView(dot);
            }

            TextView lastDigitView =
                    createPinTextView(
                            String.valueOf(
                                    pinInput.charAt(
                                            length - 1
                                    )
                            )
                    );

            pinDisplay.addView(
                    lastDigitView
            );

            Animation anim =
                    AnimationUtils.loadAnimation(
                            PinSettingActivity.this,
                            R.anim.pin_digit_in
                    );

            lastDigitView.startAnimation(anim);
        }


        /*
         * 700ms後に最後の数字を●へ
         */
        final int expectedLength =
                length;

        maskPinRunnable = () -> {

            if (pinInput.length()
                    != expectedLength) {
                return;
            }

            int childCount =
                    pinDisplay.getChildCount();

            if (childCount == 0) {
                return;
            }

            TextView lastView =
                    (TextView)
                            pinDisplay.getChildAt(
                                    childCount - 1
                            );

            lastView.setText("●");
        };

        handler.postDelayed(
                maskPinRunnable,
                700
        );
    }


    /**
     * PIN用TextViewを生成
     */
    private TextView createPinTextView(
            String text) {

        TextView view =
                new TextView(this);

        view.setText(text);
        view.setTextSize(20);
        view.setGravity(Gravity.CENTER);
        view.setIncludeFontPadding(true);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        dpToPx(32),
                        dpToPx(44)
                );

        view.setLayoutParams(params);

        return view;
    }


    /**
     * dp → px
     */
    private int dpToPx(int dp) {

        return (int) (
                dp *
                        getResources()
                                .getDisplayMetrics()
                                .density
                        + 0.5f
        );
    }


    /**
     * PIN入力をClear
     */
    private void clearPinInput() {

        if (maskPinRunnable != null) {
            handler.removeCallbacks(
                    maskPinRunnable
            );

            maskPinRunnable = null;
        }

        pinInput.setLength(0);

        pinDisplay.removeAllViews();
    }


    /**
     * PIN入力をアニメーション付きでClear
     */
    private void animatePinClear() {

        if (maskPinRunnable != null) {
            handler.removeCallbacks(
                    maskPinRunnable
            );

            maskPinRunnable = null;
        }

        int childCount =
                pinDisplay.getChildCount();

        if (childCount == 0) {

            pinInput.setLength(0);

            return;
        }


        for (int i = 0;
             i < childCount;
             i++) {

            View child =
                    pinDisplay.getChildAt(i);

            Animation anim =
                    AnimationUtils.loadAnimation(
                            PinSettingActivity.this,
                            R.anim.pin_digit_out
                    );

            anim.setStartOffset(
                    i * 20L
            );

            child.startAnimation(anim);
        }


        handler.postDelayed(() -> {

            pinInput.setLength(0);

            pinDisplay.removeAllViews();

        }, 220);
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

                Background.setBackgroundColor(
                        white
                );

                break;


            case THEME_DARK:

                Background.setBackgroundColor(
                        black
                );

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

                    Background.setBackgroundColor(
                            black
                    );

                } else {

                    Background.setBackgroundColor(
                            white
                    );
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


    @Override
    protected void onDestroy() {

        super.onDestroy();

        handler.removeCallbacksAndMessages(
                null
        );
    }
}
