package com.aozora.aozora;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import java.io.File;
import java.net.URLConnection;
import java.util.ArrayList;

public class DownloadListActivity extends Activity {

    private ListView downloadList;
    private ArrayList<File> downloadedFiles;
    private ArrayAdapter<String> adapter;
    private Button closeButton;
    private static final int REQUEST_OPEN_DOCUMENT = 101;
    private static final int REQUEST_STORAGE_PERMISSION = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_list);
        downloadList = findViewById(R.id.downloadList);
        closeButton = findViewById(R.id.closeButton);

        loadDownloadedFiles();

        // Action Bar が表示されているか確認
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        closeButton.setOnClickListener(v -> finish());

        downloadList.setOnItemClickListener((parent, view, position, id) -> openFile(downloadedFiles.get(position)));

        downloadList.setOnItemLongClickListener((parent, view, position, id) -> {
            showDeleteDialog(position);
            return true;
        });
    }

    // 権限がない場合にダイアログを表示し、設定画面へ誘導
    private void showPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("ストレージ権限が必要です")
                .setMessage("ダウンロードしたファイルを開くにはストレージの読み取り権限が必要です。設定画面で許可をしてください。")
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

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // 前の画面に戻る
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO},
                REQUEST_STORAGE_PERMISSION);
    }

    private void loadDownloadedFiles() {
        downloadedFiles = new ArrayList<>();
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File[] files = downloadDir.listFiles();

        if (files != null) {
            for (File file : files) {
                downloadedFiles.add(file);
            }
        }

        ArrayList<String> fileNames = new ArrayList<>();
        for (File file : downloadedFiles) {
            fileNames.add(file.getName());
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, fileNames);
        downloadList.setAdapter(adapter);
    }

    private void openFile(File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10 以降（API 29+）
            openFileWithDocumentPicker();
        } else { // API 28 以下
            if (!checkStoragePermission()) {
                return; // 権限がない場合はダイアログを表示し、処理を中断
            }
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri fileUri = Uri.fromFile(file);
            String mimeType = getMimeType(file);
            intent.setDataAndType(fileUri, mimeType);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                Intent chooser = Intent.createChooser(intent, "ファイルを開くアプリを選択");
                startActivity(chooser);
            } catch (Exception e) {
                Toast.makeText(this, "ファイルを開けません", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OPEN_DOCUMENT && resultCode == RESULT_OK) {
            if (data != null) {
                Uri fileUri = data.getData();

                // ファイルを開く
                Intent openIntent = new Intent(Intent.ACTION_VIEW);
                openIntent.setData(fileUri);
                openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try {
                    startActivity(openIntent);
                } catch (Exception e) {
                    Toast.makeText(this, "ファイルを開けません", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void openFileWithDocumentPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); // すべてのファイルを開けるようにする
        startActivityForResult(intent, REQUEST_OPEN_DOCUMENT);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "ストレージ権限が許可されました", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "ストレージ権限が必要です", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getMimeType(File file) {
        String mimeType = null;

        // 1. ContentResolverを使用
        ContentResolver cR = getContentResolver();
        Uri fileUri = Uri.fromFile(file);
        mimeType = cR.getType(fileUri);

        // 2. MimeTypeMapを使用（ContentResolverで特定できない場合）
        if (mimeType == null) {
            String extension = MimeTypeMap.getFileExtensionFromUrl(file.getAbsolutePath());
            if (extension != null) {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            }
        }

        // 3. URLConnectionを使用（MimeTypeMapでも特定できない場合）
        if (mimeType == null) {
            try {
                mimeType = URLConnection.guessContentTypeFromName(file.getName());
            } catch (Exception e) {
                // エラー処理
            }
        }

        // 4. 汎用タイプ（上記すべてで特定できない場合）
        if (mimeType == null) {
            mimeType = "*/*";
        }

        return mimeType;
    }

    private void showDeleteDialog(int position) {
        new AlertDialog.Builder(this)
                .setTitle("ファイル削除")
                .setMessage("選択したファイルを削除しますか？")
                .setPositiveButton("削除", (dialog, which) -> deleteFile(position))
                .setNegativeButton("キャンセル", null)
                .show();
    }

    private void deleteFile(int position) {
        File file = downloadedFiles.get(position);
        if (file.delete()) {
            downloadedFiles.remove(position);
            adapter.remove(adapter.getItem(position));
            adapter.notifyDataSetChanged();
            Toast.makeText(this, "ファイルを削除しました", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "ファイル削除に失敗しました", Toast.LENGTH_SHORT).show();
        }
    }
}