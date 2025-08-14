package com.aozora.aozora;

public class SavedBookMark {
    int id;
    String url;
    String title;
    String screenshotPath;

    public SavedBookMark(int id, String url, String title, String screenshotPath) {
        this.id = id;
        this.url = url;
        this.title = title;
        this.screenshotPath = screenshotPath;
    }
}
