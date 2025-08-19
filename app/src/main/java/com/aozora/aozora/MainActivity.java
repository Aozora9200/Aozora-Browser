package com.aozora.aozora;

import android.Manifest;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.AlertDialog;
import android.app.Instrumentation;
import android.app.ProgressDialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.support.annotation.WorkerThread;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.LruCache;
import android.os.PersistableBundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.PixelCopy;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.support.annotation.Nullable;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.HttpAuthHandler;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebViewDatabase;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import android.content.pm.PackageInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.*;
import java.io.File;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;


public class MainActivity extends Activity {

    private static final Pattern CACHE_MODE_PATTERN = Pattern.compile("(^|[/.])(?:(chatx2|chatx|chat|auth|login|disk|cgi|session|cloud))($|[/.])", Pattern.CASE_INSENSITIVE);
    private AutoCompleteTextView urlEditText;
    private WebView webView; // WebViewをクラス変数として定義
    private LinearLayout bottomBar, action_Bar, Bar;
    private TextView tabCountTextView;
    private DatabaseHelper dbHelper;
    private DBHelper dbH;
    private DBBM dbbm;
    private DBHistory dbHistory;
    private ImageButton backButton, forwardButton, bmbutton;
    private FrameLayout webViewContainer;
    private ProgressDialog progressDialog, progressResetDialog;
    private ProgressBar progressBar;
    private static final String PREF_NAME = "AdvancedBrowserPrefs";
    private static final String KEY_DARK_MODE = "dark_mode";
    private boolean darkModeEnabled = false;
    private static final String KEY_BASIC_AUTH = "basic_auth";
    private static final String APPEND_STR = " AozoraBrowser";
    private static final String KEY_ZOOM_ENABLED = "zoom_enabled";
    private static final String KEY_CT3UA_ENABLED = "ct3ua_enabled";
    private static final String KEY_JS_ENABLED = "js_enabled";
    private static final int FILE_CHOOSER_REQUEST_CODE = 1001;
    private static Method sSetSaveFormDataMethod;
    private static Method sSetDatabaseEnabledMethod;
    private static Method sSetAppCacheEnabledMethod;
    private static Method sSetAppCachePathMethod;

    private FrameLayout fullscreenContainer;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;

    private static final int REQUEST_CODE_IMPORT = 1001;

    private RecyclerView recyclerView;
    private SavedPageAdapter adapter;
    private List<SavedPage> pageList = new ArrayList<>();

    private ArrayList<WebView> tabs = new ArrayList<>();
    private ArrayList<TabInfo> tabInfos = new ArrayList<>();
    private int currentTabIndex = 0;
    private TabListAdapter tabListAdapter = null;
    private SharedPreferences prefs, pref;
    private int currentHistoryIndex = -1;
    private int totalMatches = 0;
    private int currentMatchIndex = 0;
    private final Map<WebView, Bitmap> webViewFavicons = new HashMap<>();
    private boolean uaEnabled = false;
    private boolean deskuaEnabled = false;
    private boolean ct3uaEnabled = false;
    private boolean jsEnabled = false;
    private boolean imgBlockEnabled = false;

    private WebView preloadedWebView = null;

    private static final String KEY_TABS = "saved_tabs";
    private ValueCallback<Uri[]> filePathCallback;
    private static final String KEY_CURRENT_TAB_ID = "current_tab_id";
    private static final String KEY_BOOKMARKS = "bookmarks";
    private static final String KEY_HISTORY = "history";
    private static final String KEY_UA_ENABLED = "ua_enabled";
    private static final String KEY_DESKUA_ENABLED = "deskua_enabled";
    private static final int REQUEST_CODE_IMPORT_BOOKMARKS = 1001;
    private static final String KEY_IMG_BLOCK_ENABLED = "img_block_enabled";
    private final Map<WebView, String> originalUserAgents = new HashMap<>();
    private int nextTabId = 0;
    private boolean isBackNavigation = false;
    private static final int MAX_HISTORY_SIZE = 100;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LruCache<String, Bitmap> faviconCache;
    private ImageView faviconImageView;
    private final ExecutorService backgroundExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors());
    private final ArrayList<WebView> webViews = new ArrayList<>();
    private View findInPageBarView;
    private EditText etFindQuery;
    private TextView tvFindCount;
    private Button btnFindPrev, btnFindNext, btnFindClose;

    private ArrayAdapter<String> suggestionAdapter;

    private final List<Bookmark> bookmarks = new ArrayList<>();
    private boolean basicAuthEnabled = false;
    private boolean zoomEnabled = false;
    private boolean defaultLoadsImagesAutomatically;
    private boolean defaultLoadsImagesAutomaticallyInitialized = false;
    private final List<HistoryItem> historyItems = new ArrayList<>();
    private AlertDialog dialog;
    // 選択されたURLとタイプを保持
    private String selectedUrl;
    private int selectedType;

    private static final int MENU_ID_SETTING = 5;
    private static final int MENU_ID_CLOSE = 6;
    private static final String GITHUB_API_URL =
            "https://api.github.com/repos/Aozora9200/Aozora-Browser/releases/latest";

    static {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            try {
                sSetSaveFormDataMethod = WebSettings.class.getMethod("setSaveFormData", boolean.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                sSetDatabaseEnabledMethod = WebSettings.class.getMethod("setDatabaseEnabled", boolean.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                sSetAppCacheEnabledMethod = WebSettings.class.getMethod("setAppCacheEnabled", boolean.class);
                sSetAppCachePathMethod = WebSettings.class.getMethod("setAppCachePath", String.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static class HistoryItem {
        private final String title;
        private final String url;
        public HistoryItem(String title, String url) {
            this.title = title;
            this.url = url;
        }
        public String getTitle() { return title; }
        public String getUrl() { return url; }
    }

    public static class Bookmark {
        private final String title;
        private final String url;
        public Bookmark(String title, String url) {
            this.title = title;
            this.url = url;
        }
        public String getTitle() { return title; }
        public String getUrl() { return url; }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        urlEditText = findViewById(R.id.urlEditText);
        backButton = findViewById(R.id.backButton);
        forwardButton = findViewById(R.id.forwardButton);
        webViewContainer = findViewById(R.id.webViewContainer);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        faviconImageView = (ImageView) findViewById(R.id.favicon);
        progressBar = findViewById(R.id.progressBar);
        tabCountTextView = (TextView) findViewById(R.id.tabCountTextView);
        // ImageButton tabButton = findViewById(R.id.action_tab);
        ImageButton reloadButton = findViewById(R.id.action_reload);
        ImageButton bmbutton = findViewById(R.id.action_bookmark);
        ImageButton popupbutton = findViewById(R.id.action_popup);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 123);
            }
        }

        dbH = new DBHelper(this);
        dbbm = new DBBM(this);
        dbHistory = new DBHistory(this);

        // ProgressResetDialog の初期設定
        progressResetDialog = new ProgressDialog(this);
        progressResetDialog.setMessage("Erasing...");
        progressResetDialog.setCancelable(false);
        prefs = getSharedPreferences("WebViewTabs", MODE_PRIVATE);
        pref = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        darkModeEnabled = pref.getBoolean(KEY_DARK_MODE, false);
        basicAuthEnabled = pref.getBoolean(KEY_BASIC_AUTH, false);
        zoomEnabled = pref.getBoolean(KEY_ZOOM_ENABLED, false);
        jsEnabled = pref.getBoolean(KEY_JS_ENABLED, false);
        imgBlockEnabled = pref.getBoolean(KEY_IMG_BLOCK_ENABLED, false);
        uaEnabled = pref.getBoolean(KEY_UA_ENABLED, false);
        deskuaEnabled = pref.getBoolean(KEY_DESKUA_ENABLED, false);
        ct3uaEnabled = pref.getBoolean(KEY_CT3UA_ENABLED, false);

        prefs = getSharedPreferences("WebViewTabs", MODE_PRIVATE);

        fullscreenContainer = findViewById(R.id.fullscreenContainer);
        bottomBar = findViewById(R.id.bottomBar);
        action_Bar = findViewById(R.id.action_Bar);
        webView = findViewById(R.id.webView);

        dbHelper = new DatabaseHelper(this);

        webView.setWebChromeClient(new MyWebChromeClient());

        loadAdHostsFromAssets();

        backButton.setOnClickListener(v -> goBack());
        forwardButton.setOnClickListener(v -> goForward());

        // インテントで渡された URL を取得
        String url = getIntent().getStringExtra("url");

        // ボタンにクリックイベントを設定
        //tabButton.setOnClickListener(v ->
        //        showTabMenu()
        //);

        tabCountTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTabMenu();
            }
        });

        reloadButton.setOnClickListener(v ->
                onRefresh()
        );

        bmbutton.setOnClickListener(v ->
                new AlertDialog.Builder(MainActivity.this)
                        .setPositiveButton("ページを保存", (dialog, which) -> {
                            savePage();
                        })
                        .setNegativeButton("ブックマーク", (dialog, which) -> {
                            savebm();
                        })
                        .show()
        );

        popupbutton.setOnClickListener(v ->
                showBottomMenu(v)
        );

        // URL入力でエンターを押したら現在のタブでページを開く
        urlEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                loadUrlInCurrentTab(urlEditText.getText().toString());
                closeKeyboard();
                urlEditText.clearFocus();
                return true;
            }
            return false;
        });

        urlEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    urlEditText.selectAll();
                }
                faviconImageView.setVisibility(hasFocus ? View.GONE : View.VISIBLE);
            }
        });

        urlEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                urlEditText.selectAll();
            }
        });

        int maxMemory = (int)(Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;
        faviconCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };

        swipeRefreshLayout.setOnChildScrollUpCallback(new SwipeRefreshLayout.OnChildScrollUpCallback() {
            @Override
            public boolean canChildScrollUp(SwipeRefreshLayout parent, @Nullable View child) {
                WebView current = getCurrentWebView();
                return (current != null && current.getScrollY() > 0);
            }
        });
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                WebView current = getCurrentWebView();
                if (current != null) current.reload();
            }
        });

        ArrayAdapter<String> suggestionAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line);
        urlEditText.setAdapter(suggestionAdapter);
        urlEditText.setThreshold(1); // 1文字以上でサジェスト開始

        urlEditText.addTextChangedListener(new TextWatcher() {
            private Timer timer = new Timer();
            private final long DELAY = 100; // 入力後に待つミリ秒

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(final Editable s) {
                timer.cancel();
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        fetchSuggestions(s.toString(), suggestionAdapter);
                    }
                }, DELAY);
            }
        });

        urlEditText.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedSuggestion = (String) parent.getItemAtPosition(position);
                String searchUrl = "https://www.google.com/search?q=" + Uri.encode(selectedSuggestion);

                // WebViewに読み込ませる
                load(searchUrl);

                // 入力欄を更新
                urlEditText.setText(selectedSuggestion);
                urlEditText.clearFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(urlEditText.getWindowToken(), 0);
                }
            }
        });

        boolean isFirstLaunch = pref.getBoolean("isFirstLaunch", true);

        // タブを復元
        loadTabsState();
        if (tabs.isEmpty()) {
            if (isFirstLaunch) {
                addNewTab("file:///android_asset/index.html");

                // 初回起動済みを保存
                SharedPreferences.Editor editor = pref.edit();
                editor.putBoolean("isFirstLaunch", false);
                editor.apply();
            } else {
                addNewTab("file:///android_asset/index.html");
            }
        }

        updateTabCount();
        new CheckUpdateTask().execute();

        preInitializeWebView();
        if (!defaultLoadsImagesAutomaticallyInitialized && !webViews.isEmpty()) {
            defaultLoadsImagesAutomatically = webViews.get(0).getSettings().getLoadsImagesAutomatically();
            defaultLoadsImagesAutomaticallyInitialized = true;
        }

        loadBookmarks();
        loadHistory();
        if (!historyItems.isEmpty()) {
            currentHistoryIndex = historyItems.size() - 1;
        }
        initializePersistentFavicons();
        switchToTab(currentTabIndex);
        Intent intent = getIntent();
        Uri uri = intent.getData();

        if (uri != null) {
            if ("file".equals(uri.getScheme()) || "content".equals(uri.getScheme())) {
                addNewTab(uri.toString()); // WebViewが自動で読み込み
            } else {
                Toast.makeText(this, "対応していない URI 形式です。", Toast.LENGTH_SHORT).show();
            }
        }
        if (url != null && !url.isEmpty()) {
            // URL が渡された場合は WebView で開く
            load(url);
        } else {
            return;
        }

    }

    private class CheckUpdateTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            try {
                URL url = new URL(GITHUB_API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                InputStream in = conn.getInputStream();
                Scanner scanner = new Scanner(in).useDelimiter("\\A");
                String result = scanner.hasNext() ? scanner.next() : "";
                conn.disconnect();

                JSONObject json = new JSONObject(result);
                return json.getString("tag_name"); // バージョン
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(String latestVersion) {
            if (latestVersion == null) return;

            try {
                PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                String currentVersion = pInfo.versionName;

                if (!currentVersion.equals(latestVersion)) {
                    showUpdateDialog(latestVersion);
                }
            } catch (Exception ignored) {}
        }
    }

    private void showUpdateDialog(final String latestVersion) {
        new AlertDialog.Builder(this)
                .setTitle("アップデートがあります")
                .setMessage("最新バージョン (" + latestVersion + ") が利用可能です。更新しますか？")
                .setPositiveButton("更新する", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // GitHubリリースページへ飛ばす
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/Aozora9200/Aozora-Browser/releases/latest"));
                        startActivity(browserIntent);
                    }
                })
                .setNegativeButton("後で", null)
                .show();
    }

    private void fetchSuggestions(String query, ArrayAdapter<String> adapter) {
        if (query.isEmpty()) return;

        String urlStr = "https://suggestqueries.google.com/complete/search?client=firefox&q=" + Uri.encode(query);

        new Thread(() -> {
            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                JSONArray jsonArray = new JSONArray(sb.toString());
                JSONArray suggestions = jsonArray.getJSONArray(1);
                List<String> suggestionList = new ArrayList<>();

                for (int i = 0; i < suggestions.length(); i++) {
                    suggestionList.add(suggestions.getString(i));
                }

                runOnUiThread(() -> {
                    adapter.clear();
                    adapter.addAll(suggestionList);
                    adapter.notifyDataSetChanged();
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void updateTabCount() {
        if (tabCountTextView != null) {
            tabCountTextView.setText(String.valueOf(tabs.size()));
        }
    }

    // カスタム WebChromeClient
    private class MyWebChromeClient extends WebChromeClient {
        private View mVideoProgressView;

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            // 既にフルスクリーンなら戻す
            if (customView != null) {
                callback.onCustomViewHidden();
                return;
            }

            // 隠すべきビューを隠す
            webView.setVisibility(View.GONE);
            bottomBar.setVisibility(View.GONE);
            action_Bar.setVisibility(View.GONE);

            // フルスクリーン表示
            fullscreenContainer.setVisibility(View.VISIBLE);
            fullscreenContainer.addView(view);
            customView = view;
            customViewCallback = callback;


            // ステータスバーなどを隠す
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }

        @Override
        public void onHideCustomView() {
            if (customView == null) {
                return;
            }

            // フルスクリーンビューを削除
            fullscreenContainer.removeView(customView);
            customView = null;
            fullscreenContainer.setVisibility(View.GONE);
            customViewCallback.onCustomViewHidden();

            // 元のビューを戻す
            webView.setVisibility(View.VISIBLE);
            bottomBar.setVisibility(View.VISIBLE);
            action_Bar.setVisibility(View.VISIBLE);

            // ステータスバー復帰
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String url = intent.getStringExtra("url");
        if (url != null && !url.isEmpty()) {
            load(url);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        WebView webView = tabs.get(currentTabIndex);
        // 戻ってきたときに実行したい処理
        String url = webView.getUrl();
        webView.onResume();

        ImageButton bmbutton = findViewById(R.id.action_bookmark);

        // SQLiteOpenHelper 例: dbbm
        SQLiteDatabase db = dbbm.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM pages WHERE url = ?", new String[]{url});

        boolean isBookmarked = false;
        if (cursor.moveToFirst()) {
            isBookmarked = cursor.getInt(0) > 0;
        }
        cursor.close();

        if (isBookmarked) {
            bmbutton.setImageResource(R.drawable.bookmark_star);
        } else {
            bmbutton.setImageResource(R.drawable.bookmark_black);
        } // 独自のリフレッシュ処理を呼ぶ
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        if (selectedUrl == null) return;

        // リンクを長押しした場合のメニュー
        if (selectedType == WebView.HitTestResult.SRC_ANCHOR_TYPE || selectedType == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
            menu.setHeaderTitle("リンクメニュー");
            menu.add(0, 1, 0, "リンクをコピー");
            menu.add(0, 2, 0, "リンク先をダウンロード");
            menu.add(0, 3, 0, "リンク先を新しいタブで開く");
        }

        // 画像を長押しした場合のメニュー
        if (selectedType == WebView.HitTestResult.IMAGE_TYPE || selectedType == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
            menu.add(0, 4, 0, "画像を保存");
        }

    }

    // メニュー選択時の処理
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (selectedUrl == null) return false;

        switch (item.getItemId()) {
            case 1: // リンクをコピー
                copyLink(selectedUrl);
                return true;
            case 2: // リンク先をダウンロード
                downloadLink(selectedUrl);
                return true;
            case 3: // 新しいタブで開く
                addNewTab(selectedUrl);
                return true;
            case 4: // 画像を保存
                downloadImage(selectedUrl);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem darkModeItem = menu.findItem(R.id.action_dark_mode);
        darkModeItem.setVisible(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q);
        darkModeItem.setCheckable(true);
        darkModeItem.setChecked(darkModeEnabled);
        MenuItem uaItem = menu.findItem(R.id.action_ua);
        if (uaItem != null) uaItem.setChecked(uaEnabled);
        MenuItem deskuaItem = menu.findItem(R.id.action_deskua);
        if (deskuaItem != null) deskuaItem.setChecked(deskuaEnabled);
        MenuItem ct3uaItem = menu.findItem(R.id.action_ct3ua);
        if (ct3uaItem != null) ct3uaItem.setChecked(ct3uaEnabled);
        MenuItem zoomItem = menu.findItem(R.id.action_zoom_toggle);
        if (zoomItem != null) zoomItem.setChecked(zoomEnabled);
        //MenuItem jsItem = menu.findItem(R.id.action_js);
        //if (jsItem != null) jsItem.setChecked(jsEnabled);
        MenuItem imgItem = menu.findItem(R.id.action_img);
        if (imgItem != null) imgItem.setChecked(imgBlockEnabled);
        MenuItem basicAuthItem = menu.findItem(R.id.action_basic_auth);
        if (basicAuthItem != null) basicAuthItem.setChecked(basicAuthEnabled);
        return super.onPrepareOptionsMenu(menu);
       }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_tabs) {
            showTabMenu();
            return true;
        } else if (itemId == R.id.menu_downloads) {
            Intent intent = new Intent(this, DownloadListActivity.class);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.action_basic_auth) {
            if (!basicAuthEnabled) {
                basicAuthEnabled = true;
                item.setChecked(true);
                Toast.makeText(MainActivity.this, "Basic認証 ON", Toast.LENGTH_SHORT).show();
            } else {
                basicAuthEnabled = false;
                item.setChecked(false);
                clearBasicAuthCacheAndReload();
                Toast.makeText(MainActivity.this, "Basic認証 OFF", Toast.LENGTH_SHORT).show();
            }
            pref.edit().putBoolean(KEY_BASIC_AUTH, basicAuthEnabled).apply();
        } else if (itemId == R.id.action_ior1orl) {
            Intent intent = new Intent(this, Ior1orl.class);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.action_screenshot) {
            takeScreenshot();
        } else if (itemId == R.id.action_txtphoto) {
            startActivity(new Intent(MainActivity.this, txtphoto.class));
        } else if (itemId == R.id.action_asciiart) {
            startActivity(new Intent(MainActivity.this, asciiart.class));
        } else if (itemId == R.id.action_pgdl) {
            startActivity(new Intent(MainActivity.this, pagedl.class));
        } else if (itemId == R.id.action_num) {
            startActivity(new Intent(MainActivity.this, num.class));
        } else if (itemId == R.id.action_qr) {
            startActivity(new Intent(MainActivity.this, QrCodeActivity.class));
        } else if (itemId == R.id.action_exec) {
            startActivity(new Intent(MainActivity.this, exec.class));
        } else if (itemId == R.id.action_grep) {
            startActivity(new Intent(MainActivity.this, grepmd5appActivity.class));
        } else if (itemId == R.id.action_htmlview) {
            startActivity(new Intent(MainActivity.this, htmlview.class));
        } else if (itemId == R.id.action_notepad) {
            startActivity(new Intent(MainActivity.this, notepad.class));
        } else if (itemId == R.id.action_qrcamera) {
            startActivity(new Intent(MainActivity.this, QrCameraActivity.class));
        } else if (itemId == R.id.action_File) {
            startActivity(new Intent(MainActivity.this, FileManager.class));
        } else if (itemId == R.id.action_dark_mode) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                darkModeEnabled = !darkModeEnabled;
                item.setChecked(darkModeEnabled);
                updateDarkMode();
                pref.edit().putBoolean(KEY_DARK_MODE, darkModeEnabled).apply();
                Toast.makeText(this, "ダークモード " + (darkModeEnabled ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "この機能はAndroid 10以上で利用可能です", Toast.LENGTH_SHORT).show();
            }
        } else if (itemId == R.id.action_zoom_toggle) {
            if (item.isChecked()) {
                disableZoom();
                zoomEnabled = false;
                Toast.makeText(MainActivity.this, "ズームを無効にしました.", Toast.LENGTH_SHORT).show();
            } else {
                enableZoom();
                zoomEnabled = true;
                Toast.makeText(MainActivity.this, "ズームを有効にしました.", Toast.LENGTH_SHORT).show();
            }
            item.setChecked(zoomEnabled);
            pref.edit().putBoolean(KEY_ZOOM_ENABLED, zoomEnabled).apply();
        } else if (itemId == R.id.action_negapoji) {
            applyNegapoji();
        } else if (itemId == R.id.action_deskua) {
            if (!deskuaEnabled) {
                if (uaEnabled) {
                    disableUA();
                    uaEnabled = false;
                    pref.edit().putBoolean(KEY_UA_ENABLED, false).apply();
                }
                if (ct3uaEnabled) {
                    disableCT3UA();
                    ct3uaEnabled = false;
                    pref.edit().putBoolean(KEY_CT3UA_ENABLED, false).apply();
                }
                enabledeskUA();
                deskuaEnabled = true;
            } else {
                disabledeskUA();
                deskuaEnabled = false;
            }
            item.setChecked(deskuaEnabled);
            pref.edit().putBoolean(KEY_DESKUA_ENABLED, deskuaEnabled).apply();
            invalidateOptionsMenu();
        } else if (itemId == R.id.action_ct3ua) {
            if (!ct3uaEnabled) {
                if (uaEnabled) {
                    disableUA();
                    uaEnabled = false;
                    pref.edit().putBoolean(KEY_UA_ENABLED, false).apply();
                }
                if (deskuaEnabled) {
                    disabledeskUA();
                    deskuaEnabled = false;
                    pref.edit().putBoolean(KEY_DESKUA_ENABLED, false).apply();
                }
                enableCT3UA();
                ct3uaEnabled = true;
            } else {
                disableCT3UA();
                ct3uaEnabled = false;
            }
            item.setChecked(ct3uaEnabled);
            pref.edit().putBoolean(KEY_CT3UA_ENABLED, ct3uaEnabled).apply();
            invalidateOptionsMenu();
        } else if (itemId == R.id.action_translate) {
            translatePageToJapanese();
            return true;
        //} else if (itemId == R.id.action_js) {
        //    if (item.isChecked()) {
        //        disablejs();
        //        jsEnabled = false;
        //        Toast.makeText(MainActivity.this, "JavaScript無効", Toast.LENGTH_SHORT).show();
        //    } else {
        //        enablejs();
        //        jsEnabled = true;
        //        Toast.makeText(MainActivity.this, "JavaScript有効", Toast.LENGTH_SHORT).show();
        //    }
        //    item.setChecked(jsEnabled);
        //    pref.edit().putBoolean(KEY_JS_ENABLED, jsEnabled).apply();
        } else if (itemId == R.id.action_img) {
            if (item.isChecked()) {
                disableimgunlock();
                imgBlockEnabled = false;
                Toast.makeText(MainActivity.this, "画像ブロック無効", Toast.LENGTH_SHORT).show();
            } else {
                enableimgblock();
                imgBlockEnabled = true;
                Toast.makeText(MainActivity.this, "画像ブロック有効", Toast.LENGTH_SHORT).show();
            }
            item.setChecked(imgBlockEnabled);
            pref.edit().putBoolean(KEY_IMG_BLOCK_ENABLED, imgBlockEnabled).apply();
        } else if (itemId == R.id.action_ua) {
            if (!uaEnabled) {
                if (deskuaEnabled) {
                    disabledeskUA();
                    deskuaEnabled = false;
                    pref.edit().putBoolean(KEY_DESKUA_ENABLED, false).apply();
                }
                if (ct3uaEnabled) {
                    disableCT3UA();
                    ct3uaEnabled = false;
                    pref.edit().putBoolean(KEY_CT3UA_ENABLED, false).apply();
                }
                enableUA();
                uaEnabled = true;
            } else {
                disableUA();
                uaEnabled = false;
            }
            item.setChecked(uaEnabled);
            pref.edit().putBoolean(KEY_UA_ENABLED, uaEnabled).apply();
            invalidateOptionsMenu();
        } else if (itemId == R.id.action_settings) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        } else if (itemId == R.id.action_Dhistory) {
            Intent intent = new Intent(MainActivity.this, DownloadListActivity.class);
            intent.putExtra("clear_history", true);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.action_Delhistory) {
            historyLastCheck();
        }
        return super.onOptionsItemSelected(item);
    }

    public void onRefresh() {
        WebView webView = getCurrentWebView();
        if (webView != null) webView.reload();
    }

    private void showBottomMenu(View anchor) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_menu, null);

        closeOptionsMenu();

        final PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        popupWindow.setBackgroundDrawable(new ColorDrawable());
        popupWindow.setOutsideTouchable(true);
        popupWindow.setAnimationStyle(R.style.PopupAnimation);
        popupWindow.showAtLocation(anchor, Gravity.BOTTOM, 0, 0);

        // ボタンにIDでアクセス
        LinearLayout button1 = popupView.findViewById(R.id.bookmark);
        LinearLayout button2 = popupView.findViewById(R.id.history);
        LinearLayout button3 = popupView.findViewById(R.id.toppage);
        LinearLayout button4 = popupView.findViewById(R.id.savedpage);
        LinearLayout button5 = popupView.findViewById(R.id.download);
        LinearLayout button6 = popupView.findViewById(R.id.settings);
        LinearLayout button7 = popupView.findViewById(R.id.search_page);
        LinearLayout button8 = popupView.findViewById(R.id.secret);
        LinearLayout button9 = popupView.findViewById(R.id.share);
        LinearLayout button10 = popupView.findViewById(R.id.help);

        registerForContextMenu(button6);

        // ボタン1の処理
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, BmHisActivity.class));
                popupWindow.dismiss();
            }
        });

        // ボタン2の処理
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHistoryDialog();
                popupWindow.dismiss();
            }
        });

        // ボタン3の処理
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                load("file:///android_asset/index.html");
                popupWindow.dismiss();
            }
        });

        // ボタン4の処理
        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, HisBMActivity.class));
                popupWindow.dismiss();
            }
        });

        // ボタン5の処理
        button5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, DownloadListActivity.class));
                popupWindow.dismiss();
            }
        });

        // ボタン6の処理
        button6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
                // showCustomPopupMenu(v);
                invalidateOptionsMenu();

                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        openOptionsMenu();
                    }
                }, 100); // 100ms 遅延で十分なことが多い
            }
        });

        // ボタン7の処理
        button7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFindInPageBar();
                popupWindow.dismiss();
            }
        });

        // ボタン8の処理
        button8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SecretActivity.class));
                Toast.makeText(getApplicationContext(), "シークレットモード", Toast.LENGTH_SHORT).show();
                popupWindow.dismiss();
            }
        });

        // ボタン9の処理
        button9.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareCurrentUrl();
                popupWindow.dismiss();
            }
        });

        // ボタン10の処理
        button10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                load("file:///android_asset/help.html");
                popupWindow.dismiss();
            }
        });
    }

    private void showCustomPopupMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_main, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            return onOptionsItemSelected(item);
        });

        popup.show();
    }

    private void shareCurrentUrl() {
        String currentUrl = webView.getUrl();
        if (currentUrl != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, currentUrl);
            startActivity(Intent.createChooser(shareIntent, "共有"));
        }
    }

    public void openHistoryActivity() {
        DBHistory dbHistory = new DBHistory(this);
        HashMap<String, List<String>> historyMap = dbHistory.getHistoryByCategory();

        // 🔹 履歴データがすべて空かチェック
        boolean hasHistory = false;
        for (List<String> list : historyMap.values()) {
            if (!list.isEmpty()) {
                hasHistory = true;
                break;
            }
        }

        if (hasHistory) {
            Intent intent = new Intent(this, HistoryActivity.class);
            startActivity(intent);
        } else {
            Toast.makeText(this, "履歴がありません。", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressWarnings("deprecation")
    private void clearWebStorage() {
        WebStorage.getInstance().deleteAllData();
    }

    private void clearPageCache() {
        for (WebView wv : webViews) {
            wv.clearCache(true);
        }
    }

    private void resetLastCheck() {
        new AlertDialog.Builder(this)
                .setTitle("本当によろしいですか？")
                .setMessage("この操作を行うと、アプリケーションの全データが削除されます:")
                .setPositiveButton("アプリケーションをリセット", (dialog, which) -> reset())
                .setNegativeButton("キャンセル", null)
                .show();
    }

    private void reset() {
        progressResetDialog.show();
        if (webView != null) webView.clearHistory();
        historyItems.clear();
        bookmarks.clear();
        saveHistory();
        clearWebStorage();
        clearPageCache();
        closeAllTabs();
        WebViewDatabase.getInstance(MainActivity.this).clearFormData();
        CookieManager cm = CookieManager.getInstance();
        cm.removeAllCookie(); // API 19 では removeAllCookie を使用
        CookieSyncManager.getInstance().sync(); // flush の代わりに CookieSyncManager を使用
        urlEditText.setInputType(EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_URI | EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        urlEditText.setRawInputType(EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_URI | EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        urlEditText.setPrivateImeOptions("nm");
        String currentText = urlEditText.getText().toString();
        urlEditText.setText("");
        urlEditText.setText(currentText);
        progressResetDialog.dismiss();
        Toast.makeText(MainActivity.this, "履歴、フォームデータ、ブックマーク、検索候補、及びタブとCookieを消去しました", Toast.LENGTH_SHORT).show();
    }

    private void translatePageToJapanese() {
        String currentUrl = getCurrentWebView().getUrl();
        if (currentUrl == null || currentUrl.isEmpty()) {
            Toast.makeText(MainActivity.this, "翻訳するページが見つかりません", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String encoded = URLEncoder.encode(currentUrl, "UTF-8");
            String translateUrl = "https://translate.google.com/translate?hl=ja&sl=auto&tl=ja&u=" + encoded;
            getCurrentWebView().loadUrl(translateUrl);
        } catch (UnsupportedEncodingException e) {
            Toast.makeText(MainActivity.this, "翻訳中にエラーが発生しました", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void applyNegapoji() {
        String js = "javascript:(function(){" +
                "document.documentElement.style.filter='invert(1)';" +
                "var imgs = document.getElementsByTagName('img');" +
                "for(var i=0;i<imgs.length;i++){ imgs[i].style.filter='invert(1)'; }" +
                "})()";
        webView.evaluateJavascript(js, null);
    }

    private void downloadLink(String url) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, Uri.parse(url).getLastPathSegment());

        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        manager.enqueue(request);
        Toast.makeText(this, "ダウンロードを開始します...", Toast.LENGTH_SHORT).show();
    }

    private void downloadImage(String imageUrl) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(imageUrl));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, Uri.parse(imageUrl).getLastPathSegment());

        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        manager.enqueue(request);
        Toast.makeText(this, "画像を保存しました", Toast.LENGTH_SHORT).show();
    }

    // ページ内検索バーの表示
    private void showFindInPageBar() {
        if (findInPageBarView == null) {
            LayoutInflater inflater = LayoutInflater.from(this);
            findInPageBarView = inflater.inflate(R.layout.find_in_page_bar, null);
            etFindQuery = findInPageBarView.findViewById(R.id.etFindQuery);
            tvFindCount = findInPageBarView.findViewById(R.id.tvFindCount);
            btnFindPrev = findInPageBarView.findViewById(R.id.btnFindPrev);
            btnFindNext = findInPageBarView.findViewById(R.id.btnFindNext);
            btnFindClose = findInPageBarView.findViewById(R.id.btnFindClose);

            etFindQuery.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
            etFindQuery.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        performFindInPage();
                        return true;
                    }
                    return false;
                }
            });
            etFindQuery.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                        performFindInPage();
                        return true;
                    }
                    return false;
                }
            });

            btnFindNext.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (totalMatches > 0) {
                        webView.findNext(true);
                    }
                }
            });
            btnFindPrev.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (totalMatches > 0) {
                        webView.findNext(false);
                    }
                }
            });
            btnFindClose.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    etFindQuery.setText("");
                    hideFindInPageBar();
                }
            });

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP);
            addContentView(findInPageBarView, params);
        }
        findInPageBarView.setVisibility(View.VISIBLE);
        etFindQuery.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(etFindQuery, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void performFindInPage() {
        String query = etFindQuery.getText().toString().trim();
        if (query.isEmpty()) {
            webView.clearMatches();
            tvFindCount.setText("0/0");
            totalMatches = 0;
            return;
        }

        webView.clearMatches();
        currentMatchIndex = 0;
        webView.findAllAsync(query);
        webView.setFindListener(new WebView.FindListener() {
            @Override
            public void onFindResultReceived(int activeMatchOrdinal, int numberOfMatches, boolean isDoneCounting) {
                currentMatchIndex = activeMatchOrdinal;
                totalMatches = numberOfMatches;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (totalMatches > 0) {

                            tvFindCount.setText((activeMatchOrdinal + 1) + "/" + totalMatches);
                        } else {
                            tvFindCount.setText("0/0");
                        }
                    }
                });
            }
        });
    }
    private void hideFindInPageBar() {
        if (findInPageBarView != null) {
            findInPageBarView.setVisibility(View.GONE);
            webView.clearMatches();
            if (tvFindCount != null) {
                tvFindCount.setText("0/0");
            }
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(etFindQuery.getWindowToken(), 0);
            }
        }
    }

    private void clearBasicAuthCacheAndReload() {
        WebView current = getCurrentWebView();
        if (current != null) {
            current.clearCache(true);
            current.reload();
            reloadCurrentPage();
        }
    }

    private void reloadCurrentPage() {
        WebView current = getCurrentWebView();
        if (current != null) {
            current.clearCache(true);
            String url = current.getUrl();
            if (url != null && !url.isEmpty()) {
                current.loadUrl(url);
            }
        }
    }

    private void takeScreenshot() {
        View root = getWindow().getDecorView().getRootView();
        int w = root.getWidth();
        int h = root.getHeight();
        if (w <= 0 || h <= 0) {
            Toast.makeText(MainActivity.this, "スクリーンショット取得エラー: ビューサイズが無効", Toast.LENGTH_SHORT).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Handler handler = new Handler(Looper.getMainLooper());
            PixelCopy.request(getWindow(), bmp, new PixelCopy.OnPixelCopyFinishedListener() {
                @Override
                public void onPixelCopyFinished(int copyResult) {
                    if (copyResult == PixelCopy.SUCCESS) {
                        saveScreenshot(bmp);
                    } else {
                        Toast.makeText(MainActivity.this, "スクリーンショットの取得に失敗しました", Toast.LENGTH_SHORT).show();
                    }
                }
            }, handler);
        } else {
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            root.draw(canvas);
            saveScreenshot(bmp);
        }
    }

    private void saveScreenshot(Bitmap bmp) {
        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    File dir = new File(Environment.getExternalStorageDirectory(), "DCIM/AozoraBrowser/Screenshot");
                    if (!dir.exists()) dir.mkdirs();
                    String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                    String name = ts + ".png";
                    File file = new File(dir, name);
                    FileOutputStream fos = new FileOutputStream(file);
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.flush();
                    fos.close();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "スクリーンショットを保存しました: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "スクリーンショット保存中にエラー: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }

    private void updateDarkMode() {
        for (WebView wv : webViews) {
            WebSettings s = wv.getSettings();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                s.setForceDark(darkModeEnabled ? WebSettings.FORCE_DARK_ON : WebSettings.FORCE_DARK_OFF);
            }
            if (wv == getCurrentWebView()) {
                wv.reload();
            }
        }
    }

    private void enableCT3UA() {
        WebSettings s = getCurrentWebView().getSettings();
        s.setUserAgentString("Mozilla/5.0 (Linux; Android 7.0; TAB-A03-BR3 Build/02.05.000; wv) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/83.0.4103.106 Safari/537.36");
        Toast.makeText(MainActivity.this, "CT3UA適用", Toast.LENGTH_SHORT).show();
        reloadCurrentPage();
    }

    private void disableCT3UA() {
        WebSettings s = getCurrentWebView().getSettings();
        String orig = originalUserAgents.get(getCurrentWebView());
        if (orig != null) s.setUserAgentString(orig + APPEND_STR);
        else s.setUserAgentString(APPEND_STR.trim());
        Toast.makeText(MainActivity.this, "CT3UA解除", Toast.LENGTH_SHORT).show();
        reloadCurrentPage();
    }

    private void enabledeskUA() {
        WebSettings s = getCurrentWebView().getSettings();
        String orig = originalUserAgents.get(getCurrentWebView());
        if (orig == null) orig = s.getUserAgentString();
        String desktop = orig.replace("Mobile", "").replace("Android", "");
        s.setUserAgentString(desktop + APPEND_STR);
        Toast.makeText(MainActivity.this, "デスクトップ表示有効", Toast.LENGTH_SHORT).show();
        reloadCurrentPage();
    }

    private void disabledeskUA() {
        WebSettings s = getCurrentWebView().getSettings();
        String orig = originalUserAgents.get(getCurrentWebView());
        if (orig != null) s.setUserAgentString(orig + APPEND_STR);
        else s.setUserAgentString(APPEND_STR.trim());
        Toast.makeText(MainActivity.this, "デスクトップ表示解除", Toast.LENGTH_SHORT).show();
        reloadCurrentPage();
    }

    private void enableUA() {
        WebSettings s = getCurrentWebView().getSettings();
        s.setUserAgentString("DoCoMo/2.0 SH902i(c100;TB)");
        Toast.makeText(MainActivity.this, "UA適用", Toast.LENGTH_SHORT).show();
        reloadCurrentPage();
    }

    private void disableUA() {
        WebSettings s = getCurrentWebView().getSettings();
        String orig = originalUserAgents.get(getCurrentWebView());
        if (orig != null) s.setUserAgentString(orig + APPEND_STR);
        else s.setUserAgentString(APPEND_STR.trim());
        Toast.makeText(MainActivity.this, "UA解除", Toast.LENGTH_SHORT).show();
        reloadCurrentPage();
    }

    private void enableZoom() {
        WebSettings s = getCurrentWebView().getSettings();
        s.setBuiltInZoomControls(true);
        s.setSupportZoom(true);
        reloadCurrentPage();
    }
    private void disableZoom() {
        WebSettings s = getCurrentWebView().getSettings();
        s.setBuiltInZoomControls(false);
        s.setSupportZoom(false);
        reloadCurrentPage();
    }
    private void enablejs() {
        WebSettings s = getCurrentWebView().getSettings();
        s.setJavaScriptEnabled(true);
        reloadCurrentPage();
    }
    private void disablejs() {
        WebSettings s = getCurrentWebView().getSettings();
        s.setJavaScriptEnabled(false);
        reloadCurrentPage();
    }
    private void enableimgblock() {
        WebSettings s = getCurrentWebView().getSettings();
        s.setLoadsImagesAutomatically(false);
        reloadCurrentPage();
    }
    private void disableimgunlock() {
        WebSettings s = getCurrentWebView().getSettings();
        s.setLoadsImagesAutomatically(true);
        reloadCurrentPage();
    }

    public void load(String url) {
        tabs.get(currentTabIndex).loadUrl(url);
        tabInfos.get(currentTabIndex).setUrl(url);
        saveTabsState();
    }

    private void applyOptimizedSettings(WebSettings settings) {
        settings.setJavaScriptEnabled(true);
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        settings.setDomStorageEnabled(true);
        settings.setGeolocationEnabled(false);
        settings.setTextZoom(100);
        settings.setDisplayZoomControls(false);
        settings.setBuiltInZoomControls(false);
        settings.setSupportZoom(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setOffscreenPreRaster(true);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            settings.setForceDark(darkModeEnabled ? WebSettings.FORCE_DARK_ON : WebSettings.FORCE_DARK_OFF);
        }
    }

    private void preInitializeWebView() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                WebView wv = new WebView(MainActivity.this);
                WebSettings s = wv.getSettings();
                applyOptimizedSettings(s);
                String defaultUA = s.getUserAgentString();
                s.setUserAgentString(defaultUA + APPEND_STR);
                preloadedWebView = wv;
            }
        });
    }

    private class AndroidBridge {
        @JavascriptInterface
        public void onUrlChange(final String url) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (url.startsWith("https://m.youtube.com/watch") ||
                            url.startsWith("https://chatgpt.com/") ||
                            url.startsWith("https://m.youtube.com/shorts/")) {
                        swipeRefreshLayout.setEnabled(false);
                        urlEditText.setText(url);
                    } else {
                        swipeRefreshLayout.setEnabled(true);
                    }
                }
            });
        }
    }

    private void onProgressChanged(int percentage){
        Animator animation = ObjectAnimator.ofInt(progressBar,"progress",percentage);
        animation.setDuration(500); // 0.5秒間でアニメーションする
        animation.setInterpolator(new DecelerateInterpolator());
        animation.start();
    }

    private void hideProgressBarDelayed() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (progressBar != null) {
                progressBar.setProgress(0);
            }
        }, 1000); // 1000ミリ秒 (1秒) 後に実行
    }

    private WebViewClient createWebViewClient(final int index) { // index を追加
        return new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                webView = view; // WebViewを保存
                urlEditText.setText(url);
                updateUrlBar(view);
                ImageButton bmbutton = findViewById(R.id.action_bookmark);
                // JavaScript を使用して Favicon を取得
                view.evaluateJavascript("(function() { " +
                        "var link = document.querySelector('link[rel~=\"icon\"]');" +
                        "return link ? link.href : ''; " +
                        "})()", new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        value = value.replace("\"", ""); // 取得した URL の " を削除
                        if (!value.isEmpty()) {
                            new DownloadFaviconTask().execute(value);
                        } else {
                            faviconImageView.setImageResource(R.drawable.transparent_vector); // デフォルトアイコン
                        }
                    }
                });
                String lower = url.toLowerCase();
                boolean matched = CACHE_MODE_PATTERN.matcher(lower).find();
                if (matched) {
                    view.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
                } else {
                    view.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
                }
                progressBar.setVisibility(View.VISIBLE);
                onProgressChanged(60);
                super.onPageStarted(view, url, favicon);
                // SQLiteOpenHelper 例: dbbm
                SQLiteDatabase db = dbbm.getReadableDatabase();
                Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM pages WHERE url = ?", new String[]{url});

                boolean isBookmarked = false;
                if (cursor.moveToFirst()) {
                    isBookmarked = cursor.getInt(0) > 0;
                }
                cursor.close();

                if (isBookmarked) {
                    bmbutton.setImageResource(R.drawable.bookmark_star);
                } else {
                    bmbutton.setImageResource(R.drawable.bookmark_black);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                applyCombinedOptimizations(view);
                if (url.startsWith("https://m.youtube.com")) {
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            injectLazyLoading(view);
                        }
                    }, 1000);
                }
                updateNavigationButtons();
                //urlEditText.setText(url);
                //updateUrlBar(view);
                int currentTabIndex = tabs.indexOf(view);
                if (!isBackNavigation) {
                    if (historyItems.size() > currentHistoryIndex + 1) {
                        historyItems.subList(currentHistoryIndex + 1, historyItems.size()).clear();
                    }
                    if (historyItems.isEmpty() || !historyItems.get(historyItems.size() - 1).getUrl().equals(url)) {
                        historyItems.add(new HistoryItem(view.getTitle(), url));
                        if (historyItems.size() > MAX_HISTORY_SIZE) {
                            historyItems.remove(0);
                        }
                        currentHistoryIndex = historyItems.size() - 1;
                        saveHistory();
                    }
                } else {
                    isBackNavigation = false;
                }
                if (swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }

                String jsOverride = "javascript:(function(){" +
                        "function notify(){ AndroidBridge.onUrlChange(location.href); }" +
                        "var ps = history.pushState; history.pushState = function(){ ps.apply(history, arguments); notify(); };" +
                        "var rs = history.replaceState; history.replaceState = function(){ rs.apply(history, arguments); notify(); };" +
                        "window.addEventListener('popstate', notify);" +
                        "notify();" +
                        "})()";
                view.loadUrl(jsOverride);

                if (currentTabIndex >= 0) {
                    TabInfo tabInfo = tabInfos.get(currentTabIndex);
                    tabInfo.setUrl(url);
                    tabInfo.setTitle(view.getTitle());

                    // 履歴を追加
                    ArrayList<String> history = tabInfo.getHistory();
                    if (history.isEmpty() || !history.get(history.size() - 1).equals(url)) {
                        history.add(url);
                    }
                    saveTabsState(); //  ここで履歴も保存
                }
                // 🔹 追加: 履歴を DatabaseHelper に保存
                DBHistory dbHistory = new DBHistory(view.getContext());
                dbHistory.saveHistory(url, view.getTitle(), System.currentTimeMillis());
                onProgressChanged(100);
                hideProgressBarDelayed();
                updateUrlBar(view);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleIntentScheme(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // Android 6.0 以降用
                return handleIntentScheme(view, request.getUrl().toString());
            }

            private boolean handleIntentScheme(WebView view, String url) {
                try {
                    Uri uri = Uri.parse(url);
                    String scheme = uri.getScheme();

                    if ("intent".equalsIgnoreCase(scheme) || "android-app".equalsIgnoreCase(scheme)) {
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        PackageManager pm = getPackageManager();
                        if (intent != null && intent.resolveActivity(pm) != null) {
                            startActivity(intent);
                            return true;
                        } else {
                            // fallback URL が含まれていれば開く
                            String fallbackUrl = intent.getStringExtra("browser_fallback_url");
                            if (fallbackUrl != null) {
                                view.loadUrl(fallbackUrl);
                                return true;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // 通常のURLは WebView で表示
                return false;
            }

            public void onReceivedError() {
                load("file:///android_asset/error.html");
            }
            @Override
            public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
                if (!basicAuthEnabled) {
                    super.onReceivedHttpAuthRequest(view, handler, host, realm);
                    return;
                }
                LinearLayout layout = new LinearLayout(MainActivity.this);
                layout.setOrientation(LinearLayout.VERTICAL);
                int pad = (int)(16 * getResources().getDisplayMetrics().density);
                layout.setPadding(pad, pad, pad, pad);
                final EditText usernameInput = new EditText(MainActivity.this);
                usernameInput.setHint("ユーザー名");
                usernameInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
                layout.addView(usernameInput);
                final EditText passwordInput = new EditText(MainActivity.this);
                passwordInput.setHint("パスワード");
                passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                layout.addView(passwordInput);
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Basic認証情報を入力")
                        .setView(layout)
                        .setPositiveButton("ログイン", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String user = usernameInput.getText().toString().trim();
                                String pass = passwordInput.getText().toString().trim();
                                if (!user.isEmpty() && !pass.isEmpty()) {
                                    handler.proceed(user, pass);
                                } else {
                                    Toast.makeText(MainActivity.this, "ユーザー名とパスワードを入力してください", Toast.LENGTH_SHORT).show();
                                    handler.cancel();
                                }
                            }
                        })
                        .setNegativeButton("キャンセル", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                handler.cancel();
                            }
                        })
                        .show();
            }
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                try {
                    Uri uri = Uri.parse(url);
                    String host = uri.getHost();
                    if (host != null && isAdHost(host)) {
                        return new WebResourceResponse("text/plain", "utf-8",
                                new ByteArrayInputStream("".getBytes()));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return super.shouldInterceptRequest(view, url);
            }

            private boolean isAdHost(String host) {
                host = host.toLowerCase();
                // 完全一致だけでなく、サブドメインも含めてマッチ
                for (String adHost : adHostSet) {
                    if (host.equals(adHost) || host.endsWith("." + adHost)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    private void injectAdBlockJS(WebView view) {
        String js = "javascript:setTimeout(function() {"
                + "var adSelectors = ["
                + "'iframe[src*=\"ads\"]',"
                + "'iframe[src*=\"doubleclick\"]',"
                + "'div[id^=\"ad\"], div[class*=\"ad\"], section[class*=\"ad\"]',"
                + "'div[class*=\"sponsored\"]',"
                + "'div[class*=\"banner\"]',"
                + "'div[data-ad]'"  // data-ad 属性を持つ要素
                + "];"
                + "adSelectors.forEach(function(selector) {"
                + "  var ads = document.querySelectorAll(selector);"
                + "  for (var i = 0; i < ads.length; i++) {"
                + "    ads[i].remove();"
                + "  }"
                + "});"
                + "}, 1000);";

        view.evaluateJavascript(js, null);
    }

    private Set<String> adHostSet = new HashSet<>();

    private void loadAdHostsFromAssets() {
        try {
            InputStream inputStream = getAssets().open("hosts.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    adHostSet.add(line.toLowerCase());
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void savePage() {
        WebView webView = tabs.get(currentTabIndex);
        String url = webView.getUrl();
        String title = webView.getTitle();
        String dateSaved = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        if (url == null || title == null) {
            Toast.makeText(this, "ページを取得できません", Toast.LENGTH_SHORT).show();
            return;
        }

        // スクリーンショットを撮る
        webView.post(() -> {
            Bitmap bitmap = Bitmap.createBitmap(webView.getWidth(), webView.getHeight(), Bitmap.Config.ARGB_8888);
            webView.draw(new android.graphics.Canvas(bitmap));

            // 保存フォルダ作成
            File dir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "PageScreenshots");
            if (!dir.exists()) dir.mkdirs();

            // ファイル保存
            File imageFile = new File(dir, "screenshot_" + System.currentTimeMillis() + ".png");
            try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "スクリーンショットの保存に失敗しました", Toast.LENGTH_SHORT).show();
                return;
            }

            // データベースに保存
            SQLiteDatabase db = dbH.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("url", url);
            values.put("title", title);
            values.put("screenshot_path", imageFile.getAbsolutePath());
            values.put("date_saved", dateSaved);
            db.insert("pages", null, values);

            Toast.makeText(this, "ページを保存しました", Toast.LENGTH_SHORT).show();
        });
    }

    private void savebm() {
        WebView webView = tabs.get(currentTabIndex);
        String url = webView.getUrl();
        String title = webView.getTitle();
        ImageButton bmbutton = findViewById(R.id.action_bookmark);

        if (url == null || title == null) {
            Toast.makeText(this, "ページを取得できません", Toast.LENGTH_SHORT).show();
            return;
        }

        // 登録済みか先に判定
        SQLiteDatabase db = dbbm.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM pages WHERE url = ?", new String[]{url});

        boolean isBookmarked = false;
        if (cursor.moveToFirst()) {
            isBookmarked = cursor.getInt(0) > 0;
        }
        cursor.close();

        if (isBookmarked) {
            // 登録済みなら削除
            SQLiteDatabase dbw = dbbm.getWritableDatabase();
            dbw.delete("pages", "url = ?", new String[]{url});
            bmbutton.setImageResource(R.drawable.bookmark_black);
            Toast.makeText(this, "ブックマークを削除しました", Toast.LENGTH_SHORT).show();
        } else {
            // 未登録ならスクショ撮って保存
            webView.post(() -> {
                Bitmap bitmap = Bitmap.createBitmap(webView.getWidth(), webView.getHeight(), Bitmap.Config.ARGB_8888);
                webView.draw(new android.graphics.Canvas(bitmap));

                File dir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "PageScreenshots");
                if (!dir.exists()) dir.mkdirs();

                File imageFile = new File(dir, "screenshot_" + System.currentTimeMillis() + ".png");
                try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "スクリーンショットの保存に失敗しました", Toast.LENGTH_SHORT).show();
                    return;
                }

                SQLiteDatabase dbw = dbbm.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("url", url);
                values.put("title", title);
                values.put("screenshot_path", imageFile.getAbsolutePath());
                dbw.insert("pages", null, values);

                bmbutton.setImageResource(R.drawable.bookmark_star);
                Toast.makeText(this, "ブックマークを保存しました", Toast.LENGTH_SHORT).show();
            });
        }
    }

    static class DatabaseHelper extends SQLiteOpenHelper {
        private static final String DB_NAME = "web_pages.db";
        private static final int DB_VERSION = 1;

        DatabaseHelper(android.content.Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE pages (id INTEGER PRIMARY KEY, url TEXT, html TEXT)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS pages");
            onCreate(db);
        }

        boolean savePage(String url, String html) {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("url", url);
            values.put("html", html);
            return db.insert("pages", null, values) != -1;
        }
    }

    static class BMDatabaseHelper extends SQLiteOpenHelper {
        private static final String DB_NAME = "web_bm.db"; // 新しいデータベース名
        private static final int DB_VERSION = 1;

        BMDatabaseHelper(android.content.Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE pages (id INTEGER PRIMARY KEY, url TEXT, html TEXT)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS pages");
            onCreate(db);
        }

        boolean savebm(String url, String html) {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("url", url);
            values.put("html", html);
            return db.insert("pages", null, values) != -1;
        }
    }

    public class DBHistoryHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "history.db";
        private static final int DATABASE_VERSION = 1;

        public DBHistoryHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE history (id INTEGER PRIMARY KEY AUTOINCREMENT, url TEXT, title TEXT, timestamp INTEGER, visit_count INTEGER)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS history");
            onCreate(db);
        }

        public void saveHistory(String url, String title, long timestamp) {
            SQLiteDatabase db = this.getWritableDatabase();
            Cursor cursor = db.rawQuery("SELECT visit_count FROM history WHERE url = ?", new String[]{url});
            if (cursor.moveToFirst()) {
                int visitCount = cursor.getInt(0) + 1;
                db.execSQL("UPDATE history SET visit_count = ?, timestamp = ? WHERE url = ?", new Object[]{visitCount, timestamp, url});
            } else {
                db.execSQL("INSERT INTO history (url, title, timestamp, visit_count) VALUES (?, ?, ?, ?)", new Object[]{url, title, timestamp, 1});
            }
            cursor.close();
            db.close();
        }
    }

    private class DownloadFaviconTask extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... urls) {
            String url = urls[0];
            try {
                InputStream in = new java.net.URL(url).openStream();
                return BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                faviconImageView.setImageBitmap(result);
            } else {
                faviconImageView.setImageResource(R.drawable.transparent_vector);
            }
        }
    }

    private void updateUrlBar(WebView webView) {
        String currentUrl = webView.getUrl();

        // 指定URLの場合は空白を表示
        if (currentUrl != null && (
                currentUrl.equals("file:///android_asset/index.html") ||
                        currentUrl.equals("file:///android_asset/help.html")
        )) {
            urlEditText.setText(""); // URLを非表示
        } else {
            urlEditText.setText(currentUrl); // 通常のURLを表示
        }
    }

    private void injectLazyLoading(WebView wv) {
        String js = "javascript:(function() {" +
                "try {" +
                "var placeholder = 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7';" +
                "var imgs = document.querySelectorAll('img[src^=\"https://i.ytimg.com/\"]:not([data-lazy-loaded])');" +
                "if(imgs.length===0)return;" +
                "imgs.forEach(function(img){ img.setAttribute('data-lazy-loaded','true');" +
                "if(img.hasAttribute('src')){ img.setAttribute('data-src', img.src); img.src=placeholder; img.style.opacity='0'; img.style.transition='opacity 0.3s'; if(!img.style.transform){ img.style.transform='translateZ(0)'; } } });" +
                "if(window.IntersectionObserver){" +
                "var observer = new IntersectionObserver(function(entries){" +
                "entries.forEach(function(entry){ if(entry.isIntersecting){ var i = entry.target; if(i.dataset.src){ i.src = i.dataset.src; i.removeAttribute('data-src'); i.onload=function(){ i.style.opacity='1'; }; i.onerror=function(){ console.warn('Image load failed: '+i.src); }; } observer.unobserve(i); } });" +
                "}, {root:null, rootMargin:'0px', threshold:0.1});" +
                "imgs.forEach(function(i){ observer.observe(i); });" +
                "} else {" +
                "var loadOnScroll = function(){" +
                "imgs.forEach(function(i){ if(i.dataset.src && (i.getBoundingClientRect().top >=0 && i.getBoundingClientRect().left >=0 && i.getBoundingClientRect().bottom <= (window.innerHeight || document.documentElement.clientHeight) && i.getBoundingClientRect().right <= (window.innerWidth || document.documentElement.clientWidth))){ i.src = i.dataset.src; i.removeAttribute('data-src'); i.onload=function(){ i.style.opacity='1'; }; i.onerror=function(){ console.warn('Image load failed: '+i.src); }; } });" +
                "};" +
                "window.addEventListener('scroll', loadOnScroll);" +
                "window.addEventListener('resize', loadOnScroll);" +
                "window.addEventListener('load', loadOnScroll);" +
                "loadOnScroll();" +
                "}" +
                "} catch(e){ console.error('Lazy loading failed: '+e.message); }" +
                "})();";
        wv.evaluateJavascript(js, null);
    }

    private void applyCombinedOptimizations(WebView wv) {
        String js = "javascript:(function() {" +
                "try {" +
                "var animated = document.querySelectorAll('.animated, .transition');" +
                "animated.forEach(function(el){ if(!el.style.transform){ el.style.transform='translateZ(0)'; } if(!el.style.willChange){ el.style.willChange='transform, opacity'; } });" +
                "var fixedEls = document.querySelectorAll('.fixed');" +
                "fixedEls.forEach(function(el){ if(el.style.position !== 'fixed'){ el.style.position='fixed'; } });" +
                "} catch(e){ console.error('Optimization failed: '+e.message); }" +
                "})();";
        wv.evaluateJavascript(js, null);
    }

    private void startDownload(String url, String userAgent, String contentDisposition, String mimetype) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setMimeType(mimetype);
        request.addRequestHeader("User-Agent", userAgent);
        request.setDescription("ダウンロード中...");
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        String fileName = Uri.parse(url).getLastPathSegment();
        if (fileName == null || fileName.isEmpty()) {
            fileName = "downloaded_file";
        }
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        dm.enqueue(request);
        Toast.makeText(getApplicationContext(), "ダウンロードを開始します...", Toast.LENGTH_LONG).show();
    }

    private WebView createWebView(int id) {
        WebView webView;
        if (preloadedWebView != null) {
            webView = preloadedWebView;
            preloadedWebView = null;
            preInitializeWebView();
        } else {
            webView = new WebView(this);
        }
        webView.setTag(id); // ✅ タグを設定して、NullPointerExceptionを防止
        WebSettings s = webView.getSettings();

        // ProgressDialog の初期設定
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("ページを読み込み中...");
        progressDialog.setCancelable(false);
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "キャンセル", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (webView != null) {
                    webView.stopLoading(); // ページ読み込みをキャンセル
                }
                dialog.dismiss(); // ダイアログを閉じる
            }
        });

        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");
        webView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setGeolocationEnabled(false);
        webView.getSettings().setTextZoom(100);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setBuiltInZoomControls(false);
        webView.getSettings().setSupportZoom(false);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(true);
        String defaultUA = s.getUserAgentString();
        originalUserAgents.put(webView, defaultUA);
        applyOptimizedSettings(s);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            if (sSetSaveFormDataMethod != null) {
                try {
                    sSetSaveFormDataMethod.invoke(s, false);
                } catch (Exception e) { e.printStackTrace(); }
            }
            if (sSetDatabaseEnabledMethod != null) {
                try {
                    sSetDatabaseEnabledMethod.invoke(s, true);
                } catch (Exception e) { e.printStackTrace(); }
            }
            if (sSetAppCacheEnabledMethod != null && sSetAppCachePathMethod != null) {
                try {
                    sSetAppCacheEnabledMethod.invoke(s, true);
                    sSetAppCachePathMethod.invoke(s, getCacheDir().getAbsolutePath());
                } catch (Exception e) { e.printStackTrace(); }
            }
        }
        if (zoomEnabled) {
            s.setBuiltInZoomControls(true);
            s.setSupportZoom(true);
        } else {
            s.setBuiltInZoomControls(false);
            s.setSupportZoom(false);
        }
        s.setJavaScriptEnabled(!jsEnabled);
        s.setLoadsImagesAutomatically(!imgBlockEnabled);
        if (uaEnabled) {
            s.setUserAgentString("DoCoMo/2.0 SH902i(c100;TB)");
        } else if (deskuaEnabled) {
            String desktopUA = defaultUA.replace("Mobile", "").replace("Android", "");
            s.setUserAgentString(desktopUA + APPEND_STR);
        } else if (ct3uaEnabled) {
            s.setUserAgentString("Mozilla/5.0 (Linux; Android 7.0; TAB-A03-BR3 Build/02.05.000; wv) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/83.0.4103.106 Safari/537.36");
        } else {
            s.setUserAgentString(defaultUA + APPEND_STR);
        }

        // ✅ JavaScript インターフェース追加
        webView.addJavascriptInterface(new BlobDownloadInterface(), "BlobDownloader");

        // コンテキストメニューを作成
        registerForContextMenu(webView);

        webView.setOnLongClickListener(v -> {
            WebView.HitTestResult result = webView.getHitTestResult();
            if (result == null) {
                return false;
            }

            // メニューを開く
            selectedUrl = result.getExtra(); // 長押しされたURL
            selectedType = result.getType();
            openContextMenu(webView);

            return true;
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView,
                                             ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                if (MainActivity.this.filePathCallback != null) {
                    MainActivity.this.filePathCallback.onReceiveValue(null);
                }
                MainActivity.this.filePathCallback = filePathCallback;

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");

                startActivityForResult(Intent.createChooser(intent, "ファイルを選択"), FILE_CHOOSER_REQUEST_CODE);
                return true;
            }
            @Override
            public void onReceivedTitle(WebView view, String title) {
                tabInfos.get(currentTabIndex).setTitle(title);
                if (tabListAdapter != null) {
                    tabListAdapter.notifyDataSetChanged();
                }
                saveTabsState(); // ✅ タイトル更新時に保存
            }
            @Override
            public void onReceivedIcon(WebView view, Bitmap icon) {
                if (currentTabIndex >= 0 && currentTabIndex < tabInfos.size()) {
                    tabInfos.get(currentTabIndex).setIcon(icon);
                    if (tabListAdapter != null) {
                        tabListAdapter.notifyDataSetChanged();
                    }
                }
            }
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                super.onShowCustomView(view, callback);

                webView.scrollTo(0, 0);

                webView.addView(
                        view,
                        new ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                        )
                );

                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }

                // フルスクリーンビューをセット
                customView = view;
                customViewCallback = callback;

                // 下部バーも非表示（必要に応じて）
                bottomBar.setVisibility(View.GONE);
                action_Bar.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);

                // URL テキストボックスも非表示
                urlEditText.setVisibility(View.GONE);

                // ステータスバーなどを隠す
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                );
            }

            @Override
            public void onHideCustomView() {
                super.onHideCustomView();
                webView.removeAllViews();
                if (customView == null) {
                    return;
                }

                // フルスクリーンビューを削除
                fullscreenContainer.removeView(customView);
                fullscreenContainer.setVisibility(View.GONE);
                customView = null;
                customViewCallback.onCustomViewHidden();

                // 下部バーを戻す
                bottomBar.setVisibility(View.VISIBLE);
                action_Bar.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.VISIBLE);

                // URL テキストボックスを戻す
                urlEditText.setVisibility(View.VISIBLE);

                // ステータスバーを復帰
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle("権限リクエスト");
                        StringBuilder message = new StringBuilder("このサイトが以下を要求しています:\n\n");
                        for (String res : request.getResources()) {
                            message.append(resourceToString(res)).append("\n");
                        }
                        builder.setMessage(message.toString());

                        builder.setPositiveButton("許可", (dialog, which) -> {
                            checkAndRequestPermissions();
                            request.grant(request.getResources());
                        });
                        builder.setNegativeButton("拒否", (dialog, which) -> request.deny());
                        builder.setCancelable(false);
                        builder.show();
                    }
                });
            }

            // オプション: 権限文字列を分かりやすくする
            private String resourceToString(String resource) {
                switch (resource) {
                    case PermissionRequest.RESOURCE_AUDIO_CAPTURE: return "マイク";
                    case PermissionRequest.RESOURCE_VIDEO_CAPTURE: return "カメラ";
                    case PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID: return "保護されたメディアID";
                    default: return resource;
                }
            }

        });

        // ✅ ダウンロード機能の追加（API 19 互換）
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

            if (!downloadsDir.canWrite()) {
                // ✅ 権限がない場合に設定画面を開くよう促す
                new AlertDialog.Builder(this)
                        .setTitle("ストレージ権限が必要です")
                        .setMessage("ファイルをダウンロードするにはストレージへの書き込み権限が必要です。設定画面で許可してください。")
                        .setPositiveButton("設定を開く", (dialog, which) -> {
                            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        })
                        .setNegativeButton("キャンセル", null)
                        .show();
            } else {
                // ✅ 権限がある場合はダウンロード開始
                startDownload(url, userAgent, contentDisposition, mimetype);
            }
        });

        webView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                WebView.HitTestResult result = webView.getHitTestResult();
                if (result != null) {
                    final int type = result.getType();
                    selectedUrl = result.getExtra();
                    boolean isDataUrl = selectedUrl != null && selectedUrl.startsWith("data:");
                    if (type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                        final String[] opts;
                        if (isDataUrl) {
                            opts = new String[]{"新しいタブで開く", "リンクをコピー"};
                        } else {
                            opts = new String[]{"新しいタブで開く", "リンクをコピー", "リンクをダウンロード", "画像を保存"};
                        }
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("オプションを選択")
                                .setItems(opts, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (which == 0) {
                                            addNewTab(selectedUrl);
                                        } else if (which == 1) {
                                            copyLink(selectedUrl);
                                        } else if (which == 2) {
                                            downloadLink(selectedUrl);
                                        } else if (which == 3 && !isDataUrl) {
                                            if (selectedUrl != null && !selectedUrl.isEmpty()) {
                                                downloadImage(selectedUrl);
                                            }
                                        }
                                    }
                                }).show();
                        return true;
                    } else if (type == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
                        final String[] opts = new String[]{"新しいタブで開く", "リンクをコピー", "リンクをダウンロード"};
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("オプションを選択")
                                .setItems(opts, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (which == 0) {
                                            addNewTab(selectedUrl);
                                        } else if (which == 1) {
                                            copyLink(selectedUrl);
                                        } else if (which == 2) {
                                            downloadLink(selectedUrl);
                                        }
                                    }
                                }).show();
                        return true;
                    } else if (type == WebView.HitTestResult.IMAGE_TYPE) {
                        final String[] opts;
                        boolean isDataUrlLocal = selectedUrl != null && selectedUrl.startsWith("data:");
                        if (isDataUrlLocal) {
                            opts = new String[]{"リンクをコピー", "画像を保存"};
                        } else {
                            opts = new String[]{"新しいタブで開く", "リンクをコピー", "リンクをダウンロード", "画像を保存"};
                        }
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("オプションを選択")
                                .setItems(opts, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (which == 0) {
                                            if (!isDataUrlLocal) {
                                                addNewTab(selectedUrl);
                                            } else {
                                                copyLink(selectedUrl);
                                            }
                                        } else if (which == 1) {
                                            if (isDataUrlLocal) {
                                                if (selectedUrl != null && !selectedUrl.isEmpty()) {
                                                    downloadImage(selectedUrl);
                                                }
                                            } else {
                                                copyLink(selectedUrl);
                                            }
                                        } else if (which == 2) {
                                           downloadLink(selectedUrl);
                                        } else if (which == 3 && !isDataUrlLocal) {
                                            if (selectedUrl != null && !selectedUrl.isEmpty()) {
                                                downloadImage(selectedUrl);
                                            }
                                        }
                                    }
                                }).show();
                        return true;
                    }
                }
                return false;
            }
        });

        webView.setWebViewClient(createWebViewClient(id)); // id を渡す
        return webView;
    }

    private void checkAndRequestPermissions() {
        List<String> permissions = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO);
        }

        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissions.toArray(new String[0]),
                    123); // 任意のリクエストコード
        }
    }

    private class BlobDownloadInterface {
        @JavascriptInterface
        public void onBlobDownloaded(String base64Data, String mimeType, String fileName) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int comma = base64Data.indexOf(",");
                        String pureBase64 = base64Data.substring(comma + 1);
                        byte[] data = Base64.decode(pureBase64, Base64.DEFAULT);
                        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                        if (!downloadDir.exists()) downloadDir.mkdirs();
                        File file = new File(downloadDir, fileName);
                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(data);
                        fos.flush();
                        fos.close();
                        Toast.makeText(MainActivity.this, "blob ダウンロード完了: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "blob ダウンロードエラー: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
        @JavascriptInterface
        public void onBlobDownloadError(String errorMessage) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "blob ダウンロードエラー: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void updateNavigationButtons() {
        WebView currentWebView = tabs.get(currentTabIndex);
        backButton.setEnabled(currentWebView.canGoBack());   // 🔹 戻るボタンの有効/無効を設定
        forwardButton.setEnabled(currentWebView.canGoForward()); // 🔹 進むボタンの有効/無効を設定
    }

    private void goBack() {
        WebView currentWebView = tabs.get(currentTabIndex);
        if (currentWebView.canGoBack()) {
            currentWebView.goBack();
            new android.os.Handler().postDelayed(this::updateNavigationButtons, 300); // 300ms遅延
        }
    }

    private void goForward() {
        WebView currentWebView = tabs.get(currentTabIndex);
        if (currentWebView.canGoForward()) {
            currentWebView.goForward();
            new android.os.Handler().postDelayed(this::updateNavigationButtons, 300);
        }
    }

    public void updatebmbutton() {
        WebView webView = tabs.get(currentTabIndex);
        String url = webView.getUrl();

        ImageButton bmbutton = findViewById(R.id.action_bookmark);

        // SQLiteOpenHelper 例: dbbm
        SQLiteDatabase db = dbbm.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM pages WHERE url = ?", new String[]{url});

        boolean isBookmarked = false;
        if (cursor.moveToFirst()) {
            isBookmarked = cursor.getInt(0) > 0;
        }
        cursor.close();

        if (isBookmarked) {
            bmbutton.setImageResource(R.drawable.bookmark_star);
        } else {
            bmbutton.setImageResource(R.drawable.bookmark_black);
        }
    }

    private void switchToTab(int index) {
        if (index < 0 || index >= tabs.size()) return;

        // ✅ 全てのWebViewを非表示に
        for (WebView webView : tabs) {
            webView.setVisibility(View.GONE);
        }

        // ✅ 選択したタブを表示
        WebView currentWebView = tabs.get(index);
        currentWebView.setVisibility(View.VISIBLE);
        currentWebView.requestLayout(); // 再描画をリクエスト
        currentWebView.invalidate(); // 画面を再描画

        String url = currentWebView.getUrl();

        ImageButton bmbutton = findViewById(R.id.action_bookmark);

        // SQLiteOpenHelper 例: dbbm
        SQLiteDatabase db = dbbm.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM pages WHERE url = ?", new String[]{url});

        boolean isBookmarked = false;
        if (cursor.moveToFirst()) {
            isBookmarked = cursor.getInt(0) > 0;
        }
        cursor.close();

        if (isBookmarked) {
            bmbutton.setImageResource(R.drawable.bookmark_star);
        } else {
            bmbutton.setImageResource(R.drawable.bookmark_black);
        }

        // JavaScript を使用して Favicon を取得
        currentWebView.evaluateJavascript("(function() { " +
                "var link = document.querySelector('link[rel~=\"icon\"]');" +
                "return link ? link.href : ''; " +
                "})()", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                value = value.replace("\"", ""); // 取得した URL の " を削除
                if (!value.isEmpty()) {
                    new DownloadFaviconTask().execute(value);
                } else {
                    faviconImageView.setImageResource(R.drawable.transparent_vector); // デフォルトアイコン
                }
            }
        });

        currentTabIndex = index;
        urlEditText.setText(currentWebView.getUrl());
        updateUrlBar(currentWebView);
        updateNavigationButtons();
        // 🔹 タブ復元時にタイトルが `null` の場合、強制的に取得
        if (tabInfos.get(index).getTitle().equals("読込中...")) {
            tabInfos.get(index).setTitle(currentWebView.getTitle());
            if (tabListAdapter != null) {
                tabListAdapter.notifyDataSetChanged();
            }
        }
        updateNavigationButtons();
        saveTabsState();

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                WebView webView = tabs.get(currentTabIndex);
                updateUrlBar(webView);
                // JavaScript を使用して Favicon を取得
                webView.evaluateJavascript("(function() { " +
                        "var link = document.querySelector('link[rel~=\"icon\"]');" +
                        "return link ? link.href : ''; " +
                        "})()", new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        value = value.replace("\"", ""); // 取得した URL の " を削除
                        if (!value.isEmpty()) {
                            new DownloadFaviconTask().execute(value);
                        } else {
                            faviconImageView.setImageResource(R.drawable.transparent_vector); // デフォルトアイコン
                        }
                    }
                });
            }
        }, 2500);
    }

    private void closeTab(int index) {
        if (index < 0 || index >= tabs.size()) return;

        WebView webView = tabs.remove(index);
        tabInfos.remove(index);
        webViewContainer.removeView(webView); // 🔹 WebView を削除

        if (tabs.isEmpty()) {
            addNewTab("file:///android_asset/index.html");
        } else {
            currentTabIndex = Math.max(0, currentTabIndex - 1);
        }

        String url = webView.getUrl();

        ImageButton bmbutton = findViewById(R.id.action_bookmark);

        // SQLiteOpenHelper 例: dbbm
        SQLiteDatabase db = dbbm.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM pages WHERE url = ?", new String[]{url});

        boolean isBookmarked = false;
        if (cursor.moveToFirst()) {
            isBookmarked = cursor.getInt(0) > 0;
        }
        cursor.close();

        if (isBookmarked) {
            bmbutton.setImageResource(R.drawable.bookmark_star);
        } else {
            bmbutton.setImageResource(R.drawable.bookmark_black);
        }
        switchToTab(currentTabIndex);
        updateTabCount();
    }

    public void onReceivedIcon(WebView view, Bitmap icon) {
        if (view == webView) {
            faviconImageView.setImageBitmap(icon);
        }
        webViewFavicons.put(view, icon);
        String curUrl = view.getUrl();
        if (curUrl != null) {
            faviconCache.put(curUrl, icon);
            backgroundExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    saveFaviconToFile(curUrl, icon);
                }
            });
        }
    }

    private void saveFaviconToFile(String url, Bitmap bitmap) {
        File faviconsDir = new File(getFilesDir(), "favicons");
        if (!faviconsDir.exists()) {
            faviconsDir.mkdirs();
        }
        File file = new File(faviconsDir, getFaviconFilename(url));
        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 50, fos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getFaviconFilename(String url) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(url.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString() + ".png";
        } catch (Exception e) {
            return Integer.toString(url.hashCode()) + ".png";
        }
    }

    private void loadFaviconFromDisk(String url) {
        File faviconsDir = new File(getFilesDir(), "favicons");
        File file = new File(faviconsDir, getFaviconFilename(url));
        if (file.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (bitmap != null) {
                faviconCache.put(url, bitmap);
            }
        }
    }

    private void initializePersistentFavicons() {
        for (Bookmark bm : bookmarks) {
            final String url = bm.getUrl();
            backgroundExecutor.execute(() -> loadFaviconFromDisk(url));
        }
        for (HistoryItem hi : historyItems) {
            final String url = hi.getUrl();
            backgroundExecutor.execute(() -> loadFaviconFromDisk(url));
        }
    }

    private void loadHistory() {
        String json = prefs.getString(KEY_HISTORY, "[]");
        try {
            JSONArray array = new JSONArray(json);
            historyItems.clear();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                historyItems.add(new HistoryItem(obj.getString("title"), obj.getString("url")));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // 履歴の保存（JSON 配列として SharedPreferences に保存）
    private void saveHistory() {
        JSONArray arr = new JSONArray();
        for (HistoryItem item : historyItems) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("title", item.getTitle());
                obj.put("url", item.getUrl());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            arr.put(obj);
        }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply();
    }

    private void showHistoryDialog() {
        if (historyItems.isEmpty()) {
            Toast.makeText(this, "履歴がありません", Toast.LENGTH_SHORT).show();
            return;
        }
        RecyclerView recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("履歴")
                .setPositiveButton("すべての履歴を削除する", (d, which) -> historyLastCheck())
                .setNeutralButton("閉じる", null)
                .setView(recyclerView)
                .create();
        HistoryAdapter adapter = new HistoryAdapter(historyItems, dialog);
        recyclerView.setAdapter(adapter);
        dialog.show();
    }

    private void historyLastCheck() {
        new AlertDialog.Builder(this)
                .setTitle("履歴を削除")
                .setMessage("この操作を行うと、すべての履歴が削除されます")
                .setPositiveButton("削除する", (dialog, which) -> historyreset())
                .setNegativeButton("キャンセル", null)
                .show();
    }

    private void historyreset() {
        progressResetDialog.show();
        if (webView != null) webView.clearHistory();
        historyItems.clear();
        saveHistory();
        progressResetDialog.dismiss();
        Toast.makeText(MainActivity.this, "履歴を消去しました", Toast.LENGTH_SHORT).show();
    }

    private void loadBookmarks() {
        String json = prefs.getString(KEY_BOOKMARKS, "[]");
        try {
            JSONArray array = new JSONArray(json);
            bookmarks.clear();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                bookmarks.add(new Bookmark(obj.getString("title"), obj.getString("url")));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void saveBookmarks() {
        JSONArray array = new JSONArray();
        for (Bookmark bm : bookmarks) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("title", bm.getTitle());
                obj.put("url", bm.getUrl());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            array.put(obj);
        }
        prefs.edit().putString(KEY_BOOKMARKS, array.toString()).apply();
    }

    // ブックマークインポート処理（ファイル選択後、JSON を読み込んで保存）
    public void importBookmarksFromFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            intent.setType("*/*");
        } else {
            intent.setType("application/json");
        }
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_CODE_IMPORT_BOOKMARKS);
    }

    private Bitmap fetchFavicon(String bookmarkUrl) {
        try {
            URL urlObj = new URL(bookmarkUrl);
            String protocol = urlObj.getProtocol();
            String host = urlObj.getHost();
            String faviconUrl = protocol + "://" + host + "/favicon.ico";
            HttpURLConnection connection = (HttpURLConnection) new URL(faviconUrl).openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setRequestMethod("GET");
            connection.connect();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (InputStream is = connection.getInputStream()) {
                    return BitmapFactory.decodeStream(is);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String readTextFromUri(Uri uri) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private void parseAndImportBookmarks(String jsonStr) throws JSONException {
        JSONArray array = new JSONArray(jsonStr);
        bookmarks.clear();
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);
            String title = obj.optString("title", "Untitled");
            String url = obj.optString("url", "");
            if (!url.isEmpty()) {
                bookmarks.add(new Bookmark(title, url));
                backgroundExecutor.execute(() -> {
                    Bitmap favicon = fetchFavicon(url);
                    if (favicon != null) {
                        runOnUiThread(() -> faviconCache.put(url, favicon));
                        saveFaviconToFile(url, favicon);
                    }
                });
            }
        }
        saveBookmarks();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            if (filePathCallback == null) return;

            Uri[] results = null;

            if (resultCode == RESULT_OK && data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    results = new Uri[]{uri};
                }
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void showBookmarksManagementDialog() {
        if (bookmarks.isEmpty()) {
            Toast.makeText(this, "ブックマークがありません", Toast.LENGTH_SHORT).show();
            return;
        }
        RecyclerView recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("ブックマーク")
                .setNegativeButton("閉じる", null)
                .setView(recyclerView)
                .create();
        BookmarkAdapter adapter = new BookmarkAdapter(bookmarks, true, dialog);
        recyclerView.setAdapter(adapter);
        dialog.show();
    }

    private void showEditBookmarkDialog(final int position, final BookmarkAdapter adapter) {
        Bookmark bm = bookmarks.get(position);

        // レイアウトのインフレート
        ViewGroup parent = findViewById(android.R.id.content);
        View editView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_bookmark, parent, false);

        EditText etTitle = editView.findViewById(R.id.editTitle);
        EditText etUrl = editView.findViewById(R.id.editUrl);

        etTitle.setText(bm.getTitle());
        etUrl.setText(bm.getUrl());

        // Holo スタイルの AlertDialog を使用
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Holo_Dialog);

        builder.setTitle("ブックマーク")
                .setView(editView)
                .setPositiveButton("保存", (dialog, which) -> {
                    String newTitle = etTitle.getText().toString().trim();
                    String newUrl = etUrl.getText().toString().trim();

                    // URL のバリデーション
                    if (!newUrl.startsWith("http://") && !newUrl.startsWith("https://")) {
                        newUrl = "http://" + newUrl;
                    }

                    // ブックマーク更新
                    bookmarks.set(position, new Bookmark(newTitle, newUrl));
                    saveBookmarks();
                    adapter.notifyDataSetChanged();

                    Toast.makeText(MainActivity.this, "保存しました", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("キャンセル", (dialog, which) -> dialog.dismiss()) // 明示的に dismiss()
                .show();
    }

    private void showTabMenu() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("タブ一覧");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        // タブリスト
        ListView listView = new ListView(this);
        tabListAdapter = new TabListAdapter(this, tabInfos, currentTabIndex);
        listView.setAdapter(tabListAdapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView.setItemChecked(currentTabIndex, true);

        // item_tab_add.xml を inflate
        View tabAddView = LayoutInflater.from(this).inflate(R.layout.item_tab_add, null);
        ImageButton tabAddButton = tabAddView.findViewById(R.id.tabAddButton);

        dialog = builder.setView(layout)
                .setNeutralButton("閉じる", null)
                .setPositiveButton("すべてのタブを閉じる", (d, which) -> show_check_tabClose())
                .create();

        // タブ追加ボタンの処理
        tabAddButton.setOnClickListener((v) -> {
            addNewTab("file:///android_asset/index.html");
            dialog.dismiss();
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            switchToTab(position);
            dialog.dismiss();
        });

        // レイアウトに追加
        layout.addView(listView);
        layout.addView(tabAddView);

        dialog.show();
    }

    private void show_check_tabClose() {
        new AlertDialog.Builder(this)
                .setTitle("すべてのタブを閉じる")
                .setMessage("本当にすべてのタブを閉じますか？")
                .setPositiveButton("はい", (dialog, which) -> closeAllTabs())
                .setNegativeButton("キャンセル", null)
                .show();
    }

    private void closeAllTabs() {
        if (!tabInfos.isEmpty()) {
            tabInfos.clear();
            tabs.clear();
            currentTabIndex = -1;
            tabListAdapter.notifyDataSetChanged();

            // UI更新を確実に反映した後、新規タブを追加
            new Handler(Looper.getMainLooper()).post(() -> {
                addNewTab("file:///android_asset/index.html");
                Toast.makeText(this, "すべてのタブを閉じました", Toast.LENGTH_SHORT).show();
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
            });

        } else {
            Toast.makeText(this, "タブがありません", Toast.LENGTH_SHORT).show();
        }
    }

    public void onTabClose(int position) {
        if (position >= 0 && position < tabInfos.size()) {
            closeTab(position);
            tabListAdapter.notifyDataSetChanged(); // リストビューを更新
            switchToTab(position);
        }
    }


    private void loadUrlInCurrentTab(String url) {
        if (!url.startsWith("https://") && !url.startsWith("http://")) {
            if (url.contains(".")) {
                url = "https://" + url;
            } else {
                // 検索エンジンのURLを付加
                url = "https://www.google.com/search?q=" + url;
            }
        }
        tabs.get(currentTabIndex).loadUrl(url);
        tabInfos.get(currentTabIndex).setUrl(url);
        saveTabsState();
    }

    private void closeKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(urlEditText.getWindowToken(), 0);
    }

    private void addNewTab(String url) {
        int newId = nextTabId++;
        WebView webView = createWebView(newId);
        webView.loadUrl(url);

        tabs.add(webView);

        // ✅ タブ情報を必ず追加
        if (tabInfos.size() < tabs.size()) {
            tabInfos.add(new TabInfo("読込中...", url, null));
        }

        webViewContainer.addView(webView);
        switchToTab(tabs.size() - 1);
        updateTabCount();
    }

    // ✅ タブ状態の保存
    private void saveTabsState() {
        JSONArray tabsArray = new JSONArray();
        for (int i = 0; i < tabs.size(); i++) {
            WebView webView = tabs.get(i);
            int id = (int) webView.getTag();
            String url = webView.getUrl();
            if (url == null) url = "";
            JSONObject obj = new JSONObject();
            try {
                obj.put("id", id);
                obj.put("url", url);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            tabsArray.put(obj);

            // ✅ WebViewの状態を保存
            Bundle state = new Bundle();
            webView.saveState(state);
            saveBundleToFile(state, "tab_state_" + id + ".dat");
        }
        int currentTabId = (int) getCurrentWebView().getTag();
        prefs.edit()
                .putString(KEY_TABS, tabsArray.toString())
                .putInt(KEY_CURRENT_TAB_ID, currentTabId)
                .apply();
    }

    // ✅ タブ状態の読み込み
    private void loadTabsState() {
        String json = prefs.getString(KEY_TABS, "[]");
        int currentTabId = prefs.getInt(KEY_CURRENT_TAB_ID, -1);

        try {
            JSONArray array = new JSONArray(json);
            tabs.clear();
            tabInfos.clear();
            webViewContainer.removeAllViews();
            int maxId = 0;

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                int id = obj.getInt("id");
                String url = obj.getString("url");

                WebView webView = createWebView(id);
                webView.setTag(id);
                if (id > maxId) maxId = id;
                tabs.add(webView);
                tabInfos.add(new TabInfo("読込中...", url, null)); // ✅ タブ情報を追加
                webViewContainer.addView(webView);
                Bundle state = loadBundleFromFile("tab_state_" + id + ".dat");
                if (state != null) {
                    webView.restoreState(state);
                } else {
                    webView.loadUrl(url);
                }
            }
            nextTabId = maxId + 1;

            if (tabs.isEmpty()) {
                addNewTab("file:///android_asset/index.html"); // ✅ タブがない場合、初期タブを作成
            } else {
                boolean found = false;
                for (int i = 0; i < tabs.size(); i++) {
                    if ((int) tabs.get(i).getTag() == currentTabId) {
                        currentTabIndex = i;
                        found = true;
                        break;
                    }
                }
                if (!found) currentTabIndex = 0;
            }
            switchToTab(currentTabIndex);
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    WebView webView = tabs.get(currentTabIndex);
                    updateUrlBar(webView);
                    // JavaScript を使用して Favicon を取得
                    webView.evaluateJavascript("(function() { " +
                            "var link = document.querySelector('link[rel~=\"icon\"]');" +
                            "return link ? link.href : ''; " +
                            "})()", new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            value = value.replace("\"", ""); // 取得した URL の " を削除
                            if (!value.isEmpty()) {
                                new DownloadFaviconTask().execute(value);
                            } else {
                                faviconImageView.setImageResource(R.drawable.transparent_vector); // デフォルトアイコン
                            }
                        }
                    });
                }
            }, 2500);

        } catch (JSONException e) {
            e.printStackTrace();
            addNewTab("file:///android_asset/index.html"); // ✅ JSONエラー時も初期タブを作成
        }
    }

    private WebView getCurrentWebView() {
        return tabs.get(currentTabIndex);
    }

    // ✅ WebViewの状態をファイルに保存
    private void saveBundleToFile(Bundle bundle, String fileName) {
        File file = new File(getFilesDir(), fileName);
        Parcel parcel = Parcel.obtain();
        try {
            bundle.writeToParcel(parcel, 0);
            byte[] bytes = parcel.marshall();
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(bytes);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            parcel.recycle();
        }
    }

    private void addBookmark() {
        String title = getCurrentWebView().getTitle();
        String url = getCurrentWebView().getUrl();
        if (title == null || title.isEmpty()) title = url;
        bookmarks.add(new Bookmark(title, url));
        // 保存：ブックマーク一覧を JSON に変換して保存
        JSONArray arr = new JSONArray();
        for (Bookmark bm : bookmarks) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("title", bm.getTitle());
                obj.put("url", bm.getUrl());
            } catch (JSONException e) { e.printStackTrace(); }
            arr.put(obj);
        }
        prefs.edit().putString(KEY_BOOKMARKS, arr.toString()).apply();
        Toast.makeText(this, "ブックマークを追加しました", Toast.LENGTH_SHORT).show();
    }

    public void exportBookmarksToFile() {
        final String bookmarksJson = prefs.getString(KEY_BOOKMARKS, "[]");
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!downloadDir.exists()) downloadDir.mkdirs();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        final File file;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            file = new File(downloadDir, "JSON-bookmark" + timeStamp + ".txt");
        } else {
            file = new File(downloadDir, timeStamp + "-bookmark.json");
        }
        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(bookmarksJson.getBytes("UTF-8"));
                    fos.flush();
                    fos.close();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "ブックマークをエクスポートしました: " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "ブックマークのエクスポートに失敗しました: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                    e.printStackTrace();
                }
            }
        });
    }

    private void copyLink(String link) {
        ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("リンク", link);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(MainActivity.this, "リンクをコピーしました", Toast.LENGTH_SHORT).show();
    }

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {
        private final List<HistoryItem> items;
        private final AlertDialog dialog;

        public HistoryAdapter(List<HistoryItem> items, AlertDialog dialog) {
            this.items = items;
            this.dialog = dialog;
        }

        @Override
        public HistoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
            return new HistoryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final HistoryViewHolder holder, int position) {
            final HistoryItem item = items.get(position);
            if (item.getTitle() != null && !item.getTitle().isEmpty()) {
                holder.title.setText(item.getTitle());
            } else {
                holder.title.setText(item.getUrl());
            }
            holder.url.setText(item.getUrl());
            Bitmap icon = faviconCache.get(item.getUrl());
            if (icon != null) {
                holder.favicon.setImageBitmap(icon);
            } else {
                holder.favicon.setImageResource(R.drawable.transparent_vector);
            }
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getCurrentWebView().loadUrl(item.getUrl());
                    dialog.dismiss();
                }
            });
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    final int currentPosition = holder.getAdapterPosition();
                    if (currentPosition == RecyclerView.NO_POSITION) {
                        return true;
                    }
                    final HistoryItem currentItem = items.get(currentPosition);
                    final String[] options = { "URLコピー", "削除" };
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("操作を選択")
                            .setItems(options, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int which) {
                                    if (which == 0) {
                                        copyLink(currentItem.getUrl());
                                    } else if (which == 1) {
                                        items.remove(currentPosition);
                                        notifyItemRemoved(currentPosition);
                                        saveHistory();
                                        Toast.makeText(MainActivity.this, "削除しました", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }).show();
                    return true;
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class HistoryViewHolder extends RecyclerView.ViewHolder {
            ImageView favicon;
            TextView title;
            TextView url;

            public HistoryViewHolder(View itemView) {
                super(itemView);
                favicon = itemView.findViewById(R.id.historyFavicon);
                title = itemView.findViewById(R.id.historyTitle);
                url = itemView.findViewById(R.id.historyUrl);
            }
        }
    }

    private class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.BookmarkViewHolder> {
        private final List<Bookmark> items;
        private final boolean managementMode;
        private final AlertDialog dialog;
        public BookmarkAdapter(List<Bookmark> items, boolean managementMode, AlertDialog dialog) {
            this.items = items;
            this.managementMode = managementMode;
            this.dialog = dialog;
        }
        @Override
        public BookmarkViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bookmark, parent, false);
            return new BookmarkViewHolder(view);
        }

        @Override
        public void onBindViewHolder(BookmarkViewHolder holder, int position) {
            Bookmark bm = items.get(position);
            holder.title.setText(bm.getTitle());
            holder.url.setText(bm.getUrl());
            Bitmap icon = faviconCache.get(bm.getUrl());
            if (icon != null) {
                holder.favicon.setImageBitmap(icon);
            } else {
                holder.favicon.setImageResource(R.drawable.transparent_vector);
            }
            holder.itemView.setOnClickListener(v -> {
                getCurrentWebView().loadUrl(bm.getUrl());
                dialog.dismiss();
            });
            if (managementMode) {
                holder.itemView.setOnLongClickListener(v -> {
                    int currentPosition = holder.getAdapterPosition();
                    if (currentPosition == RecyclerView.NO_POSITION) return true;
                    String[] options = {"編集", "削除"};
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("操作を選択")
                            .setItems(options, (dialogInterface, which) -> {
                                if (which == 0) {
                                    showEditBookmarkDialog(currentPosition, this);
                                } else if (which == 1) {
                                    items.remove(currentPosition);
                                    notifyItemRemoved(currentPosition);
                                    saveBookmarks();
                                    Toast.makeText(MainActivity.this, "削除しました", Toast.LENGTH_SHORT).show();
                                }
                            }).show();
                    return true;
                });
            }
        }
        @Override
        public int getItemCount() { return items.size(); }
        class BookmarkViewHolder extends RecyclerView.ViewHolder {
            ImageView favicon;
            TextView title;
            TextView url;
            public BookmarkViewHolder(View itemView) {
                super(itemView);
                favicon = itemView.findViewById(R.id.bookmarkFavicon);
                title = itemView.findViewById(R.id.bookmarkTitle);
                url = itemView.findViewById(R.id.bookmarkUrl);
            }
        }
    }


    // ✅ WebViewの状態をファイルから読み込み
    private Bundle loadBundleFromFile(String fileName) {
        File file = new File(getFilesDir(), fileName);
        if (!file.exists()) return null;

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            fis.read(bytes);
            Parcel parcel = Parcel.obtain();
            parcel.unmarshall(bytes, 0, bytes.length);
            parcel.setDataPosition(0);
            Bundle bundle = Bundle.CREATOR.createFromParcel(parcel);
            parcel.recycle();
            return bundle;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        WebView webView = tabs.get(currentTabIndex);
        // 戻ってきたときに実行したい処理
        saveTabsState();
        webView.onPause();
    }

    @Override
    public void onBackPressed() {
        if (customView != null) {
            ((WebChromeClient) webView.getWebChromeClient()).onHideCustomView();
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}