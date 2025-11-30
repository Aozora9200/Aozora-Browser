package com.aozora.aozora;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.SystemClock;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DownloadHistoryManager {

    private static final String PREF_NAME = "AdvancedBrowserPrefs";
    private static final String KEY_DOWNLOAD_HISTORY = "download_history";

    public static void addDownloadHistory(Context context, long downloadId, String fileName, String filePath) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        try {
            JSONArray array = new JSONArray(pref.getString(KEY_DOWNLOAD_HISTORY, "[]"));
            JSONObject obj = new JSONObject();
            obj.put("id", downloadId);
            obj.put("fileName", fileName);
            obj.put("filePath", filePath);
            array.put(obj);
            pref.edit().putString(KEY_DOWNLOAD_HISTORY, array.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void monitorDownloadProgress(Context context, long downloadId, DownloadManager dm) {
        new Thread(() -> {
            long startTime = SystemClock.elapsedRealtime();
            while (true) {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(downloadId);
                try (Cursor cursor = dm.query(query)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int bytesDownloaded = safeGetInt(cursor, DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                        int status = safeGetInt(cursor, DownloadManager.COLUMN_STATUS);

                        if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                            break;
                        }

                        if (bytesDownloaded > 0) {
                            startTime = SystemClock.elapsedRealtime();
                        } else if (SystemClock.elapsedRealtime() - startTime > 60000) {
                            dm.remove(downloadId);
                            if (context instanceof Activity) {
                                ((Activity) context).runOnUiThread(() ->
                                        Toast.makeText(context, "ダウンロードが進行しなかったためキャンセルしました", Toast.LENGTH_SHORT).show());
                            }
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }).start();
    }

    private static int safeGetInt(Cursor cursor, String columnName) {
        try {
            int index = cursor.getColumnIndexOrThrow(columnName);
            return cursor.getInt(index);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}
