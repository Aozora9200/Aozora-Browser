package com.aozora.aozora;

import android.graphics.Bitmap;
import android.webkit.WebView;
import android.widget.FrameLayout;

import java.util.List;
import java.util.Map;

public class TabManager {
    private static TabManager instance;

    private List<WebView> tabs;
    private Map<WebView, Bitmap> snapshots;
    private FrameLayout container;

    // 通常タブ専用の補助情報。メモリ解放済み(WebView == null)のタブでも
    // タイトル表示・サムネイル読み込みができるようにするためのもの。
    // シークレットは setTabs() だけを呼ぶので常に null のまま = ディスクには一切触れない。
    private List<Integer> tabIds;
    private List<TabInfo> tabInfos;

    private TabManager() {}

    public static TabManager getInstance() {
        if (instance == null) instance = new TabManager();
        return instance;
    }

    public void setTabs(List<WebView> tabs, Map<WebView, Bitmap> snapshots, FrameLayout container) {
        this.tabs = tabs;
        this.snapshots = snapshots;
        this.container = container;
        // 前回(別Activity)の補助情報が残らないよう必ずリセットする
        this.tabIds = null;
        this.tabInfos = null;
    }

    /** setTabs() の直後に、通常タブ側(MainActivity)だけが呼ぶ。 */
    public void setTabMeta(List<Integer> tabIds, List<TabInfo> tabInfos) {
        this.tabIds = tabIds;
        this.tabInfos = tabInfos;
    }

    public List<WebView> getTabs() { return tabs; }
    public Map<WebView, Bitmap> getSnapshots() { return snapshots; }
    public FrameLayout getContainer() { return container; }

    /** 永続化用のタブID。補助情報が無い(シークレット等)場合は null。 */
    public Integer getTabId(int index) {
        if (tabIds == null || index < 0 || index >= tabIds.size()) return null;
        return tabIds.get(index);
    }

    /** WebView が破棄されている時用のタイトル。無ければ null。 */
    public String getTabTitle(int index) {
        if (tabInfos == null || index < 0 || index >= tabInfos.size()) return null;
        TabInfo info = tabInfos.get(index);
        return info != null ? info.getTitle() : null;
    }
}