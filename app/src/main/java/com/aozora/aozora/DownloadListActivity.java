package com.aozora.aozora;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DownloadListActivity extends Activity {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvEmpty;
    private DownloadAdapter adapter;
    private List<DownloadItem> downloadItems;
    private SharedPreferences pref;
    private static final String PREF_NAME = "AdvancedBrowserPrefs";
    private static final String KEY_DOWNLOAD_HISTORY = "download_history";

    private DownloadManager downloadManager;
    private Handler updateHandler = new Handler(Looper.getMainLooper());
    private ExecutorService executor = Executors.newSingleThreadExecutor();

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

    private Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            boolean needUpdate = false;
            if (downloadItems != null && adapter != null) {
                for (int i = 0, size = downloadItems.size(); i < size; i++) {
                    DownloadItem currentItem = downloadItems.get(i);
                    DownloadItem updated = getDownloadItem(currentItem.downloadId);
                    if (updated != null) {
                        if (currentItem.status != updated.status ||
                                currentItem.downloadedSize != updated.downloadedSize ||
                                currentItem.totalSize != updated.totalSize) {
                            currentItem.status = updated.status;
                            currentItem.downloadedSize = updated.downloadedSize;
                            currentItem.totalSize = updated.totalSize;
                            needUpdate = true;
                        }
                        if (currentItem.title == null || currentItem.title.isEmpty()) {
                            currentItem.title = updated.title;
                        }
                        currentItem.localUri = updated.localUri;
                    }
                }
                if (needUpdate) {
                    adapter.notifyDataSetChanged();
                }
            }
            updateHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applySavedTheme();
        setContentView(R.layout.activity_download_history);
        Background = findViewById(R.id.background);
        applySavedBackground();
        applyBackTheme();

        recyclerView = findViewById(R.id.recyclerView);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        tvEmpty = findViewById(R.id.tvEmpty);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        pref = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        if (getIntent().getBooleanExtra("clear_history", false)) {
            clearDownloadHistory();
        }
        loadDownloadHistory();

        swipeRefresh.setOnRefreshListener(() -> {
            loadDownloadHistory();
            swipeRefresh.setRefreshing(false);
        });

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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

    @Override
    protected void onResume() {
        super.onResume();
        updateHandler.post(updateRunnable);
    }

    @Override
    protected void onPause() {
        updateHandler.removeCallbacks(updateRunnable);
        super.onPause();
    }

    private void loadDownloadHistory() {
        executor.execute(() -> {
            List<DownloadItem> items = new ArrayList<>();
            String jsonStr = pref.getString(KEY_DOWNLOAD_HISTORY, "[]");
            try {
                JSONArray array = new JSONArray(jsonStr);
                for (int i = 0, len = array.length(); i < len; i++) {
                    JSONObject obj = array.getJSONObject(i);
                    long downloadId = obj.getLong("id");
                    String storedFileName = obj.optString("fileName", "");
                    String filePath = obj.optString("filePath", "");
                    DownloadItem item = getDownloadItem(downloadId);
                    if (item == null) {
                        File file = new File(filePath);
                        int status = file.exists() ? DownloadManager.STATUS_SUCCESSFUL : DownloadManager.STATUS_FAILED;
                        String fileName = !storedFileName.isEmpty() ? storedFileName : file.getName();
                        item = new DownloadItem(downloadId, fileName, "", status, 0, file.exists() ? file.length() : 0,
                                "file://" + filePath, "");
                    } else {
                        if (item.title == null || item.title.isEmpty()) {
                            if (!storedFileName.isEmpty()) {
                                item.title = storedFileName;
                            } else if (!filePath.isEmpty()) {
                                File file = new File(filePath);
                                item.title = file.getName();
                            }
                        }
                        item.localUri = "file://" + filePath;
                    }
                    item.filePath = filePath;
                    items.add(item);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            downloadItems = items;
            runOnUiThread(() -> {
                if (downloadItems.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
                adapter = new DownloadAdapter(DownloadListActivity.this, downloadItems);
                recyclerView.setAdapter(adapter);
            });
        });
    }

    private void saveDownloadHistory() {
        JSONArray array = new JSONArray();
        for (DownloadItem item : downloadItems) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("id", item.downloadId);
                obj.put("fileName", item.title);
                obj.put("filePath", item.filePath);
                array.put(obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        pref.edit().putString(KEY_DOWNLOAD_HISTORY, array.toString()).apply();
    }

    private DownloadItem getDownloadItem(long downloadId) {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        try (Cursor cursor = downloadManager.query(query)) {
            if (cursor != null && cursor.moveToFirst()) {
                String title = getRobustFileName(cursor);
                String description = safeGetString(cursor, DownloadManager.COLUMN_DESCRIPTION);
                int status = safeGetInt(cursor, DownloadManager.COLUMN_STATUS);
                long totalSize = safeGetLong(cursor, DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                long downloadedSize = safeGetLong(cursor, DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                String localUri = safeGetString(cursor, DownloadManager.COLUMN_LOCAL_URI);
                String downloadUrl = safeGetString(cursor, DownloadManager.COLUMN_URI);
                return new DownloadItem(downloadId, title, description, status, downloadedSize, totalSize, localUri, downloadUrl);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getRobustFileName(Cursor cursor) {
        String title = safeGetString(cursor, DownloadManager.COLUMN_TITLE);
        if (title == null || title.isEmpty()) {
            String localUri = safeGetString(cursor, DownloadManager.COLUMN_LOCAL_URI);
            if (localUri != null && !localUri.isEmpty()) {
                try {
                    Uri uri = Uri.parse(localUri);
                    String path = uri.getPath();
                    if (path != null && !path.isEmpty()) {
                        title = new File(path).getName();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (title == null || title.isEmpty()) {
            String downloadUrl = safeGetString(cursor, DownloadManager.COLUMN_URI);
            title = URLUtil.guessFileName(downloadUrl, null, null);
        }
        return (title == null || title.isEmpty()) ? "Unknown" : title;
    }

    private String safeGetString(Cursor cursor, String columnName) {
        try {
            int index = cursor.getColumnIndexOrThrow(columnName);
            return cursor.getString(index);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private int safeGetInt(Cursor cursor, String columnName) {
        try {
            int index = cursor.getColumnIndexOrThrow(columnName);
            return cursor.getInt(index);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private long safeGetLong(Cursor cursor, String columnName) {
        try {
            int index = cursor.getColumnIndexOrThrow(columnName);
            return cursor.getLong(index);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void clearDownloadHistory() {
        pref.edit().remove(KEY_DOWNLOAD_HISTORY).apply();
        Toast.makeText(this, "ダウンロード履歴を全消去しました", Toast.LENGTH_SHORT).show();
    }

    private String getMimeType(File file) {
        String type = null;
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex != -1) {
            String extension = fileName.substring(dotIndex + 1).toLowerCase();
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        // ファイル名でフォールバック
        if (type == null) {
            if (fileName.endsWith(".mp4")) type = "video/mp4";
            else if (fileName.endsWith(".pdf")) type = "application/pdf";
            else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) type = "image/jpeg";
            else if (fileName.endsWith(".png")) type = "image/png";
            else type = "*/*";
        }
        return type != null ? type : "*/*";
    }

    private void openFile(File file) {
        try {
            String mimeType = getMimeType(file);

            Uri fileUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // FileProvider を経由して安全な URI を生成
                fileUri = FileProvider.getUriForFile(
                        this,
                        getApplicationContext().getPackageName() + ".fileprovider",
                        file
                );
            } else {
                fileUri = Uri.fromFile(file);
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, mimeType);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // URI権限をすべての候補アプリに付与
            List<ResolveInfo> resInfoList = getPackageManager()
                    .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                grantUriPermission(resolveInfo.activityInfo.packageName,
                        fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "対応アプリが見つかりません", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "ファイルを開けません: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public static class DownloadItem {
        public long downloadId;
        public String title;
        public String description;
        public int status;
        public long downloadedSize;
        public long totalSize;
        public String localUri;
        public String downloadUrl;
        public boolean isPaused;
        public String filePath;

        public DownloadItem(long downloadId, String title, String description, int status, long downloadedSize, long totalSize, String localUri, String downloadUrl) {
            this.downloadId = downloadId;
            this.title = title;
            this.description = description;
            this.status = status;
            this.downloadedSize = downloadedSize;
            this.totalSize = totalSize;
            this.localUri = localUri;
            this.downloadUrl = downloadUrl;
            this.isPaused = false;
            this.filePath = "";
        }

        public int getProgress() {
            return totalSize > 0 ? (int) ((downloadedSize * 100) / totalSize) : 0;
        }
    }

    public class DownloadAdapter extends RecyclerView.Adapter<DownloadAdapter.ViewHolder> {

        private List<DownloadItem> items;
        private Context context;

        public DownloadAdapter(Context context, List<DownloadItem> items) {
            this.context = context;
            this.items = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_download, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            DownloadItem item = items.get(position);
            File file = new File(item.filePath);
            if (item.title == null || item.title.isEmpty()) {
                item.title = "ダウンロード " + item.downloadId;
            }
            if (!file.exists()) {
                holder.fileTitle.setText(item.title + " [削除済]");
                holder.fileTitle.setPaintFlags(holder.fileTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                holder.fileTitle.setText(item.title);
                holder.fileTitle.setPaintFlags(holder.fileTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            }
            String statusText;
            boolean showProgress = false;
            boolean showOpenButton = false;
            switch (item.status) {
                case DownloadManager.STATUS_SUCCESSFUL:
                    statusText = "完了 (" + formatSize(item.totalSize) + ")";
                    showOpenButton = true;
                    break;
                case DownloadManager.STATUS_FAILED:
                    statusText = "失敗";
                    break;
                case DownloadManager.STATUS_RUNNING:
                    statusText = "ダウンロード中 (" + formatSize(item.downloadedSize) + " / " + formatSize(item.totalSize) + ", " + item.getProgress() + "%)";
                    showProgress = true;
                    break;
                case DownloadManager.STATUS_PAUSED:
                    statusText = "一時停止中 (" + formatSize(item.downloadedSize) + " / " + formatSize(item.totalSize) + ", " + item.getProgress() + "%)";
                    showProgress = true;
                    break;
                case DownloadManager.STATUS_PENDING:
                    statusText = "待機中";
                    break;
                default:
                    statusText = "不明";
            }
            if (item.isPaused) {
                statusText = "一時停止中 (" + formatSize(item.downloadedSize) + " / " + formatSize(item.totalSize) + ", " + item.getProgress() + "%)";
                showProgress = true;
            }
            holder.fileStatus.setText(statusText);
            if (showProgress) {
                holder.progressBar.setVisibility(View.VISIBLE);
                holder.progressBar.setProgress(item.getProgress());
            } else {
                holder.progressBar.setVisibility(View.GONE);
            }
            if (showOpenButton) {
                holder.btnOpenFile.setVisibility(View.VISIBLE);
                holder.btnOpenFile.setOnClickListener(v -> {
                    try {
                        File files = new File(item.filePath);
                        openFile(files);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(context, "ファイルを開けません", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                holder.btnOpenFile.setVisibility(View.GONE);
            }


            if (item.filePath != null && item.filePath.toLowerCase().endsWith(".apk")) {
                holder.itemView.setOnClickListener(v -> {
                    File apkFile = new File(item.filePath);
                    if (apkFile.exists()) {
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                Uri apkUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", apkFile);
                                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                            } else {
                                intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
                            }
                            context.startActivity(intent);
                        } catch (Exception e) {
                            Toast.makeText(context, "インストールできません: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(context, "ファイルが存在しません", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                holder.itemView.setOnClickListener(v -> {
                    File files = new File(item.filePath);
                    openFile(files);
                });
            }

            holder.itemView.setOnLongClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return true; // 無効位置を防ぐ
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                if (!file.exists()) {
                    builder.setTitle("操作を選択")
                            .setItems(new String[]{"履歴から消去"}, (dialog, which) -> {
                                if (which == 0) {
                                    items.remove(pos);
                                    notifyItemRemoved(pos);
                                    saveDownloadHistory();
                                    if (downloadItems.isEmpty()) {
                                        tvEmpty.setVisibility(View.VISIBLE);
                                        recyclerView.setVisibility(View.GONE);
                                    } else {
                                        tvEmpty.setVisibility(View.GONE);
                                        recyclerView.setVisibility(View.VISIBLE);
                                    }
                                    Toast.makeText(context, "履歴から消去しました", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("閉じる", null)
                            .show();
                } else {
                    if (item.status == DownloadManager.STATUS_SUCCESSFUL) {
                        builder.setTitle("操作を選択")
                                .setItems(new String[]{"ファイル削除"}, (dialog, which) -> {
                                    builder.setTitle("確認")
                                            .setMessage("このファイルを削除してもよろしいですか？")
                                            .setPositiveButton("削除", (dialogs, whichs) -> {
                                                if (which == 0) {
                                                    File delFile = new File(item.filePath);
                                                    if (delFile.exists() && delFile.delete()) {
                                                        Toast.makeText(context, "ファイルを削除しました", Toast.LENGTH_SHORT).show();
                                                    } else {
                                                        Toast.makeText(context, "ファイルの削除に失敗しました", Toast.LENGTH_SHORT).show();
                                                    }
                                                    notifyDataSetChanged();
                                                }
                                            })
                                            .setNegativeButton("キャンセル", null)
                                            .show();
                                })
                                .setNegativeButton("閉じる", null)
                                .show();
                    } else {
                        if (!item.isPaused) {
                            builder.setTitle("操作を選択")
                                    .setItems(new String[]{"キャンセル", "停止"}, (dialog, which) -> {
                                        if (which == 0) {
                                            downloadManager.remove(item.downloadId);
                                            Toast.makeText(context, "ダウンロードをキャンセルしました", Toast.LENGTH_SHORT).show();
                                            items.remove(position);
                                            notifyItemRemoved(position);
                                        } else if (which == 1) {
                                            downloadManager.remove(item.downloadId);
                                            item.isPaused = true;
                                            Toast.makeText(context, "ダウンロードを一時停止しました", Toast.LENGTH_SHORT).show();
                                            notifyItemChanged(position);
                                        }
                                    })
                                    .setNegativeButton("閉じる", null)
                                    .show();
                        } else {
                            builder.setTitle("操作を選択")
                                    .setItems(new String[]{"キャンセル", "再開"}, (dialog, which) -> {
                                        if (which == 0) {
                                            Toast.makeText(context, "ダウンロードをキャンセルしました", Toast.LENGTH_SHORT).show();
                                            items.remove(position);
                                            notifyItemRemoved(position);
                                        } else if (which == 1) {
                                            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(item.downloadUrl));
                                            request.setTitle(item.title);
                                            request.setDescription(item.description != null ? item.description : "Downloading file...");
                                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                                            long newDownloadId = downloadManager.enqueue(request);
                                            item.downloadId = newDownloadId;
                                            item.isPaused = false;
                                            Toast.makeText(context, "ダウンロードを再開しました", Toast.LENGTH_SHORT).show();
                                            notifyItemChanged(position);
                                        }
                                    })
                                    .setNegativeButton("閉じる", null)
                                    .show();
                        }
                    }
                }
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            ImageView fileIcon;
            TextView fileTitle;
            TextView fileStatus;
            ProgressBar progressBar;
            Button btnOpenFile;

            public ViewHolder(View itemView) {
                super(itemView);
                fileIcon = itemView.findViewById(R.id.fileIcon);
                fileTitle = itemView.findViewById(R.id.fileTitle);
                fileStatus = itemView.findViewById(R.id.fileStatus);
                progressBar = itemView.findViewById(R.id.progressBar);
                btnOpenFile = itemView.findViewById(R.id.btnOpenFile);
            }
        }

        private String formatSize(long size) {
            if (size <= 0) return "0 B";
            final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
            int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
            return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
        }
    }
}
