package com.aozora.aozora;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.os.Environment;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class DBBM extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "saved_bm.db";
    private static final int DATABASE_VERSION = 1;

    public DBBM(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE pages (id INTEGER PRIMARY KEY AUTOINCREMENT, url TEXT, title TEXT, screenshot_path TEXT, date_saved TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS pages");
        onCreate(db);
    }

    public boolean exportBookmarksToJson(Context context) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT title, url FROM pages", null);
        JSONArray jsonArray = new JSONArray();

        try {
            while (cursor.moveToNext()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("title", cursor.getString(0)); // title
                jsonObject.put("url", cursor.getString(1)); // url
                jsonArray.put(jsonObject);
            }
            cursor.close();

            // 📂 API 19 でも保存可能なディレクトリを使用
            File directory = new File(Environment.getExternalStorageDirectory(), "Download");
            if (!directory.exists()) directory.mkdirs();

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File file = new File(directory, "bookmark_" + timeStamp + ".json");

            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(jsonArray.toString(4));
            fileWriter.close();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            cursor.close();
        }
    }

    public boolean importBookmarksFromJson(String jsonString) {
        SQLiteDatabase db = getWritableDatabase();

        try {
            // 📌 既存のデータを削除
            db.execSQL("DELETE FROM pages");

            // JSONをパース
            JSONArray jsonArray = new JSONArray(jsonString);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String title = jsonObject.getString("title");
                String url = jsonObject.getString("url");

                // データベースに追加
                ContentValues values = new ContentValues();
                values.put("title", title);
                values.put("url", url);
                db.insert("pages", null, values);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}