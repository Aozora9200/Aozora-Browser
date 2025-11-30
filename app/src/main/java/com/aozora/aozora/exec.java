package com.aozora.aozora;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class exec extends Activity {

    private WebView webView;
    private Process currentProcess;
    private ScheduledExecutorService timeoutExecutor;
    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME = "selected_theme";
    private static final int THEME_LIGHT = 0;
    private static final int THEME_DARK = 1;
    private static final int THEME_SYSTEM = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applySavedTheme();
        setContentView(R.layout.exec);
        webView = findViewById(R.id.webview);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        // Action Bar が表示されているか確認
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                // ★ タイトルを自分で指定（ここが「〇〇のページ」相当）
                String customTitle = "このページからのメッセージ"; // ←ここを好きな文字に変える

                new AlertDialog.Builder(view.getContext())
                        .setTitle(customTitle)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        result.confirm();
                                    }
                                })
                        .setCancelable(false)
                        .create()
                        .show();

                // trueを返すことで、デフォルトの「○○のページ」ダイアログを無効化
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
                // confirm用（同様にタイトルを変えられる）
                String customTitle = "このページからのメッセージ";

                new AlertDialog.Builder(view.getContext())
                        .setTitle(customTitle)
                        .setMessage(message)
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        result.confirm();
                                    }
                                })
                        .setNegativeButton("キャンセル",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        result.cancel();
                                    }
                                })
                        .create()
                        .show();

                return true;
            }

        });
        webView.addJavascriptInterface(new JSInterface(), "Android");
        webView.loadUrl("file:///android_asset/exec.html");
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

    public boolean onOptionsItemSelected(MenuItem item) {
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

    public class JSInterface {
        @JavascriptInterface
        public void executeCommand(String command) {
            if (command.trim().isEmpty()) {
                runOnUiThread(() -> webView.evaluateJavascript(
                        "javascript:showToast('コマンドを入力してください。')", null));
                return;
            }
            executeCommandInternal(command);
        }

        @JavascriptInterface
        public void stopCommand() {
            if (currentProcess != null && isProcessAlive(currentProcess)) {
                currentProcess.destroy();
                runOnUiThread(() -> webView.evaluateJavascript(
                        "javascript:appendOutput('INFO: コマンドが強制終了されました\\n')", null));
            } else {
                runOnUiThread(() -> webView.evaluateJavascript(
                        "javascript:showToast('実行中のプロセスはありません。')", null));
            }
        }

    }

    private boolean isProcessAlive(Process process) {
        try {
            process.exitValue(); // プロセスが終了していれば値を返す
            return false;        // exitValue()が例外を投げなければ、終了している
        } catch (IllegalThreadStateException e) {
            return true;         // プロセスはまだ生きている
        }
    }

    private void executeCommandInternal(String command) {
        runOnUiThread(() -> webView.evaluateJavascript("javascript:clearOutput()", null));
        try {
            currentProcess = Runtime.getRuntime().exec(command);
            StringBuilder outputBuilder = new StringBuilder();

            timeoutExecutor = Executors.newSingleThreadScheduledExecutor();
            timeoutExecutor.schedule(() -> {
                if (isProcessAlive(currentProcess)) {
                    currentProcess.destroy();
                    runOnUiThread(() -> webView.evaluateJavascript(
                            "javascript:appendOutput('INFO: タイムアウトにより強制終了されました\\n')", null));
                }
            }, 30, TimeUnit.SECONDS);

            Executors.newSingleThreadExecutor().submit(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(currentProcess.getInputStream()));
                     BufferedReader errorReader = new BufferedReader(
                             new InputStreamReader(currentProcess.getErrorStream()))) {

                    String line;
                    while ((line = reader.readLine()) != null) {
                        outputBuilder.append(line).append("\n");
                        final String finalLine = line;
                        runOnUiThread(() -> webView.evaluateJavascript(
                                "javascript:appendOutput('" + escapeForJS(finalLine) + "\\n')", null));
                    }
                    while ((line = errorReader.readLine()) != null) {
                        outputBuilder.append("ERROR: ").append(line).append("\n");
                        final String finalLine = line;
                        runOnUiThread(() -> webView.evaluateJavascript(
                                "javascript:appendOutput('ERROR: " + escapeForJS(finalLine) + "\\n')", null));
                    }
                } catch (IOException e) {
                    runOnUiThread(() -> webView.evaluateJavascript(
                            "javascript:appendOutput('ERROR: " + escapeForJS(e.getMessage()) + "\\n')", null));
                }
            });
        } catch (IOException e) {
            runOnUiThread(() -> webView.evaluateJavascript(
                    "javascript:appendOutput('ERROR: " + escapeForJS(e.getMessage()) + "')", null));
        }
    }

    private String escapeForJS(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}