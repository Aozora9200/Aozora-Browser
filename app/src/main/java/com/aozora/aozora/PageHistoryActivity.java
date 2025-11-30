package com.aozora.aozora;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;

public class PageHistoryActivity extends Activity {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private ArrayList<MainActivity.HistoryItem> historyItems;
    private TextView textViewNoHistory;
    private DBBM dbbm;

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applySavedTheme();
        setContentView(R.layout.activity_history_list);
        Background = findViewById(R.id.background);
        applySavedBackground();
        applyBackTheme();

        recyclerView = findViewById(R.id.recyclerViewHistory);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setReverseLayout(true);   // 並びを逆にする（下が古い）
        layoutManager.setStackFromEnd(true);    // スクロールの基準を末尾に
        recyclerView.setLayoutManager(layoutManager);

        // MainActivity から受け取った履歴データ
        historyItems = (ArrayList<MainActivity.HistoryItem>)
                getIntent().getSerializableExtra("history_list");

        textViewNoHistory = findViewById(R.id.textViewNoHistory);

        dbbm = new DBBM(this);

        // 🚫 ローカルHTMLを削除
        Iterator<MainActivity.HistoryItem> it = historyItems.iterator();
        while (it.hasNext()) {
            String url = it.next().getUrl();
            if (url.startsWith("file:///android_asset/index.html") ||
                    url.startsWith("file:///android_asset/index_white.html") ||
                    url.startsWith("file:///android_asset/help.html")) {
                it.remove();
            }
        }

        if (historyItems == null || historyItems.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            textViewNoHistory.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            textViewNoHistory.setVisibility(View.GONE);

        }

        adapter = new HistoryAdapter(this, historyItems, null, new HistoryAdapter.HistoryListener() {
            @Override
            public void onHistoryItemClick(String url) {
                Intent data = new Intent();
                data.putExtra("selected_url", url);
                setResult(RESULT_OK, data);
                finish();
            }

            @Override
            public void onHistoryItemNewTab(String url) {
                Intent data = new Intent();
                data.putExtra("historyAddNewTab", url);
                setResult(RESULT_OK, data);
                finish();
            }

            @Override
            public void onHistoryDeleted() {
                // 履歴保存処理 or Activityに反映
            }

            @Override
            public Bitmap getFavicon(String url) {
                return null; // or 共有キャッシュから取得
            }

            @Override
            public void copyToClipboard(String text) {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("url", text));
            }

            @Override
            public void historySavebm(String url, String title) {
                savebm(url, title);
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
        });
        recyclerView.setAdapter(adapter);

        // Action Bar が表示されているか確認
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
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

    private void savebm(String url, String title) {
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

    private void savebmProcess(String url, String title) {

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
            Toast.makeText(this, "ブックマークを削除しました", Toast.LENGTH_SHORT).show();
        } else {

                SQLiteDatabase dbw = dbbm.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("url", url);
                values.put("title", title);
                values.put("screenshot_path", R.drawable.transparent_vector);
                dbw.insert("pages", null, values);

                Toast.makeText(this, "ブックマークを保存しました", Toast.LENGTH_SHORT).show();
        }
    }

    private void returnResult() {
        Intent data = new Intent();
        data.putExtra("history_list", historyItems);
        setResult(RESULT_OK, data);
    }

    private void allclose() {
        new AlertDialog.Builder(this)
                .setTitle("履歴の削除")
                .setMessage("履歴をすべて削除してもよろしいですか?")
                .setPositiveButton("削除", (dialog, which) -> {
                    historyItems.clear();
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "履歴を削除しました", Toast.LENGTH_SHORT).show();
                    returnResult();
                    textViewNoHistory.setVisibility(View.VISIBLE);
                    new Handler().postDelayed(() -> {
                        finish();
                        overridePendingTransition(R.anim.no_animation,  R.anim.slide_out_down_low);
                    }, 500);
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_history, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.history_removeall) {
            allclose();
        }
        if (itemId == R.id.history_close) {
            finish();
            overridePendingTransition(R.anim.no_animation,  R.anim.slide_out_down_low);
        }
        if (item.getItemId() == android.R.id.home) {
            finish(); // 前の画面に戻る
            overridePendingTransition(R.anim.no_animation,  R.anim.slide_out_down_low);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        finish(); // 前の画面に戻る
        overridePendingTransition(R.anim.no_animation,  R.anim.slide_out_down_low);
    }

}