package com.aozora.aozora;

import android.content.Context;
import android.webkit.WebStorage;
import android.webkit.WebView;

import java.io.File;

public class WebViewUtils {
    public static void clearWebViewCache(Context context) {
        try {
            // WebViewキャッシュ削除
            WebView webView = new WebView(context);
            webView.clearCache(true);
            webView.clearHistory();
            webView.clearFormData();

            // WebStorage削除
            WebStorage.getInstance().deleteAllData();

            // Appキャッシュフォルダ削除
            deleteRecursive(context.getCacheDir());
            deleteRecursive(new File(context.getFilesDir(), "webview"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory != null && fileOrDirectory.exists()) {
            if (fileOrDirectory.isDirectory()) {
                for (File child : fileOrDirectory.listFiles()) {
                    deleteRecursive(child);
                }
            }
            fileOrDirectory.delete();
        }
    }
}