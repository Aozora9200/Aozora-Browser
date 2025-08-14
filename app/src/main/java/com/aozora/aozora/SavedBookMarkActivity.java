package com.aozora.aozora;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class SavedBookMarkActivity extends Activity {

    private RecyclerView recyclerView;
    private SavedBookMarkAdapter adapter;
    private List<SavedBookMark> pageList = new ArrayList<>();
    private DBBM dbHelper;
    private static final int PICK_JSON_FILE_REQUEST = 1; // ファイル選択リクエストコード

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.savedpagelist);
        dbHelper = new DBBM(this);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        loadBookMarks();
        // Action Bar が表示されているか確認
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    // 📂 ユーザーに JSON ファイルを選択させる
    public void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/json");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "JSON ファイルを選択"), PICK_JSON_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_JSON_FILE_REQUEST && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                importBookmarksFromJson(uri);
            }
        }
    }

    // 📥 JSON ファイルをデータベースにインポート
    private void importBookmarksFromJson(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder jsonText = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonText.append(line);
            }
            reader.close();

            // データベースの既存データを削除してからインポート
            if (dbHelper.importBookmarksFromJson(jsonText.toString())) {
                Toast.makeText(this, "ブックマークをインポートしました", Toast.LENGTH_SHORT).show();
                recreate(); // 画面を再読み込み
            } else {
                Toast.makeText(this, "インポートに失敗しました", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "ファイルを読み込めませんでした", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // 前の画面に戻る
            return true;
        }
        return true;
    }

    private void loadBookMarks() {
        pageList.clear();
        Cursor cursor = dbHelper.getReadableDatabase().rawQuery("SELECT id, url, title, screenshot_path FROM pages", null);
        while (cursor.moveToNext()) {
            pageList.add(new SavedBookMark(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getString(3)));
        }
        cursor.close();

        adapter = new SavedBookMarkAdapter(this, pageList, new SavedBookMarkAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(SavedBookMark page) {
                Intent intent = new Intent(SavedBookMarkActivity.this, MainActivity.class);
                intent.putExtra("url", page.url);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP); // MainActivity を復帰させる
                startActivity(intent);
                finish();
            }

            public void onItemLongClick(SavedBookMark page) {
                new AlertDialog.Builder(SavedBookMarkActivity.this)
                        .setPositiveButton("編集", (dialog, which) -> {
                            showEditDialog(page);
                        })
                        .setNegativeButton("削除", (dialog, which) -> {
                            checkbookmarksdel(page);
                        })
                        .show();
            }
        });

        recyclerView.setAdapter(adapter);
    }
    private void checkbookmarksdel(SavedBookMark page) {
        new AlertDialog.Builder(SavedBookMarkActivity.this)
                .setMessage("このブックマークを削除しますか？")
                .setPositiveButton("OK", (dialog, which) -> {
                    dbHelper.getWritableDatabase().delete("pages", "id=?", new String[]{String.valueOf(page.id)});
                    loadBookMarks();
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }

    public void showEditDialog(final SavedBookMark bookmark) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("ブックマークの編集");

        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_edit_bookmark, null);
        final EditText inputUrl = viewInflated.findViewById(R.id.editUrl);
        final EditText inputTitle = viewInflated.findViewById(R.id.editTitle);

        inputUrl.setText(bookmark.url);
        inputTitle.setText(bookmark.title);

        builder.setView(viewInflated);

        builder.setPositiveButton("保存", (dialog, which) -> {
            String newUrl = inputUrl.getText().toString().trim();
            String newTitle = inputTitle.getText().toString().trim();
            updateBookMark(bookmark.id, newUrl, newTitle);
        });
        builder.setNegativeButton("キャンセル", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void updateBookMark(int id, String newUrl, String newTitle) {
        ContentValues values = new ContentValues();
        values.put("url", newUrl);
        values.put("title", newTitle);

        int rowsUpdated = dbHelper.getWritableDatabase().update("pages", values, "id=?", new String[]{String.valueOf(id)});
        if (rowsUpdated > 0) {
            Toast.makeText(this, "ブックマークを更新しました", Toast.LENGTH_SHORT).show();
            loadBookMarks();
        } else {
            Toast.makeText(this, "ブックマークの更新に失敗しました", Toast.LENGTH_SHORT).show();
        }
    }

}
