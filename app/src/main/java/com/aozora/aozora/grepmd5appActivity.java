package com.aozora.aozora;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class grepmd5appActivity extends Activity {

    private static final int PICK_FILE_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private Uri selectedFileUri;
    private EditText grepInput;
    private TextView resultTextView;
    private Uri lastMd5Uri;
    private String lastMd5Result;
    private Uri lastGrepUri;
    private String lastGrepKeyword;
    private String lastGrepResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grep);

        Button selectFileButton = findViewById(R.id.selectFileButton);
        Button grepButton = findViewById(R.id.grepButton);
        Button md5Button = findViewById(R.id.md5Button);
        grepInput = findViewById(R.id.grepInput);
        resultTextView = findViewById(R.id.resultTextView);

        // Action Bar が表示されているか確認
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (!checkPermissions()) {
            requestPermissions();
        }

        selectFileButton.setOnClickListener(v -> openFilePicker());
        grepButton.setOnClickListener(v -> executeGrep());
        md5Button.setOnClickListener(v -> checkMd5());
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // 前の画面に戻る
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true; // API 22以下はインストール時に全権限が許可される
        }
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                showPermissionDialog(); // 権限がないので、ダイアログを表示
            }
        }
    }

    private void showPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("権限が必要です")
                .setMessage("この機能を使用するにはストレージの読み取り権限が必要です。権限を有効にしてください。")
                .setPositiveButton("許可", (dialog, which) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestPermissions(new String[]{
                                Manifest.permission.READ_MEDIA_IMAGES,
                                Manifest.permission.READ_MEDIA_VIDEO,
                                Manifest.permission.READ_MEDIA_AUDIO
                        }, PERMISSION_REQUEST_CODE);
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE
                        }, PERMISSION_REQUEST_CODE);
                    }
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "ストレージ権限が許可されました。", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "権限が拒否されました。アプリの機能が制限されます。", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void openFilePicker() {
        if (checkPermissions()) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE); // 安全のため追加
            try {
                startActivityForResult(Intent.createChooser(intent, "ファイルを選択"), PICK_FILE_REQUEST);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "ファイルマネージャーが見つかりませんでした。", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "権限がありません。ファイルを選択できません。", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            if (fileUri == null) {
                Toast.makeText(this, "ファイルが選択されませんでした。", Toast.LENGTH_SHORT).show();
                return;
            }
            selectedFileUri = fileUri;
            String fileName = getFileName(fileUri);
            Toast.makeText(this, "ファイルが選択されました: " + (fileName != null ? fileName : "不明"), Toast.LENGTH_SHORT).show();
        }
    }

    private String getFileName(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DISPLAY_NAME};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    private void executeGrep() {
        if (selectedFileUri == null || grepInput.getText().toString().isEmpty()) {
            resultTextView.setText("ファイルとキーワードを選択してください。");
            return;
        }
        String keyword = grepInput.getText().toString();
        new GrepTask().execute(selectedFileUri, keyword);
    }

    private class GrepTask extends AsyncTask<Object, Void, String> {
        @Override
        protected String doInBackground(Object... params) {
            Uri uri = (Uri) params[0];
            String keyword = (String) params[1];
            return grepFile(uri, keyword);
        }

        @Override
        protected void onPostExecute(String result) {
            resultTextView.setText(result);
            saveLog("grep_log", result);
        }
    }

    private String grepFile(Uri uri, String keyword) {
        if (lastGrepUri != null && lastGrepUri.equals(uri)
                && lastGrepKeyword != null && lastGrepKeyword.equals(keyword)) {
            return lastGrepResult;
        }
        StringBuilder result = new StringBuilder();
        Pattern pattern = Pattern.compile(keyword, Pattern.CASE_INSENSITIVE);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(getContentResolver().openInputStream(uri)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String filteredLine = line.replaceAll("[^A-Za-z0-9 ]", "");
                Matcher matcher = pattern.matcher(filteredLine);
                if (matcher.find()) {
                    result.append(filteredLine.trim()).append("\n");
                }
            }
        } catch (IOException e) {
            result.append("エラー: ").append(e.getMessage());
        }
        lastGrepUri = uri;
        lastGrepKeyword = keyword;
        lastGrepResult = result.toString();
        return lastGrepResult;
    }

    private void checkMd5() {
        if (selectedFileUri == null) {
            resultTextView.setText("ファイルを選択してください。");
            return;
        }
        new Md5Task().execute(selectedFileUri);
    }

    private class Md5Task extends AsyncTask<Uri, Void, String> {
        @Override
        protected String doInBackground(Uri... uris) {
            return getMd5Checksum(uris[0]);
        }

        @Override
        protected void onPostExecute(String md5) {
            resultTextView.setText("MD5: " + md5);
            saveLog("md5sum_log", md5);
        }
    }

    private String getMd5Checksum(Uri uri) {
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is == null) return "エラー: ファイルが開けません。";
            if (lastMd5Uri != null && lastMd5Uri.equals(uri) && lastMd5Result != null) {
                return lastMd5Result;
            }
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            byte[] md5Bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : md5Bytes) {
                sb.append(String.format("%02x", b));
            }
            lastMd5Uri = uri;
            lastMd5Result = sb.toString();
            return lastMd5Result;
        } catch (Exception e) {
            return "エラー: " + e.getMessage();
        }
    }

    private void saveLog(String logType, String content) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = getFileName(selectedFileUri);
        if (fileName == null) {
            fileName = "unknown";
        }
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File logFile = new File(downloadDir, logType + "_" + fileName + "_" + timestamp + ".txt");

        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.write(content + "\n");
            runOnUiThread(() -> Toast.makeText(grepmd5appActivity.this, "ログを保存しました！", Toast.LENGTH_SHORT).show());
        } catch (IOException e) {
            Log.e("grepmd5appActivity", "ログ保存エラー", e);
        }
    }
}