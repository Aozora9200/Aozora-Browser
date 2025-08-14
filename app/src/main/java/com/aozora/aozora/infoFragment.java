package com.aozora.aozora;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class infoFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.infopreference);
        Preference openActivityPreference = findPreference("getpropinfo");
        if (openActivityPreference != null) {
            openActivityPreference.setOnPreferenceClickListener(preference -> {
                showDeviceInfo();
                return true;
            });
        }
        Preference licensePreference = findPreference("license");
        if (licensePreference != null) {
            licensePreference.setOnPreferenceClickListener(preference -> {
                show(getActivity(), "ライセンス");
                return true;
            });
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ✅ 見切れ対策のためにパディングを追加
        View rootView = getView();
        if (rootView != null) {
            ListView listView = (ListView) rootView.findViewById(android.R.id.list);
            if (listView != null) {
                int statusBarHeight = getStatusBarHeight();
                listView.setPadding(0, statusBarHeight, 0, 0);
                listView.setClipToPadding(false);
            }
        }
    }

    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    private void showDeviceInfo() {
        StringBuilder result = new StringBuilder();
        try {
            Process process = Runtime.getRuntime().exec("getprop");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }
            reader.close();
        } catch (Exception e) {
            result.append("取得失敗");
        }
        new AlertDialog.Builder(getActivity())
                .setTitle("端末情報")
                .setMessage(result.toString())
                .setPositiveButton("OK", (dialog, which) -> {
                })
                .show();
    }

    private static void show(Context context, String title) {
        // WebView を作成
        WebView webView = new WebView(context);
        webView.setWebViewClient(new WebViewClient()); // リンククリックで外部ブラウザを開かない

        // assets/license.md を読み込んで HTML に変換
        String html = loadMarkdownAsHtml(context, "LICENSE.MD");

        // WebView に HTML を読み込む
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);

        // ダイアログの表示
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setView(webView);
        builder.setPositiveButton("閉じる", null);
        builder.show();
    }

    private static String loadMarkdownAsHtml(Context context, String fileName) {
        try {
            InputStream inputStream = context.getAssets().open(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder mdBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                mdBuilder.append(line).append("\n");
            }
            reader.close();

            // Markdown を簡易 HTML に変換（※必要に応じて改良）
            String body = mdBuilder.toString()
                    .replaceAll("(?m)^# (.+)", "<h1>$1</h1>")
                    .replaceAll("(?m)^## (.+)", "<h2>$1</h2>")
                    .replaceAll("(?m)^### (.+)", "<h3>$1</h3>")
                    .replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>")
                    .replaceAll("\\*(.*?)\\*", "<i>$1</i>")
                    .replaceAll("`(.*?)`", "<code>$1</code>")
                    .replaceAll("\n", "<br>");

            return "<html><body style='padding:16px;font-family:sans-serif;'>" + body + "</body></html>";

        } catch (IOException e) {
            e.printStackTrace();
            return "<html><body><p>読み込みエラー</p></body></html>";
        }
    }
}
