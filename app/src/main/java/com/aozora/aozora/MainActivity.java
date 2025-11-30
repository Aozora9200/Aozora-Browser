package com.aozora.aozora;

import android.Manifest;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.IntentFilter;
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
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.PointF;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Environment;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.util.LruCache;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.PixelCopy;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import androidx.annotation.Nullable;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.GeolocationPermissions;
import android.webkit.HttpAuthHandler;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
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
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.content.pm.PackageInfo;
import android.widget.ZoomControls;

import com.airbnb.lottie.LottieAnimationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.*;
import java.io.File;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Collections;
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

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;


public class MainActivity extends Activity implements HistoryAdapter.HistoryListener {

    private final List<HistoryItem> historyItems = new ArrayList<>();
    private final Map<String, Bitmap> faviconCaches = new HashMap<>();

    @Override
    public void onHistoryItemClick(String url) {
        getCurrentWebView().loadUrl(url);
    }

    @Override
    public void onHistoryItemNewTab(String url) {
        addNewTab(url);
    }

    @Override
    public void onHistoryDeleted() {
        saveHistory();
    }

    @Override
    public Bitmap getFavicon(String url) {
        return faviconCaches.get(url);
    }

    @Override
    public void copyToClipboard(String text) {
        copyLink(text);
    }

    @Override
    public void historySavebm(String url, String title) {
        savebm();
    }

    @Override
    public void historyUrlShare(String url) {
        if (url != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, url);
            startActivity(Intent.createChooser(shareIntent, "共有"));
        }
    }

    private Toast toast;
    private static final Pattern CACHE_MODE_PATTERN = Pattern.compile("(^|[/.])(?:(chatx2|chatx|chat|auth|login|disk|cgi|session|cloud))($|[/.])", Pattern.CASE_INSENSITIVE);
    private AutoCompleteTextView urlEditText;
    private WebView webView; // WebViewをクラス変数として定義
    private LinearLayout bottomBar, action_Bar, Bar, StatusBar;
    private TextView tabCountTextView, batteryStatus, sitename;
    private ImageView connectionStatus, wifi, forwardView, backView;
    private ZoomControls zoomButton;
    private DatabaseHelper dbHelper;
    private DBHelper dbH;
    private DBBM dbbm;
    private DBHistory dbHistory;
    private ImageButton backButton, forwardButton, bmbutton, reloadButton, hideBottomButton, showBottomButton, donttouch, popupbutton;
    private FrameLayout webViewContainer;
    private ProgressDialog progressDialog, progressResetDialog;
    private ProgressBar progressBar, pageloading;
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

    public ArrayList<WebView> tabs = new ArrayList<>();
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
    private ImageButton faviconImageView;
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
    private AlertDialog dialog;
    // 選択されたURLとタイプを保持
    private String selectedUrl;
    private int selectedType;

    private static final int MENU_ID_SETTING = 5;
    private static final int MENU_ID_CLOSE = 6;
    private static final String GITHUB_API_URL =
            "https://api.github.com/repos/Aozora9200/Aozora-Browser/releases/latest";
    private boolean isLoading = false; // ページ読み込み中かどうか
    private boolean isNewTab = false;

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private BroadcastReceiver batteryReceiver;

    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;

    private ColorFilter invertFilter, grayFilter;

    private static final String PREFS_NAME = "theme_prefs";
    private static final String POPUP_PREFS_NAME = "popup_prefs";
    private static final String ARROW_PREFS_NAME = "arrow_prefs";
    private static final String KEY_THEME = "selected_theme";
    private static final String KEY_POPUP = "selected_popupbutton";
    private static final String KEY_ARROW = "selected_arrowbutton";

    private static final int THEME_LIGHT = 0;
    private static final int THEME_DARK = 1;
    private static final int THEME_SYSTEM = 2;

    private static final int POPUPBUTTON_LIGHT = 0;
    private static final int POPUPBUTTON_DARK = 1;
    private static final int POPUPBUTTON_SYSTEM = 2;

    private static final int ARROWBUTTON_OLD = 0;
    private static final int ARROWBUTTON_NEW = 1;
    private static final int ARROWBUTTON_IOS = 2;

    private static final String SENTINEL_FILENAME = "cache_sentinel.txt";

    // 重要: フィールドとして保持
    private ViewPager2 listViewPager;
    private TabSnapshotAdapter listAdapter;
    private AlertDialog TabDialog;

    private final Map<WebView, Bitmap> tabSnapshots = new HashMap<>();

    private boolean isUrlBarVisible = true;
    private int lastScrollY = 0;

    private boolean nohideurl = false;
    private boolean loadTabnoHideurl = false;

    private static final int REQUEST_HISTORY = 1002;
    private boolean noUpdateUrl = false;

    private long lastBackPressedTime = 0; // 最後にBackキーが押された時間

    private boolean dialogShown = false; // 重複表示防止

    private PopupWindow currentPopupWindow;

    private FrameLayout effectLayer; // エフェクト用レイヤー
    private long lastTouchTime = 0;
    private static final long TRAIL_INTERVAL = 0;

    private static final int REQUEST_LOCATION = 1;
    private SharedPreferences geoPrefs;

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

    public static class HistoryItem implements java.io.Serializable {
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
        applySavedTheme();

        setContentView(R.layout.activity_main);
        effectLayer = findViewById(R.id.effectLayer);
        urlEditText = findViewById(R.id.urlEditText);
        backButton = findViewById(R.id.backButton);
        forwardButton = findViewById(R.id.forwardButton);
        webViewContainer = findViewById(R.id.webViewContainer);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        faviconImageView = (ImageButton) findViewById(R.id.favicon);
        progressBar = findViewById(R.id.progressBar);
        tabCountTextView = (TextView) findViewById(R.id.tabCountTextView);
        // ImageButton tabButton = findViewById(R.id.action_tab);
        reloadButton = findViewById(R.id.action_reload);
        ImageButton bmbutton = findViewById(R.id.action_bookmark);
        popupbutton = findViewById(R.id.action_popup);
        connectionStatus = findViewById(R.id.network);
        batteryStatus = findViewById(R.id.batteryStatus);
        StatusBar = findViewById(R.id.statusbar);
        pageloading = findViewById(R.id.pageloading);
        sitename = findViewById(R.id.sitename);
        hideBottomButton = findViewById(R.id.hidebottom);
        showBottomButton = findViewById(R.id.showbottom);
        zoomButton = findViewById(R.id.simpleZoomControl);
        donttouch = findViewById(R.id.donttouch);
        forwardView = findViewById(R.id.forwardView);
        backView = findViewById(R.id.backView);

        applySavedPopup();
        applySavedArrow();

        showBottomButton.setVisibility(View.GONE);
        donttouch.setVisibility(View.GONE);

        geoPrefs = getSharedPreferences("GeoPermissionStore", MODE_PRIVATE);

        SharedPreferences setupprefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isFirstRun = setupprefs.getBoolean("isFirstRun", true);

        if (isFirstRun) {
            // 初回起動 → セットアップ画面へ
            Intent intent = new Intent(this, SetupActivity.class);
            startActivity(intent);
            finish(); // MainActivityを閉じてセットアップから開始
        }

        wifi = findViewById(R.id.wifi);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            // API 24 以上のときだけ実行する処理
            connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    runOnUiThread(() -> updateConnectionStatus());
                }

                @Override
                public void onLost(Network network) {
                    runOnUiThread(() -> showDisconnected());
                }

                @Override
                public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                    runOnUiThread(() -> updateConnectionStatus());
                }
            };
        } else {
            // API23以下（Android 6.0以下）のときの処理
            wifi.setVisibility(View.GONE);
            connectionStatus.setVisibility(View.GONE);
        }

        // 色反転フィルタを準備
        ColorMatrix colorMatrix_Invert = new ColorMatrix(new float[] {
                -1,  0,  0,  0, 255, // R
                0, -1,  0,  0, 255, // G
                0,  0, -1,  0, 255, // B
                0,  0,  0,  1,   0  // A
        });
        ColorMatrix colorMatrix_Gray75 = new ColorMatrix(new float[] {
                0, 0, 0, 0, 107, // R
                0, 0, 0, 0, 107, // G
                0, 0, 0, 0, 107, // B
                0, 0, 0, 1,   0  // A
        });
        invertFilter = new ColorMatrixColorFilter(colorMatrix_Invert);
        grayFilter = new ColorMatrixColorFilter(colorMatrix_Gray75);

        applyThemeToIcon(); // 起動時に反映

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

        basicAuthEnabled = true;
        zoomEnabled = true;

        // インテントで渡された URL を取得
        String url = getIntent().getStringExtra("url");

        // ボタンにクリックイベントを設定
        //tabButton.setOnClickListener(v ->
        //        showTabMenu()
        //);

        tabCountTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WebView webView = tabs.get(currentTabIndex);
                captureTabSnapshot(webView);
                showTabMenu();
            }
        });

        hideBottomButton.setOnClickListener(v ->
                hideBottomBar()
        );

        showBottomButton.setOnClickListener(v ->
                showBottomBar()
        );

        // バッテリー残量監視
        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int batteryPct = (int) ((level / (float) scale) * 100);

                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                String chargeStatus;
                if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
                    chargeStatus = "+";
                } else if (status == BatteryManager.BATTERY_STATUS_FULL) {
                    chargeStatus = "+";
                } else {
                    chargeStatus = "";
                }

                if (level >= 0 && scale > 0) {
                    //充電状態をチェック
                    boolean isCharging =
                            status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                    status == BatteryManager.BATTERY_STATUS_FULL;

                    //20％以下 & 未充電時のみダイアログ表示
                    if (batteryPct <= 20 && !isCharging && !dialogShown) {
                        SharedPreferences setupprefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                        boolean batteryalert = setupprefs.getBoolean("batteryalert", true);
                        if (batteryalert) {
                            showBatteryLowDialog(batteryPct);
                        }
                        dialogShown = true;
                    } else if (batteryPct > 20 || isCharging) {
                        //条件から外れたら再度出せるようにリセット
                        dialogShown = false;
                    }
                }

                batteryStatus.setText(chargeStatus + batteryPct + "%");
            }
        };

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            // API 24 以上のときだけ実行する処理
            // モバイル電波強度リスナー
            phoneStateListener = new PhoneStateListener() {
                @Override
                public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                    super.onSignalStrengthsChanged(signalStrength);
                    int level = signalStrength.getLevel(); // 0〜4
                    updateSignalIcon(level, false);
                }
            };
        }

        pageloading.setVisibility(View.INVISIBLE);
        zoomButton.setVisibility(View.GONE);

        reloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isLoading) {
                    webView.stopLoading(); // 読み込み中止
                    isLoading = false;
                    reloadButton.setImageResource(R.drawable.reload);
                } else {
                    webView.reload(); // リロード
                }
            }
        });

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

        faviconImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSiteDialog();
            }
        });

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

        boolean isSwipeReload = setupprefs.getBoolean("isSwipeReload", true);
        swipeRefreshLayout.setEnabled(isSwipeReload);

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

                loadUrlInCurrentTab(selectedSuggestion);

                // WebViewに読み込ませる
                //load(searchUrl);

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
                newStartPage();

                // 初回起動済みを保存
                SharedPreferences.Editor editor = pref.edit();
                editor.putBoolean("isFirstLaunch", false);
                editor.apply();
            } else {
                newStartPage();
            }
        }

        updateTabCount();

        boolean isUpdate = setupprefs.getBoolean("updateDialog", true);
        if (isUpdate) {
            new CheckUpdateTask().execute();
        }

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
        String action = intent.getAction();
        Uri uri = intent.getData();

        if (Intent.ACTION_VIEW.equals(action) && uri != null) {
            addNewTab(uri.toString());
        } else {
            return;
        }

        //if (uri != null) {
        //    if ("file".equals(uri.getScheme()) || "content".equals(uri.getScheme())) {
        //        addNewTab(uri.toString()); // WebViewが自動で読み込み
        //    } else {
        //        Toast.makeText(this, "対応していない URI 形式です。", Toast.LENGTH_SHORT).show();
        //    }
        //}
        if (url != null && !url.isEmpty()) {
            // URL が渡された場合は WebView で開く
            load(url);
        } else {
            return;
        }

    }

    private void showEffect(PointF point, float scale, float speed) {
        LottieAnimationView lottie = new LottieAnimationView(this);
        lottie.setAnimation("tap_effect.json"); // assets に配置
        lottie.setRepeatCount(0);
        lottie.setSpeed(speed);

        int size = (int) (300 * scale);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(size, size);
        lp.leftMargin = (int) (point.x - size / 2f);
        lp.topMargin = (int) (point.y - size / 2f);
        effectLayer.addView(lottie, lp);

        lottie.playAnimation();

        lottie.addAnimatorUpdateListener(animation -> {
            if (animation.getAnimatedFraction() >= 1.0f) {
                effectLayer.removeView(lottie);
            }
        });
    }

    private void showBatteryLowDialog(int batteryPct) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.battery_low, null);

        TextView percent = view.findViewById(R.id.level_percent);
        percent.setText("残り" + batteryPct + "%");

        new AlertDialog.Builder(this)
                .setTitle("充電してください")
                .setView(view)
                .setPositiveButton("OK", null)
                .setCancelable(true)
                .show();
    }

    private void startPage() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int theme = prefs.getInt(KEY_THEME, THEME_SYSTEM);

        switch (theme) {
            case THEME_LIGHT:
                load("file:///android_asset/index.html");
                break;

            case THEME_DARK:
                load("file:///android_asset/index.html");
                break;

            case THEME_SYSTEM:
            default:
                // OS 側の設定に従う
                int nightModeFlags = getResources().getConfiguration().uiMode
                        & android.content.res.Configuration.UI_MODE_NIGHT_MASK;

                if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                    load("file:///android_asset/index.html");
                } else {
                    load("file:///android_asset/index.html");
                }
                break;
        }
    }

    private void newStartPage() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int theme = prefs.getInt(KEY_THEME, THEME_SYSTEM);

        switch (theme) {
            case THEME_LIGHT:
                addNewTab("file:///android_asset/index.html");
                break;

            case THEME_DARK:
                addNewTab("file:///android_asset/index.html");
                break;

            case THEME_SYSTEM:
            default:
                // OS 側の設定に従う
                int nightModeFlags = getResources().getConfiguration().uiMode
                        & android.content.res.Configuration.UI_MODE_NIGHT_MASK;

                if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                    addNewTab("file:///android_asset/index.html");
                } else {
                    addNewTab("file:///android_asset/index.html");
                }
                break;
        }
    }

    private void animateTopMargin(int from, int to) {
        ValueAnimator animator = ValueAnimator.ofInt(from, to);
        animator.setDuration(200);
        animator.addUpdateListener(animation -> {
            int margin = (int) animation.getAnimatedValue();
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) swipeRefreshLayout.getLayoutParams();
            lp.topMargin = margin;
            swipeRefreshLayout.setLayoutParams(lp);
        });
        animator.start();
    }

    private void animateBottomMargin(int from, int to) {
        ValueAnimator animator = ValueAnimator.ofInt(from, to);
        animator.setDuration(200);
        animator.addUpdateListener(animation -> {
            int margin = (int) animation.getAnimatedValue();
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) swipeRefreshLayout.getLayoutParams();
            lp.bottomMargin = margin;
            swipeRefreshLayout.setLayoutParams(lp);
        });
        animator.start();
    }

    private void checkHideUrlBar() {
        if (!nohideurl) {
            hideUrlBar();
        }
    }

    private void hideBottomBar() {
        int bottombar = bottomBar.getHeight();
        bottomBar.animate().translationY(bottomBar.getHeight()).setDuration(200).start();
        animateBottomMargin(bottombar, 0);
        Animation anim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fade);
        showBottomButton.startAnimation(anim);
        showBottomButton.setVisibility(View.VISIBLE);
    }

    private void showBottomBar() {
        int bottombar = bottomBar.getHeight();
        bottomBar.animate().translationY(0).setDuration(200).start();
        animateBottomMargin(0, bottombar);
        Animation anim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fadeout);
        showBottomButton.startAnimation(anim);
        showBottomButton.setVisibility(View.GONE);
    }

    private void hideUrlBar() {
        if (!isUrlBarVisible) return;
        SharedPreferences setupprefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean hidebottom = setupprefs.getBoolean("hidebottom", false);

        int topbar = (action_Bar.getHeight())+(StatusBar.getHeight());

        action_Bar.animate().translationY(-((action_Bar.getHeight())+(StatusBar.getHeight()))).setDuration(200).start();
        StatusBar.animate().translationY(-StatusBar.getHeight()).setDuration(200).start();
        if (hidebottom) {
            bottomBar.animate().translationY(bottomBar.getHeight()).setDuration(200).start();
        }
        progressBar.animate().translationY(-((action_Bar.getHeight())+(StatusBar.getHeight()))).setDuration(200).start();
        boolean zoombutton = setupprefs.getBoolean("zoomButton", true);
        if (zoombutton) {
            Animation anim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fade);
            zoomButton.startAnimation(anim);
            zoomButton.setVisibility(View.VISIBLE);
        }
        animateTopMargin(topbar, 0);
        if (hidebottom) {
            animateBottomMargin(bottomBar.getHeight(), 0);
            Animation anim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fade);
            showBottomButton.startAnimation(anim);
            showBottomButton.setVisibility(View.VISIBLE);
        }

        isUrlBarVisible = false;
    }

    private void showUrlBar() {
        if (isUrlBarVisible) return;
        SharedPreferences setupprefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean hidebottom = setupprefs.getBoolean("hidebottom", false);

        int topbar = (action_Bar.getHeight())+(StatusBar.getHeight())+(progressBar.getHeight());

        action_Bar.animate().translationY(0).setDuration(200).start();
        StatusBar.animate().translationY(0).setDuration(200).start();
        if (hidebottom) {
            bottomBar.animate().translationY(0).setDuration(200).start();
        }
        progressBar.animate().translationY(0).setDuration(200).start();
        boolean zoombutton = setupprefs.getBoolean("zoomButton", true);
        if (zoombutton) {
            Animation anim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fadeout);
            zoomButton.startAnimation(anim);
            zoomButton.setVisibility(View.GONE);
        }
        zoomButton.setVisibility(View.GONE);
        animateTopMargin(0, topbar);
        if (hidebottom) {
            animateBottomMargin(0, bottomBar.getHeight());
            Animation anim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fadeout);
            showBottomButton.startAnimation(anim);
            showBottomButton.setVisibility(View.GONE);
        }

        isUrlBarVisible = true;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void applySavedTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int theme = prefs.getInt(KEY_THEME, THEME_SYSTEM);

        switch (theme) {
            case THEME_LIGHT:
                setTheme(android.R.style.Theme_Holo_Light_NoActionBar_Fullscreen);
                break;
            case THEME_DARK:
                setTheme(android.R.style.Theme_Holo_NoActionBar_Fullscreen);
                break;
            case THEME_SYSTEM:
            default:

                break;
        }
    }

    private void applySavedPopup() {
        SharedPreferences prefs = getSharedPreferences(POPUP_PREFS_NAME, MODE_PRIVATE);
        int popup = prefs.getInt(KEY_POPUP, POPUPBUTTON_SYSTEM);
        SharedPreferences themeprefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int theme = themeprefs.getInt(KEY_THEME, THEME_SYSTEM);

        switch (popup) {
            case POPUPBUTTON_LIGHT:
                popupbutton.setImageResource(R.mipmap.aozora1);
                break;
            case POPUPBUTTON_DARK:
                popupbutton.setImageResource(R.mipmap.aozora);
                break;
            case POPUPBUTTON_SYSTEM:
            default:
                switch (theme) {
                    case THEME_LIGHT:
                        popupbutton.setImageResource(R.mipmap.aozora1);
                        break;
                    case THEME_DARK:
                        popupbutton.setImageResource(R.mipmap.aozora);
                        break;
                    case THEME_SYSTEM:
                    default:
                        // OS 側の設定に従う
                        int nightModeFlags = getResources().getConfiguration().uiMode
                                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;

                        if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                            popupbutton.setImageResource(R.mipmap.aozora);
                        } else {
                            popupbutton.setImageResource(R.mipmap.aozora1);
                        }
                        break;
                }
                break;
        }
    }

    private void applySavedArrow() {
        SharedPreferences prefs = getSharedPreferences(ARROW_PREFS_NAME, MODE_PRIVATE);
        int theme = prefs.getInt(KEY_ARROW, ARROWBUTTON_NEW);

        switch (theme) {
            case ARROWBUTTON_OLD:
                backButton.setImageResource(R.drawable.back);
                forwardButton.setImageResource(R.drawable.forward);
                break;
            case ARROWBUTTON_IOS:
                backButton.setImageResource(R.drawable.ios_back);
                forwardButton.setImageResource(R.drawable.ios_forward);
                break;
            case ARROWBUTTON_NEW:
            default:
                backButton.setImageResource(R.drawable.ic_sysbar_back);
                forwardButton.setImageResource(R.drawable.ic_sysbar_forward);
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void updateSignalIcon(int level, boolean isWifi) {
        int resId;
        switch (level) {
            case 4:
                resId = isWifi ? R.drawable.ic_qs_wifi_full_4 : R.drawable.ic_qs_signal_full_4;
                break;
            case 3:
                resId = isWifi ? R.drawable.ic_qs_wifi_full_3 : R.drawable.ic_qs_signal_full_3;
                break;
            case 2:
                resId = isWifi ? R.drawable.ic_qs_wifi_full_2 : R.drawable.ic_qs_signal_full_2;
                break;
            case 1:
                resId = isWifi ? R.drawable.ic_qs_wifi_full_1 : R.drawable.ic_qs_signal_full_1;
                break;
            default:
                resId = isWifi ? R.drawable.ic_qs_wifi_0 : R.drawable.ic_qs_signal_full_0;
                break;
        }
        connectionStatus.setImageResource(resId);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void updateWifiIcon(int level, boolean isWifi) {
        int resId;
        switch (level) {
            case 4:
                resId = isWifi ? R.drawable.ic_qs_wifi_full_4 : R.drawable.ic_qs_signal_full_4;
                break;
            case 3:
                resId = isWifi ? R.drawable.ic_qs_wifi_full_3 : R.drawable.ic_qs_signal_full_3;
                break;
            case 2:
                resId = isWifi ? R.drawable.ic_qs_wifi_full_2 : R.drawable.ic_qs_signal_full_2;
                break;
            case 1:
                resId = isWifi ? R.drawable.ic_qs_wifi_full_1 : R.drawable.ic_qs_signal_full_1;
                break;
            default:
                resId = isWifi ? R.drawable.ic_qs_wifi_0 : R.drawable.ic_qs_signal_full_0;
                break;
        }
        wifi.setImageResource(resId);
    }

    private void debug() {
        new AlertDialog.Builder(this)
                .setTitle("ℹ\uFE0F デバッグ")
                .setMessage("デフォルトのブラウザに設定(Test)")
                .setNegativeButton("設定", (dialog, which) -> defaultsetting())
                .setPositiveButton("閉じる", null)
                .show();
    }

    private void defaultsetting() {
        Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
        startActivity(intent);
    }

    private void applyThemeToIcon() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int theme = prefs.getInt(KEY_THEME, THEME_SYSTEM);
        int color = getResources().getColor(R.color.progresswhite);
        int colordark = getResources().getColor(R.color.progressblack);
        int darkcolor = getResources().getColor(R.color.black);
        int whitetext = getResources().getColor(R.color.white);
        int whitecolor = getResources().getColor(R.color.white);
        int darktext = getResources().getColor(R.color.textdark);
        bottomBar = findViewById(R.id.bottomBar);

        switch (theme) {
            case THEME_LIGHT:
                // ユーザーがライトモード強制
                connectionStatus.setColorFilter(invertFilter);
                wifi.setColorFilter(invertFilter);
                progressBar.setBackgroundColor(color);
                bottomBar.setBackgroundColor(whitecolor);
                backButton.setColorFilter(grayFilter);
                forwardButton.setColorFilter(grayFilter);
                reloadButton.setColorFilter(grayFilter);
                hideBottomButton.setColorFilter(grayFilter);
                tabCountTextView.setTextColor(darktext);
                break;

            case THEME_DARK:
                // ユーザーがダークモード強制
                connectionStatus.clearColorFilter();
                wifi.clearColorFilter();
                progressBar.setBackgroundColor(colordark);
                bottomBar.setBackgroundColor(darkcolor);
                backButton.clearColorFilter();
                forwardButton.clearColorFilter();
                reloadButton.clearColorFilter();
                hideBottomButton.clearColorFilter();
                tabCountTextView.setTextColor(whitetext);
                break;

            case THEME_SYSTEM:
            default:
                // OS 側の設定に従う
                int nightModeFlags = getResources().getConfiguration().uiMode
                        & android.content.res.Configuration.UI_MODE_NIGHT_MASK;

                if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                    connectionStatus.clearColorFilter(); // ダーク → 元色
                    wifi.clearColorFilter();
                    progressBar.setBackgroundColor(colordark);
                    bottomBar.setBackgroundColor(darkcolor);
                    backButton.clearColorFilter();
                    forwardButton.clearColorFilter();
                    reloadButton.clearColorFilter();
                    hideBottomButton.clearColorFilter();
                    tabCountTextView.setTextColor(whitetext);
                } else {
                    connectionStatus.setColorFilter(invertFilter); // ライト → 反転
                    wifi.setColorFilter(invertFilter);
                    progressBar.setBackgroundColor(color);
                    bottomBar.setBackgroundColor(whitecolor);
                    backButton.setColorFilter(grayFilter);
                    forwardButton.setColorFilter(grayFilter);
                    reloadButton.setColorFilter(grayFilter);
                    hideBottomButton.setColorFilter(grayFilter);
                    tabCountTextView.setTextColor(darktext);
                }
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void updateConnectionStatus() {
        if (connectivityManager == null) {
            connectionStatus.setVisibility(View.INVISIBLE);
            return;
        }
        if (connectivityManager != null) {
            connectionStatus.setVisibility(View.VISIBLE);
        }

        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) {
            showDisconnected();
            return;
        }

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        if (capabilities == null) {
            showDisconnected();
            return;
        }

        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            wifi.setImageResource(R.drawable.ic_qs_wifi_0);
            updateWifiSignal();
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            connectionStatus.setImageResource(R.drawable.ic_qs_signal_full_0);
            // モバイル電波強度は PhoneStateListener が自動更新
            // すぐに現在のモバイル電波強度を取得できるようにリスナーを一時的にトリガー
            if (telephonyManager != null) {
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
            }
        } else {
            connectionStatus.setImageResource(R.drawable.ic_qs_signal_4);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void showDisconnected() {
        connectionStatus.setVisibility(View.INVISIBLE);
        wifi.setImageResource(View.GONE);
        if (connectivityManager != null) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
        }
        applyThemeToIcon();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void updateWifiSignal() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int rssi = wifiInfo.getRssi();
            int level = WifiManager.calculateSignalLevel(rssi, 5); // 0〜4
            if (level == 0) {
                Animation anim = AnimationUtils.loadAnimation(this, R.anim.slide_out_down);
                wifi.startAnimation(anim);
                wifi.setVisibility(View.INVISIBLE);
            } else {
                Animation anim = AnimationUtils.loadAnimation(this, R.anim.slide_in_up);
                wifi.startAnimation(anim);
                wifi.setVisibility(View.VISIBLE);
            }
            updateWifiIcon(level, true);
        }
    }

    private void showCertificateDialog() {
        WebView webView = tabs.get(currentTabIndex);
        SslCertificate cert = webView.getCertificate();
        if (cert == null) {
            new AlertDialog.Builder(this)
                    .setTitle("\uD83E\uDEAA セキュリティ証明書")
                    .setMessage("証明書情報は、このサイトでは利用できません。")
                    .setPositiveButton("OK", (dialog, which) -> showSiteDialog())
                    .show();
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_certificate_info, null);

        TextView tvIssuedToCN = dialogView.findViewById(R.id.tvIssuedToCN);
        TextView tvIssuedToO  = dialogView.findViewById(R.id.tvIssuedToO);
        TextView tvIssuedToOU = dialogView.findViewById(R.id.tvIssuedToOU);

        TextView tvIssuedByCN = dialogView.findViewById(R.id.tvIssuedByCN);
        TextView tvIssuedByO  = dialogView.findViewById(R.id.tvIssuedByO);
        TextView tvIssuedByOU = dialogView.findViewById(R.id.tvIssuedByOU);

        TextView tvValidFrom  = dialogView.findViewById(R.id.tvValidFrom);
        TextView tvValidUntil = dialogView.findViewById(R.id.tvValidUntil);

        TextView tvSHA256     = dialogView.findViewById(R.id.tvSHA256);
        TextView tvSHA1       = dialogView.findViewById(R.id.tvSHA1);

        // 日付フォーマット
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());

        // SslCertificate から文字列を取得
        SslCertificate.DName to = cert.getIssuedTo();
        SslCertificate.DName by = cert.getIssuedBy();

        tvIssuedToCN.setText("共通名: " + to.getCName());
        tvIssuedToO.setText("組織: " + to.getOName());
        tvIssuedToOU.setText("組織単位: " + to.getUName());

        tvIssuedByCN.setText("共通名: " + by.getCName());
        tvIssuedByO.setText("組織: " + by.getOName());
        tvIssuedByOU.setText("組織単位: " + by.getUName());

        tvValidFrom.setText("発行: " + sdf.format(cert.getValidNotBeforeDate()));
        tvValidUntil.setText("有効期限: " + sdf.format(cert.getValidNotAfterDate()));

        boolean trusted = true;

        // X509Certificate に変換してフィンガープリント計算
        try {
            byte[] der = SslCertificate.saveState(cert).getByteArray("x509-certificate");
            if (der != null) {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                X509Certificate x509 = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(der));

                tvSHA256.setText("SHA-256指紋: " + getFingerprint(x509, "SHA-256"));
                tvSHA1.setText("SHA-1指紋: " + getFingerprint(x509, "SHA-1"));

                trusted = isCertificateTrusted(x509);
            }
        } catch (Exception e) {
            e.printStackTrace();
            trusted = false;
        }

        if (!trusted) {
            // 信頼できない証明書の場合、別の処理へ
            showUntrustedCertificateDialog();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("\uD83E\uDEAA セキュリティ証明書")
                .setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> showSiteDialog())
                .show();
    }

    private void showCertificateDialogUrl(String urlString) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                URL url = new URL(urlString);
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

                // ▼ 信頼チェックをスキップする SSLContext を作成
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                    @Override
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                }}, new SecureRandom());

                connection.setSSLSocketFactory(sslContext.getSocketFactory());
                connection.connect();

                Certificate[] certs = connection.getServerCertificates();
                if (certs == null || certs.length == 0) {
                    runOnUiThread(this::showNoCertificateDialog);
                    return;
                }

                X509Certificate x509 = (X509Certificate) certs[0];
                connection.disconnect();

                runOnUiThread(() -> showCertificateInfoDialogFromX509(x509));

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(this::showNoCertificateDialog);
            }
        });
    }

    private void showNoCertificateDialog() {
        new AlertDialog.Builder(this)
                .setTitle("\uD83E\uDEAA セキュリティ証明書")
                .setMessage("証明書情報は、このサイトでは利用できません。")
                .setPositiveButton("OK", (dialog, which) -> showSiteDialog())
                .show();
    }

    private void showCertificateInfoDialogFromX509(X509Certificate x509) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_certificate_info_failed, null);

        TextView tvIssuedToCN = dialogView.findViewById(R.id.tvIssuedToCN);
        TextView tvIssuedToO  = dialogView.findViewById(R.id.tvIssuedToO);
        TextView tvIssuedToOU = dialogView.findViewById(R.id.tvIssuedToOU);

        TextView tvIssuedByCN = dialogView.findViewById(R.id.tvIssuedByCN);
        TextView tvIssuedByO  = dialogView.findViewById(R.id.tvIssuedByO);
        TextView tvIssuedByOU = dialogView.findViewById(R.id.tvIssuedByOU);

        TextView tvValidFrom  = dialogView.findViewById(R.id.tvValidFrom);
        TextView tvValidUntil = dialogView.findViewById(R.id.tvValidUntil);

        TextView tvSHA256     = dialogView.findViewById(R.id.tvSHA256);
        TextView tvSHA1       = dialogView.findViewById(R.id.tvSHA1);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());

        // 発行先（Issued To）
        tvIssuedToCN.setText("共通名: " + x509.getSubjectX500Principal().getName());
        tvIssuedToO.setText("組織: " + getFieldFromDN(x509.getSubjectX500Principal().getName(), "O"));
        tvIssuedToOU.setText("組織単位: " + getFieldFromDN(x509.getSubjectX500Principal().getName(), "OU"));

        // 発行元（Issued By）
        tvIssuedByCN.setText("共通名: " + x509.getIssuerX500Principal().getName());
        tvIssuedByO.setText("組織: " + getFieldFromDN(x509.getIssuerX500Principal().getName(), "O"));
        tvIssuedByOU.setText("組織単位: " + getFieldFromDN(x509.getIssuerX500Principal().getName(), "OU"));

        // 有効期限
        tvValidFrom.setText("発行: " + sdf.format(x509.getNotBefore()));
        tvValidUntil.setText("有効期限: " + sdf.format(x509.getNotAfter()));

        // フィンガープリント
        try {
            tvSHA256.setText("SHA-256指紋: " + getFingerprint(x509, "SHA-256"));
            tvSHA1.setText("SHA-1指紋: " + getFingerprint(x509, "SHA-1"));
        } catch (Exception e) {
            tvSHA256.setText("SHA-256指紋: 取得失敗");
            tvSHA1.setText("SHA-1指紋: 取得失敗");
        }

        new AlertDialog.Builder(this)
                .setTitle("❌️ セキュリティ証明書")
                .setView(dialogView)
                .setPositiveButton("OK", null)
                .show();
    }

    private String getFieldFromDN(String dn, String field) {
        String[] tokens = dn.split(",");
        for (String token : tokens) {
            String[] kv = token.trim().split("=");
            if (kv.length == 2 && kv[0].equalsIgnoreCase(field)) {
                return kv[1];
            }
        }
        return "";
    }

    private boolean isCertificateTrusted(X509Certificate cert) {
        try {
            // デフォルトのTrustManagerを取得
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null);

            for (TrustManager tm : tmf.getTrustManagers()) {
                if (tm instanceof X509TrustManager) {
                    try {
                        ((X509TrustManager) tm).checkServerTrusted(new X509Certificate[]{cert}, "RSA");
                        return true; // 例外が出なければ信頼済み
                    } catch (CertificateException e) {
                        return false; // 検証失敗
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void showUntrustedCertificateDialog() {
        WebView webView = tabs.get(currentTabIndex);
        SslCertificate cert = webView.getCertificate();
        if (cert == null) {
            new AlertDialog.Builder(this)
                    .setTitle("\uD83E\uDEAA セキュリティ証明書")
                    .setMessage("証明書情報は, このサイトでは利用できません。")
                    .setPositiveButton("OK", (dialog, which) -> showSiteDialog())
                    .show();
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_certificate_info_failed, null);

        TextView tvIssuedToCN = dialogView.findViewById(R.id.tvIssuedToCN);
        TextView tvIssuedToO  = dialogView.findViewById(R.id.tvIssuedToO);
        TextView tvIssuedToOU = dialogView.findViewById(R.id.tvIssuedToOU);

        TextView tvIssuedByCN = dialogView.findViewById(R.id.tvIssuedByCN);
        TextView tvIssuedByO  = dialogView.findViewById(R.id.tvIssuedByO);
        TextView tvIssuedByOU = dialogView.findViewById(R.id.tvIssuedByOU);

        TextView tvValidFrom  = dialogView.findViewById(R.id.tvValidFrom);
        TextView tvValidUntil = dialogView.findViewById(R.id.tvValidUntil);

        TextView tvSHA256     = dialogView.findViewById(R.id.tvSHA256);
        TextView tvSHA1       = dialogView.findViewById(R.id.tvSHA1);

        // 日付フォーマット
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());

        // SslCertificate から文字列を取得
        SslCertificate.DName to = cert.getIssuedTo();
        SslCertificate.DName by = cert.getIssuedBy();

        tvIssuedToCN.setText("共通名: " + to.getCName());
        tvIssuedToO.setText("組織: " + to.getOName());
        tvIssuedToOU.setText("組織単位: " + to.getUName());

        tvIssuedByCN.setText("共通名: " + by.getCName());
        tvIssuedByO.setText("組織: " + by.getOName());
        tvIssuedByOU.setText("組織単位: " + by.getUName());

        tvValidFrom.setText("発行: " + sdf.format(cert.getValidNotBeforeDate()));
        tvValidUntil.setText("有効期限: " + sdf.format(cert.getValidNotAfterDate()));

        // X509Certificate に変換してフィンガープリント計算
        try {
            byte[] der = SslCertificate.saveState(cert).getByteArray("x509-certificate");
            if (der != null) {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                X509Certificate x509 = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(der));

                tvSHA256.setText("SHA-256指紋: " + getFingerprint(x509, "SHA-256"));
                tvSHA1.setText("SHA-1指紋: " + getFingerprint(x509, "SHA-1"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        new AlertDialog.Builder(this)
                .setTitle("❌️ セキュリティ証明書")
                .setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> showSiteDialog())
                .show();
    }

    // faviconボタンのクリック時に呼び出すメソッド
    private void showSiteDialog() {
        WebView webView = tabs.get(currentTabIndex);
        String url = webView.getUrl();
        if (url == null) return;

        Uri uri = Uri.parse(url);
        String host = uri.getHost();
        if (host == null) host = url;

        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_site_info, null);

        TextView siteTitle = dialogView.findViewById(R.id.siteTitle);
        TextView siteUrl = dialogView.findViewById(R.id.siteUrl);

        if (url != null && (
                url.equals("file:///android_asset/index.html") ||
                        url.equals("file:///android_asset/help.html") ||
                        url.equals("file:///android_asset/index_white.html") ||
                                url.equals("file:///android_asset/error.html")
        )) {
            siteTitle.setText("ブラウザ");
            siteUrl.setText(""); // URLを非表示
        } else {
            siteTitle.setText(host);
            siteUrl.setText(url); // 通常のURLを表示
        }

        // URL長押しでコピー
        siteUrl.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("URL", url);
                cm.setPrimaryClip(clip);
                Toast.makeText(MainActivity.this, "コピーしました.", Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        if (url != null && (
                url.equals("file:///android_asset/index.html") ||
                        url.equals("file:///android_asset/help.html") ||
                                url.equals("file:///android_asset/index_white.html")
        )) {
            new AlertDialog.Builder(this)
                    .setTitle("ℹ\uFE0F ページ情報")
                    .setView(dialogView)
                    .setPositiveButton("閉じる", null)
                    .show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("ℹ\uFE0F ページ情報")
                    .setView(dialogView)
                    .setNegativeButton("セキュリティ証明書を表示", (dialog, which) -> showCertificateDialog())
                    .setPositiveButton("閉じる", null)
                    .show();
        }
    }

    private String getFingerprint(X509Certificate cert, String algorithm) throws Exception {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        byte[] der = cert.getEncoded();
        byte[] digest = md.digest(der);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digest.length; i++) {
            sb.append(String.format("%02X", digest[i]));
            if (i < digest.length - 1) sb.append(":");
        }
        return sb.toString();
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
                        addNewTab("https://github.com/Aozora9200/Aozora-Browser/releases/latest");
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

    public void updateTabCount() {
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
            zoomButton.setVisibility(View.GONE);

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
            SharedPreferences setupprefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
            boolean zoombutton = setupprefs.getBoolean("zoomButton", true);
            if (zoombutton) {
                zoomButton.setVisibility(View.VISIBLE);
            }

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
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        applyThemeToIcon(); // ダーク/ライト切り替え時に反映
        // もしポップアップが表示中なら閉じて再表示
        if (currentPopupWindow != null && currentPopupWindow.isShowing()) {
            currentPopupWindow.dismiss();
            showBottomMenu(findViewById(android.R.id.content)); // anchorは画面ルート
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences setupprefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean rebootApp = setupprefs.getBoolean("rebootApp", false);
        if (rebootApp) {
            SharedPreferences.Editor editor = setupprefs.edit();
            editor.putBoolean("rebootApp", false);
            editor.apply();
            Intent intent = new Intent(this, BootingActivity.class);
            finish();
            overridePendingTransition(0, 0);
            startActivity(intent);
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            // API 24 以上のときだけ実行する処理
            if (connectivityManager != null) {
                connectivityManager.registerDefaultNetworkCallback(networkCallback);
            }
        }
        applyThemeToIcon();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            // API 24 以上のときだけ実行する処理
            updateWifiSignal();
            updateConnectionStatus();
        }
        // バッテリー残量更新開始
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            // API 24 以上のときだけ実行する処理
            if (telephonyManager != null) {
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
            }
        }
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
                saveImage(selectedUrl);
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
        //MenuItem jsItem = menu.findItem(R.id.action_js);
        //if (jsItem != null) jsItem.setChecked(jsEnabled);
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
        } else if (itemId == R.id.action_negapoji) {
            applyNegapoji();
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
        } else if (itemId == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivityForResult(intent, 1001); // requestCode を指定
        }
        return super.onOptionsItemSelected(item);
    }

    public void onRefresh() {
        WebView webView = getCurrentWebView();
        if (webView != null) webView.reload();
    }

    private void popupLight(View anchor) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_menu_white, null);

        closeOptionsMenu();

        currentPopupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        currentPopupWindow.setBackgroundDrawable(new ColorDrawable());
        currentPopupWindow.setOutsideTouchable(true);
        currentPopupWindow.setAnimationStyle(R.style.PopupAnimation);
        currentPopupWindow.showAtLocation(anchor, Gravity.BOTTOM, 0, 0);

        RecyclerView recycler = popupView.findViewById(R.id.popupRecycler);

        // --- 🔹 画面幅に応じて列数を動的計算 ---
        int columnWidthDp = 100; // 1アイテムの目安サイズ（変更OK）
        float density = getResources().getDisplayMetrics().density;
        int screenWidthPx = getResources().getDisplayMetrics().widthPixels;
        int columnWidthPx = (int) (columnWidthDp * density);
        int spanCount = Math.max(1, screenWidthPx / columnWidthPx); // 最低3列確保
        // --------------------------------------

        recycler.setLayoutManager(new GridLayoutManager(this, spanCount));

        // --- 以下、あなたの既存処理をそのまま維持 ---
        List<PopupMenuItem> items = new ArrayList<>();
        items.add(new PopupMenuItem(R.drawable.bookmark_star, "ブックマーク"));
        items.add(new PopupMenuItem(R.drawable.clock, "履歴"));
        items.add(new PopupMenuItem(R.drawable.toppage, "トップページ"));
        items.add(new PopupMenuItem(R.drawable.ic_new_incognito_holo_dark, "シークレット"));
        items.add(new PopupMenuItem(R.drawable.camera, "保存済ページ"));
        items.add(new PopupMenuItem(R.drawable.download, "ダウンロード"));
        items.add(new PopupMenuItem(R.mipmap.ic_launcher_settings, "設定"));
        items.add(new PopupMenuItem(R.drawable.search_page, "ページ内検索"));
        items.add(new PopupMenuItem(R.drawable.share_white, "共有"));
        items.add(new PopupMenuItem(R.drawable.help_white, "ヘルプ"));
        items.add(new PopupMenuItem(R.drawable.tools, "ツール"));

        PopupMenuAdapterWhite adapter = new PopupMenuAdapterWhite(items, position -> {
            switch (position) {
                case 0:
                    new Handler().postDelayed(() -> {
                        currentPopupWindow.dismiss();
                        startActivity(new Intent(MainActivity.this, BmHisActivity.class));
                        overridePendingTransition(R.anim.slide_in_up_low, R.anim.no_animation);
                    }, 0);
                    break;
                case 1:
                    new Handler().postDelayed(() -> {
                        currentPopupWindow.dismiss();
                        showHistoryDialog();
                    }, 0);
                    break;
                case 2:
                    new Handler().postDelayed(() -> {
                        currentPopupWindow.dismiss();
                        startPage();
                    }, 0);
                    break;
                case 3:
                    new Handler().postDelayed(() -> {
                        currentPopupWindow.dismiss();
                        startActivity(new Intent(MainActivity.this, SecretActivity.class));
                        overridePendingTransition(R.anim.slide_in_up_low, R.anim.no_animation);
                        Toast.makeText(getApplicationContext(), "シークレットモード", Toast.LENGTH_SHORT).show();
                    }, 0);
                    break;
                case 4:
                    new Handler().postDelayed(() -> {
                        currentPopupWindow.dismiss();
                        startActivity(new Intent(MainActivity.this, HisBMActivity.class));
                        overridePendingTransition(R.anim.slide_in_up_low, R.anim.no_animation);
                    }, 0);
                    break;
                case 5:
                    new Handler().postDelayed(() -> {
                        currentPopupWindow.dismiss();
                        startActivity(new Intent(MainActivity.this, DownloadListActivity.class));
                        overridePendingTransition(R.anim.slide_in_up_low, R.anim.no_animation);
                    }, 0);
                    break;
                case 6:
                    new Handler().postDelayed(() -> {
                        currentPopupWindow.dismiss();
                        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                        startActivityForResult(intent, 1001);
                        overridePendingTransition(R.anim.slide_in_up_low, R.anim.no_animation);
                    }, 0);
                    break;
                case 7:
                    new Handler().postDelayed(() -> {
                        currentPopupWindow.dismiss();
                        showFindInPageBar();
                    }, 0);
                    break;
                case 8:
                    new Handler().postDelayed(() -> {
                        currentPopupWindow.dismiss();
                        shareCurrentUrl();
                    }, 0);
                    break;
                case 9:
                    new Handler().postDelayed(() -> {
                        currentPopupWindow.dismiss();
                        Intent intent = new Intent(this, AozoraHelp.class);
                        startActivityForResult(intent, 1001);
                        overridePendingTransition(R.anim.slide_in_up_low, R.anim.no_animation);
                    }, 0);
                    break;
                case 10:
                    new Handler().postDelayed(() -> {
                        currentPopupWindow.dismiss();
                        Intent tools = new Intent(MainActivity.this, toolsActivity.class);
                        startActivityForResult(tools, 1001);
                        overridePendingTransition(R.anim.slide_in_up_low, R.anim.no_animation);
                    }, 0);
                    break;
            }
            //currentPopupWindow.dismiss();
        });

        recycler.setAdapter(adapter);
    }



    private void popupDark(View anchor) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_menu, null);

        closeOptionsMenu();

        currentPopupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        currentPopupWindow.setBackgroundDrawable(new ColorDrawable());
        currentPopupWindow.setOutsideTouchable(true);
        currentPopupWindow.setAnimationStyle(R.style.PopupAnimation);
        currentPopupWindow.showAtLocation(anchor, Gravity.BOTTOM, 0, 0);

        RecyclerView recycler = popupView.findViewById(R.id.popupRecycler);

        // --- 🔹 画面幅に応じて列数を動的計算 ---
        int columnWidthDp = 100; // 1アイテムの目安サイズ（変更OK）
        float density = getResources().getDisplayMetrics().density;
        int screenWidthPx = getResources().getDisplayMetrics().widthPixels;
        int columnWidthPx = (int) (columnWidthDp * density);
        int spanCount = Math.max(1, screenWidthPx / columnWidthPx); // 最低3列確保
        // --------------------------------------

        recycler.setLayoutManager(new GridLayoutManager(this, spanCount));

        // --- 以下、あなたの既存処理をそのまま維持 ---
        List<PopupMenuItem> items = new ArrayList<>();
        items.add(new PopupMenuItem(R.drawable.bookmark_star, "ブックマーク"));
        items.add(new PopupMenuItem(R.drawable.clock, "履歴"));
        items.add(new PopupMenuItem(R.drawable.toppage_white, "トップページ"));
        items.add(new PopupMenuItem(R.drawable.ic_new_incognito_holo_dark, "シークレット"));
        items.add(new PopupMenuItem(R.drawable.camera, "保存済ページ"));
        items.add(new PopupMenuItem(R.drawable.download, "ダウンロード"));
        items.add(new PopupMenuItem(R.mipmap.ic_launcher_settings, "設定"));
        items.add(new PopupMenuItem(R.drawable.search_page, "ページ内検索"));
        items.add(new PopupMenuItem(R.drawable.share_white, "共有"));
        items.add(new PopupMenuItem(R.drawable.help_white, "ヘルプ"));
        items.add(new PopupMenuItem(R.drawable.tools, "ツール"));

        PopupMenuAdapter adapter = new PopupMenuAdapter(items, position -> {
            switch (position) {
                case 0:
                    new Handler().postDelayed(() -> {
                        currentPopupWindow.dismiss();
                        startActivity(new Intent(MainActivity.this, BmHisActivity.class));
                        overridePendingTransition(R.anim.slide_in_up_low, R.anim.no_animation);
                    }, 0);
                    break;
                case 1:
                    new Handler().postDelayed(() -> {
                        currentPopupWindow.dismiss();
                        showHistoryDialog();
                    }, 0);
                    break;
                case 2:
                    new Handler().postDelayed(() -> {
                        currentPopupWindow.dismiss();
                        startPage();
                    }, 0);
                    break;
                case 3:
                    new Handler().postDelayed(() -> {
                        currentPopupWindow.dismiss();
                        startActivity(new Intent(MainActivity.this, SecretActivity.class));
                        overridePendingTransition(R.anim.slide_in_up_low, R.anim.no_animation);
                        Toast.makeText(getApplicationContext(), "シークレットモード", Toast.LENGTH_SHORT).show();
                    }, 0);
                    break;
                case 4:
                    new Handler().postDelayed(() -> {
                        currentPopupWindow.dismiss();
                        startActivity(new Intent(MainActivity.this, HisBMActivity.class));
                        overridePendingTransition(R.anim.slide_in_up_low, R.anim.no_animation);
                    }, 0);
                    break;
                case 5:
                    new Handler().postDelayed(() -> {
                        currentPopupWindow.dismiss();
                        startActivity(new Intent(MainActivity.this, DownloadListActivity.class));
                        overridePendingTransition(R.anim.slide_in_up_low, R.anim.no_animation);
                    }, 0);
                    break;
                case 6:
                    new Handler().postDelayed(() -> {
                        currentPopupWindow.dismiss();
                        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                        startActivityForResult(intent, 1001);
                        overridePendingTransition(R.anim.slide_in_up_low, R.anim.no_animation);
                    }, 0);
                    break;
                case 7:
                    new Handler().postDelayed(() -> {
                        currentPopupWindow.dismiss();
                        showFindInPageBar();
                    }, 0);
                    break;
                case 8:
                    new Handler().postDelayed(() -> {
                        currentPopupWindow.dismiss();
                        shareCurrentUrl();
                    }, 0);
                    break;
                case 9:
                    new Handler().postDelayed(() -> {
                        currentPopupWindow.dismiss();
                        Intent intent = new Intent(this, AozoraHelp.class);
                        startActivityForResult(intent, 1001);
                        overridePendingTransition(R.anim.slide_in_up_low, R.anim.no_animation);
                    }, 0);
                    break;
                case 10:
                    new Handler().postDelayed(() -> {
                        currentPopupWindow.dismiss();
                        Intent tools = new Intent(MainActivity.this, toolsActivity.class);
                        startActivityForResult(tools, 1001);
                        overridePendingTransition(R.anim.slide_in_up_low, R.anim.no_animation);
                    }, 0);
                    break;
            }
            //currentPopupWindow.dismiss();
        });

        recycler.setAdapter(adapter);
    }

    private void showBottomMenu(View anchor) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int theme = prefs.getInt(KEY_THEME, THEME_SYSTEM);

        switch (theme) {
            case THEME_LIGHT:
                popupLight(anchor);
                break;

            case THEME_DARK:
                popupDark(anchor);
                break;

            case THEME_SYSTEM:
            default:
                // OS 側の設定に従う
                int nightModeFlags = getResources().getConfiguration().uiMode
                        & android.content.res.Configuration.UI_MODE_NIGHT_MASK;

                if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                    popupDark(anchor);
                } else {
                    popupLight(anchor);
                }
                break;
        }

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

    //現在未使用
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
        Animation anim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fade);
        findInPageBarView.startAnimation(anim);
        findInPageBarView.setVisibility(View.VISIBLE);
        etFindQuery.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(etFindQuery, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void performFindInPage() {
        WebView webView = tabs.get(currentTabIndex);
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
            Animation anim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fadeout);
            findInPageBarView.startAnimation(anim);
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
        SharedPreferences setupprefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean acceptCookies = setupprefs.getBoolean("acceptCookies", true);
        boolean geoLocation = setupprefs.getBoolean("geoLocation", true);
        boolean javaScript = setupprefs.getBoolean("javaScript", true);
        boolean popupBlock = setupprefs.getBoolean("popupBlock", false);
        CookieManager.getInstance().setAcceptCookie(acceptCookies);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, acceptCookies);
        CookieManager.getInstance().flush();
        settings.setJavaScriptCanOpenWindowsAutomatically(popupBlock);
        settings.setJavaScriptEnabled(javaScript);
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        settings.setDomStorageEnabled(true);
        settings.setTextZoom(100);
        settings.setDisplayZoomControls(false);
        settings.setBuiltInZoomControls(false);
        settings.setSupportZoom(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setAllowFileAccess(true);            // file:///android_asset/ を読むため
        settings.setAllowFileAccessFromFileURLs(true);   // file:// → file:// アクセス許可
        settings.setAllowUniversalAccessFromFileURLs(true); // file:// → http/https アクセス許可

        settings.setGeolocationEnabled(geoLocation);

        settings.setBuiltInZoomControls(true);
        settings.setSupportZoom(true);

        settings.setSupportMultipleWindows(true);

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
                            url.startsWith("https://m.youtube.com/shorts/") ||
                            url.startsWith("https://www.google.com")) {
                        swipeRefreshLayout.setEnabled(false);
                    } else {
                        SharedPreferences setupprefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                        boolean isSwipeReload = setupprefs.getBoolean("isSwipeReload", true);
                        swipeRefreshLayout.setEnabled(isSwipeReload);
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

    private void onProgressStep(int percentage) {
        ValueAnimator animator = ValueAnimator.ofInt(progressBar.getProgress(), percentage);
        animator.setDuration(500);
        animator.setInterpolator(null); // 線形
        animator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            // 10刻みでしか進まないように丸める
            int stepped = (value / 10) * 10;
            progressBar.setProgress(stepped);
        });
        animator.start();
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
                if (loadTabnoHideurl) {
                    nohideurl=true;
                }
                isLoading = true;
                Animation anim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fade);
                pageloading.startAnimation(anim);
                pageloading.setVisibility(View.VISIBLE);
                sitename.setText("Loading...");
                showUrlBar();
                reloadButton.setImageResource(R.drawable.ic_close); // ×アイコンに変更
                webView = view; // WebViewを保存
                WebView webUrl = tabs.get(currentTabIndex);
                String lower = url.toLowerCase();
                boolean matched = CACHE_MODE_PATTERN.matcher(lower).find();
                if (matched) {
                    view.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
                } else {
                    view.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
                }
                String urlhere = webUrl.getUrl();
                urlEditText.setText(urlhere);
                if (!noUpdateUrl) {
                    updateUrlBar(view);
                }
                SharedPreferences setupprefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                boolean ProgressBarAnimation = setupprefs.getBoolean("ProgressBarAnimation", true);
                progressBar.setVisibility(View.VISIBLE);
                if (ProgressBarAnimation) {
                    onProgressChanged(10);
                }
                ImageButton bmbutton = findViewById(R.id.action_bookmark);
                if (ProgressBarAnimation) {
                    onProgressChanged(30);
                }
                // JavaScript を使用して Favicon を取得
                webUrl.evaluateJavascript("(function() { " +
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

                new Handler().postDelayed(() -> {
                    if (urlhere != null && (
                            urlhere.equals("file:///android_asset/index.html") ||
                                    urlhere.equals("file:///android_asset/index_white.html") ||
                                    urlhere.equals("file:///android_asset/help.html") ||
                                            urlhere.equals("file:///android_asset/error.html")
                    )) {
                        nohideurl = true;
                    } else {
                        nohideurl = false;
                    }
                }, 500);

                if (ProgressBarAnimation) {
                    onProgressChanged(40);
                } else {
                    onProgressStep(40);
                }

                if (urlhere.equals("file:///android_asset/index.html") ||
                        urlhere.equals("file:///android_asset/index_white.html")) {
                    sitename.startAnimation(anim);
                    sitename.setText("Aozora");
                } else {
                    new Handler().postDelayed(() -> {
                        String pageTitle = webUrl.getTitle();
                        sitename.startAnimation(anim);
                        sitename.setText(pageTitle);
                    }, 600);
                }

                if (ProgressBarAnimation) {
                    onProgressChanged(60);
                } else {
                    progressBar.setProgress(60);
                }
                super.onPageStarted(view, url, favicon);
                if (!isNewTab) {
                    // フェードインアニメーションを適用
                    android.view.animation.Animation fadeIn =
                            android.view.animation.AnimationUtils.loadAnimation(MainActivity.this, R.anim.fade);
                    webView.startAnimation(fadeIn);
                }
                Executors.newSingleThreadExecutor().execute(() -> {
                    SQLiteDatabase db = dbbm.getReadableDatabase();
                    Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM pages WHERE url = ?", new String[]{url});
                    boolean isBookmarked = (cursor.moveToFirst() && cursor.getInt(0) > 0);
                    cursor.close();

                    runOnUiThread(() -> {
                        bmbutton.setImageResource(isBookmarked ?
                                R.drawable.bookmark_star : R.drawable.bookmark_black);
                    });
                });
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                WebView webUrl = tabs.get(currentTabIndex);
                String urlhere = webUrl.getUrl();
                SharedPreferences setupprefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                boolean ProgressBarAnimation = setupprefs.getBoolean("ProgressBarAnimation", true);
                isLoading = false;
                reloadButton.setImageResource(R.drawable.reload); // リロードアイコンに戻す
                applyCombinedOptimizations(view);
                if (url.startsWith("https://m.youtube.com") || url.startsWith("https://chatgpt.com/")) {
                    view.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
                    new Handler(Looper.getMainLooper()).postDelayed(() -> injectLazyLoading(view), 200);
                }
                if (!ProgressBarAnimation) {
                    progressBar.setProgress(80);
                }
                updateNavigationButtons();
                //urlEditText.setText(url);
                //updateUrlBar(view);
                int currentTabIndex = tabs.indexOf(view);
                if (!isBackNavigation) {
                    if (!noUpdateUrl) {
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
                    }
                } else {
                    isBackNavigation = false;
                }
                if (swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }

                String jsOverrideHistory = "(function(){" +
                        "function notifyUrlChange(){AndroidBridge.onUrlChange(location.href);}" +
                        "var pushState=history.pushState;" +
                        "history.pushState=function(){pushState.apply(history,arguments);notifyUrlChange();};" +
                        "var replaceState=history.replaceState;" +
                        "history.replaceState=function(){replaceState.apply(history,arguments);notifyUrlChange();};" +
                        "window.addEventListener('popstate',function(){notifyUrlChange();});" +
                        "notifyUrlChange();" +
                        "})();";
                view.evaluateJavascript(jsOverrideHistory, null);

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
                if (ProgressBarAnimation) {
                    onProgressChanged(100);
                } else {
                    progressBar.setProgress(100);
                }
                hideProgressBarDelayed();
                new Handler().postDelayed(() -> {
                    Animation anim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fadeout);
                    pageloading.startAnimation(anim);
                    pageloading.setVisibility(View.INVISIBLE);
                }, 500);

            }

            @Override
            public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
                SharedPreferences setupprefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                boolean securityAlert = setupprefs.getBoolean("securityAlert", true);
                String errorUrl = error.getUrl();
                if (securityAlert) {
                    WebView webview = tabs.get(currentTabIndex);
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("セキュリティ警告");
                    builder.setIcon(android.R.drawable.ic_dialog_alert);
                    builder.setMessage("このサイトのセキュリティ証明書には問題があります。");

                    builder.setPositiveButton("戻る", (dialog, which) -> {
                        handler.cancel(); // 読み込み中止
                    });

                    builder.setNeutralButton("証明書を表示", (dialog, which) -> {
                        Toast.makeText(MainActivity.this, "しばらくお待ち下さい...", Toast.LENGTH_SHORT).show();
                        showCertificateDialogUrl(errorUrl);
                    });

                    builder.setNegativeButton("続行", (dialog, which) -> {
                        handler.proceed(); // 無視して続行
                    });

                    builder.show();
                } else {
                    handler.proceed();
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleCustomUrl(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleCustomUrl(view, request.getUrl().toString());
            }

            private boolean handleCustomUrl(WebView view, String url) {
                try {
                    SharedPreferences setupprefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                    boolean intentJump = setupprefs.getBoolean("intentJump", true);
                    if (url.startsWith("http:") || url.startsWith("https:") || url.startsWith("file:")) {
                        // 通常のWebリンクはWebViewで開く
                        return false;
                    } else if (url.startsWith("tel:")) {
                        if (intentJump) {
                            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse(url)));
                        }
                        return true;
                    } else if (url.startsWith("mailto:")) {
                        if (intentJump) {
                            startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse(url)));
                        }
                        return true;
                    } else if (url.startsWith("intent:")) {
                        // intent:// スキーム対応（Play Store fallback含む）
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        if (intent != null) {
                            if (intent.resolveActivity(getPackageManager()) != null) {
                                if (intentJump) {
                                    startActivity(intent);
                                }
                            } else {
                                String fallbackUrl = intent.getStringExtra("browser_fallback_url");
                                if (fallbackUrl != null) {
                                    view.loadUrl(fallbackUrl);
                                } else {
                                    // Play ストアへ誘導（例: market://details?id=com.example）
                                    String packageName = intent.getPackage();
                                    if (packageName != null) {
                                        Uri marketUri = Uri.parse("market://details?id=" + packageName);
                                        Intent marketIntent = new Intent(Intent.ACTION_VIEW, marketUri);
                                        if (marketIntent.resolveActivity(getPackageManager()) != null) {
                                            if (intentJump) {
                                                startActivity(marketIntent);
                                            }
                                        }
                                    }
                                }
                            }
                            return true;
                        }
                    } else if (url.startsWith("fb:") || url.startsWith("facebook:") ||
                            url.startsWith("line:") || url.startsWith("twitter:") ||
                            url.startsWith("instagram:") || url.startsWith("zoomus:") ||
                            url.startsWith("market:")) {
                        // 対応アプリが存在する場合 → 直接開く
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        if (intentJump) {
                            startActivity(intent);
                        }
                    } else if ((url.startsWith("about:"))) {
                        return false;
                    } else {
                        // 対応アプリが存在しない場合 → Playストアに誘導
                        String packageName = getPackageNameFromScheme(url);
                        if (packageName != null) {
                            Uri marketUri = Uri.parse("market://details?id=" + packageName);
                            Intent marketIntent = new Intent(Intent.ACTION_VIEW, marketUri);
                            if (marketIntent.resolveActivity(getPackageManager()) != null) {
                                if (intentJump) {
                                    startActivity(marketIntent);
                                }
                            }
                        } else {
                            // パッケージ不明 → 検索にフォールバック
                            Uri marketUri = Uri.parse("market://search?q=" + Uri.parse(url).getScheme());
                            if (intentJump) {
                            startActivity(new Intent(Intent.ACTION_VIEW, marketUri));
                            }
                        }
                    }
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }

            private String getPackageNameFromScheme(String url) {
                if (url.startsWith("fb:")) return "com.facebook.katana";
                if (url.startsWith("twitter:")) return "com.twitter.android";
                if (url.startsWith("line:")) return "jp.naver.line.android";
                if (url.startsWith("instagram:")) return "com.instagram.android";
                if (url.startsWith("zoomus:")) return "us.zoom.videomeetings";
                if (url.startsWith("market:")) return "com.android.vending";
                return null;
            }

            @Override
            public void onReceivedError(WebView webview, int errorCode, String description, String failingUrl) {
                if (errorCode < 0) {
                    noUpdateUrl = true;
                    Animation anim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fadeout);
                    webViewContainer.startAnimation(anim);
                    webViewContainer.setVisibility(View.INVISIBLE);
                    new Handler().postDelayed(() -> {
                    WebView webView = tabs.get(currentTabIndex);
                    webView.goBack();
                    }, 100);
                    new Handler().postDelayed(() -> {
                        String html = "<!DOCTYPE html>" +
                                "<html lang=\"ja\">" +
                                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                                "<head><title>エラー</title></head>" +
                                "<style>" +
                                "body { font-family: sans-serif; background-color:#111111; color:#aaaaaa; padding: 20px; line-height:1.6; }" +
                                ".container { max-width:600px; margin:0 auto; text-align:left; }" +
                                "h1 { font-size: 48px; margin-bottom: 10px; }" +
                                "h2 { font-size: 20px; margin-bottom: 20px; }" +
                                "p, li { font-size: 16px; }" +
                                "ul { padding-left: 20px; }" +
                                ".error-box { background:#222; padding:15px; border-radius:8px; margin-top:15px; }" +
                                "</style>" +
                                "</head>" +
                                "<body>" +
                                "<h1>:(</h1>" +
                                "<h2>ページの読み込み時に問題が発生しました</h2>" +
                                "<p>" + description + "</p>" +
                                "<p>エラーコード: " + errorCode + "</p>" +
                                "<p>次をお試しください:</p>" +
                                "<ul>" +
                                "<li>機内モードをオフにする</li>" +
                                "<li>モバイルデータ または Wi-Fi を有効にする</li>" +
                                "<li>電波状況を確認する</li>" +
                                "<li>アプリケーションの更新を確認する</li>" +
                                "</ul>" +
                                "</body></html>";
                        WebView view = tabs.get(currentTabIndex);
                        view.loadDataWithBaseURL(failingUrl, html, "text/html", "UTF-8", null);
                        noUpdateUrl = false;
                        urlEditText.setText(failingUrl);
                        Animation animin = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fade);
                        webViewContainer.startAnimation(animin);
                        webViewContainer.setVisibility(View.VISIBLE);
                    }, 500);
                }
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
                AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
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
                        .create();
                // 外をタップしても閉じない
                dialog.setCanceledOnTouchOutside(false);
                // 必要なら戻るキーでも閉じないように
                dialog.setCancelable(false);
                dialog.show();
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
        ImageButton bmbutton = findViewById(R.id.action_bookmark);
        String url = webView.getUrl();
        String title = webView.getTitle();
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

            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_save_bookmark, null);
            final EditText inputUrl = viewInflated.findViewById(R.id.editUrl);
            final EditText inputTitle = viewInflated.findViewById(R.id.editTitle);

            inputUrl.setText(url);
            inputTitle.setText(title);

            builder.setView(viewInflated);

            builder.setPositiveButton("OK", (dialog, which) -> {
                String newUrl = inputUrl.getText().toString().trim();
                String newTitle = inputTitle.getText().toString().trim();
                savebmProcess(newUrl, newTitle);
            });
            builder.setNegativeButton("キャンセル", (dialog, which) -> {
                dialog.cancel();
            });

            builder.show();
        }
    }

    private void savebmProcess(String url, String title) {
        WebView webView = tabs.get(currentTabIndex);
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
        WebView webUrl = tabs.get(currentTabIndex);
        String currentUrl = webUrl.getUrl();
        ImageButton bmbutton = findViewById(R.id.action_bookmark);

        // 指定URLの場合は空白を表示
        if (currentUrl != null && (
                currentUrl.equals("file:///android_asset/index.html") ||
                        currentUrl.equals("file:///android_asset/help.html") ||
                        currentUrl.equals("file:///android_asset/index_white.html") ||
                                currentUrl.equals("file:///android_asset/error.html")
        )) {
            urlEditText.setText(""); // URLを非表示
            bmbutton.setVisibility(View.GONE);
        } else {
            urlEditText.setText(currentUrl); // 通常のURLを表示
            bmbutton.setVisibility(View.VISIBLE);
        }
    }

    private void ensureCacheSentinelExists() {
        File filesDir = getFilesDir();
        File sentinel = new File(filesDir, SENTINEL_FILENAME);
        if (!sentinel.exists()) {
            try {
                sentinel.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        File cacheDir = getCacheDir();
        File cacheSentinel = new File(cacheDir, SENTINEL_FILENAME);
        if (!cacheSentinel.exists()) {
            try {
                cacheSentinel.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void applyCombinedOptimizations(WebView webView) {
        String js = "javascript:(function(){" +
                "var animatedElements=document.querySelectorAll('.animated,.transition');" +
                "animatedElements.forEach(function(el){" +
                "if(!el.style.transform){el.style.transform='translateZ(0)';}" +
                "if(!el.style.willChange){el.style.willChange='transform,opacity';}" +
                "});" +
                "var fixedElements=document.querySelectorAll('.fixed');" +
                "fixedElements.forEach(function(el){" +
                "if(el.style.position!=='fixed'){el.style.position='fixed';}" +
                "});" +
                "})();";
        webView.evaluateJavascript(js, null);
    }
    private void injectLazyLoading(WebView webView) {
        String js = "javascript:(function(){" +
                "var placeholder='data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7';" +
                "var images=document.querySelectorAll('img[src^=\"https://i.ytimg.com/\"]:not([data-lazy-loaded])');" +
                "if(images.length===0)return;" +
                "images.forEach(function(img){" +
                "img.setAttribute('data-lazy-loaded','true');" +
                "if(img.hasAttribute('src')){" +
                "img.setAttribute('data-src',img.src);" +
                "img.src=placeholder;" +
                "img.style.opacity='0';" +
                "img.style.transition='opacity 0.3s';" +
                "if(!img.style.transform){img.style.transform='translateZ(0)';}" +
                "}" +
                "});" +
                "if('IntersectionObserver'in window){" +
                "var observer=new IntersectionObserver(function(entries){" +
                "entries.forEach(function(entry){" +
                "if(entry.isIntersecting){" +
                "var img=entry.target;" +
                "if(img.dataset.src){" +
                "img.src=img.dataset.src;" +
                "img.removeAttribute('data-src');" +
                "img.onload=function(){img.style.opacity='1';};" +
                "img.onerror=function(){console.warn('Image load failed: '+img.src);};" +
                "}" +
                "observer.unobserve(img);" +
                "}" +
                "});" +
                "},{root:null,rootMargin:'0px',threshold:0.1});" +
                "images.forEach(function(img){observer.observe(img);});" +
                "}else{" +
                "var loadImagesOnScroll=function(){" +
                "images.forEach(function(img){" +
                "if(img.dataset.src&&isElementInViewport(img)){" +
                "img.src=img.dataset.src;" +
                "img.removeAttribute('data-src');" +
                "img.onload=function(){img.style.opacity='1';};" +
                "img.onerror=function(){console.warn('Image load failed: '+img.src);};" +
                "}" +
                "});" +
                "};" +
                "var isElementInViewport=function(el){" +
                "var rect=el.getBoundingClientRect();" +
                "return(rect.top>=0&&rect.left>=0&&rect.bottom<=(window.innerHeight||document.documentElement.clientHeight)&&rect.right<=(window.innerWidth||document.documentElement.clientWidth));" +
                "};" +
                "window.addEventListener('scroll',loadImagesOnScroll);" +
                "window.addEventListener('resize',loadImagesOnScroll);" +
                "window.addEventListener('load',loadImagesOnScroll);" +
                "loadImagesOnScroll();" +
                "}" +
                "})();";
        webView.evaluateJavascript(js, null);
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

        SharedPreferences setupprefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean acceptCookies = setupprefs.getBoolean("acceptCookies", true);
        boolean javaScript = setupprefs.getBoolean("javaScript", true);
        boolean popupBlock = setupprefs.getBoolean("popupBlock", false);
        CookieManager.getInstance().setAcceptCookie(acceptCookies);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, acceptCookies);
        CookieManager.getInstance().flush();

        webView.getSettings().setJavaScriptEnabled(javaScript);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(popupBlock);
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
        webView.getSettings().setSupportMultipleWindows(true);
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
        s.setBuiltInZoomControls(true);
        s.setSupportZoom(true);
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
            OnWindowCloseListener onWindowCloseListener;
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                // 新しい WebView を作成して設定
                WebView tmpWebView = new WebView(view.getContext());
                tmpWebView.setWebViewClient(new WebViewClient(){
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                        String url = request.getUrl().toString(); // ←ここでURL取得

                        addNewTab(url);

                        if(view != null) view.destroy();
                        return false;
                    }
                });
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(tmpWebView);
                resultMsg.sendToTarget();
                return true;

            }

            @Override
            public void onCloseWindow(WebView window) {
                super.onCloseWindow(window);

                // JS の window.close() が呼ばれた時に発火
                if (onWindowCloseListener != null) {
                    onWindowCloseListener.onWindowClose();
                }

                // WebView を破棄してメモリリーク防止
                if (window != null) {
                    android.view.animation.Animation fadeIn =
                            android.view.animation.AnimationUtils.loadAnimation(MainActivity.this, R.anim.tab_out);
                    webView.startAnimation(fadeIn);
                    closeTab(currentTabIndex);
                    WebView webViews = tabs.get(currentTabIndex);
                    saveTabsState();
                    webViews.onPause();
                    // 戻ってきたときに実行したい処理
                    webViews.onResume();
                    window.destroy();
                }
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                String host = getDomainFromUrl(origin);

                if (geoPrefs.contains(host)) {
                    // すでに記録されている設定を使う
                    boolean allowed = geoPrefs.getBoolean(host, false);
                    callback.invoke(origin, allowed, false);
                    return;
                }

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("位置情報")
                        .setMessage("現在のサイトが位置情報を要求しています。")
                        .setPositiveButton("許可", (dialog, which) -> {
                            checkLocationPermission();
                            geoPrefs.edit().putBoolean(host, true).apply();
                            callback.invoke(origin, true, false);
                        })
                        .setNegativeButton("拒否", (dialog, which) -> {
                            geoPrefs.edit().putBoolean(host, false).apply();
                            callback.invoke(origin, false, false);
                        })
                        .show();
            }
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
                String url = view.getUrl();
                if (url != null && icon != null) {
                    faviconCaches.put(url, icon);
                    // ここで履歴のfavicon更新を通知してもOK
                }
            }
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                super.onShowCustomView(view, callback);

                webView.scrollTo(0, 0);

                hideBottomBar();
                hideUrlBar();

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
                StatusBar.setVisibility(View.GONE);
                zoomButton.setVisibility(View.GONE);
                showBottomButton.setVisibility(View.GONE);

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
                StatusBar.setVisibility(View.VISIBLE);
                SharedPreferences setupprefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                boolean zoombutton = setupprefs.getBoolean("zoomButton", true);
                if (zoombutton) {
                    zoomButton.setVisibility(View.VISIBLE);
                }
                showBottomButton.setVisibility(View.VISIBLE);

                showUrlBar();
                showBottomBar();

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
            WebView view = tabs.get(currentTabIndex);

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
                if (url.startsWith("blob:")) {
                    handleBlobDownload(url, userAgent, contentDisposition, mimetype, contentLength);
                } else {
                    handleDownload(url, userAgent, contentDisposition, mimetype, contentLength);
                }
                if ("external".equals(view.getTag())) {
                    closeTab(currentTabIndex);
                }
            }
        });

        webView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                WebView webView = tabs.get(currentTabIndex);
                WebView.HitTestResult result = webView.getHitTestResult();
                if (result != null) {
                    final int type = result.getType();
                    selectedUrl = result.getExtra();
                    //boolean isDataUrl = selectedUrl != null && selectedUrl.startsWith("data:");
                    if (type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                        // メッセージを準備
                        final Message message = new Handler(Looper.getMainLooper()) {
                            @Override
                            public void handleMessage(Message msg) {
                                // 必要ならここで値を受け取る処理を書ける
                            }
                        }.obtainMessage();

                        // requestFocusNodeHref で href と src を取得
                        webView.requestFocusNodeHref(message);

                        // href（リンク URL）
                        String linkUrl = message.getData().getString("url");

                        // 画像の URL
                        String imageUrl = message.getData().getString("src");
                        final String[] opts;
                        opts = new String[]{"新しいタブで開く", "リンクをコピー", "リンクをダウンロード", "画像を保存"};
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("オプションを選択")
                                .setItems(opts, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (which == 0) {
                                            addNewTab(linkUrl);
                                        } else if (which == 1) {
                                            copyLink(linkUrl);
                                        } else if (which == 2) {
                                            downloadLink(linkUrl);
                                        } else if (which == 3) {
                                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                                saveImage(imageUrl);
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
                            opts = new String[]{"新しいタブで画像を開く", "リンクをコピー", "リンクをダウンロード", "画像を保存"};
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
                                                    saveImage(selectedUrl);
                                                }
                                            } else {
                                                copyLink(selectedUrl);
                                            }
                                        } else if (which == 2) {
                                           downloadLink(selectedUrl);
                                        } else if (which == 3 && !isDataUrlLocal) {
                                            if (selectedUrl != null && !selectedUrl.isEmpty()) {
                                                saveImage(selectedUrl);
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

    public interface OnWindowCloseListener {
        void onWindowClose();
    }

    private void checkLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_LOCATION
                );
            }
        }
    }

    private String getDomainFromUrl(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null) return url;
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return url;
        }
    }

    private void handleDownload(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        if (mimeType != null) {
            request.setMimeType(mimeType);
        }
        String cookies = CookieManager.getInstance().getCookie(url);
        request.addRequestHeader("cookie", cookies);
        if (userAgent != null) {
            request.addRequestHeader("User-Agent", userAgent);
        }
        request.setDescription("Downloading file...");
        String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
        request.setTitle(fileName);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        try {
            long downloadId = dm.enqueue(request);
            String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    .getAbsolutePath() + "/" + fileName;
            DownloadHistoryManager.addDownloadHistory(MainActivity.this, downloadId, fileName, filePath);
            DownloadHistoryManager.monitorDownloadProgress(MainActivity.this, downloadId, dm);
            Toast.makeText(MainActivity.this, "ダウンロードを開始します...", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "ダウンロードに失敗しました", Toast.LENGTH_SHORT).show();
        }
    }
    private void handleBlobDownload(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
        String js = "javascript:(function(){" +
                "fetch('" + url + "').then(function(response){return response.blob();}).then(function(blob){" +
                "var reader=new FileReader();" +
                "reader.onloadend=function(){var base64data=reader.result;" +
                "var fileName='" + generateBlobFileName(mimeType) + "';" +
                "window.BlobDownloader.onBlobDownloaded(base64data,'" + (mimeType != null ? mimeType : "application/octet-stream") + "',fileName);" +
                "};" +
                "reader.readAsDataURL(blob);" +
                "}).catch(function(error){window.BlobDownloader.onBlobDownloadError(error.toString());});" +
                "})();";
        getCurrentWebView().evaluateJavascript(js, null);
    }

    private String generateBlobFileName(String mimeType) {
        String ext = "";
        if (mimeType != null) {
            if (mimeType.contains("pdf")) {
                ext = ".pdf";
            } else if (mimeType.contains("image/png")) {
                ext = ".png";
            } else if (mimeType.contains("image/jpeg")) {
                ext = ".jpg";
            } else if (mimeType.contains("text/html")) {
                ext = ".html";
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return "blob_download_" + timeStamp + ext;
    }


    private class BlobDownloadInterface {
        @JavascriptInterface
        public void onBlobDownloaded(String base64Data, String mimeType, String fileName) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int commaIndex = base64Data.indexOf(",");
                        String pureBase64 = base64Data.substring(commaIndex + 1);
                        byte[] data = Base64.decode(pureBase64, Base64.DEFAULT);
                        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                        if (!downloadDir.exists()) {
                            downloadDir.mkdirs();
                        }
                        File file = new File(downloadDir, fileName);
                        FileOutputStream fos = new FileOutputStream(file);
                        try {
                            fos.write(data);
                            fos.flush();
                        } finally {
                            try { fos.close(); } catch (Exception ignored) {}
                        }
                        Toast.makeText(MainActivity.this, "blob ダウンロード完了: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "blob ダウンロードエラー: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

        @JavascriptInterface
        public void onBlobDownloadError(final String errorMessage) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "blob ダウンロードエラー: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        }
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

    private void saveImage(String imageUrl) {
        try {
            if (imageUrl != null && imageUrl.startsWith("data:")) {
                int commaIndex = imageUrl.indexOf(',');
                if (commaIndex == -1) {
                    Toast.makeText(MainActivity.this, "無効なURLです", Toast.LENGTH_SHORT).show();
                    return;
                }
                String metadata = imageUrl.substring(5, commaIndex);
                boolean isBase64 = metadata.contains("base64");
                String mimeType = "image/*";
                if (metadata.contains(";")) {
                    mimeType = metadata.split(";")[0];
                }
                byte[] imageData;
                if (isBase64) {
                    String base64Data = imageUrl.substring(commaIndex + 1);
                    imageData = Base64.decode(base64Data, Base64.DEFAULT);
                } else {
                    String dataPart = imageUrl.substring(commaIndex + 1);
                    imageData = dataPart.getBytes("UTF-8");
                }
                String fileName = "saved_image_" + System.currentTimeMillis();
                if (mimeType.equalsIgnoreCase("image/png")) {
                    fileName += ".png";
                } else if (mimeType.equalsIgnoreCase("image/jpeg")) {
                    fileName += ".jpg";
                } else if (mimeType.equalsIgnoreCase("image/bmp")) {
                    fileName += ".bmp";
                } else if (mimeType.equalsIgnoreCase("image/gif")) {
                    fileName += ".gif";
                } else if (mimeType.equalsIgnoreCase("image/img")) {
                    fileName += ".img";
                }
                File picturesDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES);
                File file = new File(picturesDir, fileName);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(imageData);
                    fos.flush();
                }
                Toast.makeText(MainActivity.this,
                        "画像を保存しました\n保存先: " + file.getAbsolutePath(),
                        Toast.LENGTH_LONG).show();
                return;
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                    ContextCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this,
                        "ストレージ権限が必要です", Toast.LENGTH_SHORT).show();
                return;
            }
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            DownloadManager.Request request =
                    new DownloadManager.Request(Uri.parse(imageUrl));
            request.setMimeType("image/*");
            String fileName = URLUtil.guessFileName(imageUrl, null, "image/*");
            request.setTitle(fileName);
            request.setDescription("画像を保存中...");
            request.setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_PICTURES, fileName);
            dm.enqueue(request);
            Toast.makeText(MainActivity.this,
                    "ダウンロードを開始します...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(MainActivity.this,
                    "画像の保存に失敗しました", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void updateNavigationButtons() {
        WebView currentWebView = tabs.get(currentTabIndex);
        // 🔹 戻るボタンの有効/無効を設定
        if (currentWebView.canGoBack()) {
            backView.setVisibility(View.GONE);
            backButton.setVisibility(View.VISIBLE);
        } else {
            backButton.setVisibility(View.GONE);
            backView.setVisibility(View.VISIBLE);
        }
        // 🔹 進むボタンの有効/無効を設定
        if (currentWebView.canGoForward()) {
            forwardView.setVisibility(View.GONE);
            forwardButton.setVisibility(View.VISIBLE);
        } else {
            forwardButton.setVisibility(View.GONE);
            forwardView.setVisibility(View.VISIBLE);
        }
    }

    private void goBack() {
        WebView currentWebView = tabs.get(currentTabIndex);
        if (currentWebView.canGoBack()) {
            nohideurl=true;
            currentWebView.goBack();
            new android.os.Handler().postDelayed(this::updateNavigationButtons, 300); // 300ms遅延
        }
    }

    private void goForward() {
        WebView currentWebView = tabs.get(currentTabIndex);
        if (currentWebView.canGoForward()) {
            nohideurl=true;
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
        zoomButton.setOnZoomInClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentWebView.zoomBy(1.5f);
            }
        });

        zoomButton.setOnZoomOutClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nohideurl = true;
                currentWebView.zoomBy(0.5f);
                new Handler().postDelayed(() -> {
                    nohideurl = false;
                }, 200);
            }
        });

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
        showUrlBar();
        WebView webtitle = tabs.get(currentTabIndex);
        String pageUrl = webtitle.getUrl();
        if (pageUrl.equals("file:///android_asset/index.html") ||
                pageUrl.equals("file:///android_asset/index_white.html")) {
            sitename.setText("Aozora");
        } else {
            String pageTitle = webtitle.getTitle();
            sitename.setText(pageTitle);
        }
        if (pageUrl != null && (
                pageUrl.equals("file:///android_asset/index.html") ||
                        pageUrl.equals("file:///android_asset/help.html") ||
                        pageUrl.equals("file:///android_asset/index_white.html") ||
                                pageUrl.equals("file:///android_asset/error.html")
        )) {
            nohideurl = true;
        } else {
            nohideurl = false;
        }
        saveTabsState();

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                WebView webView = tabs.get(currentTabIndex);
                // updateUrlBar(webView);
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

    private void webSlideIn() {
        WebView webView = tabs.get(currentTabIndex);
        android.view.animation.Animation fadeIn =
                android.view.animation.AnimationUtils.loadAnimation(this, R.anim.page_in_right);
        webView.startAnimation(fadeIn);
    }

    private void webSlideOut() {
        WebView webView = tabs.get(currentTabIndex);
        android.view.animation.Animation fadeIn =
                android.view.animation.AnimationUtils.loadAnimation(this, R.anim.page_out_right);
        webView.startAnimation(fadeIn);
    }

    private void closeTab(int index) {
        if (index < 0 || index >= tabs.size()) return;

        WebView webView = tabs.remove(index);
        tabSnapshots.remove(webView);
        tabInfos.remove(index);
        webViewContainer.removeView(webView); // 🔹 WebView を削除

        if (tabs.isEmpty()) {
            newStartPage();
        } else {
            currentTabIndex = Math.max(0, currentTabIndex - 1);
        }

        android.view.animation.Animation fadeIn =
                android.view.animation.AnimationUtils.loadAnimation(this, R.anim.tab_out);
        webView.startAnimation(fadeIn);

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
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged(); // ← 全ページ再描画（シンプルに）
            // もしページ単位で削除通知するなら：
            // listAdapter.notifyItemRemoved(index / 4);
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
        Intent intent = new Intent(this, PageHistoryActivity.class);
        intent.putExtra("history_list", new ArrayList<>(historyItems));
        startActivityForResult(intent, REQUEST_HISTORY);
        overridePendingTransition(R.anim.slide_in_up_low, R.anim.no_animation);
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

    private void recreateweb() {
        WebSettings settings = getCurrentWebView().getSettings();
        applyOptimizedSettings(settings);
        SharedPreferences setupprefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean zoombutton = setupprefs.getBoolean("zoomButton", true);
        boolean acceptCookies = setupprefs.getBoolean("acceptCookies", true);
        CookieManager.getInstance().setAcceptCookie(acceptCookies);
        pref = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        darkModeEnabled = pref.getBoolean(KEY_DARK_MODE, false);
        basicAuthEnabled = pref.getBoolean(KEY_BASIC_AUTH, false);
        zoomEnabled = pref.getBoolean(KEY_ZOOM_ENABLED, false);
        jsEnabled = pref.getBoolean(KEY_JS_ENABLED, false);
        imgBlockEnabled = pref.getBoolean(KEY_IMG_BLOCK_ENABLED, false);
        uaEnabled = pref.getBoolean(KEY_UA_ENABLED, false);
        deskuaEnabled = pref.getBoolean(KEY_DESKUA_ENABLED, false);
        ct3uaEnabled = pref.getBoolean(KEY_CT3UA_ENABLED, false);

        if (uaEnabled) {
            WebSettings s = getCurrentWebView().getSettings();
            s.setUserAgentString("DoCoMo/2.0 SH902i(c100;TB)");
        }

        if (!uaEnabled && !deskuaEnabled && !ct3uaEnabled) {
            WebSettings s = getCurrentWebView().getSettings();
            String orig = originalUserAgents.get(getCurrentWebView());
            if (orig != null) s.setUserAgentString(orig + APPEND_STR);
            else s.setUserAgentString(APPEND_STR.trim());
        }

        if (deskuaEnabled) {
            WebSettings s = getCurrentWebView().getSettings();
            String orig = originalUserAgents.get(getCurrentWebView());
            if (orig == null) orig = s.getUserAgentString();
            String desktop = orig.replace("Mobile", "").replace("Android", "");
            s.setUserAgentString(desktop + APPEND_STR);
        }

        if (ct3uaEnabled) {
            WebSettings s = getCurrentWebView().getSettings();
            s.setUserAgentString("Mozilla/5.0 (Linux; Android 7.0; TAB-A03-BR3 Build/02.05.000; wv) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/83.0.4103.106 Safari/537.36");
        }

        if (imgBlockEnabled) {
            WebSettings s = getCurrentWebView().getSettings();
            s.setLoadsImagesAutomatically(false);
        }
        if (!imgBlockEnabled) {
            WebSettings s = getCurrentWebView().getSettings();
            s.setLoadsImagesAutomatically(true);
        }
        if (!zoombutton) {
            zoomButton.setVisibility(View.GONE);
        }
        reloadCurrentPage();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_HISTORY && resultCode == RESULT_OK && data != null) {
            // URLが返ってきた場合 → そのままWebViewで開く
            String url = data.getStringExtra("selected_url");
            if (url != null) {
                getCurrentWebView().loadUrl(url);
                return; // URLが返ってきたらそれを優先
            }

            String addnewtaburl = data.getStringExtra("historyAddNewTab");
            if (addnewtaburl != null) {
                addNewTab(addnewtaburl);
                return; // URLが返ってきたらそれを優先
            }

            // 更新後の履歴リストを受け取る場合
            ArrayList<HistoryItem> updatedList =
                    (ArrayList<HistoryItem>) data.getSerializableExtra("history_list");
            if (updatedList != null) {
                historyItems.clear();
                historyItems.addAll(updatedList);
                saveHistory();
            }
        }
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            String action = data.getStringExtra("action");
            if ("switchTab".equals(action)) {
                int idx = data.getIntExtra("tabIndex", -1);
                if (idx >= 0) switchToTab(idx);

            } else if ("closeTabs".equals(action)) {
                ArrayList<Integer> closed = data.getIntegerArrayListExtra("closedIndices");
                if (closed != null) {
                    // index がずれるので降順で処理
                    Collections.sort(closed, Collections.reverseOrder());
                    for (int idx : closed) {
                        closeTab(idx);
                    }
                }
            } else if ("closeTabsAddTab".equals(action)) {
                ArrayList<Integer> closed = data.getIntegerArrayListExtra("closedIndices");
                if (closed != null) {
                    // index がずれるので降順で処理
                    Collections.sort(closed, Collections.reverseOrder());
                    for (int idx : closed) {
                        closeTab(idx);
                        newStartPage();
                    }
                }
            } else if ("newTab".equals(action)) {
                newStartPage();
            } else if ("contact".equals(action)) {
                addNewTab("https://forms.gle/BEmR3Gms7LazZvPs7");
            } else if ("recreate".equals(action)) {
                recreateweb();
            } else if ("negapoji".equals(action)) {
                new Handler().postDelayed(() -> {
                    applyNegapoji();
                }, 500);
            } else if ("translate".equals(action)) {
                new Handler().postDelayed(() -> {
                    translatePageToJapanese();
                }, 500);
            } else if ("screenshot".equals(action)) {
                new Handler().postDelayed(() -> {
                    takeScreenshot();
                }, 500);
            } else if ("settings".equals(action)) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivityForResult(intent, 1001);
            } else if ("closeTabsSettings".equals(action)) {
                ArrayList<Integer> closed = data.getIntegerArrayListExtra("closedIndices");
                if (closed != null) {
                    // index がずれるので降順で処理
                    Collections.sort(closed, Collections.reverseOrder());
                    for (int idx : closed) {
                        closeTab(idx);
                        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                        startActivityForResult(intent, 1001);
                    }
                }
            } else if ("secret".equals(action)) {
                startActivity(new Intent(MainActivity.this, SecretActivity.class));
            } else if ("closeTabsSecret".equals(action)) {
                ArrayList<Integer> closed = data.getIntegerArrayListExtra("closedIndices");
                if (closed != null) {
                    // index がずれるので降順で処理
                    Collections.sort(closed, Collections.reverseOrder());
                    for (int idx : closed) {
                        closeTab(idx);
                        startActivity(new Intent(MainActivity.this, SecretActivity.class));
                    }
                }
            } else if ("savedpage".equals(action)) {
                showTabMenu();
                startActivity(new Intent(MainActivity.this, HisBMActivity.class));
                overridePendingTransition(R.anim.no_animation, R.anim.fadeout);
            } else if ("closeTabsSavedpage".equals(action)) {
                ArrayList<Integer> closed = data.getIntegerArrayListExtra("closedIndices");
                if (closed != null) {
                    // index がずれるので降順で処理
                    Collections.sort(closed, Collections.reverseOrder());
                    for (int idx : closed) {
                        closeTab(idx);
                        showTabMenu();
                        startActivity(new Intent(MainActivity.this, HisBMActivity.class));
                        overridePendingTransition(R.anim.no_animation, R.anim.fadeout);
                    }
                }
            } else if ("history".equals(action)) {
                showTabMenu();
                showHistoryDialog();
            } else if ("closeTabsHistory".equals(action)) {
                ArrayList<Integer> closed = data.getIntegerArrayListExtra("closedIndices");
                if (closed != null) {
                    // index がずれるので降順で処理
                    Collections.sort(closed, Collections.reverseOrder());
                    for (int idx : closed) {
                        closeTab(idx);
                        showTabMenu();
                        showHistoryDialog();
                    }
                }
            } else if ("bookmark".equals(action)) {
                showTabMenu();
                startActivity(new Intent(MainActivity.this, BmHisActivity.class));
                overridePendingTransition(R.anim.no_animation, R.anim.fadeout);
            } else if ("closeTabsBookmark".equals(action)) {
                ArrayList<Integer> closed = data.getIntegerArrayListExtra("closedIndices");
                if (closed != null) {
                    // index がずれるので降順で処理
                    Collections.sort(closed, Collections.reverseOrder());
                    for (int idx : closed) {
                        closeTab(idx);
                        showTabMenu();
                        startActivity(new Intent(MainActivity.this, BmHisActivity.class));
                        overridePendingTransition(R.anim.no_animation, R.anim.fadeout);
                    }
                }
            } else if ("allTabClose".equals(action)) {
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setMessage("タブを削除しています。しばらくお待ち下さい...")
                        .create(); // ← show() ではなく create()

                // 外をタップしても閉じない
                dialog.setCanceledOnTouchOutside(false);
                // 必要なら戻るキーでも閉じないように
                dialog.setCancelable(false);
                dialog.show();
                donttouch.setVisibility(View.VISIBLE);
                new Handler().postDelayed(() -> {
                    closeAllTabs();
                    donttouch.setVisibility(View.GONE);
                    dialog.dismiss();
                }, 500);
            }
        }
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

    private void captureTabSnapshot(WebView webView) {
        View root = getWindow().getDecorView().getRootView();
        int w = root.getWidth();
        int h = root.getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Handler handler = new Handler(Looper.getMainLooper());
            PixelCopy.request(getWindow(), bmp, new PixelCopy.OnPixelCopyFinishedListener() {
                @Override
                public void onPixelCopyFinished(int copyResult) {
                    if (copyResult == PixelCopy.SUCCESS) {
                        tabSnapshots.put(webView, bmp);
                        Object tag = webView.getTag();
                        int id = -1;
                        if (tag instanceof Integer) id = (Integer) tag;
                        if (id != -1) {
                            final int finalId = id;
                            final Bitmap finalBitmap = bmp;
                            backgroundExecutor.execute(() -> {
                                try {
                                    File outFile = new File(getFilesDir(), "tab_snapshot_" + finalId + ".png");
                                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                                        finalBitmap.compress(Bitmap.CompressFormat.PNG, 80, fos);
                                        fos.flush();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            });
                        }
                    }
                }
            }, handler);
        } else {
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            root.draw(canvas);
            tabSnapshots.put(webView, bmp);
            Object tag = webView.getTag();
            int id = -1;
            if (tag instanceof Integer) id = (Integer) tag;
            if (id != -1) {
                final int finalId = id;
                final Bitmap finalBitmap = bmp;
                backgroundExecutor.execute(() -> {
                    try {
                        File outFile = new File(getFilesDir(), "tab_snapshot_" + finalId + ".png");
                        try (FileOutputStream fos = new FileOutputStream(outFile)) {
                            finalBitmap.compress(Bitmap.CompressFormat.PNG, 80, fos);
                            fos.flush();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    private void showTabMenu() {
        // タブ情報をシングルトンに保存
        TabManager.getInstance().setTabs(tabs, tabSnapshots, webViewContainer);

        Intent intent = new Intent(this, TabListActivity.class);
        intent.putExtra("currentTabIndex", currentTabIndex);
        startActivityForResult(intent, 1001);
        overridePendingTransition(R.anim.tab_in, R.anim.no_animation);
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
            tabSnapshots.clear();
            webViewContainer.removeView(webView);

            // UI更新を確実に反映した後、新規タブを追加
            new Handler(Looper.getMainLooper()).post(() -> {
                newStartPage();
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
        if (url.contains(" ")) {
            url = "https://www.google.com/search?q=" + url;
        } else if (!url.startsWith("https://") && !url.startsWith("http://")) {
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
        isNewTab = true;
        int newId = nextTabId++;
        WebView webView = createWebView(newId);
        webView.loadUrl(url);

        tabs.add(webView);

        // ✅ タブ情報を必ず追加
        if (tabInfos.size() < tabs.size()) {
            tabInfos.add(new TabInfo("読込中...", url, null));
        }

        android.view.animation.Animation fadeIn =
                android.view.animation.AnimationUtils.loadAnimation(this, R.anim.tab_in);
        webView.startAnimation(fadeIn);

        webViewContainer.addView(webView);
        webView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if (nohideurl) return; // 読み込み中は隠さない

                if (scrollY > lastScrollY + 30) {
                    // ↓ 下スクロール → URLバー隠す
                    checkHideUrlBar();
                } else if (scrollY < lastScrollY - 30) {
                    // ↑ 上スクロール → URLバー再表示
                    showUrlBar();
                }
                lastScrollY = scrollY;
            }
        });
        zoomButton.setOnZoomInClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.zoomBy(1.5f);
            }
        });

        zoomButton.setOnZoomOutClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nohideurl = true;
                webView.zoomBy(0.5f);
                new Handler().postDelayed(() -> {
                    nohideurl = false;
                }, 200);
            }
        });
        switchToTab(tabs.size() - 1);
        updateTabCount();
        new Handler().postDelayed(() -> {
            isNewTab = false;
        }, 500);
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
            if (tabSnapshots.containsKey(webView)) {
                Bitmap snap = tabSnapshots.get(webView);
                if (snap != null) {
                    final int finalIdForSnap = id;
                    final Bitmap finalSnap = snap;
                    backgroundExecutor.execute(() -> {
                        try {
                            File outFile = new File(getFilesDir(), "tab_snapshot_" + finalIdForSnap + ".png");
                            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                                finalSnap.compress(Bitmap.CompressFormat.PNG, 80, fos);
                                fos.flush();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
            }
        }
        int currentTabId = (int) getCurrentWebView().getTag();
        prefs.edit()
                .putString(KEY_TABS, tabsArray.toString())
                .putInt(KEY_CURRENT_TAB_ID, currentTabId)
                .apply();
    }

    // ✅ タブ状態の読み込み
    private void loadTabsState() {
        loadTabnoHideurl = true;
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

                File snapFile = new File(getFilesDir(), "tab_snapshot_" + id + ".png");
                if (snapFile.exists()) {
                    try {
                        Bitmap bm = BitmapFactory.decodeFile(snapFile.getAbsolutePath());
                        if (bm != null) {
                            WebView webView = tabs.get(i);
                            tabSnapshots.put(webView, bm);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                WebView webView = createWebView(id);
                webView.setTag(id);
                if (id > maxId) maxId = id;
                tabs.add(webView);
                tabInfos.add(new TabInfo("読込中...", url, null)); // ✅ タブ情報を追加
                webViewContainer.addView(webView);
                webView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
                    @Override
                    public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                        if (nohideurl) return; // 読み込み中は隠さない

                        if (scrollY > lastScrollY + 30) {
                            // ↓ 下スクロール → URLバー隠す
                            checkHideUrlBar();
                        } else if (scrollY < lastScrollY - 30) {
                            // ↑ 上スクロール → URLバー再表示
                            showUrlBar();
                        }
                        lastScrollY = scrollY;
                    }
                });
                zoomButton.setOnZoomInClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        webView.zoomBy(1.5f);
                    }
                });

                zoomButton.setOnZoomOutClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        nohideurl = true;
                        webView.zoomBy(0.5f);
                        new Handler().postDelayed(() -> {
                            nohideurl = false;
                        }, 200);
                    }
                });
                Bundle state = loadBundleFromFile("tab_state_" + id + ".dat");
                if (state != null) {
                    webView.restoreState(state);
                } else {
                    webView.loadUrl(url);
                }
            }
            nextTabId = maxId + 1;

            if (tabs.isEmpty()) {
                newStartPage(); // ✅ タブがない場合、初期タブを作成
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
            new Handler().postDelayed(() -> {
                switchToTab(currentTabIndex);
            }, 1000);
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    WebView webView = tabs.get(currentTabIndex);
                    //updateUrlBar(webView);
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
                    loadTabnoHideurl = false;
                }
            }, 2500);

        } catch (JSONException e) {
            e.printStackTrace();
            newStartPage(); // ✅ JSONエラー時も初期タブを作成
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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            if (connectivityManager != null) {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            }
        }
        // バッテリー残量更新停止
        unregisterReceiver(batteryReceiver);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            if (telephonyManager != null) {
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
            }
        }
        WebView webView = tabs.get(currentTabIndex);
        // 戻ってきたときに実行したい処理
        saveTabsState();
        webView.onPause();
    }

    @Override
    public void onBackPressed() {
        WebView webView = tabs.get(currentTabIndex);
        if (customView != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ((WebChromeClient) webView.getWebChromeClient()).onHideCustomView();
            } else {
                // For older APIs, manually hide the custom view and restore UI
                fullscreenContainer.removeView(customView);
                fullscreenContainer.setVisibility(View.GONE);
                customView = null;
                // Restore other UI elements if necessary
                bottomBar.setVisibility(View.VISIBLE);
                action_Bar.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.VISIBLE);
                urlEditText.setVisibility(View.VISIBLE);
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            }
        } else if (webView.canGoBack()) {
            nohideurl=true;
            webView.goBack();
        } else {
            if (tabs.size() > 1 && currentTabIndex > 0) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastBackPressedTime < 2000) {
                    android.view.animation.Animation fadeIn =
                            android.view.animation.AnimationUtils.loadAnimation(this, R.anim.tab_out);
                    webView.startAnimation(fadeIn);
                    closeTab(currentTabIndex);
                    WebView webViews = tabs.get(currentTabIndex);
                    saveTabsState();
                    webViews.onPause();
                    // 戻ってきたときに実行したい処理
                    String url = webViews.getUrl();
                    webViews.onResume();
                    if (toast != null) {
                        // 既存のToastをキャンセル
                        toast.cancel();
                    }
                    toast = Toast.makeText(this, "タブを閉じました。", Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    if (toast != null) {
                        // 既存のToastをキャンセル
                        toast.cancel();
                    }
                    toast = Toast.makeText(this, "もう一度 戻る でタブを閉じます", Toast.LENGTH_SHORT);
                    toast.show();
                    lastBackPressedTime = currentTime;
                }
            } else {
                // ルートにいる場合は2回押しで終了
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastBackPressedTime < 2000) {
                    // 2秒以内に2回押されたら終了
                    finish();
                    overridePendingTransition(R.anim.no_animation, R.anim.slide_out_down_low);
                } else {
                    if (toast != null) {
                        // 既存のToastをキャンセル
                        toast.cancel();
                    }
                    toast = Toast.makeText(this, "もう一度 戻る で終了します", Toast.LENGTH_SHORT);
                    toast.show();
                    lastBackPressedTime = currentTime;
                }
            }
        }
    }
}