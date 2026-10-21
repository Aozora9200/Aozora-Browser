package com.aozora.aozora;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcel;
import android.webkit.WebBackForwardList;
import android.webkit.WebHistoryItem;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Aozora → AirSky Browser へのデータの引っ越し(書き出し側)。
 *
 * <p>「ダウンロード/Aozora/.Aozora-西暦.az」(隠しファイル)に書き出す。中身は ZIP で、
 * AirSky が読む形式(Aozora2 の docs/aozora-transfer-format.md)に合わせて
 * {@code shared_prefs/*.xml} と {@code databases/*.db} を入れ、さらにタブ履歴・ページ履歴・
 * ブックマーク・保存ページ・設定内容をまとめた {@code aozora-data.json} を入れる。
 * タブごとの戻る/進む履歴は {@code tab_history.json} に、保存したページのファイル(.mht)と
 * スクリーンショットは {@code saved_pages/} に入れる。</p>
 *
 * <p>.az は共有ストレージに置かれるので、アプリのロック(password_prefs)などは書き出さない。</p>
 */
final class AirSkyTransfer {

    static final String AIRSKY_PACKAGE = "com.airsky.browser";
    /** AirSky を起動する時に付ける extra(true で引っ越しの確認が出る)。 */
    static final String EXTRA_TO_AIRSKY = "Aozora_to_AirSky";

    private static final String MANIFEST_FORMAT = "aozora-airsky-transfer";
    private static final int FORMAT_VERSION = 1;

    /** 書き出す SharedPreferences(AirSky が読むもの)。 */
    private static final String[] PREFS = {
            "AppPrefs", "WebViewTabs", "AdvancedBrowserPrefs", "theme_prefs",
            "popup_prefs", "arrow_prefs", "GeoPermissionStore",
    };
    /** 書き出さない設定(ロックの解除時刻・初回起動のフラグ)。 */
    private static final Set<String> EXCLUDED_KEYS = new HashSet<>(Arrays.asList(
            "password_skip_minutes", "password_skip_until", "isFirstRun_v2", "isFirstMov"));
    /** 保存したページのデータベース。 */
    private static final String SAVED_PAGES_DB = "saved_pages.db";
    /** ZIP にそのまま入れるデータベース。 */
    private static final String[] DATABASES = {"history.db", "saved_bm.db", SAVED_PAGES_DB};
    /** タブごとの戻る/進む履歴。 */
    private static final String TAB_HISTORY = "tab_history.json";
    /** 保存したページのファイルとスクリーンショットを入れるフォルダ。 */
    private static final String SAVED_PAGES_DIR = "saved_pages/";

    private AirSkyTransfer() {
    }

    /** 「ダウンロード/Aozora」。 */
    @NonNull
    static File folder() {
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Aozora");
    }

    static boolean isAirSkyInstalled(@NonNull Context context) {
        try {
            context.getPackageManager().getPackageInfo(AIRSKY_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /** AirSky を起動する Intent。インストールされていなければ null。 */
    @Nullable
    static Intent launchIntent(@NonNull Context context, boolean transferData) {
        Intent i = context.getPackageManager().getLaunchIntentForPackage(AIRSKY_PACKAGE);
        if (i == null) return null;
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (transferData) {
            i.putExtra(EXTRA_TO_AIRSKY, true);
            // AirSky が起動中だとタスクが前に出るだけで extra が届かないことがあるので、起動画面から始め直す
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }
        return i;
    }

    /**
     * データを .Aozora-西暦.az に書き出す。書き出したファイルを返す。
     *
     * @param tabHistory {@link #collectTabHistory} の結果(UI スレッドで先に作っておく)
     */
    @NonNull
    static File export(@NonNull Context context, @NonNull JSONObject tabHistory) throws IOException, JSONException {
        File dir = folder();
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("フォルダを作成できません: " + dir);
        }
        int year = Calendar.getInstance().get(Calendar.YEAR);
        File out = new File(dir, ".Aozora-" + year + ".az");
        if (out.exists() && !out.delete()) {
            throw new IOException("以前の引っ越しファイルを置き換えられません: " + out);
        }

        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(out))) {
            putText(zip, "manifest.json", manifest(context).toString());

            for (String name : PREFS) {
                Map<String, ?> all = context.getSharedPreferences(name, Context.MODE_PRIVATE).getAll();
                if (all.isEmpty()) continue;
                putText(zip, "shared_prefs/" + name + ".xml", prefsXml(all));
            }

            for (String name : DATABASES) {
                File db = context.getDatabasePath(name);
                if (!db.isFile()) continue;
                checkpoint(db);
                putFile(zip, "databases/" + name, db);
            }

            putText(zip, TAB_HISTORY, tabHistory.toString());
            putSavedPageFiles(context, zip);

            putText(zip, "aozora-data.json", dataJson(context, tabHistory).toString(2));
        } catch (IOException | JSONException | RuntimeException e) {
            //noinspection ResultOfMethodCallIgnored
            out.delete();
            throw e;
        }
        return out;
    }

    @NonNull
    private static JSONObject manifest(@NonNull Context context) throws JSONException {
        JSONObject m = new JSONObject();
        m.put("format", MANIFEST_FORMAT);
        m.put("version", FORMAT_VERSION);
        m.put("app", context.getPackageName());
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            m.put("versionName", pi.versionName);
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        m.put("created", System.currentTimeMillis());
        return m;
    }

    // ------------------------------------------------------------------
    // aozora-data.json(タブ履歴・ページ履歴・ブックマーク・保存ページ・設定)
    // ------------------------------------------------------------------

    @NonNull
    private static JSONObject dataJson(@NonNull Context context, @NonNull JSONObject tabHistory)
            throws JSONException {
        SharedPreferences tabsPrefs = context.getSharedPreferences("WebViewTabs", Context.MODE_PRIVATE);

        JSONObject root = manifest(context);

        JSONObject tabs = new JSONObject();
        tabs.put("saved_tabs", parseJson(tabsPrefs.getString("saved_tabs", null)));
        tabs.put("current_tab_id", tabsPrefs.getAll().get("current_tab_id"));
        tabs.put("history", tabHistory);
        root.put("tabs", tabs);

        JSONObject history = new JSONObject();
        history.put("list", parseJson(tabsPrefs.getString("history", null)));
        history.put("database", dumpDatabase(context.getDatabasePath("history.db")));
        root.put("history", history);

        JSONObject bookmarks = new JSONObject();
        bookmarks.put("list", parseJson(tabsPrefs.getString("bookmarks", null)));
        bookmarks.put("database", dumpDatabase(context.getDatabasePath("saved_bm.db")));
        root.put("bookmarks", bookmarks);

        root.put("savedPages", dumpDatabase(context.getDatabasePath(SAVED_PAGES_DB)));

        JSONObject settings = new JSONObject();
        for (String name : PREFS) {
            if ("WebViewTabs".equals(name)) continue; // タブ・履歴・ブックマークは上に入れた
            JSONObject p = new JSONObject();
            for (Map.Entry<String, ?> e : context.getSharedPreferences(name, Context.MODE_PRIVATE).getAll().entrySet()) {
                if (EXCLUDED_KEYS.contains(e.getKey())) continue;
                Object v = e.getValue();
                p.put(e.getKey(), v instanceof Set ? new JSONArray((Set<?>) v) : v);
            }
            settings.put(name, p);
        }
        root.put("settings", settings);
        return root;
    }

    /** JSON の文字列なら中身を、そうでなければ文字列のまま返す。 */
    @Nullable
    private static Object parseJson(@Nullable String s) {
        if (s == null) return JSONObject.NULL;
        try {
            return new JSONTokener(s).nextValue();
        } catch (JSONException e) {
            return s;
        }
    }

    /** データベースの全テーブルを {テーブル名: [行...]} にする(バイナリの列は除く)。 */
    @NonNull
    private static JSONObject dumpDatabase(@NonNull File file) throws JSONException {
        JSONObject out = new JSONObject();
        if (!file.isFile()) return out;
        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.OPEN_READONLY);
            try (Cursor t = db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name != 'android_metadata'",
                    null)) {
                while (t.moveToNext()) {
                    String table = t.getString(0);
                    JSONArray rows = new JSONArray();
                    try (Cursor c = db.query("\"" + table.replace("\"", "\"\"") + "\"", null, null, null, null, null, null)) {
                        while (c.moveToNext()) {
                            JSONObject row = new JSONObject();
                            for (int i = 0; i < c.getColumnCount(); i++) {
                                switch (c.getType(i)) {
                                    case Cursor.FIELD_TYPE_INTEGER:
                                        row.put(c.getColumnName(i), c.getLong(i));
                                        break;
                                    case Cursor.FIELD_TYPE_FLOAT:
                                        row.put(c.getColumnName(i), c.getDouble(i));
                                        break;
                                    case Cursor.FIELD_TYPE_STRING:
                                        row.put(c.getColumnName(i), c.getString(i));
                                        break;
                                    default:
                                        break;
                                }
                            }
                            rows.put(row);
                        }
                    }
                    out.put(table, rows);
                }
            }
        } catch (RuntimeException e) {
            // 読めないデータベースは空として扱う(引っ越し全体は止めない)
        } finally {
            if (db != null) db.close();
        }
        return out;
    }

    // ------------------------------------------------------------------
    // タブの戻る/進む履歴(tab_history.json)
    // ------------------------------------------------------------------

    /**
     * 各タブの戻る/進む履歴(tab_state_&lt;id&gt;.dat)を {"&lt;タブの id&gt;": {"index": 今のページ,
     * "entries": [{"url", "title"}...]}} にする。tab_state は WebView の保存形式で AirSky(GeckoView)
     * では読めないので、ここで WebView に読み戻して URL の一覧として取り出す。UI スレッドで呼ぶ。
     */
    @NonNull
    static JSONObject collectTabHistory(@NonNull Context context) {
        JSONObject out = new JSONObject();
        JSONArray tabs;
        try {
            tabs = new JSONArray(context.getSharedPreferences("WebViewTabs", Context.MODE_PRIVATE)
                    .getString("saved_tabs", "[]"));
        } catch (JSONException e) {
            return out;
        }
        for (int i = 0; i < tabs.length(); i++) {
            JSONObject tab = tabs.optJSONObject(i);
            int id = tab != null ? tab.optInt("id", -1) : -1;
            if (id < 0) continue;
            Bundle state = loadBundle(new File(context.getFilesDir(), "tab_state_" + id + ".dat"));
            if (state == null) continue;
            WebView webView = null;
            try {
                webView = new WebView(context);
                // 履歴を読むだけなので、ページは読み込ませない
                webView.getSettings().setBlockNetworkLoads(true);
                webView.getSettings().setJavaScriptEnabled(false);
                WebBackForwardList list = webView.restoreState(state);
                if (list == null || list.getSize() == 0) continue;
                JSONArray entries = new JSONArray();
                int index = 0;
                for (int k = 0; k < list.getSize(); k++) {
                    WebHistoryItem item = list.getItemAtIndex(k);
                    String url = item != null ? item.getUrl() : null;
                    // 内蔵ページ(file:///android_asset/...)などは AirSky では開けないので入れない
                    if (!isWebUrl(url)) continue;
                    if (k <= list.getCurrentIndex()) index = entries.length();
                    JSONObject e = new JSONObject();
                    e.put("url", url);
                    e.put("title", item.getTitle() != null ? item.getTitle() : "");
                    entries.put(e);
                }
                if (entries.length() == 0) continue;
                JSONObject h = new JSONObject();
                h.put("index", index);
                h.put("entries", entries);
                out.put(String.valueOf(id), h);
            } catch (RuntimeException | JSONException e) {
                // 読めないタブは URL だけで引っ越す
            } finally {
                if (webView != null) {
                    webView.stopLoading();
                    webView.destroy();
                }
            }
        }
        return out;
    }

    /** MainActivity.saveBundleToFile() で書いた Bundle を読む。 */
    @Nullable
    private static Bundle loadBundle(@NonNull File file) {
        if (!file.isFile()) return null;
        Parcel parcel = Parcel.obtain();
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            int off = 0;
            int n;
            while (off < bytes.length && (n = in.read(bytes, off, bytes.length - off)) > 0) off += n;
            parcel.unmarshall(bytes, 0, off);
            parcel.setDataPosition(0);
            return Bundle.CREATOR.createFromParcel(parcel);
        } catch (IOException | RuntimeException e) {
            return null;
        } finally {
            parcel.recycle();
        }
    }

    private static boolean isWebUrl(@Nullable String url) {
        if (url == null) return false;
        String u = url.trim().toLowerCase(Locale.ROOT);
        return u.startsWith("http://") || u.startsWith("https://");
    }

    // ------------------------------------------------------------------
    // 保存したページのファイル(saved_pages/)
    // ------------------------------------------------------------------

    /**
     * saved_pages.db に載っているページのファイル(page_*.mht)とスクリーンショットを、
     * saved_pages/&lt;ファイル名&gt; に入れる。AirSky はデータベースのパスのファイル名で探す。
     */
    private static void putSavedPageFiles(@NonNull Context context, @NonNull ZipOutputStream zip)
            throws IOException {
        File dbFile = context.getDatabasePath(SAVED_PAGES_DB);
        if (!dbFile.isFile()) return;
        Set<String> names = new HashSet<>();
        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openDatabase(dbFile.getPath(), null, SQLiteDatabase.OPEN_READONLY);
            try (Cursor c = db.rawQuery("SELECT url, screenshot_path FROM pages", null)) {
                while (c.moveToNext()) {
                    for (int i = 0; i < 2; i++) {
                        String path = c.getString(i);
                        if (path == null || !path.startsWith("/")) continue;
                        File f = new File(path);
                        // 同じ名前は 1 つだけ(AirSky は名前で探す)
                        if (f.isFile() && names.add(f.getName())) putFile(zip, SAVED_PAGES_DIR + f.getName(), f);
                    }
                }
            }
        } catch (RuntimeException e) {
            // 読めないデータベースは飛ばす(引っ越し全体は止めない)
        } finally {
            if (db != null) db.close();
        }
    }

    // ------------------------------------------------------------------
    // ZIP
    // ------------------------------------------------------------------

    /** WAL に残っている書き込みをデータベース本体へ反映する(コピーした中身を揃えるため)。 */
    private static void checkpoint(@NonNull File file) {
        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.OPEN_READWRITE);
            try (Cursor c = db.rawQuery("PRAGMA wal_checkpoint(FULL)", null)) {
                c.moveToFirst();
            }
        } catch (RuntimeException ignored) {
        } finally {
            if (db != null) db.close();
        }
    }

    private static void putText(@NonNull ZipOutputStream zip, @NonNull String name, @NonNull String text)
            throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(text.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static void putFile(@NonNull ZipOutputStream zip, @NonNull String name, @NonNull File file)
            throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) zip.write(buf, 0, n);
        }
        zip.closeEntry();
    }

    /** SharedPreferences の XML と同じ形にする(メモリ上の最新の値から作る)。 */
    @NonNull
    private static String prefsXml(@NonNull Map<String, ?> all) {
        StringBuilder sb = new StringBuilder("<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n");
        for (Map.Entry<String, ?> e : all.entrySet()) {
            String key = e.getKey();
            Object v = e.getValue();
            if (key == null || v == null || EXCLUDED_KEYS.contains(key)) continue;
            String name = escape(key);
            if (v instanceof String) {
                sb.append("    <string name=\"").append(name).append("\">")
                        .append(escape((String) v)).append("</string>\n");
            } else if (v instanceof Boolean) {
                sb.append("    <boolean name=\"").append(name).append("\" value=\"").append(v).append("\" />\n");
            } else if (v instanceof Integer) {
                sb.append("    <int name=\"").append(name).append("\" value=\"").append(v).append("\" />\n");
            } else if (v instanceof Long) {
                sb.append("    <long name=\"").append(name).append("\" value=\"").append(v).append("\" />\n");
            } else if (v instanceof Float) {
                sb.append("    <float name=\"").append(name).append("\" value=\"").append(v).append("\" />\n");
            } else if (v instanceof Set) {
                sb.append("    <set name=\"").append(name).append("\">\n");
                for (Object o : (Set<?>) v) {
                    if (o != null) sb.append("        <string>").append(escape(o.toString())).append("</string>\n");
                }
                sb.append("    </set>\n");
            }
        }
        return sb.append("</map>\n").toString();
    }

    @NonNull
    private static String escape(@NonNull String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&apos;"); break;
                default:
                    // XML 1.0 で使えない制御文字は捨てる
                    if (c < 0x20 && c != '\t' && c != '\n' && c != '\r') break;
                    sb.append(c);
            }
        }
        return sb.toString();
    }
}
