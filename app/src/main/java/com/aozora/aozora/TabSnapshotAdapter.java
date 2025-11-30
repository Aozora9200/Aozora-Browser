package com.aozora.aozora;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TabSnapshotAdapter extends RecyclerView.Adapter<TabSnapshotAdapter.PageViewHolder> {
    private final Activity activity;
    private final List<WebView> webViews;
    private final Map<WebView, Bitmap> tabSnapshots;
    private final ViewGroup webViewContainer;
    private final LayoutInflater inflater;
    private final OnTabActionListener listener;
    private final Set<Integer> closedTabs = new HashSet<>();

    public TabSnapshotAdapter(Activity activity,
                              List<WebView> webViews,
                              Map<WebView, Bitmap> tabSnapshots,
                              ViewGroup webViewContainer,
                              OnTabActionListener listener) {
        this.activity = activity;
        this.inflater = LayoutInflater.from(activity);
        this.webViews = webViews;
        this.tabSnapshots = tabSnapshots;
        this.webViewContainer = webViewContainer;
        this.listener = listener;
    }

    private List<Integer> getVisibleTabIndices() {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < webViews.size(); i++) {
            if (!closedTabs.contains(i)) {
                indices.add(i);
            }
        }
        return indices;
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 1ページ分のレイアウトを生成
        LinearLayout grid = new LinearLayout(activity);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout row1 = new LinearLayout(activity);
        LinearLayout row2 = new LinearLayout(activity);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row2.setOrientation(LinearLayout.HORIZONTAL);

        grid.addView(row1, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        grid.addView(row2, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        return new PageViewHolder(grid, row1, row2);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        holder.row1.removeAllViews();
        holder.row2.removeAllViews();

        List<Integer> visibleTabs = getVisibleTabIndices();
        int baseIndex = position * 4;

        for (int i = 0; i < 4; i++) {
            final int listIndex = baseIndex + i;
            View tile = inflater.inflate(R.layout.item_tab_tile, holder.grid, false);
            if (listIndex < visibleTabs.size()) {
                int tabIndex = visibleTabs.get(listIndex);

                ImageView img = tile.findViewById(R.id.tab_image);
                TextView title = tile.findViewById(R.id.tab_title);
                ImageView close = tile.findViewById(R.id.tab_close);

                if (tabIndex < webViews.size() && !closedTabs.contains(tabIndex)) {
                    WebView w = webViews.get(tabIndex);
                    Bitmap bm = tabSnapshots.get(w);
                    if (bm != null) {
                        img.setImageBitmap(Bitmap.createScaledBitmap(
                                bm, Math.max(1, bm.getWidth() / 4),
                                Math.max(1, bm.getHeight() / 4), true));
                    }
                    String t = w.getTitle();
                    if (t == null || t.isEmpty()) t = "読込中...";
                    title.setText(shortTitle(t));
                    title.setVisibility(View.VISIBLE);
                    img.setVisibility(View.VISIBLE);

                    img.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (listener != null) listener.onTabSelected(tabIndex);
                        }
                    });

                    // 🔹 閉じるボタンの制御
                    if (getVisibleTabIndices().size() <= 1) {
                        close.setEnabled(false);
                        close.setVisibility(View.GONE); // ボタンを隠す
                    } else {
                        close.setEnabled(true);
                        close.setVisibility(View.VISIBLE);
                    }
                    close.setOnClickListener(v -> {
                        android.view.animation.Animation fadeOut =
                                android.view.animation.AnimationUtils.loadAnimation(activity, R.anim.tab_out);

                        fadeOut.setAnimationListener(new android.view.animation.Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(android.view.animation.Animation animation) {
                            }

                            @Override
                            public void onAnimationEnd(android.view.animation.Animation animation) {
                                if (tabIndex >= 0 && tabIndex < webViews.size()) {
                                    closedTabs.add(tabIndex);
                                    if (listener != null) listener.onTabClosed(tabIndex);
                                }
                            }

                            @Override
                            public void onAnimationRepeat(android.view.animation.Animation animation) {
                            }
                        });

                        tile.startAnimation(fadeOut);
                    });

                } else {
                    tile.setVisibility(View.INVISIBLE);
                }

                LinearLayout.LayoutParams lp =
                        new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
                lp.setMargins(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6));

                if (i < 2) holder.row1.addView(tile, lp);
                else holder.row2.addView(tile, lp);
            }
        }
    }

    @Override
    public int getItemCount() {
        int visibleCount = getVisibleTabIndices().size();
        return (visibleCount + 3) / 4; // 1ページに4つずつ
    }

    static class PageViewHolder extends RecyclerView.ViewHolder {
        LinearLayout grid, row1, row2;
        PageViewHolder(LinearLayout grid, LinearLayout row1, LinearLayout row2) {
            super(grid);
            this.grid = grid;
            this.row1 = row1;
            this.row2 = row2;
        }
    }

    private String shortTitle(String url) {
        if (url == null) return "";
        if (url.length() > 40) return url.substring(0, 37) + "...";
        return url;
    }

    private int dpToPx(int dp) {
        return (int) (dp * activity.getResources().getDisplayMetrics().density + 0.5f);
    }
}