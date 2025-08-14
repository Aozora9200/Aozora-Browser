package com.aozora.aozora;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.MimeTypeMap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.provider.DocumentFile;
import android.webkit.WebChromeClient;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.pm.PackageManager;
import android.os.Build;
import android.Manifest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

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
    private List<File> selectedFiles = new ArrayList<>();
    private boolean isSelectionMode = false;
    private volatile boolean isCancelled = false;
    private static final int REQUEST_STORAGE_PERMISSION = 100;
    private static final int REQUEST_OPEN_DOCUMENT = 101;

    private enum ConflictAction {
        ASK, OVERWRITE, SKIP, KEEP_BOTH, CANCEL
    }

    private ConflictAction globalFileConflictAction = ConflictAction.ASK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.file);  // XMLレイアウトを読み込む

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
        } else {
            // API 29 以下では READ_EXTERNAL_STORAGE が必要
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
            } else {
                // 権限がある
            }
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
            if (item.file.isDirectory()) {
                loadDirectory(item.file);
            } else {
                openFile(item.file);
            }
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            FileItem item = files.get(position);
            showItemOptionsDialog(item);
            return true;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_file, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.file_close) {
            finish();
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
            if (clipboardFile != null && !pendingAction.isEmpty()) {
                startPasteOperation(clipboardFile, currentDir, pendingAction);
            } else {
                Toast.makeText(this, "カットまたはコピーを選択してください", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        if (item.getItemId() == android.R.id.home) {
            if (currentDir != null && currentDir.getParentFile() != null) {
                loadDirectory(currentDir.getParentFile());
            } else {
                finish(); // 前の画面に戻る
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
            Toast.makeText(this, selectedFiles.size() + " 件選択中", Toast.LENGTH_SHORT).show();
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
        String[] options = {"カット", "コピー", "削除", "名前の変更"};

        new AlertDialog.Builder(this)
                .setTitle(item.file.getName())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            // カット
                            pendingAction = "cut";
                            clipboardFile = item.file;
                            Toast.makeText(this, "カットしました: " + item.file.getName(), Toast.LENGTH_SHORT).show();
                            break;
                        case 1:
                            // コピー
                            pendingAction = "copy";
                            clipboardFile = item.file;
                            Toast.makeText(this, "コピーしました: " + item.file.getName(), Toast.LENGTH_SHORT).show();
                            break;
                        case 2:
                            // 削除
                            confirmAndDelete(item.file);
                            break;
                        case 3:
                            // 名前の変更
                            showRenameDialog(item.file);
                            break;
                    }
                })
                .show();
    }

    private void confirmAndDelete(File file) {
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
    }

    private void info() {
        new AlertDialog.Builder(this)
                .setTitle("アプリ情報")
                .setMessage("ファイルマネージャ バージョン 0.5 -Release")
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

    private String getMimeType(File file) {
        String ext = MimeTypeMap.getFileExtensionFromUrl(file.getAbsolutePath());
        if (ext != null) {
            String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase());
            if (type != null) return type;
        }
        return "*/*";
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

    private void openFile(File file) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10 以降（API 29+）
                openFileWithDocumentPicker();
            } else { // API 28 以下
                if (!checkStoragePermission()) {
                    return; // 権限がない場合はダイアログを表示し、処理を中断
                }
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri fileUri = Uri.fromFile(file);
                String mimeType = getMimeType(file);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setDataAndType(fileUri, mimeType);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    Intent chooser = Intent.createChooser(intent, "ファイルを開くアプリを選択");
                    startActivity(chooser);
                } catch (Exception e) {
                    Toast.makeText(this, "ファイルを開けません", Toast.LENGTH_SHORT).show();
                }
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
        // ルートでない場合は1つ上のフォルダへ戻る
        if (isSearchMode) {
            isSearchMode = false;
            searchView.setQuery("", false);
            searchView.clearFocus();
            loadDirectory(currentDir);  // 検索解除して元に戻す
        } else if (currentDir != null && currentDir.getParentFile() != null) {
            loadDirectory(currentDir.getParentFile());
        } else {
            super.onBackPressed(); // ルートなら通常の挙動（アプリ終了など）
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
                    // 通常通り上書き
                    break;
                case SKIP:
                    return true;  // 何もせずスキップ
                case KEEP_BOTH:
                    dest = generateUniqueFile(dest);
                    break;
                case CANCEL:
                    isCancelled = true;
                    return false;
                default:
                    break;
            }
        }

        try {
            if (src.isDirectory()) {
                if (!dest.exists() && !dest.mkdirs()) {
                    return false;  // ディレクトリ作成失敗
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

                try (InputStream in = new FileInputStream(src);
                     OutputStream out = new FileOutputStream(dest)) {

                    byte[] buffer = new byte[4096];
                    int len;

                    while ((len = in.read(buffer)) > 0) {
                        if (isCancelled) return false;

                        out.write(buffer, 0, len);
                        copiedBytes[0] += len;

                        int progress = (int) ((copiedBytes[0] * 100) / totalSize);
                        runOnUiThread(() -> {
                            progressBar.setProgress(progress);
                            textPercent.setText(progress + "% 完了");
                        });
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


