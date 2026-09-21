package com.aozora.aozora;

import android.content.Context;
import android.content.SharedPreferences;

public class PasswordSkipManager {

    private static final String PREF_NAME = "AppPrefs";

    private static final String KEY_SKIP_MINUTES =
            "password_skip_minutes";

    private static final String KEY_SKIP_UNTIL =
            "password_skip_until";

    /**
     * 初期化
     */
    public static void initialize(Context context) {

        SharedPreferences prefs =
                context.getSharedPreferences(
                        PREF_NAME,
                        Context.MODE_PRIVATE
                );

        // 初回は0分（スキップOFF）
        if (!prefs.contains(KEY_SKIP_MINUTES)) {

            prefs.edit()
                    .putInt(
                            KEY_SKIP_MINUTES,
                            0
                    )
                    .putLong(
                            KEY_SKIP_UNTIL,
                            0L
                    )
                    .apply();
        }
    }

    /**
     * パスワード認証成功時にスキップ期限を設定
     */
    public static void setPasswordSkipTime(
            Context context
    ) {

        initialize(context);

        SharedPreferences prefs =
                context.getSharedPreferences(
                        PREF_NAME,
                        Context.MODE_PRIVATE
                );

        int savedMinutes =
                prefs.getInt(
                        KEY_SKIP_MINUTES,
                        0
                );

        // 0分ならスキップしない
        if (savedMinutes <= 0) {

            prefs.edit()
                    .putLong(
                            KEY_SKIP_UNTIL,
                            0L
                    )
                    .apply();

            return;
        }

        long now =
                System.currentTimeMillis();

        long skipUntil =
                now + (savedMinutes * 60L * 1000L);

        prefs.edit()
                .putLong(
                        KEY_SKIP_UNTIL,
                        skipUntil
                )
                .apply();
    }

    /**
     * 現在スキップ可能か
     */
    public static boolean shouldSkipPassword(
            Context context
    ) {

        initialize(context);

        SharedPreferences prefs =
                context.getSharedPreferences(
                        PREF_NAME,
                        Context.MODE_PRIVATE
                );

        int savedMinutes =
                prefs.getInt(
                        KEY_SKIP_MINUTES,
                        0
                );

        // 0分なら無効
        if (savedMinutes <= 0) {
            return false;
        }

        long now =
                System.currentTimeMillis();

        long skipUntil =
                prefs.getLong(
                        KEY_SKIP_UNTIL,
                        0L
                );

        return now < skipUntil;
    }
}