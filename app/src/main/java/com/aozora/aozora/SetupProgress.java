package com.aozora.aozora;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SetupProgress extends Activity {
    private TextView speedText, progressText;
    private ProgressBar progressBar;
    private int progressStatus = 0;
    private Handler handler = new Handler(); // UIスレッド用ハンドラー

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setup_progress);
        speedText = findViewById(R.id.setupdownloadlog);
        progressText = findViewById(R.id.progressText);
        progressBar = findViewById(R.id.setup_progress);
        StartDownload();
    }

    private void StartDownload() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("セットアップ")
                .setMessage("回線速度の検測用に 100MB のPNGファイルをダウンロードします。モバイルデータや従量制課金接続の場合、通信料が発生する場合があります。ダウンロードは20秒後、自動的に停止します。")
                .setPositiveButton("開始", (d, which) -> {
                    // ダウンロード速度を計測
                    new SpeedTestTask().execute("https://sample-img.lb-product.com/wp-content/themes/hitchcock/images/100MB.png");
                    speedText.setText("Downloading Files...(100MB)");
                    progressText.setText("使用に必要な設定やファイルを確認しています...");
                    // 1秒ごとに5%増やす
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (progressStatus < 100) {
                                progressStatus += 5;
                                onProgressChanged(progressStatus);
                                handler.postDelayed(this, 1000); // 1000ms = 1秒
                            }
                        }
                    }, 1000); // 最初も1秒後に開始
                })
                .setNegativeButton("スキップ", (d, which) -> {
                    // セットアップ完了 → フラグ更新
                    SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("isFirstRun", false);
                    editor.apply();
                    startActivity(new Intent(this, BootingActivity.class));
                    finish();
                })
                .create(); // ← show() ではなく create()

        // 外をタップしても閉じない
        dialog.setCanceledOnTouchOutside(false);
        // 必要なら戻るキーでも閉じないように
        dialog.setCancelable(false);
        dialog.show();
    }

    private class SpeedTestTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            String result = "";
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setUseCaches(false);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.connect();

                InputStream input = conn.getInputStream();

                byte[] buffer = new byte[1024];
                long totalBytes = 0;
                int read;
                long startTime = System.currentTimeMillis();

                while ((read = input.read(buffer)) != -1) {
                    totalBytes += read;
                    // 時間制限（10秒くらいで計測を止める）
                    if (System.currentTimeMillis() - startTime > 20000) {
                        break;
                    }
                }

                long endTime = System.currentTimeMillis();
                long timeTakenMillis = endTime - startTime;

                double speedBps = (totalBytes * 8) / (timeTakenMillis / 1000.0); // bit per second
                double speedMbps = speedBps / (1024 * 1024);

                result = String.format(" %.2f Mbps", speedMbps);

                input.close();
                conn.disconnect();

            } catch (Exception e) {
                result = "Error: " + e.getMessage();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            speedText.setText(s);
            // 終了後に ResultActivity を起動
            Intent intent = new Intent(SetupProgress.this, SetupResult.class);
            intent.putExtra("speed_result", s); // 結果を渡す
            startActivity(intent);
            finish(); // MainActivity を閉じる場合
        }
    }
    private void onProgressChanged(int percentage){
        Animator animation = ObjectAnimator.ofInt(progressBar,"progress",percentage);
        animation.setDuration(500); // 0.5秒間でアニメーションする
        animation.setInterpolator(new DecelerateInterpolator());
        animation.start();
    }

}
