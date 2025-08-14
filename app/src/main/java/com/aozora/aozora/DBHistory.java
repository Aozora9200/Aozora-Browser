package com.aozora.aozora;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DBHistory extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "history.db";
    private static final int DATABASE_VERSION = 1;

    public DBHistory(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE history (id INTEGER PRIMARY KEY AUTOINCREMENT, url TEXT, title TEXT, timestamp INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS history");
        onCreate(db);
    }

    // 🔹 履歴を保存するメソッド
    public void saveHistory(String url, String title, long timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("INSERT INTO history (url, title, timestamp) VALUES (?, ?, ?)", new Object[]{url, title, timestamp});
        db.close();
    }

    // 🔹 カテゴリ別の履歴を取得
    public HashMap<String, List<String>> getHistoryByCategory() {
        HashMap<String, List<String>> categorizedHistory = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();

        long now = System.currentTimeMillis();
        long todayStart = now - (now % 86400000);
        long yesterdayStart = todayStart - 86400000;
        long lastWeekStart = todayStart - 7 * 86400000;
        long lastMonthStart = todayStart - 30 * 86400000;
        long lastYearStart = todayStart - 365 * 86400000; // 🔹 「それより前」の境界

        String[] categories = {"今日", "昨日", "今週", "今月", "先月", "それより前"};
        long[] timeRanges = {todayStart, yesterdayStart, lastWeekStart, lastMonthStart, lastYearStart};

        for (int i = 0; i < categories.length; i++) {
            List<String> urls = new ArrayList<>();
            String query;

            if (i == 0) {
                // 🔹 「今日」は `timestamp >= todayStart` のみ
                query = "SELECT title, url FROM history WHERE timestamp >= " + timeRanges[i];
            } else if (i < categories.length - 1) {
                // 🔹 「昨日」「今週」「今月」「先月」
                query = "SELECT title, url FROM history WHERE timestamp >= " + timeRanges[i] +
                        " AND timestamp < " + timeRanges[i - 1];
            } else {
                // 🔹 「それより前」は `timestamp < lastYearStart`
                query = "SELECT title, url FROM history WHERE timestamp < " + lastYearStart;
            }

            Cursor cursor = db.rawQuery(query, null);
            while (cursor.moveToNext()) {
                urls.add(cursor.getString(0) + " - " + cursor.getString(1));
            }
            cursor.close();
            categorizedHistory.put(categories[i], urls);
        }
        db.close();

        // 🔹 `categorizedHistory` にデータがない場合、空のリストを追加
        if (categorizedHistory.isEmpty()) {
            categorizedHistory.put("履歴なし", new ArrayList<>());
        }
        return categorizedHistory;
    }
}
