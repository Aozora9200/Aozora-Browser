package com.aozora.aozora;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class notepad extends Activity {

    private EditText editText;
    private final int PICK_TXT_FILE = 1;
    private final int CREATE_TXT_FILE = 2;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler uiHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notepad);

        editText = findViewById(R.id.editText);
        Button btnSave = findViewById(R.id.btnSave);
        Button btnLoad = findViewById(R.id.btnLoad);
        Button btnClear = findViewById(R.id.btnClear);

        // Action Bar が表示されているか確認
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchFileSaveIntent();
            }
        });

        btnLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFilePicker();
            }
        });

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clear();
            }
        });
        Intent intent = getIntent();
        Uri uri = intent.getData();

        if (uri != null) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }

                editText.setText(stringBuilder.toString());
                reader.close();
            } catch (IOException e) {
                editText.setText("ファイルの読み込み中にエラーが発生しました。");
                e.printStackTrace();
            }
        }
    }

    private void launchFileSaveIntent() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, "note.txt");
        startActivityForResult(intent, CREATE_TXT_FILE);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // 前の画面に戻る
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void clear() {
        new AlertDialog.Builder(this)
                .setTitle("本当によろしいですか?")
                .setMessage("内容を消す Y/N?")
                .setPositiveButton("削除", (dialog, which) -> {
                    editText.setText("");
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }

    private void saveText() {
        final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        final String fileName = timeStamp + (".txt");
        executor.execute(() -> {
            try {
                File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File file = new File(downloadDir, fileName);
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
                bos.write(editText.getText().toString().getBytes(StandardCharsets.UTF_8));
                bos.flush();
                bos.close();
                uiHandler.post(() -> Toast.makeText(notepad.this, "保存しました: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show());
            } catch (final Exception e) {
                e.printStackTrace();
                uiHandler.post(() -> Toast.makeText(notepad.this, "保存に失敗しました: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/plain");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select Text File"), PICK_TXT_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                if (requestCode == PICK_TXT_FILE) {
                    readTextFromUri(uri);
                } else if (requestCode == CREATE_TXT_FILE) {
                    writeTextToUri(uri);
                }
            }
        }
    }

    private void readTextFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
            reader.close();
            inputStream.close();
            editText.setText(builder.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeTextToUri(Uri uri) {
        try {
            OutputStream outputStream = getContentResolver().openOutputStream(uri);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
            writer.write(editText.getText().toString());
            writer.flush();
            writer.close();
            outputStream.close();
            Toast.makeText(this, "保存しました", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "保存に失敗しました", Toast.LENGTH_SHORT).show();
        }
    }
}
