package com.aozora.aozora;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class pagedl extends Activity {
    private static final String TAG = "pagedl";
    private static final long MIN_STORAGE_THRESHOLD = 512L * 1024L * 1024L;
    private static final String PC_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";
    private static final String ACCEPT_HEADER = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
    private static final String ACCEPT_LANGUAGE = "ja-JP,ja;q=0.9,en-US;q=0.8,en;q=0.7";
    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 30000;
    private static final int BUFFER_SIZE = 16384;
    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 1;
    private static final long LOAD_WAIT_MS = 10000;
    private EditText urlInput;
    private Switch jsSwitch;
    private Switch resourceSwitch;
    private Switch pcUaSwitch;
    private Button saveButton;
    private WebView webView;
    private ProgressDialog progressDialog;

    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME = "selected_theme";
    private static final int THEME_LIGHT = 0;
    private static final int THEME_DARK = 1;
    private static final int THEME_SYSTEM = 2;
    private ImageView Background;

    private static final String KEY_BACKGROUND = "selected_background";
    private static final int BACKGROUND1 = 0;
    private static final int BACKGROUND2 = 1;
    private static final int BACKGROUND3 = 2;
    private static final int BACKGROUND4 = 3;
    private static final int BACKGROUND_CUSTOM = 4;
    private static final String KEY_IMAGE_URI = "image_uri";

    public static final String ACTION_DOWNLOAD_STARTED = "com.aozora.aozora.pagedl.ACTION_DOWNLOAD_STARTED";
    public static final String ACTION_DOWNLOAD_PROGRESS = "com.aozora.aozora.pagedl.ACTION_DOWNLOAD_PROGRESS";
    public static final String ACTION_DOWNLOAD_COMPLETE = "com.aozora.aozora.pagedl.ACTION_DOWNLOAD_COMPLETE";
    public static final String ACTION_DOWNLOAD_ERROR = "com.aozora.aozora.pagedl.ACTION_DOWNLOAD_ERROR";

    public static final String EXTRA_SESSION_ID = "extra_session_id";

    private Set<String> loadedResources = new HashSet<>();
    private volatile AtomicBoolean isSaving = new AtomicBoolean(false);
    private Handler handler = new Handler(Looper.getMainLooper());

    private long currentSessionId = -1L;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ACTION_DOWNLOAD_PROGRESS.equals(action)) {
                long sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1L);
                if (sessionId != currentSessionId) {
                    return;
                }

                final String msg = intent.getStringExtra("message");
                final boolean isResourceProgress = intent.getBooleanExtra("resource_progress", false);

                handler.post(() -> {
                    if (progressDialog == null) return;
                    if (msg == null) return;

                    boolean resourceSwitchChecked = (resourceSwitch != null && resourceSwitch.isChecked());
                    boolean resourceSwitchEnabled = (resourceSwitch != null && resourceSwitch.isEnabled());

                    if (isResourceProgress && (!resourceSwitchChecked || !resourceSwitchEnabled)) {
                        progressDialog.setMessage("ダウンロード中です…");
                    } else {
                        progressDialog.setMessage(msg);
                    }
                });

            } else if (ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                long sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1L);
                if (sessionId != currentSessionId) return;

                final String path = intent.getStringExtra("outputPath");
                runOnUiThread(() -> {
                    if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
                    Toast.makeText(pagedl.this, "保存完了：\n" + path, Toast.LENGTH_LONG).show();
                    clearCacheAndCookies();
                });
            } else if (ACTION_DOWNLOAD_ERROR.equals(action)) {
                long sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1L);
                if (sessionId != currentSessionId) return;

                final String err = intent.getStringExtra("error");
                runOnUiThread(() -> {
                    if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
                    showCancellationNotification(err);
                    handler.postDelayed(() -> {
                        finishAffinity();
                        System.exit(0);
                    }, 2200);
                });
            }
        }
    };

    private void showCancellationNotification(String message) {
        final String channelId = "pagedl_cancel_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            final NotificationChannel ch = new NotificationChannel(channelId, "Page Download Cancellation", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("ページ保存キャンセルの通知");
            if (nm != null) {
                nm.createNotificationChannel(ch);
            }
        }

        final NotificationCompat.Builder nb = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("ダウンロードキャンセル")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setTimeoutAfter(1900L);
        final NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(0x1454, nb.build());
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applySavedTheme();
        setContentView(R.layout.activity_pgdl);
        Background = findViewById(R.id.background);
        applySavedBackground();
        applyBackTheme();

        urlInput = findViewById(R.id.urlInput);
        jsSwitch = findViewById(R.id.jsSwitch);
        resourceSwitch = findViewById(R.id.resourceZipSwitch);
        pcUaSwitch = findViewById(R.id.pcUaSwitch);
        saveButton = findViewById(R.id.saveButton);
        webView = findViewById(R.id.webView);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webView.setOnTouchListener((v, event) -> true);

        jsSwitch.setChecked(true);
        final WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(jsSwitch.isChecked());
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setBlockNetworkLoads(false);
        webSettings.setSupportMultipleWindows(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        updateUserAgent(webSettings);

        // Action Bar が表示されているか確認
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        jsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            webSettings.setJavaScriptEnabled(isChecked);
            boolean hasStorage = getAvailableStorage() >= MIN_STORAGE_THRESHOLD;
            resourceSwitch.setEnabled(isChecked && hasStorage);
            if (!isChecked && resourceSwitch.isChecked()) {
                resourceSwitch.setChecked(false);
            }
        });

        pcUaSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> updateUserAgent(webSettings));

        resourceSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!jsSwitch.isChecked()) {
                    resourceSwitch.setChecked(false);
                    Toast.makeText(this, "JavaScriptが無効なため、リソース保存は利用できません", Toast.LENGTH_LONG).show();
                    return;
                }
                if (getAvailableStorage() < MIN_STORAGE_THRESHOLD) {
                    resourceSwitch.setChecked(false);
                    Toast.makeText(this, "ストレージ容量が512MB未満のため、リソース保存を無効にします", Toast.LENGTH_LONG).show();
                }
            }
        });

        saveButton.setOnClickListener(v -> handleSaveButtonClick());

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("ダウンロード中です…");
        progressDialog.setCancelable(false);

        checkStorageAndUpdateUI();

        final IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_DOWNLOAD_PROGRESS);
        filter.addAction(ACTION_DOWNLOAD_COMPLETE);
        filter.addAction(ACTION_DOWNLOAD_ERROR);
        // Android 13 (API 33) 以降では export 指定が必須
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(receiver, filter);
        }

        checkNotificationPermission();
    }

    private void applySavedBackground() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int theme = prefs.getInt(KEY_BACKGROUND, BACKGROUND1);
        ImageView BackgroundImage = findViewById(R.id.backgroundImage);
        switch (theme) {
            case BACKGROUND1:
                BackgroundImage.setVisibility(View.VISIBLE);
                Background.setVisibility(View.VISIBLE);
                BackgroundImage.setImageResource(R.drawable.setupback);
                break;
            case BACKGROUND2:
                BackgroundImage.setVisibility(View.VISIBLE);
                Background.setVisibility(View.VISIBLE);
                BackgroundImage.setImageResource(R.drawable.background2);
                break;
            case BACKGROUND3:
                BackgroundImage.setVisibility(View.VISIBLE);
                Background.setVisibility(View.VISIBLE);
                BackgroundImage.setImageResource(R.drawable.background3);
                break;
            case BACKGROUND4:
                BackgroundImage.setVisibility(View.GONE);
                Background.setVisibility(View.GONE);
                break;
            case BACKGROUND_CUSTOM:
            default:
                String uriString = prefs.getString(KEY_IMAGE_URI, null);
                if (uriString != null) {
                    Uri savedUri = Uri.parse(uriString);
                    BackgroundImage.setVisibility(View.VISIBLE);
                    Background.setVisibility(View.VISIBLE);
                    BackgroundImage.setImageURI(savedUri);
                } else {
                    BackgroundImage.setVisibility(View.VISIBLE);
                    Background.setVisibility(View.VISIBLE);
                    BackgroundImage.setImageResource(R.drawable.setupback);
                }
                break;
        }
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

    private void applyBackTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int theme = prefs.getInt(KEY_THEME, THEME_SYSTEM);
        int black = getResources().getColor(R. color. backgroundBlack);
        int white = getResources().getColor(R. color. backgroundWhite);

        switch (theme) {
            case THEME_LIGHT:
                Background.setBackgroundColor(white);
                break;
            case THEME_DARK:
                Background.setBackgroundColor(black);
                break;
            case THEME_SYSTEM:
            default:
                // OS 側の設定に従う
                int nightModeFlags = getResources().getConfiguration().uiMode
                        & android.content.res.Configuration.UI_MODE_NIGHT_MASK;

                if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                    Background.setBackgroundColor(black);
                } else {
                    Background.setBackgroundColor(white);
                }
                break;
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE_POST_NOTIFICATIONS);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "通知権限が拒否されました。通知が表示されない可能性があります。", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // 前の画面に戻る
            overridePendingTransition(R.anim.no_animation,  R.anim.slide_out_down_low);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(receiver);
        } catch (final Exception ignored) {
        }
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        webView.destroy();
    }

    private void checkStorageAndUpdateUI() {
        boolean hasStorage = getAvailableStorage() >= MIN_STORAGE_THRESHOLD;
        if (!hasStorage) {
            resourceSwitch.setEnabled(false);
            if (resourceSwitch.isChecked()) resourceSwitch.setChecked(false);
            Toast.makeText(this, "ストレージ容量が512MB未満のため、リソース保存を無効にします", Toast.LENGTH_LONG).show();
        } else {
            resourceSwitch.setEnabled(jsSwitch.isChecked());
        }
    }

    private long getAvailableStorage() {
        final File path = Environment.getExternalStorageDirectory();
        return path.getFreeSpace();
    }

    private void updateUserAgent(final WebSettings webSettings) {
        if (pcUaSwitch.isChecked()) {
            webSettings.setUserAgentString(PC_USER_AGENT);
        } else {
            webSettings.setUserAgentString(WebSettings.getDefaultUserAgent(this));
        }
    }

    private void handleSaveButtonClick() {
        checkStorageAndUpdateUI();

        if (!isSaving.compareAndSet(false, true)) {
            Toast.makeText(this, "現在保存処理中です", Toast.LENGTH_SHORT).show();
            return;
        }

        final String urlString = urlInput.getText().toString().trim();
        if (urlString.isEmpty()) {
            Toast.makeText(this, "URLを入力してください", Toast.LENGTH_SHORT).show();
            isSaving.set(false);
            return;
        }
        if (urlString.startsWith("blob:")) {
            Toast.makeText(this, "blob: URLはサポートされていません", Toast.LENGTH_LONG).show();
            isSaving.set(false);
            return;
        }

        currentSessionId = System.currentTimeMillis();

        final String siteName = urlString.replaceAll("[^a-zA-Z0-9]", "_");

        progressDialog.setMessage("ダウンロード中です…");
        progressDialog.show();

        final File outputDir = createOutputDirectory(siteName);
        if (outputDir == null) {
            if (progressDialog.isShowing()) progressDialog.dismiss();
            isSaving.set(false);
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> saveSiteInfo(urlString, outputDir));

        if (urlString.startsWith("data:")) {
            Executors.newSingleThreadExecutor().execute(() -> saveDataUrlContent(urlString, outputDir, new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date())));
            isSaving.set(false);
            return;
        }

        final boolean saveResources = resourceSwitch.isChecked();
        final boolean jsEnabled = jsSwitch.isChecked();

        if (saveResources || !jsEnabled) {
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    final byte[] htmlData = downloadResourceBytes(urlString, pcUaSwitch.isChecked(), urlString);
                    if (htmlData == null) {
                        throw new IOException("HTMLダウンロード失敗");
                    }
                    final File htmlFile = new File(outputDir, "page.html");
                    try (final FileOutputStream fos = new FileOutputStream(htmlFile)) {
                        fos.write(htmlData);
                    }

                    if (!saveResources && !jsEnabled) {
                        runOnUiThread(() -> {
                            if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
                            Toast.makeText(pagedl.this, "保存完了：\n" + htmlFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                            clearCacheAndCookies();
                        });
                    }
                } catch (final Exception e) {
                    runOnUiThread(() -> {
                        if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
                        Toast.makeText(pagedl.this, "HTML保存エラー: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                } finally {
                    isSaving.set(false);
                }
            });
        }

        if (jsEnabled) {
            loadedResources.clear();
            webView.setWebViewClient(new CustomWebViewClient(urlString, siteName, outputDir));
            webView.loadUrl(urlString);
        }
    }

    private class CustomWebViewClient extends WebViewClient {
        private final String originalUrl;
        private final String siteName;
        private final File outputDir;
        private boolean pageLoaded = false;

        public CustomWebViewClient(String originalUrl, String siteName, File outputDir) {
            this.originalUrl = originalUrl;
            this.siteName = siteName;
            this.outputDir = outputDir;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (pageLoaded) return;
            pageLoaded = true;
            String idleScript = "(function() {"
                    + "  var idleTime = 0;"
                    + "  var idleInterval = setInterval(function() {"
                    + "    idleTime++;"
                    + "    if (idleTime > 3) { "
                    + "      clearInterval(idleInterval);"
                    + "      window.onGrokIdle = true;"
                    + "    }"
                    + "  }, 1000);"
                    + "  document.addEventListener('mousemove', function(){ idleTime = 0; });"
                    + "  document.addEventListener('keypress', function(){ idleTime = 0; });"
                    + "  document.addEventListener('scroll', function(){ idleTime = 0; });"
                    + "  if (document.readyState === 'complete') {"
                    + "    idleTime = 4;"
                    + "  }"
                    + "})();";
            view.evaluateJavascript(idleScript, null);
            handler.postDelayed(() -> checkAndSaveArchive(view, siteName, originalUrl, outputDir), LOAD_WAIT_MS);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            String resUrl = request.getUrl().toString();
            if (Utils.isResourceType(resUrl)) {
                loadedResources.add(resUrl);
            }
            return super.shouldInterceptRequest(view, request);
        }
    }

    private void checkAndSaveArchive(WebView view, String siteName, String urlString, File outputDir) {
        view.evaluateJavascript("window.onGrokIdle", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                if ("true".equals(value)) {
                    handlerDelayedSaveArchive(siteName, urlString, outputDir);
                } else {
                    handler.postDelayed(() -> checkAndSaveArchive(view, siteName, urlString, outputDir), 2000);
                }
            }
        });
    }

    private void handlerDelayedSaveArchive(final String siteName, final String urlString, final File outputDir) {
        try {
            final String archivePath = new File(outputDir, "page.mht").getAbsolutePath();

            final String cssScript = "(function() { var css = []; for(var i=0; i<document.styleSheets.length; i++){ var s = document.styleSheets[i]; if(s.href) css.push(s.href); else if(s.ownerNode && s.ownerNode.tagName === 'STYLE') { css.push(s.ownerNode.innerHTML); } } return JSON.stringify(css); })()";
            webView.evaluateJavascript(cssScript, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    if (value != null && !"null".equals(value)) {
                        try {
                            JSONArray array = new JSONArray(value);
                            for (int i = 0; i < array.length(); i++) {
                                String cssItem = array.getString(i);
                                if (cssItem.startsWith("http://") || cssItem.startsWith("https://")) {
                                    loadedResources.add(cssItem);
                                } else {
                                    Set<String> inlineUrls = Utils.extractResourcesFromCss(cssItem, urlString);
                                    loadedResources.addAll(inlineUrls);
                                }
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Failed to parse CSS URLs", e);
                        }
                    }

                    String domResourceScript = "(function() {"
                            + "  var resources = [];"
                            + "  var selectors = 'img,script,link,style,video,audio,source,iframe,object,embed,param';"
                            + "  var elements = document.querySelectorAll(selectors);"
                            + "  for (var i = 0; i < elements.length; i++) {"
                            + "    var el = elements[i];"
                            + "    var attrs = ['src', 'href', 'data-src', 'data-lazy-src', 'data-lazy', 'data-srcset', 'poster', 'srcset', 'data-poster', 'data-background', 'background', 'data-image', 'data-url', 'data'];"
                            + "    for (var j = 0; j < attrs.length; j++) {"
                            + "      var attrVal = el.getAttribute(attrs[j]);"
                            + "      if (attrVal) resources.push(attrVal);"
                            + "    }"
                            + "    var style = window.getComputedStyle(el).backgroundImage;"
                            + "    if (style && style !== 'none') {"
                            + "      var urlMatch = style.match(/url\\s*\\([\"']?([^\"'\\)]+)[\"']?\\)/);"
                            + "      if (urlMatch) resources.push(urlMatch[1]);"
                            + "    }"
                            + "  }"
                            + "  var scripts = document.querySelectorAll('script');"
                            + "  for (var k = 0; k < scripts.length; k++) {"
                            + "    var scriptContent = scripts[k].innerHTML;"
                            + "    if (scriptContent) {"
                            + "      var matches = scriptContent.match(/(https?:\\/\\/[^\\s\"']+)/g);"
                            + "      if (matches) resources = resources.concat(matches);"
                            + "    }"
                            + "  }"
                            + "  return JSON.stringify(resources);"
                            + "})()";
                    webView.evaluateJavascript(domResourceScript, new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String domValue) {
                            if (domValue != null && !"null".equals(domValue)) {
                                try {
                                    JSONArray domArray = new JSONArray(domValue);
                                    for (int i = 0; i < domArray.length(); i++) {
                                        String res = domArray.getString(i);
                                        if (res.startsWith("http://") || res.startsWith("https://") || res.startsWith("//")) {
                                            if (res.startsWith("//")) res = "https:" + res;
                                            loadedResources.add(res);
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.w(TAG, "Failed to parse DOM resources", e);
                                }
                            }

                            webView.saveWebArchive(archivePath, false, archiveValue -> {
                                if (archiveValue == null) {
                                    captureManualHtml(urlString, outputDir);
                                    return;
                                }

                                final boolean saveResources = resourceSwitch.isChecked();
                                if (saveResources) {
                                    final Intent svc = new Intent(pagedl.this, DownloadService.class);
                                    svc.putExtra(DownloadService.EXTRA_ARCHIVE_PATH, archiveValue);
                                    svc.putExtra(DownloadService.EXTRA_HTML_PATH, new File(outputDir, "page.html").getAbsolutePath());
                                    svc.putExtra(DownloadService.EXTRA_OUTPUT_DIR, outputDir.getAbsolutePath());
                                    svc.putExtra(DownloadService.EXTRA_SAVE_RESOURCES, true);
                                    svc.putExtra(DownloadService.EXTRA_PC_UA, pcUaSwitch.isChecked());
                                    svc.putExtra(DownloadService.EXTRA_BASE_URL, urlString);
                                    svc.putExtra(DownloadService.EXTRA_REFERER, urlString);
                                    svc.putExtra(DownloadService.EXTRA_JS_ENABLED, jsSwitch.isChecked());
                                    svc.putStringArrayListExtra(DownloadService.EXTRA_LOADED_RESOURCES, new ArrayList<>(loadedResources));
                                    svc.putExtra(EXTRA_SESSION_ID, currentSessionId);
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        startForegroundService(svc);
                                    } else {
                                        startService(svc);
                                    }
                                } else {
                                    runOnUiThread(() -> {
                                        if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
                                        Toast.makeText(pagedl.this, "保存完了：\n" + archiveValue, Toast.LENGTH_LONG).show();
                                        clearCacheAndCookies();
                                    });
                                }
                                isSaving.set(false);
                            });
                        }
                    });
                }
            });
        } catch (final Exception e) {
            if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
            Toast.makeText(this, "初期化エラー: " + e.getMessage(), Toast.LENGTH_LONG).show();
            isSaving.set(false);
        }
    }

    private void captureManualHtml(String urlString, File outputDir) {
        webView.evaluateJavascript("(function() { return ('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>'); })();", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String html) {
                if (html != null && !"null".equals(html)) {
                    html = html.replaceAll("^\"|\"$", "");
                    File htmlFile = new File(outputDir, "page_manual.html");
                    try {
                        Utils.writeStringToFile(htmlFile, html);

                        final Intent svc = new Intent(pagedl.this, DownloadService.class);
                        svc.putExtra(DownloadService.EXTRA_ARCHIVE_PATH, (String) null);
                        svc.putExtra(DownloadService.EXTRA_HTML_PATH, htmlFile.getAbsolutePath());
                        svc.putExtra(DownloadService.EXTRA_OUTPUT_DIR, outputDir.getAbsolutePath());
                        svc.putExtra(DownloadService.EXTRA_SAVE_RESOURCES, resourceSwitch.isChecked());
                        svc.putExtra(DownloadService.EXTRA_PC_UA, pcUaSwitch.isChecked());
                        svc.putExtra(DownloadService.EXTRA_BASE_URL, urlString);
                        svc.putExtra(DownloadService.EXTRA_REFERER, urlString);
                        svc.putExtra(DownloadService.EXTRA_JS_ENABLED, jsSwitch.isChecked());
                        svc.putStringArrayListExtra(DownloadService.EXTRA_LOADED_RESOURCES, new ArrayList<>(loadedResources));
                        svc.putExtra(EXTRA_SESSION_ID, currentSessionId);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(svc);
                        } else {
                            startService(svc);
                        }

                        if (!resourceSwitch.isChecked()) {
                            handler.post(() -> {
                                if (progressDialog != null) progressDialog.setMessage("ダウンロード中です…");
                            });
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Manual HTML save failed", e);
                        runOnUiThread(() -> {
                            if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
                            Toast.makeText(pagedl.this, "Manual HTML 保存失敗", Toast.LENGTH_LONG).show();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
                        Toast.makeText(pagedl.this, "HTML キャプチャ失敗", Toast.LENGTH_LONG).show();
                    });
                }
                isSaving.set(false);
            }
        });
    }

    private File createOutputDirectory(final String siteName) {
        final String datetime = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        final File baseDir = new File(Environment.getExternalStorageDirectory(), "Documents/AozoraBrowser/SavedPage");
        final File outputDir = new File(baseDir, datetime + "_" + siteName);
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            runOnUiThread(() -> Toast.makeText(pagedl.this, "フォルダ作成失敗: " + outputDir.getAbsolutePath(), Toast.LENGTH_LONG).show());
            return null;
        }
        return outputDir;
    }

    private void saveSiteInfo(final String urlString, final File outputDir) {
        try {
            final URL url = new URL(urlString);
            final StringBuilder info = new StringBuilder();
            info.append("URL: ").append(urlString).append("\n");
            info.append("Timestamp: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n");

            info.append("JavaScript Enabled: ").append(jsSwitch.isChecked()).append("\n");
            info.append("Resource Saving: ").append(resourceSwitch.isChecked()).append("\n");
            info.append("PC User Agent: ").append(pcUaSwitch.isChecked()).append("\n");

            try {
                final InetAddress[] addresses = InetAddress.getAllByName(url.getHost());
                info.append("IP Addresses:\n");
                for (final InetAddress address : addresses) {
                    info.append("  - ").append(address.getHostAddress()).append("\n");
                }
            } catch (final UnknownHostException e) {
                info.append("IP Addresses: Unable to resolve\n");
            }

            try {
                final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("HEAD");
                conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                conn.setReadTimeout(READ_TIMEOUT_MS);
                conn.connect();
                info.append("HTTP Headers:\n");
                for (final Map.Entry<String, java.util.List<String>> header : conn.getHeaderFields().entrySet()) {
                    if (header.getKey() != null) {
                        info.append("  ").append(header.getKey()).append(": ").append(header.getValue()).append("\n");
                    }
                }
                conn.disconnect();
            } catch (final IOException e) {
                info.append("HTTP Headers: Unable to retrieve\n");
            }

            info.append("Open Ports:\n");
            final int[] commonPorts = {21, 22, 80, 443, 8080, 3306, 5432};
            for (final int port : commonPorts) {
                try (final Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(url.getHost(), port), 2000);
                    info.append("  - Port ").append(port).append(": Open\n");
                } catch (final IOException e) {
                    info.append("  - Port ").append(port).append(": Closed\n");
                }
            }

            try {
                info.append("Host Name: ").append(url.getHost()).append("\n");
                info.append("Protocol: ").append(url.getProtocol()).append("\n");
                info.append("Port: ").append(url.getPort() == -1 ? "Default" : String.valueOf(url.getPort())).append("\n");
            } catch (Exception e) {
                info.append("Additional URL Info: Unable to retrieve\n");
            }

            final File infoFile = new File(outputDir, new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + "_info.txt");
            Utils.writeStringToFile(infoFile, info.toString());
        } catch (final Exception e) {
            Log.w(TAG, "Failed to save site info", e);
        }
    }

    private void saveDataUrlContent(final String dataUrl, final File outputDir, final String baseName) {
        try {
            if (getAvailableStorage() < MIN_STORAGE_THRESHOLD) {
                throw new IOException("ストレージ容量不足");
            }
            final int commaIndex = dataUrl.indexOf(",");
            if (commaIndex == -1) {
                throw new IOException("data URL の形式が不正です");
            }
            final String header = dataUrl.substring(0, commaIndex);
            final String base64Data = dataUrl.substring(commaIndex + 1);
            String mimeType = "application/octet-stream";
            final Pattern pattern = Pattern.compile("data:([^;]+);base64");
            final Matcher matcher = pattern.matcher(header);
            if (matcher.find()) {
                mimeType = matcher.group(1);
            }
            final String extension = getExtensionForMimeType(mimeType);
            final String fileName = baseName + extension;
            final File outFile = new File(outputDir, fileName);
            final byte[] data = Base64.decode(base64Data, Base64.DEFAULT);
            try (final FileOutputStream fos = new FileOutputStream(outFile)) {
                fos.write(data);
            }
            runOnUiThread(() -> {
                if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
                Toast.makeText(pagedl.this, "保存完了：\n" + outFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                clearCacheAndCookies();
            });
        } catch (final Exception e) {
            runOnUiThread(() -> {
                if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
                Toast.makeText(pagedl.this, "Data URL 保存エラー: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        } finally {
            isSaving.set(false);
        }
    }

    private String getExtensionForMimeType(final String mimeType) {
        if (mimeType == null) return "";
        switch (mimeType.toLowerCase(Locale.ROOT)) {
            case "text/html":
                return ".html";
            case "image/jpeg":
            case "image/jpg":
                return ".jpg";
            case "image/png":
                return ".png";
            case "image/gif":
                return ".gif";
            case "application/pdf":
                return ".pdf";
            default:
                return "";
        }
    }

    private void clearCacheAndCookies() {
        runOnUiThread(() -> {
            webView.clearCache(true);
            final CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookies(null);
            cookieManager.flush();
        });
    }

    @Override
    public void onBackPressed() {
        if (isSaving.get()) {
            Toast.makeText(this, "保存中はバックキーが無効です", Toast.LENGTH_SHORT).show();
        } else {
            finish(); // 前の画面に戻る
            overridePendingTransition(R.anim.no_animation,  R.anim.slide_out_down_low);
        }
    }

    public static class DownloadService extends Service {
        public static final String EXTRA_ARCHIVE_PATH = "extra_archive_path";
        public static final String EXTRA_HTML_PATH = "extra_html_path";
        public static final String EXTRA_OUTPUT_DIR = "extra_output_dir";
        public static final String EXTRA_SAVE_RESOURCES = "extra_save_resources";
        public static final String EXTRA_PC_UA = "extra_pc_ua";
        public static final String EXTRA_BASE_URL = "extra_base_url";
        public static final String EXTRA_REFERER = "extra_referer";
        public static final String EXTRA_JS_ENABLED = "extra_js_enabled";
        public static final String EXTRA_LOADED_RESOURCES = "extra_loaded_resources";
        public static final String ACTION_STOP = "com.coara.pagedl.ACTION_STOP";

        private static final String CHANNEL_ID = "pagedl_download_channel";
        private static final int NOTIF_ID = 0x1453;
        private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
        private final AtomicBoolean stopped = new AtomicBoolean(false);
        private Future<?> currentTask;

        private boolean pcUa;
        private String referer;
        private long serviceSessionId = -1L;

        @Override
        public void onCreate() {
            super.onCreate();
            createNotificationChannel();
        }

        @Override
        public int onStartCommand(final Intent intent, final int flags, final int startId) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    sendError("通知権限がありません");
                    stopSelf();
                    return START_NOT_STICKY;
                }
            }
            if (intent == null) {
                return START_NOT_STICKY;
            }
            final String action = intent.getAction();
            if (ACTION_STOP.equals(action)) {
                stopped.set(true);
                if (currentTask != null && !currentTask.isDone()) {
                    currentTask.cancel(true);
                }
                final NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null) {
                    nm.cancel(NOTIF_ID);
                }
                stopForeground(true);
                executor.shutdownNow();
                sendError("処理がキャンセルされました\nアプリを終了します。");
                stopSelf();
                return START_NOT_STICKY;
            }

            final String archivePath = intent.getStringExtra(EXTRA_ARCHIVE_PATH);
            final String htmlPath = intent.getStringExtra(EXTRA_HTML_PATH);
            final String outputDir = intent.getStringExtra(EXTRA_OUTPUT_DIR);
            final boolean saveResources = intent.getBooleanExtra(EXTRA_SAVE_RESOURCES, false);
            this.pcUa = intent.getBooleanExtra(EXTRA_PC_UA, false);
            final boolean jsEnabled = intent.getBooleanExtra(EXTRA_JS_ENABLED, true);
            final String baseUrl = intent.getStringExtra(EXTRA_BASE_URL);
            this.referer = intent.getStringExtra(EXTRA_REFERER);
            final ArrayList<String> loadedResourcesList = intent.getStringArrayListExtra(EXTRA_LOADED_RESOURCES);

            serviceSessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1L);

            final File outDirFile = new File(outputDir != null ? outputDir : getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) + "/page/default");
            final Intent stopIntent = new Intent(this, DownloadService.class);
            stopIntent.setAction(ACTION_STOP);
            final int pendingFlags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT;
            final PendingIntent stopPending = PendingIntent.getService(this, 1, stopIntent, pendingFlags);

            final NotificationCompat.Builder nb = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("ページ保存処理を実行中")
                    .setContentText(outDirFile.getAbsolutePath())
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .addAction(new NotificationCompat.Action(android.R.drawable.ic_delete, "終了", stopPending))
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            startForeground(NOTIF_ID, nb.build());

            currentTask = executor.submit(() -> {
                try {
                    sendProgress("処理開始...", false);
                    String mainHtmlNoJs = null;
                    String mainHtmlJs = null;
                    final Set<String> allResources = new HashSet<>();

                    if (archivePath != null) {
                        sendProgress("アーカイブを抽出中...", false);
                        final File archiveFile = new File(archivePath);
                        mainHtmlJs = MimeParser.getMainHtml(Utils.readFileToString(archiveFile));
                        if (mainHtmlJs.isEmpty()) {
                            throw new IOException("Main HTML extraction failed");
                        }
                        final File jsHtmlFile = new File(outDirFile, "page_js.html");
                        Utils.writeStringToFile(jsHtmlFile, mainHtmlJs);

                        allResources.addAll(Utils.extractResources(mainHtmlJs, baseUrl));
                    }

                    if (htmlPath != null) {
                        mainHtmlNoJs = Utils.readFileToString(new File(htmlPath));
                        allResources.addAll(Utils.extractResources(mainHtmlNoJs, baseUrl));
                    }

                    if (loadedResourcesList != null) {
                        allResources.addAll(loadedResourcesList);
                    }

                    if (saveResources && !stopped.get()) {
                        sendProgress("リソース抽出・ダウンロード中...", true);
                        Set<String> additional = recursiveExtractFromResources(allResources, baseUrl, new HashSet<>());
                        allResources.addAll(additional);

                        Set<String> resolvedResources = new HashSet<>();
                        for (String res : allResources) {
                            try {
                                URL resolved = new URL(new URL(baseUrl), res);
                                resolvedResources.add(resolved.toString());
                            } catch (Exception e) {

                            }
                        }
                        saveResourcesToFolderFromSet(resolvedResources, outDirFile, baseUrl);
                    }

                    sendComplete(outDirFile.getAbsolutePath());
                } catch (final Exception e) {
                    if (!stopped.get()) {
                        Log.e(TAG, "Service error", e);
                        sendError(e.getMessage());
                    }
                } finally {
                    final NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    if (nm != null) {
                        nm.cancel(NOTIF_ID);
                    }
                    stopForeground(true);
                    executor.shutdownNow();
                    stopSelf();
                }
            });

            return START_NOT_STICKY;
        }

        @Nullable
        @Override
        public IBinder onBind(final Intent intent) {
            return new Binder();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            stopped.set(true);
            if (!executor.isShutdown()) {
                executor.shutdownNow();
            }
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    Log.w(TAG, "Executor did not terminate in time");
                }
            } catch (final InterruptedException ignored) {
            }
            final NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.cancel(NOTIF_ID);
            }
        }

        private void createNotificationChannel() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                final NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                final NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Page Download", NotificationManager.IMPORTANCE_DEFAULT);
                ch.setDescription("ページ保存の進行状況を表示します");
                if (nm != null) {
                    nm.createNotificationChannel(ch);
                }
            }
        }

        private void sendProgress(final String message, final boolean isResourceProgress) {
            if (stopped.get()) {
                return;
            }
            final Intent i = new Intent(ACTION_DOWNLOAD_PROGRESS);
            i.putExtra("message", message);
            i.putExtra("resource_progress", isResourceProgress);
            i.putExtra(EXTRA_SESSION_ID, serviceSessionId);
            sendBroadcast(i);

            final Intent stopIntent = new Intent(this, DownloadService.class);
            stopIntent.setAction(ACTION_STOP);
            final int pendingFlags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT;
            final PendingIntent stopPending = PendingIntent.getService(this, 1, stopIntent, pendingFlags);
            final NotificationCompat.Builder nb = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("ページ保存中")
                    .setContentText(message)
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .addAction(new NotificationCompat.Action(android.R.drawable.ic_delete, "終了", stopPending))
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
            final NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.notify(NOTIF_ID, nb.build());
            }
        }

        private void sendComplete(final String path) {
            final Intent i = new Intent(ACTION_DOWNLOAD_COMPLETE);
            i.putExtra("outputPath", path);
            i.putExtra(EXTRA_SESSION_ID, serviceSessionId);
            sendBroadcast(i);
            final NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.cancel(NOTIF_ID);
            }
        }

        private void sendError(final String err) {
            final Intent i = new Intent(ACTION_DOWNLOAD_ERROR);
            i.putExtra("error", err);
            i.putExtra(EXTRA_SESSION_ID, serviceSessionId);
            sendBroadcast(i);
            final NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.cancel(NOTIF_ID);
            }
            stopForeground(true);
            executor.shutdownNow();
            stopSelf();
        }

        private Set<String> recursiveExtractFromResources(final Set<String> initialResources, final String baseUrl, final Set<String> visited) {
            final Set<String> additional = new HashSet<>();
            for (final String resUrl : initialResources) {
                if (visited.contains(resUrl) || stopped.get()) {
                    continue;
                }
                visited.add(resUrl);
                if (!checkStorage()) {
                    return new HashSet<>();
                }
                final byte[] data = downloadResourceBytes(resUrl);
                if (data == null) {
                    continue;
                }
                final String contentType = getContentTypeFromUrl(resUrl);
                String content;
                try {
                    content = new String(data, StandardCharsets.UTF_8);
                } catch (Exception e) {
                    content = new String(data);
                }
                Set<String> subResources = new HashSet<>();
                if (contentType.contains("css") || resUrl.endsWith(".css")) {
                    subResources = Utils.extractResourcesFromCss(content, resUrl);
                } else if (contentType.contains("javascript") || resUrl.endsWith(".js")) {
                    subResources = Utils.extractResourcesFromJs(content, resUrl);
                } else if (contentType.contains("html") || resUrl.endsWith(".html") || resUrl.endsWith(".htm") || resUrl.endsWith(".php")) {
                    subResources = Utils.extractResources(content, resUrl);
                } else if (contentType.contains("json") || resUrl.endsWith(".json")) {
                    subResources = Utils.extractResourcesFromJson(content, resUrl);
                }
                additional.addAll(subResources);
                additional.addAll(recursiveExtractFromResources(subResources, baseUrl, visited));
            }
            return additional;
        }

        private String getContentTypeFromUrl(final String urlString) {
            try {
                final URL url = new URL(urlString);
                final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("HEAD");
                conn.setRequestProperty("User-Agent", pcUa ? PC_USER_AGENT : WebSettings.getDefaultUserAgent(this));
                conn.setRequestProperty("Accept", ACCEPT_HEADER);
                conn.setRequestProperty("Accept-Language", ACCEPT_LANGUAGE);
                conn.setRequestProperty("Referer", referer);
                conn.connect();
                final String contentType = conn.getContentType();
                conn.disconnect();
                return contentType != null ? contentType : "";
            } catch (final Exception e) {
                return "";
            }
        }

        private boolean checkStorage() {
            final File path = Environment.getExternalStorageDirectory();
            return path.getFreeSpace() >= MIN_STORAGE_THRESHOLD;
        }

        private void saveResourcesToFolderFromSet(final Set<String> resources, final File outputDir, final String baseUrl) {
            if (resources.isEmpty()) {
                return;
            }
            final String datetime = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            final File resourceDir = new File(outputDir, datetime + "_resources_" + baseUrl.replaceAll("[^a-zA-Z0-9]", "_"));
            if (!resourceDir.mkdirs()) {
                sendError("リソースフォルダ作成失敗");
                return;
            }
            sendProgress("リソース保存中です", true);

            final Map<String, Integer> fileNameCounts = new HashMap<>();
            ArrayList<Future<?>> downloadFutures = new ArrayList<>();
            for (final String resUrl : resources) {
                if (stopped.get() || !checkStorage()) {
                    for (Future<?> future : downloadFutures) {
                        future.cancel(true);
                    }
                    cleanupIncompleteFiles(resourceDir);
                    return;
                }
                final Future<?> future = executor.submit(() -> {
                    try {
                        final URL resourceUrl = new URL(resUrl);
                        final String path = resourceUrl.getPath();
                        String fileName = new File(path).getName();
                        if (fileName.isEmpty()) {
                            fileName = "resource_" + System.currentTimeMillis();
                        }
                        synchronized (fileNameCounts) {
                            Integer count = fileNameCounts.get(fileName);
                            if (count == null) {
                                count = 0;
                            }
                            count++;
                            fileNameCounts.put(fileName, count);
                            if (count > 1) {
                                fileName = count + "_" + fileName;
                            }
                        }
                        final File destFile = new File(resourceDir, fileName);
                        downloadResource(resUrl, destFile);
                    } catch (final Exception e) {
                        Log.w(TAG, "リソースダウンロードエラー: " + resUrl, e);
                    }
                });
                downloadFutures.add(future);
            }

            for (Future<?> future : downloadFutures) {
                try {
                    future.get();
                } catch (Exception e) {
                    Log.w(TAG, "Download future error", e);
                }
            }
        }

        private void cleanupIncompleteFiles(final File directory) {
            if (directory.exists() && directory.isDirectory()) {
                final File[] files = directory.listFiles();
                if (files != null) {
                    for (final File file : files) {
                        if (!file.delete()) {
                            Log.w(TAG, "Failed to delete incomplete file: " + file.getAbsolutePath());
                        }
                    }
                }
                if (!directory.delete()) {
                    Log.w(TAG, "Failed to delete directory: " + directory.getAbsolutePath());
                }
            }
        }

        private byte[] downloadResourceBytes(final String resourceUrl) {
            HttpURLConnection conn = null;
            try {
                final URL url = new URL(resourceUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setInstanceFollowRedirects(true);
                conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                conn.setReadTimeout(READ_TIMEOUT_MS);
                conn.setRequestProperty("User-Agent", pcUa ? PC_USER_AGENT : WebSettings.getDefaultUserAgent(this));
                conn.setRequestProperty("Accept", ACCEPT_HEADER);
                conn.setRequestProperty("Accept-Language", ACCEPT_LANGUAGE);
                conn.setRequestProperty("Referer", referer);
                final int responseCode = conn.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    return null;
                }
                try (final InputStream in = conn.getInputStream();
                     final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    final byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        if (stopped.get()) {
                            throw new ClosedByInterruptException();
                        }
                        if (!checkStorage()) {
                            return null;
                        }
                        baos.write(buffer, 0, bytesRead);
                    }
                    return baos.toByteArray();
                }
            } catch (final ClosedByInterruptException e) {
                Log.w(TAG, "Download interrupted: " + resourceUrl);
                return null;
            } catch (final Exception e) {
                Log.w(TAG, "ダウンロードエラー: " + resourceUrl, e);
                return null;
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }

        private void downloadResource(final String resourceUrl, final File destination) throws IOException {
            if (destination.exists() || stopped.get() || !checkStorage()) {
                return;
            }
            final File tempFile = new File(destination.getAbsolutePath() + ".tmp");
            HttpURLConnection conn = null;
            try {
                final URL url = new URL(resourceUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setInstanceFollowRedirects(true);
                conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                conn.setReadTimeout(READ_TIMEOUT_MS);
                conn.setRequestProperty("User-Agent", pcUa ? PC_USER_AGENT : WebSettings.getDefaultUserAgent(this));
                conn.setRequestProperty("Accept", ACCEPT_HEADER);
                conn.setRequestProperty("Accept-Language", ACCEPT_LANGUAGE);
                conn.setRequestProperty("Referer", referer);
                try (final InputStream in = conn.getInputStream();
                     final OutputStream out = new FileOutputStream(tempFile)) {
                    final byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    long totalBytes = 0;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        if (stopped.get()) {
                            throw new ClosedByInterruptException();
                        }
                        totalBytes += bytesRead;
                        if (totalBytes > MIN_STORAGE_THRESHOLD / 2) {
                            if (!checkStorage()) {
                                throw new IOException("ストレージ容量不足");
                            }
                            totalBytes = 0;
                        }
                        out.write(buffer, 0, bytesRead);
                    }
                }
                tempFile.renameTo(destination);
            } catch (final ClosedByInterruptException e) {
                if (tempFile.exists() && !tempFile.delete()) {
                    Log.w(TAG, "Failed to delete temp file: " + tempFile.getAbsolutePath());
                }
                throw e;
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }
    }

    static class Utils {
        public static String readFileToString(final File file) throws IOException {
            final byte[] buffer = new byte[(int) file.length()];
            try (final FileInputStream fis = new FileInputStream(file)) {
                final int readBytes = fis.read(buffer);
                if (readBytes != buffer.length) {
                    throw new IOException("ファイル全体の読み込みに失敗しました");
                }
                return new String(buffer, StandardCharsets.UTF_8);
            }
        }

        public static void writeStringToFile(final File file, final String content) throws IOException {
            try (final FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(content.getBytes(StandardCharsets.UTF_8));
            }
        }

        public static Set<String> extractResources(final String html, final String baseUrl) {
            final Set<String> resources = new HashSet<>();
            final Pattern pattern = Pattern.compile("(?i)(src|href|data-src|data-lazy-src|data-lazy|data-srcset|poster|srcset|data-poster|data-background|background|data-image|data-url|data)\\s*=\\s*(?:\"([^\"]+)\"|'([^']+)'|([^\\s>]+))");
            final Matcher matcher = pattern.matcher(html);
            while (matcher.find()) {
                String originalUrl = matcher.group(2);
                if (originalUrl == null) {
                    originalUrl = matcher.group(3);
                }
                if (originalUrl == null) {
                    originalUrl = matcher.group(4);
                }
                if (originalUrl != null) {
                    final String attr = matcher.group(1).toLowerCase(Locale.ROOT);
                    if ("srcset".equals(attr) || "data-srcset".equals(attr)) {
                        final String[] srcsets = originalUrl.split(",");
                        for (final String srcset : srcsets) {
                            final String trimmed = srcset.trim().split(" ")[0];
                            addResourceIfValid(resources, trimmed, baseUrl);
                        }
                    } else {
                        addResourceIfValid(resources, originalUrl, baseUrl);
                    }
                }
            }
            final Pattern stylePattern = Pattern.compile("(?i)url\\s*\\(\\s*[\"']?([^\"'\\)]+)[\"']?\\s*\\)");
            final Matcher styleMatcher = stylePattern.matcher(html);
            while (styleMatcher.find()) {
                addResourceIfValid(resources, styleMatcher.group(1), baseUrl);
            }
            final Pattern importPattern = Pattern.compile("(?i)@import\\s*(url\\()?\\s*[\"']?([^\"'\\)]+)[\"']?\\s*\\)?");
            final Matcher importMatcher = importPattern.matcher(html);
            while (importMatcher.find()) {
                addResourceIfValid(resources, importMatcher.group(2), baseUrl);
            }
            final Pattern embedPattern = Pattern.compile("(?i)(embed|object|param|video|audio|source|iframe)\\s+[^>]*src\\s*=\\s*(?:\"([^\"]+)\"|'([^']+)'|([^\\s>]+))");
            final Matcher embedMatcher = embedPattern.matcher(html);
            while (embedMatcher.find()) {
                String originalUrl = embedMatcher.group(2);
                if (originalUrl == null) {
                    originalUrl = embedMatcher.group(3);
                }
                if (originalUrl == null) {
                    originalUrl = embedMatcher.group(4);
                }
                addResourceIfValid(resources, originalUrl, baseUrl);
            }
            final Pattern inlineStylePattern = Pattern.compile("(?is)<style[^>]*>(.*?)</style>");
            final Matcher inlineMatcher = inlineStylePattern.matcher(html);
            while (inlineMatcher.find()) {
                String inlineCss = inlineMatcher.group(1);
                resources.addAll(extractResourcesFromCss(inlineCss, baseUrl));
            }
            final Pattern inlineScriptPattern = Pattern.compile("(?is)<script[^>]*>(.*?)</script>");
            final Matcher inlineScriptMatcher = inlineScriptPattern.matcher(html);
            while (inlineScriptMatcher.find()) {
                String inlineJs = inlineScriptMatcher.group(1);
                resources.addAll(extractResourcesFromJs(inlineJs, baseUrl));
            }
            return resources;
        }

        public static void addResourceIfValid(final Set<String> resources, final String originalUrl, final String baseUrl) {
            if (originalUrl == null || originalUrl.startsWith("data:") || originalUrl.startsWith("blob:") ||
                    originalUrl.startsWith("#") || originalUrl.startsWith("javascript:") || !isResourceType(originalUrl)) {
                return;
            }
            try {
                final URL resolvedUrl = originalUrl.startsWith("http://") || originalUrl.startsWith("https://")
                        ? new URL(originalUrl)
                        : new URL(new URL(baseUrl), originalUrl);
                resources.add(resolvedUrl.toString());
            } catch (final Exception e) {
                Log.w(TAG, "Invalid URL: " + originalUrl);
            }
        }

        public static boolean isResourceType(final String url) {
            final String lowerUrl = url.toLowerCase(Locale.ROOT);
            return lowerUrl.endsWith(".js") || lowerUrl.endsWith(".png") || lowerUrl.endsWith(".jpg") ||
                    lowerUrl.endsWith(".jpeg") || lowerUrl.endsWith(".webp") || lowerUrl.endsWith(".html") ||
                    lowerUrl.endsWith(".htm") || lowerUrl.endsWith(".php") || lowerUrl.endsWith(".css") ||
                    lowerUrl.endsWith(".mp3") || lowerUrl.endsWith(".wav") || lowerUrl.endsWith(".gif") ||
                    lowerUrl.endsWith(".bmp") || lowerUrl.endsWith(".pdf") || lowerUrl.endsWith(".svg") ||
                    lowerUrl.endsWith(".ico") || lowerUrl.endsWith(".mp4") || lowerUrl.endsWith(".ogg") ||
                    lowerUrl.endsWith(".webm") || lowerUrl.endsWith(".json") || lowerUrl.endsWith(".xml") ||
                    lowerUrl.endsWith(".ini") || lowerUrl.endsWith(".py") || lowerUrl.endsWith(".m3u") ||
                    lowerUrl.endsWith(".ttf") || lowerUrl.endsWith(".woff") || lowerUrl.endsWith(".woff2") ||
                    lowerUrl.endsWith(".eot") || lowerUrl.endsWith(".avif") || lowerUrl.endsWith(".heic") ||
                    lowerUrl.endsWith(".mkv") || lowerUrl.endsWith(".mov");
        }

        public static Set<String> extractResourcesFromCss(final String css, final String baseUrl) {
            final Set<String> resources = new HashSet<>();
            final Pattern urlPattern = Pattern.compile("(?i)url\\s*\\(\\s*[\"']?([^\"'\\)]+)[\"']?\\s*\\)");
            final Matcher urlMatcher = urlPattern.matcher(css);
            while (urlMatcher.find()) {
                addResourceIfValid(resources, urlMatcher.group(1), baseUrl);
            }
            final Pattern importPattern = Pattern.compile("(?i)@import\\s*(url\\()?\\s*[\"']?([^\"'\\)]+)[\"']?\\s*\\)?");
            final Matcher importMatcher = importPattern.matcher(css);
            while (importMatcher.find()) {
                addResourceIfValid(resources, importMatcher.group(2), baseUrl);
            }
            final Pattern fontFacePattern = Pattern.compile("(?is)@font-face\\s*\\{[^}]*src:\\s*url\\s*\\(\\s*[\"']?([^\"'\\)]+)[\"']?\\s*\\)[^}]*\\}");
            final Matcher fontMatcher = fontFacePattern.matcher(css);
            while (fontMatcher.find()) {
                addResourceIfValid(resources, fontMatcher.group(1), baseUrl);
            }
            return resources;
        }

        public static Set<String> extractResourcesFromJs(final String js, final String baseUrl) {
            final Set<String> resources = new HashSet<>();
            final Pattern stringPattern = Pattern.compile("(?i)([\"'])(https?://\\S+?|[^\"'\\s]+?\\.(js|css|png|jpg|webp|html|php|mp3|wav|gif|bmp|pdf|svg|ico|mp4))\\1");
            final Matcher stringMatcher = stringPattern.matcher(js);
            while (stringMatcher.find()) {
                addResourceIfValid(resources, stringMatcher.group(2), baseUrl);
            }
            final Pattern fetchPattern = Pattern.compile("(?i)fetch\\s*\\(\\s*[\"']([^\"']+)[\"']");
            final Matcher fetchMatcher = fetchPattern.matcher(js);
            while (fetchMatcher.find()) {
                addResourceIfValid(resources, fetchMatcher.group(1), baseUrl);
            }
            final Pattern xhrPattern = Pattern.compile("(?i)xhr\\.open\\s*\\(\\s*[\"']GET[\"']\\s*,\\s*[\"']([^\"']+)[\"']");
            final Matcher xhrMatcher = xhrPattern.matcher(js);
            while (xhrMatcher.find()) {
                addResourceIfValid(resources, xhrMatcher.group(1), baseUrl);
            }
            final Pattern importPattern = Pattern.compile("(?i)import\\s*\\(\\s*[\"']([^\"']+)[\"']\\s*\\)");
            final Matcher importMatcher = importPattern.matcher(js);
            while (importMatcher.find()) {
                addResourceIfValid(resources, importMatcher.group(1), baseUrl);
            }
            return resources;
        }

        public static Set<String> extractResourcesFromJson(final String json, final String baseUrl) {
            final Set<String> resources = new HashSet<>();
            final Pattern urlPattern = Pattern.compile("\"(https?://\\S+?)\"");
            final Matcher matcher = urlPattern.matcher(json);
            while (matcher.find()) {
                addResourceIfValid(resources, matcher.group(1), baseUrl);
            }
            return resources;
        }

        public static String decodeQuotedPrintable(final String input) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);
                if (c == '=' && i + 2 < input.length()) {
                    char next1 = input.charAt(i + 1);
                    char next2 = input.charAt(i + 2);
                    if (next1 == '\r' && next2 == '\n') {
                        i += 2;
                        continue;
                    } else if (next1 == '\n') {
                        i += 1;
                        continue;
                    } else {
                        try {
                            int hex = Integer.parseInt(input.substring(i + 1, i + 3), 16);
                            sb.append((char) hex);
                            i += 2;
                        } catch (NumberFormatException e) {
                            sb.append(c);
                        }
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }
    }

    static class MimeParser {
        public static Map<String, byte[]> extractResources(String mhtContent, File dir) {
            Map<String, byte[]> resources = new HashMap<>();
            Pattern boundaryPattern = Pattern.compile("boundary=\"(.*?)\"");
            Matcher boundaryMatcher = boundaryPattern.matcher(mhtContent);
            String boundary = boundaryMatcher.find() ? boundaryMatcher.group(1) : null;
            if (boundary == null) return resources;
            String[] parts = mhtContent.split("--" + Pattern.quote(boundary));
            for (String part : parts) {
                if (part.trim().isEmpty()) continue;
                Pattern locationPattern = Pattern.compile("(?im)Content-Location: (.*?)\\r?\\n");
                Matcher locationMatcher = locationPattern.matcher(part);
                String location = locationMatcher.find() ? locationMatcher.group(1).trim() : null;
                if (location == null) continue;
                Pattern encodingPattern = Pattern.compile("(?im)Content-Transfer-Encoding: (.*?)\\r?\\n");
                Matcher encodingMatcher = encodingPattern.matcher(part);
                String encoding = encodingMatcher.find() ? encodingMatcher.group(1).trim() : "7bit";
                int bodyStart = part.indexOf("\r\n\r\n");
                if (bodyStart == -1) continue;
                String rawBody = part.substring(bodyStart + 4);
                byte[] bodyBytes;
                if ("quoted-printable".equalsIgnoreCase(encoding)) {
                    String decoded = Utils.decodeQuotedPrintable(rawBody);
                    bodyBytes = decoded.getBytes(StandardCharsets.UTF_8);
                } else if ("base64".equalsIgnoreCase(encoding)) {
                    String cleaned = rawBody.replaceAll("\\s+", "");
                    bodyBytes = Base64.decode(cleaned, Base64.DEFAULT);
                } else {
                    bodyBytes = rawBody.getBytes(StandardCharsets.UTF_8);
                }
                try {
                    URL url = new URL(location);
                    String fileName = new File(url.getPath()).getName();
                    if (fileName.isEmpty()) fileName = "resource";
                    resources.put(fileName, bodyBytes);
                } catch (Exception e) {
                }
            }
            return resources;
        }

        public static String getMainHtml(String mhtContent) {
            Pattern htmlPartPattern = Pattern.compile("(?is)Content-Type: text/html.*?\r\n\r\n(.*?)(?=(--boundary|$))");
            Matcher htmlMatcher = htmlPartPattern.matcher(mhtContent);
            if (htmlMatcher.find()) {
                String fullPart = htmlMatcher.group(0);
                Pattern encodingPattern = Pattern.compile("(?im)Content-Transfer-Encoding: (.*?)\\r?\\n");
                Matcher encodingMatcher = encodingPattern.matcher(fullPart);
                String encoding = encodingMatcher.find() ? encodingMatcher.group(1).trim() : "7bit";
                String rawBody = htmlMatcher.group(1);
                if ("quoted-printable".equalsIgnoreCase(encoding)) {
                    return Utils.decodeQuotedPrintable(rawBody);
                } else if ("base64".equalsIgnoreCase(encoding)) {
                    String cleaned = rawBody.replaceAll("\\s+", "");
                    byte[] decoded = Base64.decode(cleaned, Base64.DEFAULT);
                    return new String(decoded, StandardCharsets.UTF_8);
                } else {
                    return rawBody;
                }
            }
            return "";
        }
    }

    private byte[] downloadResourceBytes(final String resourceUrl, final boolean usePcUa, final String ref) {
        HttpURLConnection conn = null;
        try {
            final URL url = new URL(resourceUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", usePcUa ? PC_USER_AGENT : WebSettings.getDefaultUserAgent(this));
            conn.setRequestProperty("Accept", ACCEPT_HEADER);
            conn.setRequestProperty("Accept-Language", ACCEPT_LANGUAGE);
            conn.setRequestProperty("Referer", ref);
            final int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return null;
            }
            try (final InputStream in = conn.getInputStream();
                 final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                final byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    if (getAvailableStorage() < MIN_STORAGE_THRESHOLD) {
                        return null;
                    }
                    baos.write(buffer, 0, bytesRead);
                }
                return baos.toByteArray();
            }
        } catch (final Exception e) {
            Log.w(TAG, "ダウンロードエラー: " + resourceUrl, e);
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
