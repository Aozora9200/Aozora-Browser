package com.aozora.aozora;

public class SavedPage {
    int id;
    String url;
    String title;
    String screenshotPath;
    String dateSaved;

    public SavedPage(int id, String url, String title, String screenshotPath, String dateSaved) {
        this.id = id;
        this.url = url;
        this.title = title;
        this.screenshotPath = screenshotPath;
        this.dateSaved = dateSaved;
    }
}
