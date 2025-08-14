package com.aozora.aozora;

import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;

public class AdBlockWebViewClient extends WebViewClient {

    // 簡易的なブロックリスト（ドメインベース）
    private static final Set<String> AD_DOMAINS = new HashSet<>();

    static {
        AD_DOMAINS.add("doubleclick.net");
        AD_DOMAINS.add("ads.google.com");
        AD_DOMAINS.add("googlesyndication.com");
        AD_DOMAINS.add("adnxs.com");
        AD_DOMAINS.add("ads.yahoo.com");
        // 必要に応じて追加
    }

    private boolean isAdUrl(String url) {
        for (String domain : AD_DOMAINS) {
            if (url.contains(domain)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        if (isAdUrl(url)) {
            // 空のレスポンスを返して広告をブロック
            return new WebResourceResponse("text/plain", "utf-8",
                    new ByteArrayInputStream("".getBytes()));
        }
        return super.shouldInterceptRequest(view, url);
    }
}