package com.aozora.aozora;

import android.graphics.Bitmap;
import java.util.ArrayList;

public class TabInfo {
    private String title;
    private String url;
    private Bitmap icon;
    private ArrayList<String> history = new ArrayList<>();

    public TabInfo(String title, String url, Bitmap icon) {
        this.title = title;
        this.url = url;
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Bitmap getIcon() {
        return icon;
    }

    public void setIcon(Bitmap icon) {
        this.icon = icon;
    }

    public ArrayList<String> getHistory() {
        return history;
    }

    public void setHistory(ArrayList<String> history) {
        this.history = history;
    }
}