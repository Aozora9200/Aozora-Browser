package com.aozora.aozora;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.JavascriptInterface;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new JSInterface(), "Android");
        webView.loadUrl("file:///android_asset/exec.html");
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // 前の画面に戻る
            return true;
        }
        return super.onOptionsItemSelected(item);
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