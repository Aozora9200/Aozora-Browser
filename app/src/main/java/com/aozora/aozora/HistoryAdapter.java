package com.aozora.aozora;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    public interface HistoryListener {
        void onHistoryItemClick(String url);
        void onHistoryItemNewTab(String url);
        void onHistoryDeleted();
        Bitmap getFavicon(String url);
        void copyToClipboard(String text);
        void historySavebm(String url, String title);
        void historyUrlShare(String url);
    }

    private final List<MainActivity.HistoryItem> items;
    private final AlertDialog dialog;
    private final HistoryListener listener;
    private final Context context;

    public HistoryAdapter(Context context, List<MainActivity.HistoryItem> items,
                          AlertDialog dialog, HistoryListener listener) {
        this.context = context;
        this.items = items;
        this.dialog = dialog;
        this.listener = listener;
    }

    @Override
    public HistoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final HistoryViewHolder holder, int position) {
        final MainActivity.HistoryItem item = items.get(position);

        // タイトルまたはURLを表示
        holder.title.setText(
                (item.getTitle() != null && !item.getTitle().isEmpty())
                        ? item.getTitle()
                        : item.getUrl()
        );
        holder.url.setText(item.getUrl());

        // favicon を非同期で取得
        String faviconUrl = "https://www.google.com/s2/favicons?sz=64&domain=" + Uri.parse(item.getUrl()).getHost();
        new DownloadFaviconTask(holder.favicon).execute(faviconUrl);

        // クリック時：WebView で開く
        holder.itemView.setOnClickListener(v -> {
            listener.onHistoryItemClick(item.getUrl());
            if (dialog != null) dialog.dismiss();
        });

        // 長押しメニュー
        holder.itemView.setOnLongClickListener(v -> {
            final int currentPosition = holder.getAdapterPosition();
            if (currentPosition == RecyclerView.NO_POSITION) return true;

            final MainActivity.HistoryItem currentItem = items.get(currentPosition);
            final String[] options = {"開く", "新しいタブで開く", "ブックマークを保存", "リンクを共有", "URLをコピー", "履歴から消去"};

            new AlertDialog.Builder(context)
                    .setTitle(currentItem.getTitle())
                    .setItems(options, (dialogInterface, which) -> {
                        if (which == 0) {
                            listener.onHistoryItemClick(item.getUrl());
                        } else if (which == 1) {
                            listener.onHistoryItemNewTab(item.getUrl());
                        } else if (which == 2) {
                            listener.historySavebm(item.getUrl(), item.getTitle());
                        } else if (which == 3) {
                            listener.historyUrlShare(item.getUrl());
                        } else if (which == 4) {
                            listener.copyToClipboard(currentItem.getUrl());
                        } else if (which == 5) {
                            items.remove(currentPosition);
                            notifyItemRemoved(currentPosition);

                            Intent data = new Intent();
                            data.putExtra("history_list", new ArrayList<>(items));
                            ((Activity) context).setResult(Activity.RESULT_OK, data);

                            Toast.makeText(context, "削除しました", Toast.LENGTH_SHORT).show();
                        }
                    }).show();
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        ImageView favicon;
        TextView title;
        TextView url;

        public HistoryViewHolder(View itemView) {
            super(itemView);
            favicon = itemView.findViewById(R.id.historyFavicon);
            title = itemView.findViewById(R.id.historyTitle);
            url = itemView.findViewById(R.id.historyUrl);
        }
    }
    private static class DownloadFaviconTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;

        DownloadFaviconTask(ImageView imageView) {
            imageViewReference = new WeakReference<>(imageView);
        }

        @Override
        protected Bitmap doInBackground(String... urls) {
            String url = urls[0];
            try (InputStream in = new java.net.URL(url).openStream()) {
                return BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            ImageView imageView = imageViewReference.get();
            if (imageView != null) {
                if (result != null) {
                    imageView.setImageBitmap(result);
                } else {
                    imageView.setImageResource(R.drawable.transparent_vector);
                }
            }
        }
    }
}