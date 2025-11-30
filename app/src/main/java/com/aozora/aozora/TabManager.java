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

    private TabManager() {}

    public static TabManager getInstance() {
        if (instance == null) instance = new TabManager();
        return instance;
    }

    public void setTabs(List<WebView> tabs, Map<WebView, Bitmap> snapshots, FrameLayout container) {
        this.tabs = tabs;
        this.snapshots = snapshots;
        this.container = container;
    }

    public List<WebView> getTabs() { return tabs; }
    public Map<WebView, Bitmap> getSnapshots() { return snapshots; }
    public FrameLayout getContainer() { return container; }
}