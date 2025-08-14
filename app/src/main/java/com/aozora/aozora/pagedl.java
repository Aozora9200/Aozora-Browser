package com.aozora.aozora;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.ValueCallback;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import android.os.Handler;
import android.util.Log;
import android.util.Base64;
import android.webkit.CookieManager;
import android.view.View;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class pagedl extends Activity {
    private static final String TAG = "pagedl";
    private EditText urlInput;
    private Switch jsSwitch;
    private Switch localPathSwitch;
    private Button saveButton;
    private WebView webView;
    private volatile boolean isSaving = false;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pgdl);

        urlInput = (EditText) findViewById(R.id.urlInput);
        jsSwitch = (Switch) findViewById(R.id.jsSwitch);
        localPathSwitch = (Switch) findViewById(R.id.localPathSwitch);
        saveButton = (Button) findViewById(R.id.saveButton);
        webView = (WebView) findViewById(R.id.webView);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // Action Bar が表示されているか確認
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(jsSwitch.isChecked());
        jsSwitch.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                webSettings.setJavaScriptEnabled(isChecked);
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String urlString = urlInput.getText().toString().trim();
                if (urlString.isEmpty()) {
                    Toast.makeText(pagedl.this, "URLを入力してください", Toast.LENGTH_SHORT).show();
                    return;
                }
                final String siteName = urlString.replaceAll("[^a-zA-Z0-9]", "_");
                isSaving = true;
                if (urlString.startsWith("blob:")) {
                    Toast.makeText(pagedl.this, "blob: URLはサポートされていません", Toast.LENGTH_LONG).show();
                    isSaving = false;
                    return;
                }
                if (urlString.startsWith("data:")) {
                    saveDataUrl(urlString, siteName);
                    return;
                }
                if (jsSwitch.isChecked()) {
                    webView.setWebViewClient(new WebViewClient() {
                        @Override
                        public void onPageFinished(WebView view, String url) {
                            String pageTitle = view.getTitle();
                            if (pageTitle == null || pageTitle.isEmpty()) {
                                pageTitle = "untitled";
                            }
                            String safePageTitle = pageTitle.replaceAll("[^a-zA-Z0-9]", "_");
                            File outputDir = getOutputDirectory(siteName, safePageTitle);
                            if (outputDir == null) {
                                isSaving = false;
                                return;
                            }
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    String archivePath = new File(outputDir, "page_archive.mht").getAbsolutePath();
                                    webView.saveWebArchive(archivePath, false, new ValueCallback<String>() {
                                        @Override
                                        public void onReceiveValue(String value) {
                                            File archiveFile = (value != null) ? new File(value) : null;
                                            if (value == null || archiveFile == null || !archiveFile.exists()) {
                                                Log.e(TAG, "saveWebArchive 失敗。返り値: " + value);
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Toast.makeText(pagedl.this, "Web Archive 保存失敗", Toast.LENGTH_LONG).show();
                                                    }
                                                });
                                            } else {
                                                if (localPathSwitch.isChecked()) {
                                                    try {
                                                        String originalContent = Utils.readFileToString(archiveFile);
                                                        String rewrittenContent = MimeParser.rewriteContentLocations(originalContent, outputDir);
                                                        File rewrittenFile = new File(outputDir, "page_archive_rewritten.mht");
                                                        Utils.writeStringToFile(rewrittenFile, rewrittenContent);
                                                        runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                Toast.makeText(pagedl.this, "Web Archive 保存＆書換完了：\n" + rewrittenFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                                                                clearCacheAndCookies();
                                                            }
                                                        });
                                                    } catch (Exception e) {
                                                        Log.e(TAG, "MIME書換エラー", e);
                                                        runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                Toast.makeText(pagedl.this, "MIME書換エラー：" + e.getMessage(), Toast.LENGTH_LONG).show();
                                                            }
                                                        });
                                                    }
                                                } else {
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            Toast.makeText(pagedl.this, "Web Archive 保存完了：\n" + archiveFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                                                            clearCacheAndCookies();
                                                        }
                                                    });
                                                }
                                            }
                                            isSaving = false;
                                        }
                                    });
                                }
                            }, 3000);
                        }
                    });
                    webView.loadUrl(urlString);
                } else {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            HttpURLConnection conn = null;
                            try {
                                URL url = new URL(urlString);
                                conn = (HttpURLConnection) url.openConnection();
                                conn.setInstanceFollowRedirects(true);
                                conn.setRequestMethod("GET");
                                conn.setConnectTimeout(15000);
                                conn.setReadTimeout(15000);
                                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile; rv:89.0) Gecko/89.0 Firefox/89.0");
                                conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                                conn.setRequestProperty("Accept-Language", "ja-JP,ja;q=0.9,en-US;q=0.8,en;q=0.7");
                                int responseCode = conn.getResponseCode();
                                if (responseCode != HttpURLConnection.HTTP_OK) {
                                    Log.e(TAG, "HTTP error code: " + responseCode);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(pagedl.this, "HTTP error code: " + responseCode, Toast.LENGTH_LONG).show();
                                        }
                                    });
                                    return;
                                }
                                StringBuilder html = new StringBuilder();
                                InputStream is = conn.getInputStream();
                                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    html.append(line).append("\n");
                                }
                                reader.close();
                                String htmlContent = html.toString();
                                String pageTitle = extractTitle(htmlContent);
                                String safePageTitle = pageTitle.replaceAll("[^a-zA-Z0-9]", "_");
                                File outputDir = getOutputDirectory(siteName, safePageTitle);
                                if (outputDir == null) {
                                    isSaving = false;
                                    return;
                                }
                                if (localPathSwitch.isChecked()) {
                                    htmlContent = rewriteHtmlLocalPaths(htmlContent, urlString, outputDir);
                                }
                                File htmlFile = new File(outputDir, "page.html");
                                Utils.writeStringToFile(htmlFile, htmlContent);
                                if (htmlFile.exists()) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(pagedl.this, "HTML 保存完了：\n" + htmlFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                                            clearCacheAndCookies();
                                        }
                                    });
                                } else {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(pagedl.this, "HTML ファイルが存在しません", Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "HTML保存エラー", e);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(pagedl.this, "エラー：" + e.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                            } finally {
                                if (conn != null) {
                                    conn.disconnect();
                                }
                                isSaving = false;
                            }
                        }
                    }).start();
                }
            }
        });
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // 前の画面に戻る
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void clearCacheAndCookies() {
        // キャッシュをクリア
        webView.clearCache(true);
        // Cookieをクリア
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();
    }

    @Override
    public void onBackPressed() {
        if (isSaving) {
            Toast.makeText(this, "保存中はバックキーが無効です", Toast.LENGTH_SHORT).show();
        } else {
            super.onBackPressed();
        }
    }

    private String rewriteHtmlLocalPaths(String html, String baseUrl, File dir) {
        Pattern pattern = Pattern.compile("(?i)(src|href)\\s*=\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(html);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String attr = matcher.group(1);
            String originalUrl = matcher.group(2);
            try {
                URL resourceUrl = originalUrl.startsWith("http://") || originalUrl.startsWith("https://")
                        ? new URL(originalUrl)
                        : new URL(new URL(baseUrl), originalUrl);
                String fileName = new File(resourceUrl.getPath()).getName();
                if (fileName.isEmpty()) {
                    fileName = "index.html";
                }
                File resourceFile = new File(dir, fileName);
                downloadResource(resourceUrl.toString(), resourceFile);
                String localPath = "file://" + resourceFile.getAbsolutePath();
                matcher.appendReplacement(sb, attr + "=\"" + localPath + "\"");
            } catch (Exception e) {
                matcher.appendReplacement(sb, matcher.group(0));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private void downloadResource(String resourceUrl, File destination) throws IOException {
        if (destination.exists()) {
            return;
        }
        HttpURLConnection conn = null;
        try {
            URL url = new URL(resourceUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            InputStream in = conn.getInputStream();
            FileOutputStream out = new FileOutputStream(destination);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            in.close();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String extractTitle(String html) {
        Pattern pattern = Pattern.compile("<title>(.*?)</title>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "untitled";
    }

    private String getExtensionForMimeType(String mimeType) {
        if (mimeType.contains("html")) {
            return ".html";
        } else if (mimeType.contains("jpeg") || mimeType.contains("jpg")) {
            return ".jpg";
        } else if (mimeType.contains("png")) {
            return ".png";
        } else if (mimeType.contains("gif")) {
            return ".gif";
        } else if (mimeType.contains("pdf")) {
            return ".pdf";
        }
        return "";
    }

    private void saveDataUrl(String dataUrl, String siteName) {
        String datetime = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File baseDir = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "page");
        File outputDir = new File(baseDir, datetime);
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            isSaving = false;
            return;
        }
        saveDataUrlContent(dataUrl, outputDir, datetime);
    }

    private void saveDataUrlContent(String dataUrl, File outputDir, String baseName) {
        try {
            int commaIndex = dataUrl.indexOf(",");
            if (commaIndex == -1) {
                throw new IOException("data URL の形式が不正です");
            }
            String header = dataUrl.substring(0, commaIndex);
            String base64Data = dataUrl.substring(commaIndex + 1);
            String mimeType = "application/octet-stream";
            Pattern pattern = Pattern.compile("data:([^;]+);base64");
            Matcher matcher = pattern.matcher(header);
            if (matcher.find()) {
                mimeType = matcher.group(1);
            }
            String extension = getExtensionForMimeType(mimeType);
            String fileName = baseName + extension;
            File outFile = new File(outputDir, fileName);
            byte[] data = Base64.decode(base64Data, Base64.DEFAULT);
            FileOutputStream fos = new FileOutputStream(outFile);
            fos.write(data);
            fos.flush();
            fos.close();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(pagedl.this, "保存完了：\n" + outFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                    clearCacheAndCookies();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Data URL 保存エラー", e);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(pagedl.this, "Data URL 保存エラー：" + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } finally {
            isSaving = false;
        }
    }

    private File getOutputDirectory(String siteName, String safePageTitle) {
        File baseDir = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "page");
        File outputDir = new File(baseDir, siteName + "(" + safePageTitle + ")/データ保存");
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            Log.e(TAG, "フォルダ作成失敗: " + outputDir.getAbsolutePath());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(pagedl.this, "フォルダ作成失敗: " + outputDir.getAbsolutePath(), Toast.LENGTH_LONG).show();
                }
            });
            return null;
        }
        return outputDir;
    }
}

class Utils {
    public static String readFileToString(File file) throws IOException {
        byte[] data = new byte[(int) file.length()];
        FileInputStream fis = new FileInputStream(file);
        int readBytes = fis.read(data);
        if (readBytes != data.length) {
            fis.close();
            throw new IOException("ファイル全体の読み込みに失敗しました");
        }
        fis.close();
        return new String(data, StandardCharsets.UTF_8);
    }
    public static void writeStringToFile(File file, String content) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(content.getBytes(StandardCharsets.UTF_8));
        fos.close();
    }
}

class MimeParser {
    public static String rewriteContentLocations(String mhtContent, File dir) {
        Pattern pattern = Pattern.compile("(?im)^(Content-Location:\\s*)(http[s]?://\\S+)$");
        Matcher matcher = pattern.matcher(mhtContent);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String originalUrl = matcher.group(2).trim();
            try {
                URL url = new URL(originalUrl);
                String fileName = new File(url.getPath()).getName();
                if (fileName.isEmpty()) {
                    fileName = "index.html";
                }
                String localPath = "file://" + dir.getAbsolutePath() + "/" + fileName;
                String replacement = matcher.group(1) + localPath;
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            } catch (Exception e) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
