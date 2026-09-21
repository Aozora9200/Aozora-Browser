package com.aozora.aozora;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import net.lingala.zip4j.progress.ProgressMonitor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class FileManager extends Activity {

    private enum SortBy { NAME, DATE, SIZE }

    private enum ConflictAction { ASK, OVERWRITE, SKIP, KEEP_BOTH, CANCEL }

    // ---------------------------------------------------------------- 定数

    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME = "selected_theme";
    private static final int THEME_LIGHT = 0;
    private static final int THEME_DARK = 1;
    private static final int THEME_SYSTEM = 2;

    private static final String KEY_BACKGROUND = "selected_background";
    private static final int BACKGROUND1 = 0;
    private static final int BACKGROUND2 = 1;
    private static final int BACKGROUND3 = 2;
    private static final int BACKGROUND4 = 3;
    private static final int BACKGROUND_CUSTOM = 4;
    private static final String KEY_IMAGE_URI = "image_uri";

    private static final int REQUEST_STORAGE_PERMISSION = 100;

    // 状態の保存キー（画面回転・ダークモード切替・プロセス終了からの復帰用）
    private static final String STATE_DIR = "fm_dir";
    private static final String STATE_ZIP = "fm_zip";
    private static final String STATE_ZIP_PATH = "fm_zip_path";
    private static final String STATE_SORT = "fm_sort";
    private static final String STATE_ASC = "fm_asc";
    private static final String STATE_CLIP = "fm_clip";
    private static final String STATE_CLIP_ACTION = "fm_clip_action";

    private static final Pattern INVALID_NAME = Pattern.compile("[\\\\/:*?\"<>|]");
    private static final int MAX_SEARCH_RESULTS = 5000;
    private static final int MAX_SEARCH_DEPTH = 32;

    // zip4j の圧縮レベル（スピナーの並びと同じ順）
    private static final CompressionLevel[] ZIP_LEVELS = {
            CompressionLevel.NO_COMPRESSION, CompressionLevel.FASTEST, CompressionLevel.FAST,
            CompressionLevel.NORMAL, CompressionLevel.MAXIMUM, CompressionLevel.ULTRA
    };

    // ---------------------------------------------------------------- フィールド

    private ListView listView;
    private ImageView background;
    private SearchView searchView;
    private FileListAdapter adapter;

    private File currentDir;
    private SortBy sortBy = SortBy.NAME;
    private boolean ascending = true;

    private boolean isSearchMode = false;
    private String lastQuery = "";

    private final List<File> clipboardFiles = new ArrayList<>();
    private String clipboardAction = ""; // "copy" or "cut"

    private final Set<File> selectedFiles = new LinkedHashSet<>();
    private boolean isSelectionMode = false;

    private volatile boolean isCancelled = false;
    private ConflictAction globalFileConflictAction = ConflictAction.ASK;
    private volatile CountDownLatch pendingLatch;

    private long lastBackPressedTime = 0;
    private Toast currentToast;

    // 一覧の読み込み（世代番号で古い結果を捨てる）
    private volatile int loadGeneration = 0;
    private final ExecutorService listExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService opExecutor = Executors.newSingleThreadExecutor();
    private final Map<String, int[]> scrollPositions = new HashMap<>();

    // 初期化・復元
    private boolean initialized = false;
    private boolean accessRequested = false;
    private String restoredDirPath;
    private String restoredZipPath;
    private String restoredZipEntryPath = "";

    // ZIP閲覧
    private ZipSession zipSession = null;
    private String currentZipPath = ""; // "" または "dir/sub/"

    // ---------------------------------------------------------------- ライフサイクル

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applySavedTheme();
        setContentView(R.layout.file);
        TouchEffectView.attach(getWindow());
        background = findViewById(R.id.background);
        applySavedBackground();
        applyBackTheme();

        listView = findViewById(R.id.listViewFiles);
        adapter = new FileListAdapter(this, new ArrayList<FileItem>());
        listView.setAdapter(adapter);

        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        listView.setOnItemClickListener((parent, view, position, id) ->
                onItemClicked(adapter.getItem(position)));
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            showItemOptionsDialog(adapter.getItem(position));
            return true;
        });

        restoreState(savedInstanceState);

        if (savedInstanceState == null) {
            // 前回起動時のZIPプレビュー用キャッシュを掃除
            opExecutor.execute(() -> deleteRecursively(new File(getCacheDir(), "zip_preview")));
        }
        // 一覧の読み込みは onResume で行う（権限取得のために設定画面へ出て戻ってきた場合も同じ経路で初期化できる）
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!initialized) {
            if (hasStorageAccess()) {
                initializeOnce();
            } else if (!accessRequested) {
                accessRequested = true;
                requestStorageAccess();
            }
            return;
        }

        // 他のアプリから戻ってきた時は、今いる場所のまま・スクロール位置も保ったまま静かに更新する
        if (zipSession == null && !isSearchMode && currentDir != null) {
            refreshDirectory(true);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (currentDir != null) out.putString(STATE_DIR, currentDir.getAbsolutePath());
        if (zipSession != null) {
            out.putString(STATE_ZIP, zipSession.source.getAbsolutePath());
            out.putString(STATE_ZIP_PATH, currentZipPath);
        }
        out.putInt(STATE_SORT, sortBy.ordinal());
        out.putBoolean(STATE_ASC, ascending);

        ArrayList<String> clip = new ArrayList<>();
        for (File f : clipboardFiles) clip.add(f.getAbsolutePath());
        out.putStringArrayList(STATE_CLIP, clip);
        out.putString(STATE_CLIP_ACTION, clipboardAction);
    }

    private void restoreState(Bundle s) {
        if (s == null) return;
        restoredDirPath = s.getString(STATE_DIR);
        restoredZipPath = s.getString(STATE_ZIP);
        restoredZipEntryPath = s.getString(STATE_ZIP_PATH, "");
        int sort = s.getInt(STATE_SORT, 0);
        if (sort >= 0 && sort < SortBy.values().length) sortBy = SortBy.values()[sort];
        ascending = s.getBoolean(STATE_ASC, true);

        ArrayList<String> clip = s.getStringArrayList(STATE_CLIP);
        if (clip != null) {
            for (String p : clip) clipboardFiles.add(new File(p));
        }
        clipboardAction = s.getString(STATE_CLIP_ACTION, "");
    }

    /** 最初の1回だけ、保存されていた場所（無ければストレージのルート）を表示する */
    private void initializeOnce() {
        if (initialized) return;
        initialized = true;

        File start = null;
        if (restoredDirPath != null) {
            File f = new File(restoredDirPath);
            if (f.isDirectory()) start = f;
        }
        if (start == null) start = Environment.getExternalStorageDirectory();
        currentDir = start;

        if (restoredZipPath != null) {
            File zip = new File(restoredZipPath);
            if (zip.isFile()) {
                openZipFile(zip, restoredZipEntryPath);
                return;
            }
        }
        showDirectory(start);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        listExecutor.shutdownNow();
        opExecutor.shutdownNow();
        CountDownLatch latch = pendingLatch;
        if (latch != null) latch.countDown();
        closeSession(zipSession);
        zipSession = null;
    }

    // ---------------------------------------------------------------- 権限

    private boolean hasStorageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestStorageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
                toast("ファイルアクセスを許可してください", Toast.LENGTH_LONG);
            } catch (Exception e) {
                e.printStackTrace();
                toast("設定画面が開けませんでした");
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_STORAGE_PERMISSION);
        }
    }

    // ---------------------------------------------------------------- テーマ・背景

    private void applySavedBackground() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int bg = prefs.getInt(KEY_BACKGROUND, BACKGROUND1);
        ImageView image = findViewById(R.id.backgroundImage);

        if (bg == BACKGROUND4) {
            image.setVisibility(View.GONE);
            background.setVisibility(View.GONE);
            return;
        }
        image.setVisibility(View.VISIBLE);
        background.setVisibility(View.VISIBLE);

        switch (bg) {
            case BACKGROUND1:
                image.setImageResource(R.drawable.setupback);
                break;
            case BACKGROUND2:
                image.setImageResource(R.drawable.background2);
                break;
            case BACKGROUND3:
                image.setImageResource(R.drawable.background3);
                break;
            case BACKGROUND_CUSTOM:
            default:
                String uriString = prefs.getString(KEY_IMAGE_URI, null);
                if (uriString != null) {
                    try {
                        image.setImageURI(Uri.parse(uriString));
                        break;
                    } catch (Exception e) {
                        e.printStackTrace(); // 権限を失った URI など
                    }
                }
                image.setImageResource(R.drawable.setupback);
                break;
        }
    }

    private void applySavedTheme() {
        int theme = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getInt(KEY_THEME, THEME_SYSTEM);
        if (theme == THEME_LIGHT) {
            setTheme(android.R.style.Theme_Holo_Light);
        } else if (theme == THEME_DARK) {
            setTheme(android.R.style.Theme_Holo);
        }
    }

    private void applyBackTheme() {
        int theme = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getInt(KEY_THEME, THEME_SYSTEM);
        int black = ContextCompat.getColor(this, R.color.backgroundBlack);
        int white = ContextCompat.getColor(this, R.color.backgroundWhite);

        if (theme == THEME_LIGHT) {
            background.setBackgroundColor(white);
        } else if (theme == THEME_DARK) {
            background.setBackgroundColor(black);
        } else {
            int night = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            background.setBackgroundColor(night == Configuration.UI_MODE_NIGHT_YES ? black : white);
        }
    }

    // ---------------------------------------------------------------- メニュー

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_file, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_select) {
            toggleSelectionMode();
            return true;
        } else if (id == R.id.file_close) {
            closeScreen();
            return true;
        } else if (id == R.id.sort_name) {
            sortBy = SortBy.NAME;
            reloadCurrentView();
            return true;
        } else if (id == R.id.sort_date) {
            sortBy = SortBy.DATE;
            reloadCurrentView();
            return true;
        } else if (id == R.id.sort_size) {
            sortBy = SortBy.SIZE;
            reloadCurrentView();
            return true;
        } else if (id == R.id.toggle_order) {
            ascending = !ascending;
            reloadCurrentView();
            return true;
        } else if (id == R.id.menu_create_folder) {
            if (rejectInZip()) return true;
            showCreateFolderDialog();
            return true;
        } else if (id == R.id.menu_new_file) {
            if (rejectInZip()) return true;
            showNewFileDialog();
            return true;
        } else if (id == R.id.menu_info) {
            info();
            return true;
        } else if (id == R.id.action_paste) {
            if (rejectInZip()) return true;
            if (!clipboardFiles.isEmpty() && !clipboardAction.isEmpty()) {
                startPaste(new ArrayList<>(clipboardFiles), currentDir, clipboardAction);
            } else {
                toast("コピーまたはカットしたファイルがありません");
            }
            return true;
        } else if (id == android.R.id.home) {
            if (!navigateUp()) closeScreen();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean rejectInZip() {
        if (zipSession != null) {
            toast("ZIPの中では使えません");
            return true;
        }
        return false;
    }

    private void closeScreen() {
        finish();
        overridePendingTransition(R.anim.no_animation, R.anim.slide_out_down_low);
    }

    private void info() {
        new AlertDialog.Builder(this)
                .setTitle("アプリ情報")
                .setMessage("ファイルマネージャ バージョン 1.0.0 -Release")
                .setPositiveButton("OK", null)
                .show();
    }

    // ---------------------------------------------------------------- 戻る操作

    @Override
    public void onBackPressed() {
        if (navigateUp(true)) return;

        // ルートにいる場合は2回押しで終了
        long now = System.currentTimeMillis();
        if (now - lastBackPressedTime < 2000) {
            closeScreen();
        } else {
            toast("もう一度 戻る で終了します");
            lastBackPressedTime = now;
        }
    }

    private boolean navigateUp() {
        return navigateUp(false);
    }

    /**
     * 1つ上へ戻る。アプリ内で処理できたら true。
     * stopAtRoot: 戻るキー用。ストレージのルートより上には行かない（ホームボタンは従来どおり上まで行く）
     */
    private boolean navigateUp(boolean stopAtRoot) {
        if (isSearchMode) {
            exitSearch();
            return true;
        }
        if (zipSession != null) {
            zipGoUp();
            return true;
        }
        File parent = currentDir != null ? currentDir.getParentFile() : null;
        if (parent == null) return false;
        if (stopAtRoot && currentDir.getAbsolutePath()
                .equals(Environment.getExternalStorageDirectory().getAbsolutePath())) {
            return false;
        }
        loadDirectory(parent);
        return true;
    }

    // ---------------------------------------------------------------- 一覧の読み込み

    private interface ListProducer {
        List<FileItem> produce(int generation) throws Exception;
    }

    private boolean isStale(int generation) {
        return generation != loadGeneration;
    }

    /** 場所を移動する（今の場所のスクロール位置を覚えてから移動） */
    private void loadDirectory(File dir) {
        if (dir == null) return;
        saveScroll();
        leaveSearchUi();
        currentDir = dir;
        showDirectory(dir);
    }

    /** 覚えているスクロール位置があれば復元しつつ、dir を表示する */
    private void showDirectory(File dir) {
        requestListing(dir, scrollFor(dir.getAbsolutePath()));
    }

    /** その場で更新する。keepScroll=true なら今のスクロール位置を保つ */
    private void refreshDirectory(boolean keepScroll) {
        if (currentDir == null) return;
        requestListing(currentDir, keepScroll ? captureScroll() : null);
    }

    private void reloadCurrentView() {
        if (isSearchMode) {
            performSearch(lastQuery);
        } else if (zipSession != null) {
            showZipDirectory(currentZipPath);
        } else {
            refreshDirectory(false);
        }
    }

    private void requestListing(final File dir, int[] scroll) {
        final SortBy by = sortBy;
        final boolean asc = ascending;
        loadAsync(gen -> listDirectory(dir, by, asc), scroll, dir.getAbsolutePath(), null);
    }

    /** バックグラウンドで一覧を作り、最新の要求の結果だけを画面に反映する */
    private void loadAsync(final ListProducer producer, final int[] scroll,
                           String loadingTitle, final String doneTitle) {
        final int gen = ++loadGeneration;
        setTitle(loadingTitle);
        listExecutor.execute(() -> {
            List<FileItem> produced;
            try {
                produced = producer.produce(gen);
            } catch (Exception e) {
                e.printStackTrace();
                produced = new ArrayList<>();
            }
            final List<FileItem> result = produced;
            runOnUiThread(() -> {
                if (isStale(gen) || isFinishing()) return;
                applyItems(result, scroll);
                if (doneTitle != null) setTitle(doneTitle + " (" + result.size() + "件)");
            });
        });
    }

    private void applyItems(List<FileItem> items, int[] scroll) {
        adapter.setItems(items);
        if (scroll != null && !items.isEmpty()) {
            listView.setSelectionFromTop(Math.min(scroll[0], items.size() - 1), scroll[1]);
        } else {
            listView.setSelection(0);
        }
    }

    private static List<FileItem> listDirectory(File dir, SortBy by, boolean asc) {
        File[] list = dir.listFiles();
        List<FileItem> folders = new ArrayList<>();
        List<FileItem> regular = new ArrayList<>();

        if (list != null) {
            for (File f : list) {
                if (f.isHidden()) continue;
                FileItem item = new FileItem(f);
                (item.isDirectory ? folders : regular).add(item);
            }
        }

        Comparator<FileItem> cmp = comparatorFor(by, asc);
        Collections.sort(folders, cmp);
        Collections.sort(regular, cmp);

        List<FileItem> result = new ArrayList<>(folders.size() + regular.size() + 1);
        File parent = dir.getParentFile();
        if (parent != null) result.add(FileItem.parentLink(parent));
        result.addAll(folders);
        result.addAll(regular);
        return result;
    }

    private static Comparator<FileItem> comparatorFor(final SortBy by, boolean asc) {
        Comparator<FileItem> c = (a, b) -> {
            int r;
            switch (by) {
                case DATE:
                    r = Long.compare(a.time, b.time);
                    break;
                case SIZE:
                    r = Long.compare(a.size, b.size);
                    break;
                case NAME:
                default:
                    r = 0;
                    break;
            }
            return r != 0 ? r : a.file.getName().compareToIgnoreCase(b.file.getName());
        };
        return asc ? c : Collections.reverseOrder(c);
    }

    // スクロール位置の記憶（フォルダを出入りしても元の位置に戻れる）

    private String currentViewKey() {
        if (isSearchMode) return null;
        if (zipSession != null) return zipKey(zipSession, currentZipPath);
        return currentDir != null ? currentDir.getAbsolutePath() : null;
    }

    private static String zipKey(ZipSession s, String path) {
        return "zip:" + s.source.getAbsolutePath() + "!" + path;
    }

    private int[] captureScroll() {
        if (listView == null || listView.getChildCount() == 0) return null;
        View first = listView.getChildAt(0);
        return new int[]{listView.getFirstVisiblePosition(), first.getTop() - listView.getPaddingTop()};
    }

    private void saveScroll() {
        String key = currentViewKey();
        int[] pos = captureScroll();
        if (key != null && pos != null) scrollPositions.put(key, pos);
    }

    private int[] scrollFor(String key) {
        return scrollPositions.get(key);
    }

    // ---------------------------------------------------------------- クリック

    private void onItemClicked(FileItem item) {
        if (item == null) return;

        if (isSelectionMode && !item.isParentLink) {
            toggleSelection(item.file);
            return;
        }

        if (zipSession != null) {
            onZipItemClicked(item);
            return;
        }

        if (item.isParentLink || item.isDirectory) {
            loadDirectory(item.file);
        } else if (isZipFile(item.file)) {
            openZipFile(item.file, "");
        } else {
            openFile(item.file);
        }
    }

    private static boolean isZipFile(File f) {
        return f.getName().toLowerCase(Locale.ROOT).endsWith(".zip");
    }

    // ---------------------------------------------------------------- 選択モード

    public boolean isSelected(File file) {
        return selectedFiles.contains(file);
    }

    private void toggleSelectionMode() {
        if (isSelectionMode) {
            clearSelection();
            toast("選択モードが解除されました");
        } else {
            isSelectionMode = true;
            selectedFiles.clear();
            toast("選択モードになりました");
        }
    }

    private void toggleSelection(File file) {
        if (!selectedFiles.remove(file)) selectedFiles.add(file);
        adapter.notifyDataSetChanged();

        if (selectedFiles.isEmpty()) {
            isSelectionMode = false;
        } else {
            toast(selectedFiles.size() + " 件選択中");
        }
    }

    private void clearSelection() {
        selectedFiles.clear();
        isSelectionMode = false;
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    // ---------------------------------------------------------------- 長押しメニュー

    private void showOptions(String title, String[] labels, final Runnable[] actions) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setItems(labels, (dialog, which) -> actions[which].run())
                .show();
    }

    private void showItemOptionsDialog(final FileItem item) {
        if (item == null || item.isParentLink) return;
        if (zipSession != null) {
            showZipItemOptions(item);
            return;
        }

        final List<String> labels = new ArrayList<>();
        final List<Runnable> actions = new ArrayList<>();

        labels.add("圧縮");
        actions.add(() -> {
            List<File> targets = new ArrayList<>();
            if (!selectedFiles.isEmpty()) {
                targets.addAll(selectedFiles);
            } else if (item.file.exists()) {
                targets.add(item.file);
            }
            zipDialog(targets, item.file.getName());
        });
        labels.add("カット");
        actions.add(() -> setClipboard("cut", item));
        labels.add("コピー");
        actions.add(() -> setClipboard("copy", item));
        labels.add("削除");
        actions.add(() -> confirmAndDelete(item.file));
        labels.add("名前の変更");
        actions.add(() -> showRenameDialog(item.file));
        if (!item.isDirectory && isZipFile(item.file)) {
            labels.add("ZIPを展開");
            actions.add(() -> extractZipAll(item.file));
        }

        showOptions(item.file.getName(),
                labels.toArray(new String[0]), actions.toArray(new Runnable[0]));
    }

    private void setClipboard(String action, FileItem item) {
        clipboardFiles.clear();
        if (selectedFiles.isEmpty()) {
            clipboardFiles.add(item.file);
        } else {
            clipboardFiles.addAll(selectedFiles);
        }
        clipboardAction = action;

        String verb = "cut".equals(action) ? "カット" : "コピー";
        if (clipboardFiles.size() == 1) {
            toast(verb + "しました: " + clipboardFiles.get(0).getName());
        } else {
            toast(clipboardFiles.size() + " 件を" + verb + "しました");
        }
        clearSelection();
    }

    // ---------------------------------------------------------------- 作成・名前変更

    private static boolean isValidName(String name) {
        return !name.isEmpty() && !name.equals(".") && !name.equals("..")
                && !INVALID_NAME.matcher(name).find();
    }

    private void showNewFileDialog() {
        final EditText input = new EditText(this);
        input.setHint("ファイル名（例: note.txt）");
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        new AlertDialog.Builder(this)
                .setTitle("新しいファイルを作成")
                .setView(input)
                .setPositiveButton("作成", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!isValidName(name)) {
                        toast("無効なファイル名です");
                        return;
                    }

                    File newFile = new File(currentDir, name);
                    if (newFile.exists()) {
                        toast("同名のファイルが既に存在します");
                        return;
                    }

                    try {
                        if (newFile.createNewFile()) {
                            toast("ファイルを作成しました");
                            refreshDirectory(true);
                        } else {
                            toast("ファイルの作成に失敗しました");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        toast("エラー: " + e.getMessage(), Toast.LENGTH_LONG);
                    }
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }

    private void showCreateFolderDialog() {
        final EditText input = new EditText(this);
        input.setHint("フォルダ名:");

        new AlertDialog.Builder(this)
                .setTitle("フォルダの作成")
                .setView(input)
                .setPositiveButton("作成", (dialog, which) -> {
                    String folderName = input.getText().toString().trim();
                    if (!isValidName(folderName)) {
                        toast("無効なフォルダ名です（使用禁止文字が含まれています）", Toast.LENGTH_LONG);
                        return;
                    }

                    // 同名があれば " (1)", " (2)" ... を付ける
                    File newFolder = generateUniqueFolder(currentDir, folderName);
                    if (newFolder.mkdir()) {
                        toast("フォルダを作成しました");
                        refreshDirectory(true);
                    } else {
                        toast("フォルダ作成に失敗しました");
                    }
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }

    private void showRenameDialog(final File file) {
        final EditText input = new EditText(this);
        input.setText(file.getName());

        new AlertDialog.Builder(this)
                .setTitle("名前の変更")
                .setView(input)
                .setPositiveButton("変更", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!isValidName(newName)) {
                        toast("無効な名前です（使用禁止文字あり）");
                        return;
                    }

                    File newFile = new File(file.getParent(), newName);
                    if (newFile.exists()) {
                        toast("すでに存在します");
                        showRenameDialog(file);
                    } else if (file.renameTo(newFile)) {
                        toast("変更成功");
                        refreshDirectory(true);
                    } else {
                        toast("変更失敗");
                    }
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }

    private File generateUniqueFolder(File parent, String baseName) {
        File folder = new File(parent, baseName);
        int counter = 1;
        while (folder.exists()) {
            folder = new File(parent, baseName + " (" + counter + ")");
            counter++;
        }
        return folder;
    }

    private File generateUniqueFile(File file) {
        String name = file.getName();
        String baseName = name;
        String extension = "";

        int dotIndex = name.lastIndexOf('.');
        if (dotIndex != -1) {
            baseName = name.substring(0, dotIndex);
            extension = name.substring(dotIndex);
        }

        File newFile = file;
        int counter = 1;
        while (newFile.exists()) {
            newFile = new File(file.getParent(), baseName + " (" + counter + ")" + extension);
            counter++;
        }
        return newFile;
    }

    // ---------------------------------------------------------------- 進捗ダイアログ

    private class ProgressUi {
        private final AlertDialog dialog;
        private final ProgressBar bar;
        private final TextView fileText;
        private final TextView percentText;
        private long lastUpdate = 0;

        ProgressUi(String title) {
            View view = getLayoutInflater().inflate(R.layout.dialog_progress, null);
            bar = view.findViewById(R.id.progressBar);
            fileText = view.findViewById(R.id.textFileName);
            percentText = view.findViewById(R.id.textProgressPercent);
            dialog = new AlertDialog.Builder(FileManager.this)
                    .setTitle(title)
                    .setView(view)
                    .setCancelable(false)
                    .setNegativeButton("キャンセル", (d, w) -> isCancelled = true)
                    .create();
        }

        /** ダイアログを表示し、描画が始まってから task を実行する */
        void start(Runnable task) {
            dialog.show();
            dialog.getWindow().getDecorView().post(task);
        }

        void setFile(final String text) {
            runOnUiThread(() -> fileText.setText(text));
        }

        /** 更新は 100ms に1回だけ。total が 0 のとき（空ファイルのみ等）でも 0 除算しない */
        void setProgress(long done, long total) {
            long now = SystemClock.uptimeMillis();
            if (now - lastUpdate < 100 && done < total) return;
            lastUpdate = now;
            final int p = total > 0 ? (int) Math.min(100, done * 100 / total) : 100;
            runOnUiThread(() -> {
                bar.setProgress(p);
                percentText.setText(p + "% 完了");
            });
        }

        void dismiss() {
            try {
                if (dialog.isShowing()) dialog.dismiss();
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    // ---------------------------------------------------------------- 貼り付け（コピー・移動）

    private void startPaste(List<File> sources, File targetDir, String action) {
        if (sources.isEmpty() || targetDir == null) return;
        final boolean cut = "cut".equals(action);

        // 1件だけのフォルダが既にある場合は「統合／両方残す」を選んでもらう
        if (sources.size() == 1) {
            File src = sources.get(0);
            File dest = new File(targetDir, src.getName());
            if (src.isDirectory() && dest.exists() && !isSubPath(src, targetDir)
                    && !(cut && isSameFile(src.getParentFile(), targetDir))) {
                showConflictResolutionDialog(src, targetDir, action);
                return;
            }
        }

        List<File[]> pairs = new ArrayList<>();
        for (File src : sources) {
            if (src.isDirectory() && isSubPath(src, targetDir)) {
                toast("フォルダを自分自身の中には貼り付けできません");
                continue;
            }
            if (cut && src.getParentFile() != null && isSameFile(src.getParentFile(), targetDir)) {
                continue; // 同じ場所への移動は何もしない
            }

            File dest = new File(targetDir, src.getName());
            if (dest.exists()) {
                if (src.isDirectory()) {
                    dest = generateUniqueFolder(targetDir, src.getName());
                } else if (isSameFile(src, dest)) {
                    dest = generateUniqueFile(dest); // 同じフォルダ内コピーは「name (1).ext」に
                }
            }
            pairs.add(new File[]{src, dest});
        }

        if (pairs.isEmpty()) {
            toast("貼り付けるものがありません");
            return;
        }
        runTransfer(pairs, action);
    }

    private void showConflictResolutionDialog(final File source, final File targetDir, final String action) {
        new AlertDialog.Builder(this)
                .setTitle("フォルダが既に存在します")
                .setMessage("フォルダ \"" + source.getName() + "\" は既に存在します。\nどうしますか？")
                .setPositiveButton("統合する", (dialog, which) -> {
                    File dest = new File(targetDir, source.getName());
                    runTransfer(Collections.singletonList(new File[]{source, dest}), action);
                })
                .setNegativeButton("両方残す", (dialog, which) -> {
                    File dest = generateUniqueFolder(targetDir, source.getName());
                    runTransfer(Collections.singletonList(new File[]{source, dest}), action);
                })
                .setNeutralButton("キャンセル", null)
                .show();
    }

    private void runTransfer(final List<File[]> pairs, final String action) {
        final boolean cut = "cut".equals(action);
        isCancelled = false;
        globalFileConflictAction = ConflictAction.ASK; // 「すべてに適用」は今回の操作だけ有効

        final ProgressUi ui = new ProgressUi(cut ? "移動中..." : "コピー中...");
        ui.start(() -> opExecutor.execute(() -> {
            long[] sizes = new long[pairs.size()];
            long total = 0;
            for (int i = 0; i < sizes.length; i++) {
                sizes[i] = calculateTotalSize(pairs.get(i)[0]);
                total += sizes[i];
            }

            long[] done = {0};
            boolean success = true;

            for (int i = 0; i < pairs.size(); i++) {
                if (isCancelled) break;
                File src = pairs.get(i)[0];
                File dest = pairs.get(i)[1];

                // 同じドライブ内の移動は rename だけで一瞬で終わる
                if (cut && !dest.exists() && src.renameTo(dest)) {
                    done[0] += sizes[i];
                    ui.setProgress(done[0], total);
                    continue;
                }

                if (!copyRecursively(src, dest, ui, total, done)) {
                    success = false;
                    break;
                }
                if (cut && !isCancelled) deleteRecursively(src);
            }

            final boolean ok = success;
            runOnUiThread(() -> {
                ui.dismiss();
                if (isCancelled) {
                    toast("キャンセルしました");
                } else if (ok) {
                    toast(cut ? "移動が完了しました" : "コピーが完了しました");
                } else {
                    toast("一部のファイルで失敗しました");
                }
                clipboardFiles.clear();
                clipboardAction = "";
                refreshDirectory(true);
            });
        }));
    }

    private boolean copyRecursively(File src, File dest, ProgressUi ui, long total, long[] done) {
        if (isCancelled) return false;

        if (src.isDirectory()) {
            if (!dest.isDirectory() && !dest.mkdirs()) return false;
            File[] children = src.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!copyRecursively(child, new File(dest, child.getName()), ui, total, done)) {
                        return false;
                    }
                }
            }
            return true;
        }

        File target = dest;
        if (dest.exists()) {
            if (isSameFile(src, dest)) return true; // 自分自身への上書きで中身が消えるのを防ぐ
            target = resolveFileConflict(dest);
            if (target == null) {
                done[0] += src.length();
                return !isCancelled; // スキップ
            }
        }

        ui.setFile("コピー中: " + src.getName());
        return copyFile(src, target, ui, total, done);
    }

    private boolean copyFile(File src, File dest, ProgressUi ui, long total, long[] done) {
        boolean started = false;
        boolean completed = false;
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dest)) {
            started = true;
            byte[] buffer = new byte[128 * 1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                if (isCancelled) return false;
                out.write(buffer, 0, len);
                done[0] += len;
                ui.setProgress(done[0], total);
            }
            completed = true;
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (started && !completed) dest.delete(); // 中断・失敗した書きかけファイルを残さない
        }
    }

    /** 上書き/両方残す/スキップの確認。書き込み先を返す（null ならスキップ or キャンセル） */
    private File resolveFileConflict(File dest) {
        ConflictAction action = globalFileConflictAction != ConflictAction.ASK
                ? globalFileConflictAction
                : askFileConflict(dest);

        switch (action) {
            case OVERWRITE:
                return dest;
            case KEEP_BOTH:
                return generateUniqueFile(dest);
            case CANCEL:
                isCancelled = true;
                return null;
            case SKIP:
            default:
                return null;
        }
    }

    private ConflictAction askFileConflict(final File dest) {
        final ConflictAction[] choice = new ConflictAction[1];
        final boolean[] applyAll = new boolean[1];
        final CountDownLatch latch = new CountDownLatch(1);
        pendingLatch = latch;

        runOnUiThread(() -> {
            if (isFinishing()) {
                latch.countDown();
                return;
            }
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_conflict, null);
            final CheckBox checkBox = dialogView.findViewById(R.id.checkboxApplyToAll);

            new AlertDialog.Builder(this)
                    .setTitle("ファイル")
                    .setMessage("ファイル \"" + dest.getName() + "\" は既に存在します。")
                    .setView(dialogView)
                    .setPositiveButton("上書き", (d, w) -> {
                        choice[0] = ConflictAction.OVERWRITE;
                        applyAll[0] = checkBox.isChecked();
                        latch.countDown();
                    })
                    .setNegativeButton("スキップ", (d, w) -> {
                        choice[0] = ConflictAction.SKIP;
                        applyAll[0] = checkBox.isChecked();
                        latch.countDown();
                    })
                    .setNeutralButton("両方残す", (d, w) -> {
                        choice[0] = ConflictAction.KEEP_BOTH;
                        applyAll[0] = checkBox.isChecked();
                        latch.countDown();
                    })
                    .setCancelable(false)
                    .show();
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            return ConflictAction.CANCEL;
        } finally {
            pendingLatch = null;
        }

        ConflictAction result = choice[0] != null ? choice[0] : ConflictAction.CANCEL;
        if (applyAll[0] && result != ConflictAction.CANCEL) globalFileConflictAction = result;
        return result;
    }

    // ---------------------------------------------------------------- 削除

    private void confirmAndDelete(File file) {
        final List<File> targets = new ArrayList<>();
        String message;
        if (selectedFiles.isEmpty()) {
            targets.add(file);
            message = file.getName() + " を削除しますか？";
        } else {
            targets.addAll(selectedFiles);
            message = targets.size() + " 件のファイル／フォルダを削除しますか？\nこの操作は元に戻せません。";
        }

        new AlertDialog.Builder(this)
                .setTitle("削除確認")
                .setMessage(message)
                .setPositiveButton("削除", (dialog, which) -> startDelete(targets))
                .setNegativeButton("キャンセル", null)
                .show();
    }

    private void startDelete(final List<File> targets) {
        isCancelled = false;
        final ProgressUi ui = new ProgressUi("削除中...");
        ui.start(() -> opExecutor.execute(() -> {
            int total = targets.size();
            int done = 0;
            boolean allOk = true;

            for (File f : targets) {
                if (isCancelled) break;
                ui.setFile("削除中: " + f.getName());
                if (!deleteRecursively(f)) allOk = false;
                done++;
                ui.setProgress(done, total);
            }

            final boolean ok = allOk;
            runOnUiThread(() -> {
                ui.dismiss();
                if (isCancelled) {
                    toast("削除をキャンセルしました");
                } else {
                    toast(ok ? "削除が完了しました" : "削除できなかった項目があります");
                }
                clearSelection();
                refreshDirectory(true);
            });
        }));
    }

    private static boolean deleteRecursively(File file) {
        boolean ok = true;
        // シンボリックリンクの先まで消してしまわないよう、リンクは辿らない
        if (file.isDirectory() && !isSymbolicLink(file)) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    ok &= deleteRecursively(child);
                }
            }
        }
        return file.delete() && ok;
    }

    private static boolean isSymbolicLink(File file) {
        try {
            File parent = file.getParentFile();
            File canon = (parent == null) ? file : new File(parent.getCanonicalFile(), file.getName());
            return !canon.getCanonicalFile().equals(canon.getAbsoluteFile());
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean isSameFile(File a, File b) {
        if (a == null || b == null) return false;
        try {
            return a.getCanonicalPath().equals(b.getCanonicalPath());
        } catch (IOException e) {
            return false;
        }
    }

    /** child が parent と同じ、または parent の中にあるか */
    private static boolean isSubPath(File parent, File child) {
        try {
            String p = parent.getCanonicalPath();
            String c = child.getCanonicalPath();
            return c.equals(p) || c.startsWith(p + File.separator);
        } catch (IOException e) {
            return false;
        }
    }

    private static long calculateTotalSize(File file) {
        if (file.isFile()) return file.length();

        long total = 0;
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                total += calculateTotalSize(child);
            }
        }
        return total;
    }

    // ---------------------------------------------------------------- 検索

    private void performSearch(String query) {
        if (query == null) return;
        final String keyword = query.trim().toLowerCase(Locale.ROOT);
        if (keyword.isEmpty()) return;

        saveScroll();
        isSearchMode = true;
        lastQuery = query;

        final ZipSession zip = zipSession;
        final File root = currentDir;
        final SortBy by = sortBy;
        final boolean asc = ascending;

        loadAsync(gen -> {
            List<FileItem> results = new ArrayList<>();
            if (zip != null) {
                for (ZipNode n : zip.index.values()) {
                    if (results.size() >= MAX_SEARCH_RESULTS || isStale(gen)) break;
                    if (n.name.toLowerCase(Locale.ROOT).contains(keyword)) results.add(zipItem(n));
                }
            } else if (root != null) {
                searchRecursive(root, keyword, results, gen, 0);
            }
            Collections.sort(results, comparatorFor(by, asc));
            return results;
        }, null, "検索中: " + query, "検索: " + query);
    }

    private void searchRecursive(File dir, String keyword, List<FileItem> out, int gen, int depth) {
        if (depth > MAX_SEARCH_DEPTH) return;
        File[] children = dir.listFiles();
        if (children == null) return;

        for (File child : children) {
            if (isStale(gen) || out.size() >= MAX_SEARCH_RESULTS) return;
            if (child.isHidden()) continue;

            if (child.getName().toLowerCase(Locale.ROOT).contains(keyword)) {
                out.add(new FileItem(child));
            }
            if (child.isDirectory()) {
                searchRecursive(child, keyword, out, gen, depth + 1);
            }
        }
    }

    private void leaveSearchUi() {
        if (!isSearchMode) return;
        isSearchMode = false;
        searchView.setQuery("", false);
        searchView.clearFocus();
    }

    private void exitSearch() {
        leaveSearchUi();
        if (zipSession != null) {
            showZipDirectory(currentZipPath);
        } else if (currentDir != null) {
            showDirectory(currentDir);
        }
    }

    // ---------------------------------------------------------------- ファイルを開く

    private String getMimeType(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        String type = null;
        int dot = name.lastIndexOf('.');
        if (dot != -1) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substring(dot + 1));
        }
        if (type == null) {
            if (name.endsWith(".mp4")) type = "video/mp4";
            else if (name.endsWith(".pdf")) type = "application/pdf";
            else if (name.endsWith(".jpg") || name.endsWith(".jpeg")) type = "image/jpeg";
            else if (name.endsWith(".png")) type = "image/png";
        }
        return type != null ? type : "*/*";
    }

    private void openFile(File file) {
        try {
            Uri fileUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                fileUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            } else {
                fileUri = Uri.fromFile(file);
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, getMimeType(file));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            toast("対応アプリが見つかりません");
        } catch (Exception e) {
            e.printStackTrace();
            toast("ファイルを開けません: " + e.getMessage());
        }
    }

    // ================================================================
    //  ZIP 閲覧・展開
    // ================================================================

    /** ZIP内の1エントリ（フォルダは実体が無くても、パスから補完して作る） */
    private static class ZipNode {
        final String name;
        final String path;  // ZIP内フルパス（フォルダは末尾 "/"）
        final boolean isDir;
        long size;
        long time;
        FileHeader header;
        final Map<String, ZipNode> children;

        ZipNode(String name, String path, boolean isDir) {
            this.name = name;
            this.path = path;
            this.isDir = isDir;
            this.children = isDir ? new HashMap<String, ZipNode>() : null;
        }
    }

    /** 開いているZIP。ヘッダ一覧は最初に1回だけ読み、以降のフォルダ移動はメモリ上のツリーで行う */
    private static class ZipSession {
        final File source;
        final ZipFile zip;
        final ZipNode root;
        final Map<String, ZipNode> index; // 末尾 "/" 無しのパス → ノード
        boolean encrypted;
        char[] password;

        ZipSession(File source, ZipFile zip, ZipNode root, Map<String, ZipNode> index, boolean encrypted) {
            this.source = source;
            this.zip = zip;
            this.root = root;
            this.index = index;
            this.encrypted = encrypted;
        }
    }

    private interface ZipLoadListener {
        void onResult(ZipSession session, Exception error);
    }

    // ---- 読み込み

    private void loadZipSession(final File file, final ZipLoadListener listener) {
        listExecutor.execute(() -> {
            try {
                ZipFile zf = new ZipFile(file);
                if (!zf.isValidZipFile()) throw new IOException("ZIPファイルとして読み込めません");
                List<FileHeader> headers = zf.getFileHeaders();
                if (headers == null) headers = new ArrayList<>();

                // Windows標準で作ったZIPなどの Shift_JIS ファイル名は UTF-8 で読むと文字化けするので、その場合だけ読み直す
                if (hasBrokenNames(headers)) {
                    ZipFile alt = new ZipFile(file);
                    alt.setCharset(japaneseCharset());
                    List<FileHeader> altHeaders = alt.getFileHeaders();
                    if (altHeaders != null && !hasBrokenNames(altHeaders)) {
                        zf = alt;
                        headers = altHeaders;
                    }
                }

                final ZipSession session = buildSession(file, zf, headers);
                runOnUiThread(() -> listener.onResult(session, null));
            } catch (final Exception e) {
                runOnUiThread(() -> listener.onResult(null, e));
            }
        });
    }

    private static boolean hasBrokenNames(List<FileHeader> headers) {
        for (FileHeader h : headers) {
            String n = h.getFileName();
            if (n != null && n.indexOf('\uFFFD') >= 0) return true;
        }
        return false;
    }

    private static Charset japaneseCharset() {
        try {
            return Charset.forName("windows-31j");
        } catch (Exception e) {
            return Charset.forName("Shift_JIS");
        }
    }

    private static ZipSession buildSession(File file, ZipFile zf, List<FileHeader> headers) {
        ZipNode root = new ZipNode("", "", true);
        Map<String, ZipNode> index = new HashMap<>();
        boolean encrypted = false;

        for (FileHeader h : headers) {
            String name = h.getFileName();
            if (name == null || name.isEmpty()) continue;
            name = name.replace('\\', '/');

            boolean dirEntry = h.isDirectory() || name.endsWith("/");
            // フォルダの実体は暗号化されないので、ファイルだけで判定する
            if (h.isEncrypted() && !dirEntry) encrypted = true;

            String[] parts = name.split("/");
            boolean unsafe = false;
            for (String p : parts) {
                if ("..".equals(p)) {
                    unsafe = true; // ディレクトリトラバーサルを狙った名前は無視
                    break;
                }
            }
            if (unsafe) continue;

            ZipNode cur = root;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length && cur.children != null; i++) {
                String part = parts[i];
                if (part.isEmpty() || ".".equals(part)) continue;

                boolean last = (i == parts.length - 1);
                boolean isDirPart = !last || dirEntry;
                sb.append(part);
                String key = sb.toString();

                ZipNode child = cur.children.get(part);
                if (child == null) {
                    child = new ZipNode(part, isDirPart ? key + "/" : key, isDirPart);
                    cur.children.put(part, child);
                    index.put(key, child);
                }
                if (last) {
                    child.header = h;
                    if (!child.isDir) child.size = h.getUncompressedSize();
                    child.time = h.getLastModifiedTimeEpoch();
                }
                cur = child;
                sb.append('/');
            }
        }
        return new ZipSession(file, zf, root, index, encrypted);
    }

    private static void closeSession(ZipSession s) {
        if (s == null) return;
        try {
            s.zip.close();
        } catch (Exception ignored) {
        }
    }

    // ---- 表示

    private void openZipFile(final File zipFile, final String startPath) {
        saveScroll();
        final int gen = loadGeneration + 1;
        loadGeneration = gen; // 読み込み中に届く古い一覧結果を捨てる
        setTitle(zipFile.getName() + " を読み込み中...");

        loadZipSession(zipFile, (session, error) -> {
            if (isStale(gen) || isFinishing()) {
                closeSession(session);
                return;
            }
            if (error != null) {
                toast("ZIPを開けません: " + error.getMessage(), Toast.LENGTH_LONG);
                reloadCurrentView();
                return;
            }
            enterZipView(session, startPath);
        });
    }

    private void enterZipView(ZipSession session, String startPath) {
        closeSession(zipSession);
        leaveSearchUi();
        clearSelection();
        zipSession = session;
        showZipDirectory(startPath == null ? "" : startPath);
    }

    private void exitZipView() {
        closeSession(zipSession);
        zipSession = null;
        currentZipPath = "";
        clearSelection();
        if (currentDir != null) showDirectory(currentDir);
    }

    private void zipGoUp() {
        if (zipSession == null) return;
        if (currentZipPath.isEmpty()) {
            exitZipView();
            return;
        }
        int idx = currentZipPath.lastIndexOf('/', currentZipPath.length() - 2);
        showZipDirectory(idx >= 0 ? currentZipPath.substring(0, idx + 1) : "");
    }

    private void showZipDirectory(String path) {
        final ZipSession s = zipSession;
        if (s == null) return;

        ZipNode node = path.isEmpty() ? s.root : s.index.get(stripSlash(path));
        if (node == null || !node.isDir) {
            path = "";
            node = s.root;
        }
        currentZipPath = path;

        final ZipNode dirNode = node;
        final SortBy by = sortBy;
        final boolean asc = ascending;
        loadAsync(gen -> zipItems(dirNode, by, asc),
                scrollFor(zipKey(s, path)),
                s.source.getName() + "!/" + path, null);
    }

    private static String stripSlash(String p) {
        return p.endsWith("/") ? p.substring(0, p.length() - 1) : p;
    }

    private static FileItem zipItem(ZipNode n) {
        return FileItem.zipEntry(n.path, n.isDir, n.size, n.time, n.isDir ? n.children.size() : -1);
    }

    private static List<FileItem> zipItems(ZipNode node, SortBy by, boolean asc) {
        List<FileItem> folders = new ArrayList<>();
        List<FileItem> files = new ArrayList<>();
        for (ZipNode c : node.children.values()) {
            (c.isDir ? folders : files).add(zipItem(c));
        }
        Comparator<FileItem> cmp = comparatorFor(by, asc);
        Collections.sort(folders, cmp);
        Collections.sort(files, cmp);

        List<FileItem> result = new ArrayList<>(folders.size() + files.size() + 1);
        result.add(FileItem.parentLink(null)); // ルートでは ZIP を抜ける
        result.addAll(folders);
        result.addAll(files);
        return result;
    }

    private void onZipItemClicked(FileItem item) {
        if (item.isParentLink) {
            zipGoUp();
        } else if (item.isDirectory) {
            saveScroll();
            leaveSearchUi();
            showZipDirectory(item.zipEntryPath);
        } else {
            previewZipEntry(item);
        }
    }

    // ---- パスワード

    private void withPassword(final ZipSession s, final Runnable proceed) {
        if (!s.encrypted || s.password != null) {
            proceed.run();
            return;
        }

        final EditText input = new EditText(this);
        input.setHint("パスワード");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(this)
                .setTitle("パスワードを入力")
                .setView(input)
                .setPositiveButton("OK", (dialog, which) -> {
                    String pw = input.getText().toString();
                    if (pw.isEmpty()) {
                        toast("パスワードを入力してください");
                        return;
                    }
                    s.password = pw.toCharArray();
                    s.zip.setPassword(s.password);
                    proceed.run();
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }

    private static boolean isPasswordError(Exception e) {
        return e instanceof ZipException
                && ((ZipException) e).getType() == ZipException.Type.WRONG_PASSWORD;
    }

    // ---- プレビュー（1ファイルだけ展開して外部アプリで開く）

    private void previewZipEntry(final FileItem item) {
        final ZipSession s = zipSession;
        if (s == null) return;
        final ZipNode node = s.index.get(item.file.getPath());
        if (node == null || node.header == null) return;

        withPassword(s, () -> {
            String id = Integer.toHexString(
                    (s.source.getAbsolutePath() + s.source.lastModified() + "!" + node.path).hashCode());
            final File out = new File(new File(new File(getCacheDir(), "zip_preview"), id), node.name);

            if (out.isFile() && out.length() == node.size) {
                openFile(out); // 展開済みならそのまま開く
                return;
            }

            isCancelled = false;
            final ProgressUi ui = new ProgressUi("展開中...");
            ui.start(() -> opExecutor.execute(() -> {
                Exception error = null;
                boolean completed = false;
                try {
                    ui.setFile("展開中: " + node.name);
                    completed = extractEntry(s.zip, node.header, out, ui, node.size, new long[]{0});
                } catch (Exception e) {
                    error = e;
                }

                final Exception err = error;
                final boolean ok = completed;
                runOnUiThread(() -> {
                    ui.dismiss();
                    if (err != null) {
                        if (isPasswordError(err)) {
                            s.password = null;
                            s.encrypted = true;
                            toast("パスワードが違います");
                            withPassword(s, () -> previewZipEntry(item));
                        } else {
                            toast("展開に失敗しました: " + err.getMessage(), Toast.LENGTH_LONG);
                        }
                    } else if (ok) {
                        openFile(out);
                    } else {
                        toast("キャンセルしました");
                    }
                });
            }));
        });
    }

    // ---- 展開

    private void showZipItemOptions(final FileItem item) {
        final ZipSession s = zipSession;
        if (s == null) return;
        final File destRoot = s.source.getParentFile();

        showOptions(item.getDisplayName(),
                new String[]{"ここに展開（ZIPと同じフォルダ）", "すべて展開（新しいフォルダ）"},
                new Runnable[]{
                        () -> {
                            List<ZipNode> nodes = new ArrayList<>();
                            if (selectedFiles.isEmpty()) {
                                ZipNode n = s.index.get(item.file.getPath());
                                if (n != null) nodes.add(n);
                            } else {
                                for (File f : selectedFiles) {
                                    ZipNode n = s.index.get(f.getPath());
                                    if (n != null) nodes.add(n);
                                }
                            }
                            if (nodes.isEmpty()) return;
                            clearSelection();
                            startZipExtraction(s, nodes, destRoot, false);
                        },
                        () -> {
                            File dest = generateUniqueFolder(destRoot, zipBaseName(s.source));
                            startZipExtraction(s, new ArrayList<>(s.root.children.values()), dest, false);
                        }
                });
    }

    /** 通常の一覧から「ZIPを展開」: ZIPの隣に、ZIP名のフォルダを作って全部展開する */
    private void extractZipAll(final File zipFile) {
        toast("ZIPを読み込み中...");
        loadZipSession(zipFile, (session, error) -> {
            if (error != null) {
                toast("ZIPを開けません: " + error.getMessage(), Toast.LENGTH_LONG);
                return;
            }
            File dest = generateUniqueFolder(zipFile.getParentFile(), zipBaseName(zipFile));
            startZipExtraction(session, new ArrayList<>(session.root.children.values()), dest, true);
        });
    }

    private static String zipBaseName(File zip) {
        String base = zip.getName();
        if (base.toLowerCase(Locale.ROOT).endsWith(".zip")) base = base.substring(0, base.length() - 4);
        return base.isEmpty() ? "extracted" : base;
    }

    private void startZipExtraction(final ZipSession s, final List<ZipNode> roots,
                                    final File destRoot, final boolean closeAfter) {
        withPassword(s, () -> runZipExtraction(s, roots, destRoot, closeAfter));
    }

    private void runZipExtraction(final ZipSession s, final List<ZipNode> roots,
                                  final File destRoot, final boolean closeAfter) {
        isCancelled = false;
        globalFileConflictAction = ConflictAction.ASK;

        final ProgressUi ui = new ProgressUi("展開中...");
        ui.start(() -> opExecutor.execute(() -> {
            Exception error = null;
            try {
                List<ZipNode> nodes = new ArrayList<>();
                List<String> relatives = new ArrayList<>();
                for (ZipNode root : roots) {
                    collectNodes(root, parentPrefix(root.path), nodes, relatives);
                }

                long total = 0;
                for (ZipNode n : nodes) {
                    if (!n.isDir) total += n.size;
                }
                long[] done = {0};

                if (!destRoot.isDirectory() && !destRoot.mkdirs()) {
                    throw new IOException("展開先を作成できません");
                }

                for (int i = 0; i < nodes.size(); i++) {
                    if (isCancelled) break;
                    ZipNode n = nodes.get(i);
                    File target = safeChild(destRoot, relatives.get(i));

                    if (n.isDir) {
                        if (!target.isDirectory() && !target.mkdirs()) {
                            throw new IOException("フォルダを作成できません: " + n.name);
                        }
                        continue;
                    }

                    if (target.exists()) {
                        target = resolveFileConflict(target);
                        if (target == null) {
                            done[0] += n.size;
                            if (isCancelled) break;
                            continue;
                        }
                    }

                    ui.setFile("展開中: " + n.name);
                    if (!extractEntry(s.zip, n.header, target, ui, total, done)) break;
                }
            } catch (Exception e) {
                error = e;
            }

            final Exception err = error;
            runOnUiThread(() -> {
                ui.dismiss();

                if (err != null && isPasswordError(err)) {
                    s.password = null;
                    s.encrypted = true;
                    toast("パスワードが違います");
                    startZipExtraction(s, roots, destRoot, closeAfter);
                    return;
                }

                if (closeAfter) closeSession(s);
                if (err != null) {
                    toast("展開に失敗しました: " + err.getMessage(), Toast.LENGTH_LONG);
                } else if (isCancelled) {
                    toast("展開をキャンセルしました");
                } else {
                    toast("展開が完了しました");
                }
                if (zipSession == null) refreshDirectory(true);
            });
        }));
    }

    private static void collectNodes(ZipNode n, String prefix, List<ZipNode> nodes, List<String> relatives) {
        nodes.add(n);
        relatives.add(n.path.substring(prefix.length()));
        if (n.isDir) {
            for (ZipNode c : n.children.values()) {
                collectNodes(c, prefix, nodes, relatives);
            }
        }
    }

    /** "a/b/" → "a/"、"a/b/c.txt" → "a/b/"、"x" → "" */
    private static String parentPrefix(String path) {
        String p = stripSlash(path);
        int i = p.lastIndexOf('/');
        return i >= 0 ? p.substring(0, i + 1) : "";
    }

    /** ZIP内のパスが展開先の外に出ない（Zip Slip対策）ことを確認して File にする */
    private static File safeChild(File root, String relative) throws IOException {
        File f = new File(root, relative);
        String rootPath = root.getCanonicalPath();
        String path = f.getCanonicalPath();
        if (!path.equals(rootPath) && !path.startsWith(rootPath + File.separator)) {
            throw new IOException("不正なパスです: " + relative);
        }
        return f;
    }

    /** 1エントリを dest に展開する。キャンセルされたら false（書きかけは削除） */
    private boolean extractEntry(ZipFile zf, FileHeader header, File dest,
                                 ProgressUi ui, long total, long[] done) throws IOException {
        File parent = dest.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("フォルダを作成できません: " + parent.getName());
        }

        boolean started = false;
        boolean completed = false;
        try (InputStream in = zf.getInputStream(header);
             OutputStream out = new FileOutputStream(dest)) {
            started = true;
            byte[] buffer = new byte[128 * 1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                if (isCancelled) return false;
                out.write(buffer, 0, len);
                done[0] += len;
                ui.setProgress(done[0], total);
            }
            completed = true;
            return true;
        } finally {
            if (started && !completed) dest.delete();
        }
    }

    // ================================================================
    //  ZIP 圧縮
    // ================================================================

    private long parseSplitSize(String option) {
        switch (option) {
            case "5MB": return 5L * 1024 * 1024;
            case "10MB": return 10L * 1024 * 1024;
            case "24MB": return 24L * 1024 * 1024;
            case "50MB": return 50L * 1024 * 1024;
            case "100MB": return 100L * 1024 * 1024;
            case "1024MB": return 1024L * 1024 * 1024;
            case "2048MB": return 2048L * 1024 * 1024;
            default: return 0; // 分割しない
        }
    }

    private void zipDialog(final List<File> targets, String filename) {
        if (targets.isEmpty()) {
            toast("圧縮するファイルが選択されていません");
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_zip_options, null);
        final EditText inputName = dialogView.findViewById(R.id.editZipName);
        final Spinner spinnerLevel = dialogView.findViewById(R.id.spinnerCompressionLevel);
        final EditText inputPassword = dialogView.findViewById(R.id.editPassword);
        final Spinner spinnerEncryption = dialogView.findViewById(R.id.spinnerEncryptionType);
        final Spinner spinnerSplitSize = dialogView.findViewById(R.id.spinnerSplitSize);

        inputName.setText(filename + ".zip");

        String[] levelNames = {"無圧縮", "最速", "高速", "標準", "最高", "超圧縮"};
        ArrayAdapter<String> levelAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, levelNames);
        levelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLevel.setAdapter(levelAdapter);
        spinnerLevel.setSelection(3); // デフォルト: 標準

        String[] encryptionNames = {"ZipCrypto", "AES-128", "AES-256"};
        ArrayAdapter<String> encAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, encryptionNames);
        encAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEncryption.setAdapter(encAdapter);
        spinnerEncryption.setSelection(0); // デフォルト: ZipCrypto

        new AlertDialog.Builder(this)
                .setTitle("アーカイブの作成")
                .setView(dialogView)
                .setPositiveButton("圧縮", (dialog, which) -> {
                    String zipName = inputName.getText().toString().trim();
                    if (!zipName.endsWith(".zip")) zipName += ".zip";
                    if (!isValidName(zipName)) {
                        toast("無効なファイル名です");
                        return;
                    }

                    File zipFile = new File(currentDir, zipName);
                    if (zipFile.exists()) {
                        toast("同名のZIPファイルが既に存在します");
                        return;
                    }

                    int levelIndex = spinnerLevel.getSelectedItemPosition();
                    CompressionLevel level = ZIP_LEVELS[Math.max(0, Math.min(levelIndex, ZIP_LEVELS.length - 1))];
                    String password = inputPassword.getText().toString().trim();
                    String encryptionType = spinnerEncryption.getSelectedItem().toString();
                    long splitSize = parseSplitSize(spinnerSplitSize.getSelectedItem().toString());

                    startZipOperation(targets, zipFile, level, password, encryptionType, splitSize);
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }

    private void startZipOperation(final List<File> targets, final File zipFile,
                                   final CompressionLevel level, final String password,
                                   final String encryptionType, final long splitSize) {
        isCancelled = false;
        final ProgressUi ui = new ProgressUi("圧縮中...");

        ui.start(() -> opExecutor.execute(() -> {
            Exception error = null;
            try {
                ZipFile zip = password.isEmpty()
                        ? new ZipFile(zipFile)
                        : new ZipFile(zipFile, password.toCharArray());
                zip.setRunInThread(true); // 進捗を監視できるよう Zip4j 側でスレッド実行させる

                ZipParameters params = new ZipParameters();
                params.setCompressionMethod(CompressionMethod.DEFLATE);
                params.setCompressionLevel(level);

                if (!password.isEmpty()) {
                    params.setEncryptFiles(true);
                    switch (encryptionType) {
                        case "AES-128":
                            params.setEncryptionMethod(EncryptionMethod.AES);
                            params.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_128);
                            break;
                        case "AES-256":
                            params.setEncryptionMethod(EncryptionMethod.AES);
                            params.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
                            break;
                        case "ZipCrypto":
                        default:
                            params.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);
                            break;
                    }
                }

                ProgressMonitor monitor = zip.getProgressMonitor();
                long grandTotal = 0;
                for (File t : targets) grandTotal += calculateTotalSize(t);

                if (splitSize > 0) {
                    if (targets.size() == 1 && targets.get(0).isDirectory()) {
                        // フォルダ1つなら階層を保ったまま分割
                        zip.createSplitZipFileFromFolder(targets.get(0), params, true, splitSize);
                    } else {
                        List<File> allFiles = new ArrayList<>();
                        for (File target : targets) {
                            if (target.isDirectory()) {
                                allFiles.addAll(getAllFilesRecursively(target));
                            } else {
                                allFiles.add(target);
                            }
                        }
                        zip.createSplitZipFile(allFiles, params, true, splitSize);
                    }
                    waitForZip(monitor, ui, 0, grandTotal);
                } else {
                    // ファイルはまとめて1回、フォルダは1つずつ。Zip4j は同時に複数タスクを走らせられないので、
                    // 1つ終わるまで待ってから次を追加する
                    List<File> plainFiles = new ArrayList<>();
                    List<File> folders = new ArrayList<>();
                    for (File t : targets) (t.isDirectory() ? folders : plainFiles).add(t);

                    long base = 0;
                    if (!plainFiles.isEmpty()) {
                        zip.addFiles(plainFiles, params);
                        waitForZip(monitor, ui, base, grandTotal);
                        for (File f : plainFiles) base += f.length();
                    }
                    for (File folder : folders) {
                        if (isCancelled) break;
                        zip.addFolder(folder, params);
                        waitForZip(monitor, ui, base, grandTotal);
                        base += calculateTotalSize(folder);
                    }
                }
            } catch (Exception e) {
                error = e;
            }

            final Exception err = error;
            runOnUiThread(() -> {
                ui.dismiss();
                if (isCancelled) {
                    zipFile.delete();
                    toast("圧縮をキャンセルしました");
                } else if (err != null) {
                    zipFile.delete();
                    toast("圧縮中にエラー: " + err.getMessage(), Toast.LENGTH_LONG);
                } else {
                    toast("ZIP圧縮が完了しました");
                }
                clearSelection();
                refreshDirectory(true);
            });
        }));
    }

    private void waitForZip(ProgressMonitor monitor, ProgressUi ui, long base, long grandTotal) throws Exception {
        while (monitor.getState() == ProgressMonitor.State.BUSY) {
            if (isCancelled) monitor.setCancelAllTasks(true);

            String current = monitor.getFileName();
            if (current != null) ui.setFile("圧縮中: " + current);
            ui.setProgress(base + monitor.getWorkCompleted(), grandTotal);

            Thread.sleep(150);
        }
        if (monitor.getResult() == ProgressMonitor.Result.ERROR && monitor.getException() != null) {
            throw monitor.getException();
        }
    }

    private List<File> getAllFilesRecursively(File folder) {
        List<File> files = new ArrayList<>();
        File[] children = folder.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    files.addAll(getAllFilesRecursively(child));
                } else {
                    files.add(child);
                }
            }
        }
        return files;
    }

    // ---------------------------------------------------------------- Toast

    private void toast(String message) {
        toast(message, Toast.LENGTH_SHORT);
    }

    private void toast(String message, int duration) {
        if (currentToast != null) currentToast.cancel();
        currentToast = Toast.makeText(this, message, duration);
        currentToast.show();
    }
}