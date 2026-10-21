package com.aozora.aozora;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 「AirSky Browser がリリースされました！」のお知らせと、AirSky のインストール・データの引っ越し。
 *
 * <p>お知らせ → 引っ越しの確認 → (書き出し) → (ダウンロード・インストール) → 起動 の順に、
 * 下部のシートの中身を切り替えて進める。AirSky がすでにインストールされている時は
 * ダウンロードせず、引っ越しだけを行う。</p>
 *
 * <p>設定から開いた時({@link #EXTRA_FROM_SETTINGS})は、お知らせを飛ばして引っ越しの確認から始める。</p>
 */
public class AirSkyPromoActivity extends Activity {

    static final String EXTRA_FROM_SETTINGS = "from_settings";
    /**
     * AppPrefs: お知らせを表示済み。
     * 再リリース(3.2.3)で名前を変え、以前のお知らせを見たユーザーにももう一度表示する。
     */
    static final String KEY_PROMO_SHOWN = "airskyRereleasePromoShown";

    private static final String RELEASE_API =
            "https://api.github.com/repos/Aozora9200/Airsky-Glass-Browser/releases/latest";
    private static final String RELEASE_PAGE = "https://github.com/Aozora9200/Airsky-Glass-Browser/";

    private static final int REQ_STORAGE = 0xA51;
    private static final int REQ_ALL_FILES = 0xA52;
    private static final int REQ_INSTALL_SOURCE = 0xA53;

    private TextView chip;
    private TextView title;
    private TextView body;
    private TextView warning;
    private ProgressBar progress;
    private TextView progressText;
    private Button primary;
    private Button secondary;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private boolean installed;
    /** .az を書き出した(AirSky に Aozora_to_AirSky を付けて起動する)。 */
    private boolean exported;
    /** インストール画面を開いた(戻ってきたらインストールできたか確かめる)。 */
    private boolean waitingInstall;
    /** 「この提供元のアプリを許可」の設定から戻ってきたら続ける APK。 */
    @Nullable private File pendingApk;
    /** すべてのファイルへのアクセスの設定から戻ってきたら書き出しをやり直す。 */
    private boolean waitingAllFiles;

    /** お知らせを出すべきなら出す(MainActivity から呼ぶ。1 回だけ)。 */
    static void showIfNeeded(@NonNull Activity from) {
        SharedPreferences p = from.getSharedPreferences("AppPrefs", MODE_PRIVATE);
        if (p.getBoolean(KEY_PROMO_SHOWN, false)) return;
        p.edit().putBoolean(KEY_PROMO_SHOWN, true).apply();
        from.startActivity(new Intent(from, AirSkyPromoActivity.class));
        from.overridePendingTransition(R.anim.fade, R.anim.no_animation);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_airsky_promo);

        chip = findViewById(R.id.airskyChip);
        title = findViewById(R.id.airskyTitle);
        body = findViewById(R.id.airskyBody);
        warning = findViewById(R.id.airskyWarning);
        progress = findViewById(R.id.airskyProgress);
        progressText = findViewById(R.id.airskyProgressText);
        primary = findViewById(R.id.airskyPrimary);
        secondary = findViewById(R.id.airskySecondary);

        // 下部のシートをナビゲーションバーに重ねない
        View sheet = findViewById(R.id.airskySheet);
        final int basePadding = sheet.getPaddingBottom();
        sheet.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(),
                    basePadding + insets.getSystemWindowInsetBottom());
            return insets;
        });

        loadBackground(findViewById(R.id.airskyBackground));

        getSharedPreferences("AppPrefs", MODE_PRIVATE).edit().putBoolean(KEY_PROMO_SHOWN, true).apply();
        installed = AirSkyTransfer.isAirSkyInstalled(this);
        if (getIntent().getBooleanExtra(EXTRA_FROM_SETTINGS, false)) {
            showConfirm();
        } else {
            showPromo();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (waitingInstall && AirSkyTransfer.isAirSkyInstalled(this)) {
            waitingInstall = false;
            installed = true;
            showDone();
        }
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    // ------------------------------------------------------------------
    // 画面(シートの中身)
    // ------------------------------------------------------------------

    private void showPromo() {
        setSheet("再リリース",
                "AirSky Browserを再リリースしました！",
                "Aozoraをベースに全面的にリニューアル。\n今までのAozoraよりもっと使いやすく、スマートに自由に。");
        warning.setText("以前のバージョンで見つかった重大なバグを修正し、再リリースしました。"
                + "AirSky Browserへの移行をおすすめします。");
        warning.setVisibility(View.VISIBLE);
        setButtons(installed ? "引っ越し" : "インストール", v -> showConfirm(),
                "今はしない", v -> finish());
    }

    private void showConfirm() {
        setSheet("データの引っ越し",
                "データを引っ越しますか？",
                "(今後設定からでも行えます)\n\nタブ履歴、ページ履歴、ブックマーク、保存ページ、設定内容を"
                        + "ダウンロード/Aozora/ にまとめて、AirSky Browser に引き継ぎます。");
        if (installed) {
            warning.setText("AirSky Browser ですでにセットアップをしている場合、AirSky Browser の既存のデータは"
                    + "消去され、Aozora のデータで上書きされます。");
            warning.setVisibility(View.VISIBLE);
        }
        setButtons("行う", v -> startExport(),
                "行わない", v -> {
                    if (installed) {
                        finish();
                    } else {
                        startDownload();
                    }
                });
    }

    private void showWorking(@NonNull String heading, @NonNull String message, boolean indeterminate) {
        setSheet(installed ? "引っ越し" : "インストール", heading, message);
        progress.setVisibility(View.VISIBLE);
        progress.setIndeterminate(indeterminate);
        progress.setProgress(0);
        progressText.setVisibility(View.GONE);
        primary.setVisibility(View.GONE);
        secondary.setVisibility(View.GONE);
    }

    private void showDone() {
        setSheet("準備完了",
                "AirSky Browser の準備ができました",
                exported
                        ? "起動すると、Aozora のデータを引っ越すかどうかの確認が表示されます。"
                        : "さっそく AirSky Browser を使ってみましょう。");
        setButtons("AirSky Browserを起動", v -> launchAirSky(), "閉じる", v -> finish());
    }

    private void showError(@NonNull String message, @NonNull String retryLabel, @NonNull View.OnClickListener retry) {
        setSheet("エラー", "うまくいきませんでした", message);
        setButtons(retryLabel, retry, "閉じる", v -> finish());
    }

    private void setSheet(@NonNull String chipText, @NonNull String heading, @NonNull String message) {
        chip.setText(chipText);
        title.setText(heading);
        body.setText(message);
        warning.setVisibility(View.GONE);
        progress.setVisibility(View.GONE);
        progressText.setVisibility(View.GONE);
    }

    private void setButtons(@NonNull String primaryText, @NonNull View.OnClickListener onPrimary,
                            @Nullable String secondaryText, @Nullable View.OnClickListener onSecondary) {
        primary.setText(primaryText);
        primary.setOnClickListener(onPrimary);
        primary.setVisibility(View.VISIBLE);
        if (secondaryText != null) {
            secondary.setText(secondaryText);
            secondary.setOnClickListener(onSecondary);
            secondary.setVisibility(View.VISIBLE);
        } else {
            secondary.setVisibility(View.INVISIBLE);
        }
    }

    /** 背景(assets/img/bg-masthead.jpg)を画面の大きさに合わせて縮小して読む。 */
    private void loadBackground(@NonNull ImageView view) {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        final int target = Math.max(dm.widthPixels, dm.heightPixels);
        executor.execute(() -> {
            Bitmap bmp = null;
            try {
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inJustDecodeBounds = true;
                try (InputStream in = getAssets().open("assets/img/bg-masthead.jpg")) {
                    BitmapFactory.decodeStream(in, null, o);
                }
                int sample = 1;
                while (Math.min(o.outWidth, o.outHeight) / (sample * 2) >= target / 2) sample *= 2;
                BitmapFactory.Options o2 = new BitmapFactory.Options();
                o2.inSampleSize = sample;
                try (InputStream in = getAssets().open("assets/img/bg-masthead.jpg")) {
                    bmp = BitmapFactory.decodeStream(in, null, o2);
                }
            } catch (IOException | OutOfMemoryError ignored) {
            }
            final Bitmap result = bmp;
            if (result != null) main.post(() -> view.setImageBitmap(result));
        });
    }

    // ------------------------------------------------------------------
    // 書き出し(.Aozora-西暦.az)
    // ------------------------------------------------------------------

    private void startExport() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_STORAGE);
            return;
        }
        showWorking("データをまとめています", "しばらくお待ちください…", true);
        // タブの戻る/進む履歴は WebView で読むので UI スレッドで集める(表示を更新してから)
        main.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            exportInBackground(AirSkyTransfer.collectTabHistory(this));
        }, 100);
    }

    private void exportInBackground(@NonNull JSONObject tabHistory) {
        executor.execute(() -> {
            Exception error = null;
            try {
                AirSkyTransfer.export(getApplicationContext(), tabHistory);
            } catch (Exception e) {
                error = e;
            }
            final Exception failure = error;
            main.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (failure == null) {
                    exported = true;
                    if (installed) {
                        showDone();
                    } else {
                        startDownload();
                    }
                } else if (failure instanceof IOException
                        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                    // 以前の .az が別のアプリのものとして残っているなど、ダウンロードに書けない
                    askAllFilesAccess();
                } else {
                    showError("データを書き出せませんでした。\n" + failure.getMessage(), "もう一度", v -> startExport());
                }
            });
        });
    }

    private void askAllFilesAccess() {
        showError("ダウンロード/Aozora/ にデータを書き出すには、「すべてのファイルへのアクセス」を許可してください。",
                "許可する", v -> {
                    waitingAllFiles = true;
                    Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    try {
                        startActivityForResult(i, REQ_ALL_FILES);
                    } catch (ActivityNotFoundException e) {
                        startActivityForResult(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION), REQ_ALL_FILES);
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_STORAGE) return;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startExport();
        } else {
            showError("ストレージへのアクセスが許可されなかったため、データを書き出せませんでした。",
                    "もう一度", v -> startExport());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_ALL_FILES && waitingAllFiles) {
            waitingAllFiles = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                startExport();
            }
        } else if (requestCode == REQ_INSTALL_SOURCE && pendingApk != null) {
            File apk = pendingApk;
            pendingApk = null;
            install(apk);
        }
    }

    // ------------------------------------------------------------------
    // ダウンロード・インストール
    // ------------------------------------------------------------------

    private void startDownload() {
        showWorking("AirSky Browser をダウンロードしています", "", true);
        progressText.setVisibility(View.VISIBLE);
        progressText.setText("最新のリリースを確認しています…");
        final Context app = getApplicationContext();
        executor.execute(() -> {
            try {
                String apkUrl = findApkUrl();
                if (apkUrl == null) {
                    main.post(() -> {
                        if (isFinishing() || isDestroyed()) return;
                        showError("ダウンロードできる AirSky Browser がまだ公開されていません。"
                                        + "GitHub のページから入手してください。",
                                "GitHubを開く", v -> openReleasePage());
                    });
                    return;
                }
                File dir = new File(app.getExternalFilesDir(null), "airsky");
                if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("保存先を作成できません");
                File apk = new File(dir, "AirSky.apk");
                download(apkUrl, apk);
                if (!isAirSkyApk(apk)) {
                    //noinspection ResultOfMethodCallIgnored
                    apk.delete();
                    throw new IOException("ダウンロードしたファイルが AirSky Browser ではありません");
                }
                main.post(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    install(apk);
                });
            } catch (Exception e) {
                main.post(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    showError("ダウンロードに失敗しました。ネットワーク接続をご確認ください。\n" + e.getMessage(),
                            "もう一度", v -> startDownload());
                });
            }
        });
    }

    /** GitHub の最新リリースから .apk の URL を探す。無ければ null。 */
    @Nullable
    private static String findApkUrl() throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(RELEASE_API).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(15000);
        c.setRequestProperty("Accept", "application/vnd.github+json");
        c.setRequestProperty("User-Agent", "Aozora-Browser");
        try {
            int code = c.getResponseCode();
            if (code == HttpURLConnection.HTTP_NOT_FOUND) return null;
            if (code != HttpURLConnection.HTTP_OK) throw new IOException("HTTP " + code);
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            try (InputStream in = c.getInputStream()) {
                byte[] b = new byte[8192];
                int n;
                while ((n = in.read(b)) > 0) buf.write(b, 0, n);
            }
            JSONArray assets = new JSONObject(new String(buf.toByteArray(), StandardCharsets.UTF_8))
                    .optJSONArray("assets");
            if (assets == null) return null;
            for (int i = 0; i < assets.length(); i++) {
                JSONObject a = assets.optJSONObject(i);
                if (a == null) continue;
                String name = a.optString("name", "");
                String url = a.optString("browser_download_url", "");
                if (name.toLowerCase().endsWith(".apk") && url.startsWith("https://")) return url;
            }
            return null;
        } catch (org.json.JSONException e) {
            throw new IOException("リリース情報を読み込めませんでした", e);
        } finally {
            c.disconnect();
        }
    }

    private void download(@NonNull String url, @NonNull File out) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent", "Aozora-Browser");
        try {
            int code = c.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) throw new IOException("HTTP " + code);
            final long total = c.getContentLengthLong();
            try (InputStream in = c.getInputStream(); OutputStream os = new FileOutputStream(out)) {
                byte[] b = new byte[16384];
                int n;
                long done = 0;
                long lastUi = 0;
                while ((n = in.read(b)) > 0) {
                    if (Thread.currentThread().isInterrupted()) throw new IOException("中断しました");
                    os.write(b, 0, n);
                    done += n;
                    long now = System.currentTimeMillis();
                    if (now - lastUi > 150) {
                        lastUi = now;
                        final long d = done;
                        main.post(() -> updateProgress(d, total));
                    }
                }
                final long d = done;
                main.post(() -> updateProgress(d, total));
            }
        } finally {
            c.disconnect();
        }
    }

    private void updateProgress(long done, long total) {
        if (isFinishing() || isDestroyed()) return;
        if (total > 0) {
            progress.setIndeterminate(false);
            progress.setProgress((int) (done * 100 / total));
            progressText.setText(String.format(java.util.Locale.US, "%.1f MB / %.1f MB",
                    done / 1048576f, total / 1048576f));
        } else {
            progressText.setText(String.format(java.util.Locale.US, "%.1f MB", done / 1048576f));
        }
    }

    /** ダウンロードしたファイルが本当に AirSky Browser の APK か確かめる。 */
    private boolean isAirSkyApk(@NonNull File apk) {
        PackageInfo info = getPackageManager().getPackageArchiveInfo(apk.getPath(), 0);
        return info != null && AirSkyTransfer.AIRSKY_PACKAGE.equals(info.packageName);
    }

    private void install(@NonNull File apk) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !getPackageManager().canRequestPackageInstalls()) {
            showError("インストールを続けるには、次の画面で「この提供元のアプリを許可」をオンにしてください。",
                    "設定を開く", v -> {
                        pendingApk = apk;
                        startActivityForResult(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                Uri.parse("package:" + getPackageName())), REQ_INSTALL_SOURCE);
                    });
            return;
        }
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", apk);
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(uri, "application/vnd.android.package-archive");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        waitingInstall = true;
        setSheet("インストール", "AirSky Browser をインストールしています",
                "画面の案内に沿ってインストールしてください。インストールが終わると、ここに起動ボタンが表示されます。");
        setButtons("インストール画面を開く", v -> install(apk), "閉じる", v -> finish());
        try {
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            waitingInstall = false;
            showError("インストール画面を開けませんでした。", "GitHubを開く", v -> openReleasePage());
        }
    }

    private void openReleasePage() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(RELEASE_PAGE)));
        } catch (ActivityNotFoundException ignored) {
        }
    }

    // ------------------------------------------------------------------
    // 起動
    // ------------------------------------------------------------------

    private void launchAirSky() {
        Intent i = AirSkyTransfer.launchIntent(this, exported);
        if (i == null) {
            installed = false;
            new AlertDialog.Builder(this)
                    .setMessage("AirSky Browser が見つかりませんでした。")
                    .setPositiveButton("OK", null)
                    .show();
            showPromo();
            return;
        }
        startActivity(i);
        finish();
    }
}
