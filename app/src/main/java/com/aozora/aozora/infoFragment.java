package com.aozora.aozora;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class infoFragment extends PreferenceFragment {

    private static final int TAP_COUNT_TO_TRIGGER = 7; // 連打回数
    private static final int TAP_COUNT_TO_DEVELOP_TRIGGER = 7;
    private static final int TAP_COUNT_TO_DEVELOP_FIRST_TRIGGER = 3;
    private static final int TAP_COUNT_TO_DEVELOP_SECOND_TRIGGER = 4;
    private static final int TAP_COUNT_TO_DEVELOP_THIRD_TRIGGER = 5;
    private static final int TAP_COUNT_TO_DEVELOP_FOURTH_TRIGGER = 6;
    private int tapCount = 0;
    private int tapdevelopCount = 0;
    private long lastTapTime = 0;
    private long lastTapdevelopTime = 0;
    private Toast toast;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.infopreference);
        Preference openActivitylegal = findPreference("legal");
        if (openActivitylegal != null) {
            openActivitylegal.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(getActivity(), legal.class);
                startActivity(intent);
                return true;
            });
        }
        Preference openActivityStatus = findPreference("devicestatus");
        if (openActivityStatus != null) {
            openActivityStatus.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(getActivity(), devicestatus.class);
                startActivity(intent);
                return true;
            });
        }
        Preference appversion = findPreference("appversion");
        if (appversion != null) {
            appversion.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    long currentTime = System.currentTimeMillis();

                    // 前回タップから2秒以上経っていたらカウントをリセット
                    if (currentTime - lastTapTime > 2000) {
                        tapCount = 0;
                    }

                    tapCount++;
                    lastTapTime = currentTime;

                    if (tapCount >= TAP_COUNT_TO_TRIGGER) {
                        tapCount = 0; // リセットしておく

                        // 別アクティビティを起動
                        Intent intent = new Intent(getActivity(), PlatLogoActivity.class);
                        startActivity(intent);
                    }

                    return true; // 処理済み
                }
            });
        }

        Preference buildnumber = findPreference("buildnumber");
        if (buildnumber != null) {
            buildnumber.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    long currentTime = System.currentTimeMillis();

                    // 前回タップから2秒以上経っていたらカウントをリセット
                    if (currentTime - lastTapdevelopTime > 2000) {
                        tapdevelopCount = 0;
                    }

                    tapdevelopCount++;
                    lastTapdevelopTime = currentTime;

                    if (tapdevelopCount == TAP_COUNT_TO_DEVELOP_FIRST_TRIGGER) {
                        if (toast != null) {
                            // 既存のToastをキャンセル
                            toast.cancel();
                        }
                        toast = Toast.makeText(getActivity(), "デベロッパーになるまであと4ステップです。", Toast.LENGTH_SHORT);
                        toast.show();
                    }

                    if (tapdevelopCount == TAP_COUNT_TO_DEVELOP_SECOND_TRIGGER) {
                        if (toast != null) {
                            // 既存のToastをキャンセル
                            toast.cancel();
                        }
                        toast = Toast.makeText(getActivity(), "デベロッパーになるまであと3ステップです。", Toast.LENGTH_SHORT);
                        toast.show();
                    }

                    if (tapdevelopCount == TAP_COUNT_TO_DEVELOP_THIRD_TRIGGER) {
                        if (toast != null) {
                            // 既存のToastをキャンセル
                            toast.cancel();
                        }
                        toast = Toast.makeText(getActivity(), "デベロッパーになるまであと2ステップです。", Toast.LENGTH_SHORT);
                        toast.show();
                    }

                    if (tapdevelopCount == TAP_COUNT_TO_DEVELOP_FOURTH_TRIGGER) {
                        if (toast != null) {
                            // 既存のToastをキャンセル
                            toast.cancel();
                        }
                        toast = Toast.makeText(getActivity(), "デベロッパーになるまであと1ステップです。", Toast.LENGTH_SHORT);
                        toast.show();
                    }

                    if (tapdevelopCount >= TAP_COUNT_TO_DEVELOP_TRIGGER) {
                        tapdevelopCount = 0; // リセットしておく
                        if (toast != null) {
                            // 既存のToastをキャンセル
                            toast.cancel();
                        }
                        // 別アクティビティを起動
                        new AlertDialog.Builder(getActivity())
                                .setTitle("パスワード入力")
                                .setMessage("アクセスにはパスワードが必要です")
                                .setView(getPasswordInputView())  // 後で定義する関数
                                .setPositiveButton("OK", (dialog, which) -> {
                                    EditText input = ((AlertDialog) dialog).findViewById(R.id.password_edit);
                                    if (input != null) {
                                        String enteredPassword = input.getText().toString();
                                        String correctPassword = getString(R.string.password_key);

                                        if (enteredPassword.equals(correctPassword)) {
                                            // パスワード一致 → アクティビティ起動
                                            toast = Toast.makeText(getActivity(), "これでデベロッパーになりました！", Toast.LENGTH_SHORT);
                                            toast.show();
                                            Intent intent = new Intent(getActivity(), DebugActivity.class);
                                            startActivity(intent);
                                        } else {
                                            // 不一致 → エラーメッセージ
                                            Toast.makeText(getActivity(), "パスワードが違います", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                })
                                .setNegativeButton("キャンセル", null)
                                .show();
                    }

                    return true; // 処理済み
                }
            });
        }
    }

    private View getPasswordInputView() {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        return inflater.inflate(R.layout.dialog_password, null);
    }

}
