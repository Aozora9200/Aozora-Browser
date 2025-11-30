package com.aozora.aozora;

import static android.app.Activity.RESULT_OK;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TabListActivity extends Activity implements OnTabActionListener {

    private ViewPager2 listViewPager;
    private TabSnapshotAdapter listAdapter;
    private List<WebView> webViews;
    private Map<WebView, Bitmap> tabSnapshots;
    private FrameLayout webViewContainer;
    private List<Integer> closedTabIndices = new ArrayList<>(); // 閉じたタブを記録
    private boolean isAddTab = false;
    private boolean isAllClose = false;
    private boolean isSettings = false;
    private boolean isSecret = false;
    private boolean isSavedPage = false;
    private boolean isHistory = false;
    private boolean isBookmark = false;

    private FrameLayout tabActionBar;
    private ImageButton addTabButton, bmButton, menuButton;
    private ColorFilter invertFilter;

    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME = "selected_theme";
    private static final int THEME_LIGHT = 0;
    private static final int THEME_DARK = 1;
    private static final int THEME_SYSTEM = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applySavedTheme();
        setContentView(R.layout.activity_tab_list); // XML を読み込む

        TabManager holder = TabManager.getInstance();
        this.webViews = holder.getTabs();
        this.tabSnapshots = holder.getSnapshots();
        this.webViewContainer = holder.getContainer();

        // XML からビューを取得
        tabActionBar = findViewById(R.id.tab_actionbar);
        listViewPager = findViewById(R.id.viewPagerTabs);
        addTabButton = findViewById(R.id.buttonAddTab);
        bmButton = findViewById(R.id.buttonBMTab);
        menuButton = findViewById(R.id.buttonMenu);
        // 色反転フィルタを準備
        ColorMatrix colorMatrix_Invert = new ColorMatrix(new float[] {
                -1,  0,  0,  0, 255, // R
                0, -1,  0,  0, 255, // G
                0,  0, -1,  0, 255, // B
                0,  0,  0,  1,   0  // A
        });
        invertFilter = new ColorMatrixColorFilter(colorMatrix_Invert);
        applyBackTheme();
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(TabListActivity.this, v);
                popup.getMenuInflater().inflate(R.menu.menu_tablist, popup.getMenu());

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        // Activity の onOptionsItemSelected() を呼び出す
                        return onOptionsItemSelected(item);
                    }
                });

                popup.show();
            }
        });

        // アダプタ設定
        listAdapter = new TabSnapshotAdapter(this, webViews, tabSnapshots, webViewContainer, this);
        listViewPager.setAdapter(listAdapter);

        addTabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isAddTab = true;
                if (closedTabIndices.isEmpty()) {
                    Intent result = new Intent();
                    result.putExtra("action", "newTab");
                    setResult(RESULT_OK, result);
                }
                finish();
                overridePendingTransition(R.anim.no_animation, R.anim.slide_out_down_low);
                // isAddTab = false;
            }
        });

        bmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isBookmark = true;
                if (closedTabIndices.isEmpty()) {
                    Intent result = new Intent();
                    result.putExtra("action", "bookmark");
                    setResult(RESULT_OK, result);
                }
                finish();
            }
        });

        // 🔹 現在のタブ index を受け取る
        int currentTabIndex = getIntent().getIntExtra("currentTabIndex", 0);

        // 🔹 ページ番号を計算 (1ページ=4タブ)
        int currentPage = currentTabIndex / 4;

        // 🔹 ViewPager2 を指定ページに移動
        listViewPager.setCurrentItem(currentPage, false);
    }

    private void applySavedTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int theme = prefs.getInt(KEY_THEME, THEME_SYSTEM);
        int black = getResources().getColor(R. color. backgroundBlack);
        int white = getResources().getColor(R. color. backgroundWhite);

        switch (theme) {
            case THEME_LIGHT:
                setTheme(android.R.style.Theme_Holo_Light_NoActionBar);
                break;
            case THEME_DARK:
                setTheme(android.R.style.Theme_Holo_NoActionBar);
                break;
            case THEME_SYSTEM:
            default:
                break;
        }
    }

    private void applyBackTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int theme = prefs.getInt(KEY_THEME, THEME_SYSTEM);
        int white = getResources().getColor(R. color. tabactionbarwhite);
        int black = getResources().getColor(R. color. tabactionbarblack);

        switch (theme) {
            case THEME_LIGHT:
                tabActionBar.setBackgroundColor(white);
                addTabButton.setColorFilter(invertFilter);
                bmButton.setColorFilter(invertFilter);
                menuButton.setColorFilter(invertFilter);
                break;
            case THEME_DARK:
                tabActionBar.setBackgroundColor(black);
                addTabButton.clearColorFilter();
                bmButton.clearColorFilter();
                menuButton.clearColorFilter();
                break;
            case THEME_SYSTEM:
            default:
                // OS 側の設定に従う
                int nightModeFlags = getResources().getConfiguration().uiMode
                        & android.content.res.Configuration.UI_MODE_NIGHT_MASK;

                if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                    tabActionBar.setBackgroundColor(black);
                    addTabButton.clearColorFilter();
                    bmButton.clearColorFilter();
                    menuButton.clearColorFilter();
                } else {
                    tabActionBar.setBackgroundColor(white);
                    addTabButton.setColorFilter(invertFilter);
                    bmButton.setColorFilter(invertFilter);
                    menuButton.setColorFilter(invertFilter);
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_tablist, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_secret) {
            isSecret = true;
            if (closedTabIndices.isEmpty()) {
                Intent result = new Intent();
                result.putExtra("action", "secret");
                setResult(RESULT_OK, result);
            }
            finish();
            overridePendingTransition(R.anim.no_animation, R.anim.fadeout);
            return true;
        } else if (itemId == R.id.action_history) {
            isHistory = true;
            if (closedTabIndices.isEmpty()) {
                Intent result = new Intent();
                result.putExtra("action", "history");
                setResult(RESULT_OK, result);
            }
            finish();
            return true;
        } else if (itemId == R.id.action_savedpage) {
            isSavedPage = true;
            if (closedTabIndices.isEmpty()) {
                Intent result = new Intent();
                result.putExtra("action", "savedpage");
                setResult(RESULT_OK, result);
            }
            finish();
            return true;
        } else if (itemId == R.id.action_settings) {
            isSettings = true;
            if (closedTabIndices.isEmpty()) {
                Intent result = new Intent();
                result.putExtra("action", "settings");
                setResult(RESULT_OK, result);
            }
            finish();
            overridePendingTransition(R.anim.no_animation, R.anim.fadeout);
            return true;
        } else if (itemId == R.id.action_alltabclose) {
            alltabclose();
            return true;
        }
        if (item.getItemId() == android.R.id.home) {
            finish(); // 前の画面に戻る
            overridePendingTransition(R.anim.no_animation, R.anim.fadeout);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void alltabclose() {
        new AlertDialog.Builder(this)
                .setMessage("本当にすべてのタブを閉じますか？")
                .setPositiveButton("はい", (dialog, which) -> {
                    isAllClose = true;
                    Intent result = new Intent();
                    result.putExtra("action", "allTabClose");
                    setResult(RESULT_OK, result);
                    finish();
                    overridePendingTransition(R.anim.no_animation, R.anim.fadeout);
                })
                .setNegativeButton("いいえ", null)
                .show();
    }

    // 🔹 OnTabActionListener 実装
    @Override
    public void onTabSelected(int index) {
        Intent result = new Intent();
        result.putExtra("action", "switchTab");
        result.putExtra("tabIndex", index);
        setResult(RESULT_OK, result);
        finish();
        overridePendingTransition(R.anim.no_animation, R.anim.fadeout);
    }

    @Override
    public void onTabClosed(int index) {
        // どのページに影響したかを計算
        int pageIndex = index / 4;

        closedTabIndices.add(index); // 閉じた index を記録

        listAdapter.notifyItemChanged(pageIndex);

        listAdapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.no_animation, R.anim.fadeout);
        super.onBackPressed();
    }

    @Override
    public void finish() {
        // 閉じたタブをまとめて返す
        if (!isAddTab && !isAllClose && !isSettings && !isSecret && !isSavedPage && !isHistory && !isBookmark) {
            if (!closedTabIndices.isEmpty()) {
                Intent result = new Intent();
                result.putExtra("action", "closeTabs");
                result.putIntegerArrayListExtra("closedIndices", new ArrayList<>(closedTabIndices));
                setResult(RESULT_OK, result);
            }
        }
        if (isAddTab) {
            if (!closedTabIndices.isEmpty()) {
                Intent result = new Intent();
                result.putExtra("action", "closeTabsAddTab");
                result.putIntegerArrayListExtra("closedIndices", new ArrayList<>(closedTabIndices));
                setResult(RESULT_OK, result);
            }
        }

        if (isSettings) {
            if (!closedTabIndices.isEmpty()) {
                Intent result = new Intent();
                result.putExtra("action", "closeTabsSettings");
                result.putIntegerArrayListExtra("closedIndices", new ArrayList<>(closedTabIndices));
                setResult(RESULT_OK, result);
            }
        }

        if (isSecret) {
            if (!closedTabIndices.isEmpty()) {
                Intent result = new Intent();
                result.putExtra("action", "closeTabsSecret");
                result.putIntegerArrayListExtra("closedIndices", new ArrayList<>(closedTabIndices));
                setResult(RESULT_OK, result);
            }
        }

        if (isSavedPage) {
            if (!closedTabIndices.isEmpty()) {
                Intent result = new Intent();
                result.putExtra("action", "closeTabsSavedpage");
                result.putIntegerArrayListExtra("closedIndices", new ArrayList<>(closedTabIndices));
                setResult(RESULT_OK, result);
            }
        }

        if (isHistory) {
            if (!closedTabIndices.isEmpty()) {
                Intent result = new Intent();
                result.putExtra("action", "closeTabsHistory");
                result.putIntegerArrayListExtra("closedIndices", new ArrayList<>(closedTabIndices));
                setResult(RESULT_OK, result);
            }
        }

        if (isBookmark) {
            if (!closedTabIndices.isEmpty()) {
                Intent result = new Intent();
                result.putExtra("action", "closeTabsBookmark");
                result.putIntegerArrayListExtra("closedIndices", new ArrayList<>(closedTabIndices));
                setResult(RESULT_OK, result);
            }
        }

        super.finish();
    }
}