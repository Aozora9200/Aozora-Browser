package com.aozora.aozora;

import android.app.Activity;
import android.app.AlertDialog;
import android.database.Cursor;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

public class SavedPagesActivity extends Activity {

    private RecyclerView recyclerView;
    private SavedPageAdapter adapter;
    private List<SavedPage> pageList = new ArrayList<>();
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.savedpagelist);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        dbHelper = new DBHelper(this);
        loadSavedPages();
        // Action Bar が表示されているか確認
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // 前の画面に戻る
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadSavedPages() {
        pageList.clear();
        Cursor cursor = dbHelper.getReadableDatabase().rawQuery("SELECT id, url, title, screenshot_path, date_saved FROM pages", null);
        while (cursor.moveToNext()) {
            pageList.add(new SavedPage(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4)));
        }
        cursor.close();

        adapter = new SavedPageAdapter(this, pageList, new SavedPageAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(SavedPage page) {
                Intent intent = new Intent(SavedPagesActivity.this, MainActivity.class);
                intent.putExtra("url", page.url);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP); // MainActivity を復帰させる
                startActivity(intent);
                finish();
            }

            public void onItemLongClick(SavedPage page) {
                new AlertDialog.Builder(SavedPagesActivity.this)
                        .setTitle("削除")
                        .setMessage("このページを削除しますか？")
                        .setPositiveButton("削除", (dialog, which) -> {
                            dbHelper.getWritableDatabase().delete("pages", "id=?", new String[]{String.valueOf(page.id)});
                            loadSavedPages();
                        })
                        .setNegativeButton("キャンセル", null)
                        .show();
            }
        });

        recyclerView.setAdapter(adapter);
    }

}
