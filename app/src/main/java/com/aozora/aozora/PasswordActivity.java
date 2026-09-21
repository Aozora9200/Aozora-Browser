package com.aozora.aozora;

import android.app.Activity;
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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;
import android.animation.LayoutTransition;

import com.airbnb.lottie.LottieAnimationView;

public class PasswordActivity extends Activity {

    private TextView LogText;
    private EditText passwordEditText;
    private ImageButton loginButton;
    private Button enter;

    private PasswordManager passwordManager;

    private SharedPreferences appPrefs;

    private String destinationActivity;
    private boolean requirePasswordOnFirstLaunch = true;
    private boolean usePasswordSkip = false;

    private int count = 0;
    private int stop = 1;

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
    private LinearLayout passwordLayout;
    private LinearLayout pinLayout;

    private LinearLayout pinDisplay;
    private StringBuilder pinInput = new StringBuilder();
    private Runnable maskPinRunnable;

    private static final String PREF_USE_PIN = "use_pin";

    private ImageView Background;

    // SharedPreferencesのキー
    private static final String PREF_FAILED_COUNT = "password_failed_count";
    private static final String PREF_LOCK_MINUTES = "password_lock_minutes";
    private static final String PREF_LOCK_UNTIL = "password_lock_until";

    private LottieAnimationView lottieAnimationView;

    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        applySavedTheme();

        setContentView(R.layout.activity_password);
        TouchEffectView.attach(getWindow());
        Background = findViewById(R.id.background);
        applySavedBackground();
        applyBackTheme();

        destinationActivity =
                getIntent().getStringExtra("destination_activity");

        // 未指定ならMainActivity
        if (destinationActivity == null || destinationActivity.isEmpty()) {
            destinationActivity = "MainActivity";
        }

        usePasswordSkip =
                getIntent().getBooleanExtra(
                        "usePasswordSkip",
                        false
                );

        if (usePasswordSkip) {

            if (PasswordSkipManager.shouldSkipPassword(this)) {

                if (getIntent().getBooleanExtra("return_to_caller", false)) {
                    setResult(RESULT_OK);
                    finish();
                    return;
                }

                Intent intent;

                try {

                    Class<?> targetClass =
                            Class.forName(
                                    getPackageName()
                                            + "."
                                            + destinationActivity
                            );

                    intent =
                            new Intent(
                                    PasswordActivity.this,
                                    targetClass
                            );

                } catch (ClassNotFoundException e) {

                    // 存在しなければMainActivity
                    intent =
                            new Intent(
                                    PasswordActivity.this,
                                    MainActivity.class
                            );
                }

                intent.putExtra(
                        "authenticated",
                        true
                );

                final Intent finalIntent = intent;
                startActivity(finalIntent);

                overridePendingTransition(
                        R.anim.fade,
                        R.anim.no_animation
                );

                finish();
                return;
            }

        }

        requirePasswordOnFirstLaunch =
                getIntent().getBooleanExtra(
                        "requirePasswordOnFirstLaunch",
                        true
                );

        passwordEditText =
                findViewById(R.id.passwordEditText);

        loginButton =
                findViewById(R.id.loginButton);

        passwordManager =
                new PasswordManager(this);

        lottieAnimationView =
                findViewById(R.id.lottieAnimationView);

        lottieAnimationView.setAnimation(
                "animation/Password_Failure.json"
        );

        passwordLayout = findViewById(R.id.passwordLayout);
        pinLayout = findViewById(R.id.pinLayout);
        pinDisplay = findViewById(R.id.pinDisplay);
        enter =
                findViewById(R.id.pinEnter);
        LogText = findViewById(R.id.logtext);

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


        appPrefs =
                getSharedPreferences("AppPrefs", MODE_PRIVATE);

        boolean usePin =
                appPrefs.getBoolean(PREF_USE_PIN, false);

        Animation animin =
                AnimationUtils.loadAnimation(
                        PasswordActivity.this,
                        R.anim.fade
                );

        Animation animout =
                AnimationUtils.loadAnimation(
                        PasswordActivity.this,
                        R.anim.fadeout
                );

        if (usePin) {
            new Handler().postDelayed(() -> {
                LogText.startAnimation(animout);
                LogText.setVisibility(View.INVISIBLE);
                LogText.setText("PIN でロックを解除");
                LogText.startAnimation(animin);
                LogText.setVisibility(View.VISIBLE);
            }, 1300);
            pinLayout.startAnimation(animin);
            passwordLayout.setVisibility(View.GONE);
            pinLayout.setVisibility(View.VISIBLE);
            lottieAnimationView.setVisibility(View.GONE);

        } else {
            new Handler().postDelayed(() -> {
                LogText.startAnimation(animout);
                LogText.setVisibility(View.INVISIBLE);
                LogText.setText("パスワード でロックを解除");
                LogText.startAnimation(animin);
                LogText.setVisibility(View.VISIBLE);
            }, 1300);
            passwordLayout.startAnimation(animin);
            passwordLayout.setVisibility(View.VISIBLE);
            pinLayout.setVisibility(View.GONE);
        }

        new Handler().postDelayed(() -> {
            lottieAnimationView.playAnimation();
        }, 2000);

        // 保存されている値を復元
        count = appPrefs.getInt(
                PREF_FAILED_COUNT,
                0
        );

        stop = appPrefs.getInt(
                PREF_LOCK_MINUTES,
                1
        );

        // 起動時にロック状態を確認
        checkLockState();

        loginButton.setOnClickListener(v -> {

            // 念のためクリック時にも確認
            if (isLocked()) {
                openCantUseActivity();
                return;
            }

            String password =
                    passwordEditText.getText().toString();

            if (password.isEmpty()) {

                Animation anim =
                        AnimationUtils.loadAnimation(
                                PasswordActivity.this,
                                R.anim.fadeout
                        );

                lottieAnimationView.startAnimation(anim);
                lottieAnimationView.setVisibility(
                        View.INVISIBLE
                );

                new Handler().postDelayed(() -> {

                    lottieAnimationView.setAnimation(
                            "animation/Password_Failure.json"
                    );

                    lottieAnimationView.setVisibility(
                            View.VISIBLE
                    );

                    lottieAnimationView.playAnimation();

                }, 2000);

                LogText.startAnimation(animout);
                LogText.setVisibility(View.INVISIBLE);
                LogText.setText("パスワードを入力してください");
                LogText.startAnimation(animin);
                LogText.setVisibility(View.VISIBLE);

                return;
            }

            if (passwordManager.verifyPassword(password)) {

                InputMethodManager imm =
                        (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

                imm.hideSoftInputFromWindow(
                        getWindow().getDecorView().getWindowToken(),
                        0
                );

                loginButton.setEnabled(false);
                loginButton.setAlpha(0.4f);

                Animation anim =
                        AnimationUtils.loadAnimation(
                                PasswordActivity.this,
                                R.anim.fadeout
                        );

                lottieAnimationView.startAnimation(anim);
                lottieAnimationView.setVisibility(
                        View.INVISIBLE
                );

                new Handler().postDelayed(() -> {

                    lottieAnimationView.setAnimation(
                            "animation/Password_Verify.json"
                    );

                    lottieAnimationView.setVisibility(
                            View.VISIBLE
                    );

                    lottieAnimationView.playAnimation();

                    LogText.startAnimation(animout);
                    LogText.setVisibility(View.INVISIBLE);
                    LogText.setText("認証しました");
                    LogText.startAnimation(animin);
                    LogText.setVisibility(View.VISIBLE);

                }, 800);

                if (usePasswordSkip) {
                    PasswordSkipManager.setPasswordSkipTime(this);
                }

                // 認証成功したら失敗回数をリセット
                appPrefs.edit()
                        .putInt(PREF_FAILED_COUNT, 0)
                        .putInt(PREF_LOCK_MINUTES, 1)
                        .apply();

                count = 0;
                stop = 1;

                if (getIntent().getBooleanExtra("return_to_caller", false)) {
                    setResult(RESULT_OK);
                    finish();
                    return;
                }

                Intent intent;

                try {

                    Class<?> targetClass =
                            Class.forName(
                                    getPackageName()
                                            + "."
                                            + destinationActivity
                            );

                    intent =
                            new Intent(
                                    PasswordActivity.this,
                                    targetClass
                            );

                } catch (ClassNotFoundException e) {

                    // 存在しなければMainActivity
                    intent =
                            new Intent(
                                    PasswordActivity.this,
                                    MainActivity.class
                            );
                }

                intent.putExtra(
                        "authenticated",
                        true
                );

                final Intent finalIntent = intent;

                new Handler().postDelayed(() -> {

                    startActivity(finalIntent);

                    overridePendingTransition(
                            R.anim.fade,
                            R.anim.no_animation
                    );

                    finish();

                }, 2800);

            } else {

                loginButton.setEnabled(false);
                loginButton.setAlpha(0.4f);

                Animation anim =
                        AnimationUtils.loadAnimation(
                                PasswordActivity.this,
                                R.anim.fadeout
                        );

                lottieAnimationView.startAnimation(anim);
                lottieAnimationView.setVisibility(
                        View.INVISIBLE
                );

                handler.postDelayed(() -> {

                    lottieAnimationView.setAnimation(
                            "animation/Password_Failure.json"
                    );

                    lottieAnimationView.setVisibility(
                            View.VISIBLE
                    );

                    lottieAnimationView.playAnimation();

                    new Handler().postDelayed(() -> {

                        LogText.startAnimation(animout);
                        LogText.setVisibility(View.INVISIBLE);
                        LogText.setText("パスワード が違います");
                        LogText.startAnimation(animin);
                        LogText.setVisibility(View.VISIBLE);

                    }, 500);

                    count++;

                    // 失敗回数を保存
                    appPrefs.edit()
                            .putInt(
                                    PREF_FAILED_COUNT,
                                    count
                            )
                            .apply();

                    passwordEditText.setText("");

                    if (count >= 5) {

                        InputMethodManager imm =
                                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

                        imm.hideSoftInputFromWindow(
                                getWindow().getDecorView().getWindowToken(),
                                0
                        );

                        // ロック時間を2倍
                        stop *= 2;

                        // ロック時間を保存
                        appPrefs.edit()
                                .putInt(
                                        PREF_LOCK_MINUTES,
                                        stop
                                )
                                .apply();

                        // 現在時刻 + stop分
                        long lockUntil =
                                System.currentTimeMillis()
                                        + stop * 60 * 1000L;

                        // ロック解除時刻を保存
                        appPrefs.edit()
                                .putLong(
                                        PREF_LOCK_UNTIL,
                                        lockUntil
                                )
                                .apply();

                        String stopText =
                                stop + "分後";

                        LogText.startAnimation(animout);
                        LogText.setVisibility(View.INVISIBLE);
                        LogText.setText("Aozora は使用できません\n" + stopText + "にやり直して下さい");
                        LogText.startAnimation(animin);
                        LogText.setVisibility(View.VISIBLE);

                        // ロック画面へ移動
                        openCantUseActivity();

                        return;
                    }

                    loginButton.setEnabled(true);
                    loginButton.setAlpha(1.0f);

                }, 3000);
            }
        });

        if (usePin) {

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


            // バックスペース
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

                /*
                 * 削除後に残ったものは全部 ●
                 */
                for (int i = 0;
                     i < pinDisplay.getChildCount();
                     i++) {

                    TextView view =
                            (TextView) pinDisplay.getChildAt(i);

                    view.setText("●");
                }
            });


            // ✓
            enter.setOnClickListener(v -> {

                if (pinInput.length() == 0) {

                    LogText.startAnimation(animout);
                    LogText.setVisibility(View.INVISIBLE);
                    LogText.setText("PIN を入力してください");
                    LogText.startAnimation(animin);
                    LogText.setVisibility(View.VISIBLE);

                    return;
                }

                verifyPin();
            });
        }
    }

    private void updatePinDisplay() {

        // 前回のマスク処理をキャンセル
        if (maskPinRunnable != null) {
            handler.removeCallbacks(maskPinRunnable);
        }

        int length = pinInput.length();

        if (length == 0) {

            pinDisplay.removeAllViews();

            return;
        }

        /*
         * すでに表示されている最後の数字を
         * ● に変更する
         *
         * 例：
         * ● ● 3
         * ↓ 4を入力
         * ● ● 3 4
         * ↓
         * ● ● ● 4
         */
        if (pinDisplay.getChildCount() == length - 1) {

            if (pinDisplay.getChildCount() > 0) {

                TextView previousView =
                        (TextView) pinDisplay.getChildAt(
                                pinDisplay.getChildCount() - 1
                        );

                previousView.setText("●");
            }

            // 新しい最後の数字を追加
            TextView lastDigitView =
                    createPinTextView(
                            String.valueOf(
                                    pinInput.charAt(length - 1)
                            )
                    );

            pinDisplay.addView(lastDigitView);

            // 追加された数字だけアニメーション
            Animation anim =
                    AnimationUtils.loadAnimation(
                            PasswordActivity.this,
                            R.anim.pin_digit_in
                    );

            lastDigitView.startAnimation(anim);

        } else {

            // 想定外の状態になった場合の再構築
            pinDisplay.removeAllViews();

            for (int i = 0; i < length - 1; i++) {

                TextView dot =
                        createPinTextView("●");

                pinDisplay.addView(dot);
            }

            TextView lastDigitView =
                    createPinTextView(
                            String.valueOf(
                                    pinInput.charAt(length - 1)
                            )
                    );

            pinDisplay.addView(lastDigitView);

            Animation anim =
                    AnimationUtils.loadAnimation(
                            PasswordActivity.this,
                            R.anim.pin_digit_in
                    );

            lastDigitView.startAnimation(anim);
        }

        final int expectedLength = length;

        maskPinRunnable = () -> {

            if (pinInput.length() != expectedLength) {
                return;
            }

            int childCount =
                    pinDisplay.getChildCount();

            if (childCount == 0) {
                return;
            }

            TextView lastView =
                    (TextView) pinDisplay.getChildAt(
                            childCount - 1
                    );

            lastView.setText("●");
        };

        handler.postDelayed(
                maskPinRunnable,
                700
        );
    }

    private TextView createPinTextView(String text) {

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

    private int dpToPx(int dp) {

        return (int) (
                dp *
                        getResources()
                                .getDisplayMetrics()
                                .density
                        + 0.5f
        );
    }

    private void verifyPin() {

        String pin =
                pinInput.toString();

        Animation animin =
                AnimationUtils.loadAnimation(
                        PasswordActivity.this,
                        R.anim.fade
                );

        Animation animout =
                AnimationUtils.loadAnimation(
                        PasswordActivity.this,
                        R.anim.fadeout
                );

        if (passwordManager.verifyPassword(pin)) {

            // 成功時

            enter.setEnabled(false);
            enter.setAlpha(0.4f);

            appPrefs.edit()
                    .putInt(PREF_FAILED_COUNT, 0)
                    .putInt(PREF_LOCK_MINUTES, 1)
                    .remove(PREF_LOCK_UNTIL)
                    .apply();

            count = 0;
            stop = 1;

            LogText.startAnimation(animout);
            LogText.setVisibility(View.INVISIBLE);
            LogText.setText("認証しました");
            LogText.startAnimation(animin);
            LogText.setVisibility(View.VISIBLE);

            if (usePasswordSkip) {
                PasswordSkipManager.setPasswordSkipTime(this);
            }

            goToDestination();

        } else {
            enter.setEnabled(false);
            enter.setAlpha(0.4f);

            new Handler().postDelayed(() -> {

                // 失敗
                animatePinClear();

                count++;

                appPrefs.edit()
                        .putInt(
                                PREF_FAILED_COUNT,
                                count
                        )
                        .apply();

                LogText.startAnimation(animout);
                LogText.setVisibility(View.INVISIBLE);
                LogText.setText("PIN が違います");
                LogText.startAnimation(animin);
                LogText.setVisibility(View.VISIBLE);

                if (count >= 5) {

                    stop *= 2;

                    appPrefs.edit()
                            .putInt(
                                    PREF_LOCK_MINUTES,
                                    stop
                            )
                            .apply();

                    long lockUntil =
                            System.currentTimeMillis()
                                    + stop * 60 * 1000L;

                    appPrefs.edit()
                            .putLong(
                                    PREF_LOCK_UNTIL,
                                    lockUntil
                            )
                            .apply();

                    openCantUseActivity();
                }

                enter.setEnabled(true);
                enter.setAlpha(1.0f);

            }, 3000);
        }
    }

    private void animatePinClear() {

        if (maskPinRunnable != null) {
            handler.removeCallbacks(maskPinRunnable);
            maskPinRunnable = null;
        }

        int childCount =
                pinDisplay.getChildCount();

        if (childCount == 0) {

            pinInput.setLength(0);
            return;
        }

        for (int i = 0; i < childCount; i++) {

            View child =
                    pinDisplay.getChildAt(i);

            Animation anim =
                    AnimationUtils.loadAnimation(
                            PasswordActivity.this,
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

    private void goToDestination() {

        Intent intent;

        try {

            Class<?> targetClass =
                    Class.forName(
                            getPackageName()
                                    + "."
                                    + destinationActivity
                    );

            intent =
                    new Intent(
                            PasswordActivity.this,
                            targetClass
                    );

        } catch (ClassNotFoundException e) {

            intent =
                    new Intent(
                            PasswordActivity.this,
                            MainActivity.class
                    );
        }

        intent.putExtra(
                "authenticated",
                true
        );

        final Intent finalIntent = intent;

        new Handler().postDelayed(() -> {

            startActivity(finalIntent);

            overridePendingTransition(
                    R.anim.fade,
                    R.anim.no_animation
            );

            finish();

        }, 800);
    }

    /**
     * ロック中なら使用停止画面へ移動
     */
    private void checkLockState() {

        long lockUntil =
                appPrefs.getLong(
                        PREF_LOCK_UNTIL,
                        0
                );

        long now =
                System.currentTimeMillis();

        if (lockUntil > now) {

            openCantUseActivity();

            return;
        }

        // ロック期限が切れていたら削除
        if (lockUntil != 0) {

            appPrefs.edit()
                    .remove(PREF_LOCK_UNTIL)
                    .apply();
        }
    }

    /**
     * 現在ロック中か
     */
    private boolean isLocked() {

        long lockUntil =
                appPrefs.getLong(
                        PREF_LOCK_UNTIL,
                        0
                );

        long now =
                System.currentTimeMillis();

        if (lockUntil > now) {
            return true;
        }

        // ロック期限切れ
        if (lockUntil != 0) {

            appPrefs.edit()
                    .remove(PREF_LOCK_UNTIL)
                    .apply();
        }

        return false;
    }

    /**
     * Cant_Use_Aozora_Activityへ移動
     */
    private void openCantUseActivity() {

        Intent intent =
                new Intent(
                        PasswordActivity.this,
                        Cant_Use_Aozora_Activity.class
                );

        // 認証後に移動するActivityを引き継ぐ
        intent.putExtra(
                "destination_activity",
                destinationActivity
        );

        // 起動時パスワード設定を引き継ぐ
        intent.putExtra(
                "requirePasswordOnFirstLaunch",
                requirePasswordOnFirstLaunch
        );

        startActivity(intent);

        overridePendingTransition(
                R.anim.fade,
                R.anim.no_animation
        );

        finish();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();

        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onBackPressed() {

        moveTaskToBack(true);

        if (requirePasswordOnFirstLaunch) {

            finishAffinity();

        } else {

            finish();
        }
    }
}