package com.aozora.aozora;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.MimeTypeMap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;

import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;
import android.webkit.WebChromeClient;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.content.pm.PackageManager;
import android.os.Build;
import android.Manifest;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import net.lingala.zip4j.progress.ProgressMonitor;

public class FileManager extends Activity {
    private ListView listView;
    private File currentDir;
    private List<FileItem> files = new ArrayList<>();
    private List<String> fileNames = new ArrayList<>();
    private FileListAdapter adapter;
    private enum SortBy { NAME, DATE, SIZE }
    private SortBy sortBy = SortBy.NAME;
    private boolean ascending = true;
    private SearchView searchView;
    private boolean isSearchMode = false;
    private String lastQuery = "";
    private File clipboardFile = null;
    private String pendingAction = ""; // "cut" or "copy"
    private List<File> clipboardFiles = new ArrayList<>();
    private String clipboardAction = ""; // "copy" or "cut"
    public List<File> selectedFiles = new ArrayList<>();
    private boolean isSelectionMode = false;
    private volatile boolean isCancelled = false;
    private static final int REQUEST_STORAGE_PERMISSION = 100;
    private static final int REQUEST_OPEN_DOCUMENT = 101;
    private long lastBackPressedTime = 0; // 最後にBackキーが押された時間
    private Toast toast;
    private boolean isInZipView = false;
    private ZipFile currentZipFile = null;
    private String currentZipPath = "";

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

    private enum ConflictAction {
        ASK, OVERWRITE, SKIP, KEEP_BOTH, CANCEL
    }

    private ConflictAction globalFileConflictAction = ConflictAction.ASK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applySavedTheme();
        setContentView(R.layout.file);  // XMLレイアウトを読み込む
        Background = findViewById(R.id.background);
        applySavedBackground();
        applyBackTheme();

        listView = findViewById(R.id.listViewFiles);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);  // データなしで起動
                    Toast.makeText(this, "ファイルアクセスを許可してください", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "設定画面が開けませんでした", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // API 23 (Marshmallow) 以降では READ_EXTERNAL_STORAGE が必要
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
            } else {
                // 権限がある
            }
        } else {
            // API 22 以前では権限は自動的に付与される
        }

        // Action Bar が表示されているか確認
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        searchView = findViewById(R.id.searchView);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                isSearchMode = true;
                lastQuery = query;
                performSearch(currentDir, query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // オートサジェストのようにリアルタイム検索したければここで呼ぶ
                return false;
            }
        });

        // 権限がある場合はストレージを表示
        loadDirectory(Environment.getExternalStorageDirectory());

        listView.setOnItemClickListener((parent, view, position, id) -> {
            FileItem item = files.get(position);

            if (isSelectionMode) {
                // ✅ 選択モード中 → ファイルを開かずに選択トグル
                toggleSelection(item.file);
                return;
            }

            // ✅ ZIP閲覧モード中の場合
            if (isInZipView) {
                if (item.isDirectory) {
                    // ZIP内フォルダを開く
                    currentZipPath = currentZipPath + item.file.getName() + "/";
                    loadZipDirectory(currentZipPath);
                } else {
                    // ZIP内ファイルを一時展開してプレビュー（任意）
                    previewOrExtractZipEntry(item.file.getName());
                }
                return;
            }

            // ✅ 通常モード（ZIP外）
            if (item.file.isDirectory()) {
                loadDirectory(item.file);
            } else {
                // ZIPファイルなら直接開く
                //if (item.file.getName().toLowerCase().endsWith(".zip")) {
                //    openZipFile(item.file);
                //} else {
                    // 通常のファイルを開く
                    openFile(item.file);
                //}
            }
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            FileItem item = files.get(position);
            showItemOptionsDialog(item);
            return true;
        });
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

    private void openZipFile(File zipFile) {
        try {
            currentZipFile = new ZipFile(zipFile);
            isInZipView = true;
            currentZipPath = ""; // ZIP内のルート

            loadZipDirectory("");
        } catch (Exception e) {
            Toast.makeText(this, "ZIPを開けません: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadZipDirectory(String pathInZip) {
        try {
            List<FileHeader> headers = currentZipFile.getFileHeaders();
            List<FileItem> items = new ArrayList<>();
            Set<String> addedFolders = new HashSet<>();

            for (FileHeader header : headers) {
                String name = header.getFileName();
                if (!name.startsWith(pathInZip)) continue;

                String relative = name.substring(pathInZip.length());
                if (relative.isEmpty()) continue;

                if (relative.contains("/")) {
                    // フォルダ（階層1つ分）
                    String folderName = relative.substring(0, relative.indexOf("/"));
                    if (addedFolders.add(folderName)) {
                        items.add(new FileItem(new File(folderName), true, 0, header.getLastModifiedTimeEpoch()));
                    }
                } else {
                    // ファイル
                    items.add(new FileItem(
                            new File(header.getFileName().substring(pathInZip.length())),
                            false,
                            header.getUncompressedSize(),
                            header.getLastModifiedTimeEpoch()
                    ));
                }
            }

            runOnUiThread(() -> listView.setAdapter(new FileListAdapter(this, items)));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "ZIPの読み込み中にエラー: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void previewOrExtractZipEntry(String entryName) {
        try {
            FileHeader header = currentZipFile.getFileHeader(currentZipPath + entryName);
            if (header == null) return;

            File tempDir = new File(getCacheDir(), "zip_preview");
            if (!tempDir.exists()) tempDir.mkdirs();

            File outFile = new File(tempDir, entryName);
            currentZipFile.extractFile(header, tempDir.getAbsolutePath());

            Toast.makeText(this, "一時展開: " + outFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();

            // 必要ならここでIntentを使って開くことも可能
            // openFile(outFile);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "展開失敗: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_file, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
         if (id == R.id.action_select) {
             if (isSelectionMode) {
                 isSelectionMode = false;
                 selectedFiles.clear();
                 if (toast != null) {
                     // 既存のToastをキャンセル
                     toast.cancel();
                 }
                 toast = Toast.makeText(this, "選択モードが解除されました", Toast.LENGTH_SHORT);
                 toast.show();
             } else if (!isSelectionMode) {
                 isSelectionMode = true;
                 selectedFiles.clear();
                 if (toast != null) {
                     // 既存のToastをキャンセル
                     toast.cancel();
                 }
                 toast = Toast.makeText(this, "選択モードになりました", Toast.LENGTH_SHORT);
                 toast.show();
             }
            return true;
        } else if (id == R.id.file_close) {
            finish();
            overridePendingTransition(R.anim.no_animation,  R.anim.slide_out_down_low);
        } else if (id == R.id.sort_name) {
            sortBy = SortBy.NAME;
            loadDirectory(currentDir);
            return true;
        } else if (id == R.id.sort_date) {
            sortBy = SortBy.DATE;
            loadDirectory(currentDir);
            return true;
        } else if (id == R.id.sort_size) {
            sortBy = SortBy.SIZE;
            loadDirectory(currentDir);
            return true;
        } else if (id == R.id.toggle_order) {
            ascending = !ascending;
            loadDirectory(currentDir);
            return true;
        } else if (id == R.id.menu_create_folder) {
            showCreateFolderDialog();
            return true;
        } else if (id == R.id.menu_new_file) {
            showNewFileDialog();
            return true;
        } else if (id == R.id.menu_info) {
            info();
            return true;
        } else if (id == R.id.action_paste) {
             if (!clipboardFiles.isEmpty() && !clipboardAction.isEmpty()) {
                 startMultiplePasteOperation(clipboardFiles, currentDir, clipboardAction);
             } else if (clipboardFile != null && !pendingAction.isEmpty()) {
                 // 旧：単一ファイルの貼り付け対応（互換のため残す）
                 startPasteOperation(clipboardFile, currentDir, pendingAction);
             } else {
                 Toast.makeText(this, "コピーまたはカットしたファイルがありません", Toast.LENGTH_SHORT).show();
             }
            return true;
        }
        if (item.getItemId() == android.R.id.home) {
            if (currentDir != null && currentDir.getParentFile() != null) {
                loadDirectory(currentDir.getParentFile());
            } else {
                finish(); // 前の画面に戻る
                overridePendingTransition(R.anim.no_animation,  R.anim.slide_out_down_low);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleSelection(File file) {
        if (selectedFiles.contains(file)) {
            selectedFiles.remove(file);
        } else {
            selectedFiles.add(file);
        }

        adapter.notifyDataSetChanged();

        if (selectedFiles.isEmpty()) {
            isSelectionMode = false;
        } else {
            if (toast != null) {
                // 既存のToastをキャンセル
                toast.cancel();
            }
            toast = Toast.makeText(this, selectedFiles.size() + " 件選択中", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    private void showNewFileDialog() {
        EditText input = new EditText(this);
        input.setHint("ファイル名（例: note.txt）");
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        new AlertDialog.Builder(this)
                .setTitle("新しいファイルを作成")
                .setView(input)
                .setPositiveButton("作成", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (isInvalidFileName(name)) {
                        Toast.makeText(this, "無効なファイル名です", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    File newFile = new File(currentDir, name);
                    if (newFile.exists()) {
                        Toast.makeText(this, "同名のファイルが既に存在します", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        boolean created = newFile.createNewFile();
                        if (created) {
                            Toast.makeText(this, "ファイルを作成しました", Toast.LENGTH_SHORT).show();
                            loadDirectory(currentDir);
                        } else {
                            Toast.makeText(this, "ファイルの作成に失敗しました", Toast.LENGTH_SHORT).show();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "エラー: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }

    private boolean isInvalidFileName(String name) {
        // 空文字 or 禁止文字チェック
        return name.isEmpty() || name.matches(".*[\\\\/:*?\"<>|].*");
    }

    private void showItemOptionsDialog(FileItem item) {
        String[] options = {"圧縮", "カット", "コピー", "削除", "名前の変更"};

        new AlertDialog.Builder(this)
                .setTitle(item.file.getName())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            // ★ 修正ポイント ★
                            List<File> targets = new ArrayList<>();

                            if (!selectedFiles.isEmpty()) {
                                targets.addAll(selectedFiles);
                            } else if (item.file != null && item.file.exists()) {
                                // 長押しせずに単体ファイルを選択している場合にも対応
                                targets.add(item.file);
                            }
                            zipDialog(targets, item.file.getName());
                            break;
                        case 1:
                            // カット
                            if (selectedFiles.isEmpty()) {
                                pendingAction = "cut";
                                clipboardFile = item.file;
                                Toast.makeText(this, "カットしました: " + item.file.getName(), Toast.LENGTH_SHORT).show();
                                break;
                            }
                            clipboardFiles.clear();
                            clipboardFiles.addAll(selectedFiles);
                            clipboardAction = "cut";
                            isSelectionMode = false;
                            selectedFiles.clear();
                            adapter.notifyDataSetChanged();
                            Toast.makeText(this, clipboardFiles.size() + " 件をカットしました", Toast.LENGTH_SHORT).show();
                            break;
                        case 2:
                            // コピー
                            if (selectedFiles.isEmpty()) {
                                pendingAction = "copy";
                                clipboardFile = item.file;
                                Toast.makeText(this, "コピーしました: " + item.file.getName(), Toast.LENGTH_SHORT).show();
                                break;
                            }
                            clipboardFiles.clear();
                            clipboardFiles.addAll(selectedFiles);
                            clipboardAction = "copy";
                            isSelectionMode = false;
                            selectedFiles.clear();
                            adapter.notifyDataSetChanged();
                            Toast.makeText(this, clipboardFiles.size() + " 件をコピーしました", Toast.LENGTH_SHORT).show();
                            break;
                        case 3:
                            // 削除
                            confirmAndDelete(item.file);
                            break;
                        case 4:
                            // 名前の変更
                            showRenameDialog(item.file);
                            break;
                    }
                })
                .show();
    }

    private long parseSplitSize(String option) {
        switch (option) {
            case "5MB": return 5L * 1024 * 1024;
            case "10MB": return 10L * 1024 * 1024;
            case "24MB": return 24L * 1024 * 1024;
            case "50MB": return 50L * 1024 * 1024;
            case "100MB": return 100L * 1024 * 1024;
            case "1024MB": return 1024L * 1024 * 1024;
            case "2048MB": return 2048L * 1024 * 1024;
            default: return 0; // しない
        }
    }

    private void zipDialog(List<File> targets, String filename) {
        if (targets.isEmpty()) {
            Toast.makeText(this, "圧縮するファイルが選択されていません", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- ZIP名入力フィールド ---
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_zip_options, null);
        EditText inputName = dialogView.findViewById(R.id.editZipName);
        Spinner spinnerLevel = dialogView.findViewById(R.id.spinnerCompressionLevel);
        EditText inputPassword = dialogView.findViewById(R.id.editPassword);
        Spinner spinnerEncryption = dialogView.findViewById(R.id.spinnerEncryptionType);
        Spinner spinnerSplitSize = dialogView.findViewById(R.id.spinnerSplitSize);

        inputName.setText(filename + ".zip");

        // 圧縮レベル項目
        String[] levelNames = {"無圧縮", "最速", "高速", "標準", "最高", "超圧縮"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, levelNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLevel.setAdapter(adapter);
        spinnerLevel.setSelection(3); // デフォルト: 標準

        // 暗号化方式スピナー
        String[] encryptionNames = {"ZipCrypto", "AES-128", "AES-256"};
        ArrayAdapter<String> encAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, encryptionNames);
        encAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEncryption.setAdapter(encAdapter);
        spinnerEncryption.setSelection(0); // デフォルト AES-256

        new AlertDialog.Builder(this)
                .setTitle("アーカイブの作成")
                .setView(dialogView)
                .setPositiveButton("圧縮", (dialog, which) -> {
                    String zipName = inputName.getText().toString().trim();
                    if (!zipName.endsWith(".zip")) {
                        zipName += ".zip";
                    }

                    File zipFile = new File(currentDir, zipName);
                    if (zipFile.exists()) {
                        Toast.makeText(this, "同名のZIPファイルが既に存在します", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int selectedIndex = spinnerLevel.getSelectedItemPosition();
                    int compressionLevel = mapCompressionLevel(selectedIndex);
                    String password = inputPassword.getText().toString().trim();
                    String encryptionType = spinnerEncryption.getSelectedItem().toString();
                    String splitOption = spinnerSplitSize.getSelectedItem().toString();

                    long splitSize = parseSplitSize(splitOption); // 🔹 MB → byte に変換

                    startZipOperation(targets, zipFile, compressionLevel, password, encryptionType, splitSize);
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }

    private int mapCompressionLevel(int index) {
        switch (index) {
            case 0: return java.util.zip.Deflater.NO_COMPRESSION;  // 無圧縮
            case 1: return 1;  // 最速
            case 2: return 3;  // 高速
            case 3: return 5;  // 標準
            case 4: return 7;  // 最高
            case 5: return 9;  // 超圧縮
            default: return 5;
        }
    }

    private CompressionLevel mapToZip4jLevel(int level) {
        switch (level) {
            case 0: return CompressionLevel.NO_COMPRESSION;
            case 1: return CompressionLevel.FASTEST;
            case 2: return CompressionLevel.FAST;
            case 3: return CompressionLevel.NORMAL;
            case 4: return CompressionLevel.MAXIMUM;
            case 5: return CompressionLevel.ULTRA;
            default: return CompressionLevel.NORMAL;
        }
    }

    private void zipSimple(List<File> targets) {
        if (targets.isEmpty()) {
            Toast.makeText(this, "圧縮するファイルが選択されていません", Toast.LENGTH_SHORT).show();
            return;
        }

        EditText input = new EditText(this);
        input.setHint("ファイル名（例: archive.zip）");
        input.setText("archive.zip");

        new AlertDialog.Builder(this)
                .setTitle("ZIPファイル名を入力")
                .setView(input)
                .setPositiveButton("圧縮", (dialog, which) -> {
                    String zipName = input.getText().toString().trim();
                    if (!zipName.endsWith(".zip")) {
                        zipName += ".zip";
                    }

                    File zipFile = new File(currentDir, zipName);
                    if (zipFile.exists()) {
                        Toast.makeText(this, "同名のZIPファイルが既に存在します", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    startSimpleZipOperation(targets, zipFile);
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }

    private void startSimpleZipOperation(List<File> targets, File zipFile) {
        isCancelled = false;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("圧縮中...");

        View view = getLayoutInflater().inflate(R.layout.dialog_progress, null);
        ProgressBar progressBar = view.findViewById(R.id.progressBar);
        TextView textFile = view.findViewById(R.id.textFileName);
        TextView textPercent = view.findViewById(R.id.textProgressPercent);

        builder.setView(view);
        builder.setCancelable(false);
        builder.setNegativeButton("キャンセル", (dialog, which) -> {
            isCancelled = true;
            Toast.makeText(this, "圧縮をキャンセルしました", Toast.LENGTH_SHORT).show();
        });

        AlertDialog progressDialog = builder.create();
        progressDialog.show();

        new Thread(() -> {
            try {
                // 総サイズを先に算出
                long totalSize = 0;
                for (File f : targets) {
                    totalSize += calculateTotalSize(f);
                }

                long[] processed = {0};

                try (FileOutputStream fos = new FileOutputStream(zipFile);
                     java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(fos)) {

                    for (File f : targets) {
                        if (isCancelled) break;

                        // 単体ファイル or ディレクトリをそれぞれ適切に処理
                        if (f.isFile()) {
                            zipSingleFile(f, zos, processed, totalSize, textFile, progressBar, textPercent);
                        } else {
                            zipFileRecursively(f, f.getName(), zos, processed, totalSize, textFile, progressBar, textPercent);
                        }
                    }
                }

                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    if (isCancelled) {
                        zipFile.delete();
                        Toast.makeText(this, "圧縮をキャンセルしました", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "ZIP圧縮が完了しました", Toast.LENGTH_SHORT).show();
                        loadDirectory(currentDir);
                    }
                    selectedFiles.clear();
                    isSelectionMode = false;
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "圧縮中にエラー: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void startZipOperation(List<File> targets, File zipFile, int level, String password, String encryptionType, long splitSize) {
        isCancelled = false;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("圧縮中...");

        View view = getLayoutInflater().inflate(R.layout.dialog_progress, null);
        ProgressBar progressBar = view.findViewById(R.id.progressBar);
        TextView textFile = view.findViewById(R.id.textFileName);
        TextView textPercent = view.findViewById(R.id.textProgressPercent);
        builder.setView(view);
        builder.setCancelable(false);
        builder.setNegativeButton("キャンセル", (dialog, which) -> isCancelled = true);

        AlertDialog progressDialog = builder.create();
        progressDialog.show();

        // 🔹 ダイアログ描画完了後にZIP処理を開始
        progressDialog.getWindow().getDecorView().post(() -> {

        new Thread(() -> {
            try {
                ZipFile zip = (password != null && !password.isEmpty())
                        ? new ZipFile(zipFile, password.toCharArray())
                        : new ZipFile(zipFile);

                // 🔹 Zip4j に「スレッドで動かしてOK」と伝える
                zip.setRunInThread(true);

                ZipParameters params = new ZipParameters();
                params.setCompressionMethod(CompressionMethod.DEFLATE);
                params.setCompressionLevel(mapToZip4jLevel(level));

                if (password != null && !password.isEmpty()) {
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

                if (splitSize > 0) {
                    // 分割あり
                    // フォルダを含む場合は、再帰的にファイルをリスト化
                    List<File> allFiles = new ArrayList<>();
                    for (File target : targets) {
                        if (target.isDirectory()) {
                            allFiles.addAll(getAllFilesRecursively(target));
                        } else {
                            allFiles.add(target);
                        }
                    }

                    // 分割ZIP作成
                    zip.createSplitZipFile(allFiles, params, true, splitSize);
                } else {
                    // ZIPを非同期で開始
                    if (targets.size() == 1 && targets.get(0).isFile()) {
                        zip.addFile(targets.get(0), params);
                    } else {
                        for (File f : targets) {
                            zip.addFolder(f, params);
                        }
                    }
                    }

                // 🔹 ProgressMonitorで進行を監視
                ProgressMonitor monitor = zip.getProgressMonitor();

                while (monitor.getState() == ProgressMonitor.State.BUSY) {
                    if (isCancelled) {
                        monitor.setCancelAllTasks(true);
                        break;
                    }

                    int progress = monitor.getPercentDone();
                    String currentFile = monitor.getFileName();

                    runOnUiThread(() -> {
                        textFile.setText("圧縮中: " + (currentFile != null ? currentFile : ""));
                        progressBar.setProgress(progress);
                        textPercent.setText(progress + "% 完了");
                    });

                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException ignored) {}
                }

                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    if (isCancelled) {
                        zipFile.delete();
                        Toast.makeText(this, "圧縮をキャンセルしました", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "ZIP圧縮が完了しました", Toast.LENGTH_SHORT).show();
                        loadDirectory(currentDir);
                    }
                    selectedFiles.clear();
                    isSelectionMode = false;
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "圧縮中にエラー: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
        });
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

    private void zipFileRecursively(File file, String entryName, java.util.zip.ZipOutputStream zos,
                                    long[] processed, long totalSize,
                                    TextView textFile, ProgressBar progressBar, TextView textPercent) throws IOException {

        if (isCancelled) return;

        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    zipFileRecursively(child, entryName + "/" + child.getName(), zos, processed, totalSize, textFile, progressBar, textPercent);
                }
            } else {
                // 空フォルダもエントリ追加（重要！）
                zos.putNextEntry(new java.util.zip.ZipEntry(entryName + "/"));
                zos.closeEntry();
            }
            return;
        }

        runOnUiThread(() -> textFile.setText("圧縮中: " + file.getName()));

        try (FileInputStream fis = new FileInputStream(file)) {
            zos.putNextEntry(new java.util.zip.ZipEntry(entryName));

            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                if (isCancelled) return;
                zos.write(buffer, 0, len);
                processed[0] += len;

                int progress = (int) ((processed[0] * 100) / totalSize);
                runOnUiThread(() -> {
                    progressBar.setProgress(progress);
                    textPercent.setText(progress + "% 完了");
                });
            }

            zos.closeEntry();
        }
    }

    private void zipSingleFile(File file, java.util.zip.ZipOutputStream zos,
                               long[] processed, long totalSize,
                               TextView textFile, ProgressBar progressBar, TextView textPercent) throws IOException {

        if (isCancelled) return;

        runOnUiThread(() -> textFile.setText("圧縮中: " + file.getName()));

        try (FileInputStream fis = new FileInputStream(file)) {
            zos.putNextEntry(new java.util.zip.ZipEntry(file.getName()));

            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                if (isCancelled) return;
                zos.write(buffer, 0, len);
                processed[0] += len;

                int progress = (int) ((processed[0] * 100) / totalSize);
                runOnUiThread(() -> {
                    progressBar.setProgress(progress);
                    textPercent.setText(progress + "% 完了");
                });
            }

            zos.closeEntry();
        }
    }

    private void confirmAndDelete(File file) {
        if (selectedFiles.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("削除確認")
                    .setMessage(file.getName() + " を削除しますか？")
                    .setPositiveButton("削除", (dialog, which) -> {
                        boolean success = deleteRecursively(file);
                        if (success) {
                            Toast.makeText(this, "削除しました", Toast.LENGTH_SHORT).show();
                            loadDirectory(currentDir);
                        } else {
                            Toast.makeText(this, "削除に失敗しました", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("キャンセル", null)
                    .show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("削除確認")
                .setMessage(selectedFiles.size() + " 件のファイル／フォルダを削除しますか？\nこの操作は元に戻せません。")
                .setPositiveButton("削除", (dialog, which) -> {
                    startMultipleDeleteOperation(selectedFiles);
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }

    private void startMultipleDeleteOperation(List<File> targets) {
        isCancelled = false;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("削除中...");

        View view = getLayoutInflater().inflate(R.layout.dialog_progress, null);
        ProgressBar progressBar = view.findViewById(R.id.progressBar);
        TextView textFile = view.findViewById(R.id.textFileName);
        TextView textPercent = view.findViewById(R.id.textProgressPercent);
        builder.setView(view);
        builder.setCancelable(false);

        builder.setNegativeButton("キャンセル", (dialog, which) -> {
            isCancelled = true;
            Toast.makeText(this, "削除をキャンセルしました", Toast.LENGTH_SHORT).show();
        });

        AlertDialog progressDialog = builder.create();
        progressDialog.show();

        new Thread(() -> {
            int totalCount = targets.size();
            int deletedCount = 0;

            for (File file : targets) {
                if (isCancelled) break;

                runOnUiThread(() -> textFile.setText("削除中: " + file.getName()));

                boolean result = deleteRecursively(file);
                deletedCount++;

                int progress = (int) ((deletedCount * 100.0f) / totalCount);
                runOnUiThread(() -> {
                    progressBar.setProgress(progress);
                    textPercent.setText(progress + "% 完了");
                });
            }

            runOnUiThread(() -> {
                progressDialog.dismiss();

                if (isCancelled) {
                    Toast.makeText(this, "削除をキャンセルしました", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "削除が完了しました", Toast.LENGTH_SHORT).show();
                }

                selectedFiles.clear();
                isSelectionMode = false;
                loadDirectory(currentDir);
            });
        }).start();
    }

    private void info() {
        new AlertDialog.Builder(this)
                .setTitle("アプリ情報")
                .setMessage("ファイルマネージャ バージョン 0.6 -Release")
                .setPositiveButton("OK", (dialog, which) -> {
                })
                .show();
    }

    private boolean deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        return file.delete();
    }

    private void showRenameDialog(File file) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("名前の変更");

        final EditText input = new EditText(this);
        input.setText(file.getName());
        builder.setView(input);

        builder.setPositiveButton("変更", (dialog, which) -> {
            String newName = input.getText().toString().trim();

            if (!isValidFolderName(newName)) {
                Toast.makeText(this, "無効な名前です（使用禁止文字あり）", Toast.LENGTH_SHORT).show();
                return;
            }

            File newFile = new File(file.getParent(), newName);
            if (newFile.exists()) {
                Toast.makeText(this, "すでに存在します", Toast.LENGTH_SHORT).show();
                showRenameDialog(file);
            } else if (file.renameTo(newFile)) {
                Toast.makeText(this, "変更成功", Toast.LENGTH_SHORT).show();
                loadDirectory(currentDir);
            } else {
                Toast.makeText(this, "変更失敗", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("キャンセル", null);
        builder.show();
    }

    private boolean isValidFolderName(String name) {
        // 禁止文字（Windows準拠 + Unix共通）
        return !name.matches(".*[\\\\/:*?\"<>|].*");
    }

    private File generateUniqueFolder(File parent, String baseName) {
        File folder = new File(parent, baseName);
        int counter = 1;

        while (folder.exists()) {
            String newName = baseName + " (" + counter + ")";
            folder = new File(parent, newName);
            counter++;
        }

        return folder;
    }

    private void showCreateFolderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("フォルダの作成");

        final EditText input = new EditText(this);
        input.setHint("フォルダ名:");
        builder.setView(input);

        builder.setPositiveButton("作成", (dialog, which) -> {
            String folderName = input.getText().toString().trim();

            // 使用禁止文字チェック
            if (!isValidFolderName(folderName)) {
                Toast.makeText(this, "無効なフォルダ名です（使用禁止文字が含まれています）", Toast.LENGTH_LONG).show();
                return;
            }

            // リネーム案を生成（同名があれば " (1)", " (2)" ... を追加）
            File newFolder = generateUniqueFolder(currentDir, folderName);

            if (!folderName.isEmpty()) {
                if (newFolder.exists()) {
                    Toast.makeText(this, "すでに存在します", Toast.LENGTH_SHORT).show();
                } else {
                    if (newFolder.mkdir()) {
                        Toast.makeText(this, "フォルダを作成しました", Toast.LENGTH_SHORT).show();
                        loadDirectory(currentDir); // 作成後に再読み込み
                    } else {
                        Toast.makeText(this, "フォルダ作成に失敗しました", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        builder.setNegativeButton("キャンセル", null);
        builder.show();
    }

    private void loadDirectory(File dir) {
        currentDir = dir;

        File[] fileList = dir.listFiles();
        files.clear();

        // ソート基準に応じた Comparator を定義
        Comparator<FileItem> comparator = (a, b) -> {
            switch (sortBy) {
                case DATE:
                    return Long.compare(a.file.lastModified(), b.file.lastModified());
                case SIZE:
                    return Long.compare(a.file.length(), b.file.length());
                case NAME:
                default:
                    return a.getDisplayName().toLowerCase().compareTo(b.getDisplayName().toLowerCase());
            }
        };

        if (!ascending) {
            comparator = Collections.reverseOrder(comparator);
        }

        // フォルダ・ファイル分類
        List<FileItem> folders = new ArrayList<>();
        List<FileItem> regularFiles = new ArrayList<>();

        if (fileList != null) {
            for (File f : fileList) {
                if (!f.isHidden()) {
                    FileItem item = new FileItem(f);
                    if (f.isDirectory()) {
                        folders.add(item);
                    } else {
                        regularFiles.add(item);
                    }
                }
            }
        }

        // 並び替え
        Collections.sort(folders, comparator);
        Collections.sort(regularFiles, comparator);

        // 結合: ".. (Up)" → フォルダ → ファイル
        if (dir.getParentFile() != null) {
            files.add(new FileItem(dir.getParentFile()));  // ".."
        }

        files.addAll(folders);
        files.addAll(regularFiles);

        // アダプタ更新
        adapter = new FileListAdapter(this, files);
        listView.setAdapter(adapter);

        // タイトルにパスを表示
        setTitle(dir.getAbsolutePath());
    }

    private void openFileWithDocumentPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); // すべてのファイルを開けるようにする
        startActivityForResult(intent, REQUEST_OPEN_DOCUMENT);
    }

    // 権限がない場合にダイアログを表示し、設定画面へ誘導
    private void showPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("ストレージ権限が必要です")
                .setMessage("ファイルを開くにはストレージの読み取り権限が必要です。設定画面で許可をしてください。")
                .setPositiveButton("設定を開く", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {  // Android 9（API 28）以下
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                showPermissionDialog();
                return false;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {  // Android 13（API 33）以上
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestStoragePermission();
                return false;
            }
        }
        return true;
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO},
                REQUEST_STORAGE_PERMISSION);
    }

    private String getMimeType(File file) {
        String type = null;
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex != -1) {
            String extension = fileName.substring(dotIndex + 1).toLowerCase();
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        // ファイル名でフォールバック
        if (type == null) {
            if (fileName.endsWith(".mp4")) type = "video/mp4";
            else if (fileName.endsWith(".pdf")) type = "application/pdf";
            else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) type = "image/jpeg";
            else if (fileName.endsWith(".png")) type = "image/png";
            else type = "*/*";
        }
        return type != null ? type : "*/*";
    }

    private void openFile(File file) {
        try {
            String mimeType = getMimeType(file);

            Uri fileUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // FileProvider を経由して安全な URI を生成
                fileUri = FileProvider.getUriForFile(
                        this,
                        getApplicationContext().getPackageName() + ".fileprovider",
                        file
                );
            } else {
                fileUri = Uri.fromFile(file);
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, mimeType);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // URI権限をすべての候補アプリに付与
            List<ResolveInfo> resInfoList = getPackageManager()
                    .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                grantUriPermission(resolveInfo.activityInfo.packageName,
                        fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "対応アプリが見つかりません", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "ファイルを開けません: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void showFileDialog(File file) {
        new AlertDialog.Builder(this)
                .setTitle("File Selected")
                .setMessage(file.getAbsolutePath())
                .setPositiveButton("OK", null)
                .show();
    }

    private void performSearch(File root, String keyword) {
        List<FileItem> results = new ArrayList<>();
        searchRecursive(root, keyword.toLowerCase(), results);

        files.clear();
        files.addAll(results);

        adapter = new FileListAdapter(this, files);
        listView.setAdapter(adapter);
        setTitle("検索: " + keyword);
    }

    private void searchRecursive(File dir, String keyword, List<FileItem> resultList) {
        File[] children = dir.listFiles();
        if (children == null) return;

        for (File child : children) {
            if (child.isHidden()) continue;

            String name = child.getName().toLowerCase();
            if (name.contains(keyword)) {
                resultList.add(new FileItem(child));
            }

            if (child.isDirectory()) {
                searchRecursive(child, keyword, resultList);
            }
        }
    }

    private long calculateTotalSize(File file) {
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

    @Override
    public void onBackPressed() {
        if (isSearchMode) {
            // 検索中なら検索モード解除
            isSearchMode = false;
            searchView.setQuery("", false);
            searchView.clearFocus();
            loadDirectory(currentDir);
            return;
        }

        if (isInZipView) {
            if (currentZipPath.isEmpty()) {
                // ZIPモードを抜ける
                isInZipView = false;
                currentZipFile = null;
                loadDirectory(currentDir);
            } else {
                // 1階層上に戻る
                int lastSlash = currentZipPath.lastIndexOf('/', currentZipPath.length() - 2);
                currentZipPath = (lastSlash > 0) ? currentZipPath.substring(0, lastSlash + 1) : "";
                loadZipDirectory(currentZipPath);
            }
        } else if (currentDir != null && currentDir.getParentFile() != null
                && !currentDir.getAbsolutePath().equals("/storage/emulated/0")) {
            // ルート(/storage/emulated/0)以外 → 親ディレクトリへ戻る
            loadDirectory(currentDir.getParentFile());
        } else {
            // ルートにいる場合は2回押しで終了
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastBackPressedTime < 2000) {
                // 2秒以内に2回押されたら終了
                finish();
                overridePendingTransition(R.anim.no_animation, R.anim.slide_out_down_low);
            } else {
                Toast.makeText(this, "もう一度 戻る で終了します", Toast.LENGTH_SHORT).show();
                lastBackPressedTime = currentTime;
            }
        }
    }

    private void startPasteOperation(File source, File targetDir, String action) {
        isCancelled = false;

        File proposedDest = new File(targetDir, source.getName());

        if (proposedDest.exists() && source.isDirectory()) {
            showConflictResolutionDialog(source, targetDir, action);
        } else {
            startCopyWithProgress(source, proposedDest, action);
        }
    }

    private void startMultiplePasteOperation(List<File> sources, File targetDir, String action) {
        if (sources.isEmpty()) return;

        isCancelled = false;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("コピー中...");

        View view = getLayoutInflater().inflate(R.layout.dialog_progress, null);
        ProgressBar progressBar = view.findViewById(R.id.progressBar);
        TextView textFile = view.findViewById(R.id.textFileName);
        TextView textPercent = view.findViewById(R.id.textProgressPercent);
        builder.setView(view);
        builder.setCancelable(false);

        builder.setNegativeButton("キャンセル", (dialog, which) -> {
            isCancelled = true;
            Toast.makeText(this, "コピーをキャンセルしました", Toast.LENGTH_SHORT).show();
        });

        AlertDialog progressDialog = builder.create();
        progressDialog.show();

        new Thread(() -> {
            long totalSize = 0;
            for (File src : sources) {
                totalSize += calculateTotalSize(src);
            }

            long[] copiedBytes = new long[]{0};
            AtomicBoolean success = new AtomicBoolean(true);

            for (File src : sources) {
                if (isCancelled) break;

                File dest = new File(targetDir, src.getName());
                if (dest.exists() && src.isDirectory()) {
                    dest = generateUniqueFolder(targetDir, src.getName());
                }

                boolean result = copyRecursivelyWithProgress(
                        src, dest, textFile, progressBar, textPercent, totalSize, copiedBytes
                );

                if (!result) {
                    success.set(false);
                    break;
                }

                if ("cut".equals(action)) {
                    deleteRecursively(src);
                }
            }

            runOnUiThread(() -> {
                progressDialog.dismiss();

                if (isCancelled) {
                    Toast.makeText(this, "コピーをキャンセルしました", Toast.LENGTH_SHORT).show();
                } else if (success.get()) {
                    Toast.makeText(this,
                            (action.equals("cut") ? "移動" : "コピー") + "が完了しました",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "一部のファイルでコピーに失敗しました", Toast.LENGTH_SHORT).show();
                }

                clipboardFiles.clear();
                clipboardAction = "";
                loadDirectory(currentDir);
            });
        }).start();
    }

    private void showConflictResolutionDialog(File source, File targetDir, String action) {
        new AlertDialog.Builder(this)
                .setTitle("フォルダが既に存在します")
                .setMessage("フォルダ \"" + source.getName() + "\" は既に存在します。\nどうしますか？")
                .setPositiveButton("統合する", (dialog, which) -> {
                    File dest = new File(targetDir, source.getName());
                    startCopyWithProgress(source, dest, action);
                })
                .setNegativeButton("両方残す", (dialog, which) -> {
                    File dest = generateUniqueFolder(targetDir, source.getName());
                    startCopyWithProgress(source, dest, action);
                })
                .setNeutralButton("キャンセル", null)
                .show();
    }

    private void startCopyWithProgress(File source, File dest, String action) {
        // ... 前回までの progressDialog 作成・Thread などと同様

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("コピー中...");

        View view = getLayoutInflater().inflate(R.layout.dialog_progress, null);
        ProgressBar progressBar = view.findViewById(R.id.progressBar);
        TextView textFile = view.findViewById(R.id.textFileName);
        TextView textPercent = view.findViewById(R.id.textProgressPercent);

        builder.setView(view);
        builder.setCancelable(false);

        builder.setNegativeButton("キャンセル", (dialog, which) -> {
            isCancelled = true;
            Toast.makeText(this, "コピーをキャンセルしました", Toast.LENGTH_SHORT).show();
        });

        AlertDialog progressDialog = builder.create();
        progressDialog.show();

        new Thread(() -> {
            long totalSize = calculateTotalSize(source);
            long[] copiedBytes = new long[]{0};

            boolean success = copyRecursivelyWithProgress(
                    source, dest, textFile, progressBar, textPercent, totalSize, copiedBytes
            );

            runOnUiThread(() -> {
                progressDialog.dismiss();

                if (isCancelled) {
                    deleteRecursively(dest);
                    return;
                }

                if (success) {
                    if ("cut".equals(action)) {
                        deleteRecursively(source);
                        Toast.makeText(this, "移動完了", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "コピー完了", Toast.LENGTH_SHORT).show();
                    }
                    clipboardFile = null;
                    pendingAction = "";
                    loadDirectory(currentDir);
                } else {
                    Toast.makeText(this, "コピー失敗", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDirectory(currentDir);
    }

    private boolean copyRecursivelyWithProgress(File src, File dest,
                                                TextView textFile, ProgressBar progressBar, TextView textPercent,
                                                long totalSize, long[] copiedBytes) {
        if (isCancelled) return false;

        if (src.isFile() && dest.exists()) {
            ConflictAction action = (globalFileConflictAction != ConflictAction.ASK)
                    ? globalFileConflictAction
                    : showFileConflictDialogBlocking(src, dest);

            switch (action) {
                case OVERWRITE:
                    break;
                case SKIP:
                    return true;
                case KEEP_BOTH:
                    dest = generateUniqueFile(dest);
                    break;
                case CANCEL:
                    isCancelled = true;
                    return false;
            }
        }

        try {
            if (src.isDirectory()) {
                if (!dest.exists() && !dest.mkdirs()) {
                    return false;
                }

                File[] children = src.listFiles();
                if (children != null) {
                    for (File child : children) {
                        if (isCancelled) return false;
                        File newDest = new File(dest, child.getName());
                        if (!copyRecursivelyWithProgress(child, newDest, textFile, progressBar, textPercent, totalSize, copiedBytes)) {
                            return false;
                        }
                    }
                }
            } else {
                runOnUiThread(() -> textFile.setText("コピー中: " + src.getName()));

                try (InputStream in = new BufferedInputStream(new FileInputStream(src));
                     OutputStream out = new BufferedOutputStream(new FileOutputStream(dest))) {

                    byte[] buffer = new byte[1024 * 128]; // 128KB バッファで高速化
                    int len;
                    long lastUpdate = System.currentTimeMillis();

                    while ((len = in.read(buffer)) > 0) {
                        if (isCancelled) return false;

                        out.write(buffer, 0, len);
                        copiedBytes[0] += len;

                        // UI 更新は 100ms に1回だけ
                        long now = System.currentTimeMillis();
                        if (now - lastUpdate > 100) {
                            int progress = (int) ((copiedBytes[0] * 100) / totalSize);
                            runOnUiThread(() -> {
                                progressBar.setProgress(progress);
                                textPercent.setText(progress + "% 完了");
                            });
                            lastUpdate = now;
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private ConflictAction showFileConflictDialogBlocking(File src, File dest) {
        final ConflictAction[] userChoice = new ConflictAction[1];
        final boolean[] applyToAll = new boolean[]{false};

        CountDownLatch latch = new CountDownLatch(1);  // 同期処理に必要

        runOnUiThread(() -> {
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_conflict, null);
            CheckBox checkBox = dialogView.findViewById(R.id.checkboxApplyToAll);

            new AlertDialog.Builder(this)
                    .setTitle("ファイル")
                    .setMessage("ファイル \"" + dest.getName() + "\" は既に存在します。")
                    .setView(dialogView)
                    .setPositiveButton("上書き", (dialog, which) -> {
                        userChoice[0] = ConflictAction.OVERWRITE;
                        applyToAll[0] = checkBox.isChecked();
                        latch.countDown();
                    })
                    .setNegativeButton("スキップ", (dialog, which) -> {
                        userChoice[0] = ConflictAction.SKIP;
                        applyToAll[0] = checkBox.isChecked();
                        latch.countDown();
                    })
                    .setNeutralButton("両方残す", (dialog, which) -> {
                        userChoice[0] = ConflictAction.KEEP_BOTH;
                        applyToAll[0] = checkBox.isChecked();
                        latch.countDown();
                    })
                    .setCancelable(false)
                    .setOnCancelListener(dialog -> {
                        userChoice[0] = ConflictAction.CANCEL;
                        latch.countDown();
                    })
                    .show();
        });

        try {
            latch.await();  // ユーザーの操作を待つ
        } catch (InterruptedException e) {
            e.printStackTrace();
            return ConflictAction.CANCEL;
        }

        if (applyToAll[0]) {
            globalFileConflictAction = userChoice[0];
        }

        return userChoice[0];
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

        File newFile = new File(file.getParent(), baseName + extension);
        int counter = 1;
        while (newFile.exists()) {
            newFile = new File(file.getParent(), baseName + " (" + counter + ")" + extension);
            counter++;
        }
        return newFile;
    }

}


