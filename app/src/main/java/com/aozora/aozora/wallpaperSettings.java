package com.aozora.aozora;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;

public class wallpaperSettings extends Activity {
    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_BACKGROUND = "selected_background";
    private static final int BACKGROUND1 = 0;
    private static final int BACKGROUND2 = 1;
    private static final int BACKGROUND3 = 2;
    private static final int BACKGROUND4 = 3;
    private static final int BACKGROUND_CUSTOM = 4;

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String KEY_IMAGE_URI = "image_uri";

    private ImageButton backgroundButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wallpapersettings);
        backgroundButton = findViewById(R.id.background_example_image);
        Spinner spinner = findViewById(R.id.setup_backgroundList);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.background_options,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        // 現在の選択を反映
        applySavedBackgroundImage();
        applySavedBackground();
        SharedPreferences Backgroundprefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int savedBackground = Backgroundprefs.getInt(KEY_BACKGROUND, BACKGROUND1);
        spinner.setSelection(savedBackground);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                if (position != savedBackground) {
                    saveBackground(position);
                    recreate(); // Activity 再起動でテーマ反映
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Action Bar が表示されているか確認
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // 前の画面に戻る
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveBackground(int themeMode) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putInt(KEY_BACKGROUND, themeMode);
        editor.apply();
        applySavedBackgroundImage();
    }

    private void applySavedBackground() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int theme = prefs.getInt(KEY_BACKGROUND, BACKGROUND1);
        ImageView BackgroundImage = findViewById(R.id.backgroundImage);
        ImageView Background = findViewById(R.id.background);
        switch (theme) {
            case BACKGROUND1:
                prefs.edit().remove(KEY_IMAGE_URI).apply();
                BackgroundImage.setVisibility(View.VISIBLE);
                BackgroundImage.setImageResource(R.drawable.setupback);
                break;
            case BACKGROUND2:
                prefs.edit().remove(KEY_IMAGE_URI).apply();
                BackgroundImage.setVisibility(View.VISIBLE);
                BackgroundImage.setImageResource(R.drawable.background2);
                break;
            case BACKGROUND3:
                prefs.edit().remove(KEY_IMAGE_URI).apply();
                BackgroundImage.setVisibility(View.VISIBLE);
                BackgroundImage.setImageResource(R.drawable.background3);
                break;
            case BACKGROUND4:
                prefs.edit().remove(KEY_IMAGE_URI).apply();
                BackgroundImage.setVisibility(View.GONE);
                break;
            case BACKGROUND_CUSTOM:
            default:
                String uriString = prefs.getString(KEY_IMAGE_URI, null);
                if (uriString != null) {
                    Uri savedUri = Uri.parse(uriString);
                    BackgroundImage.setVisibility(View.VISIBLE);
                    BackgroundImage.setImageURI(savedUri);
                } else {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.setType("image/*");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(intent, PICK_IMAGE_REQUEST);
                }
                break;
        }
    }

    private void applySavedBackgroundImage() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int theme = prefs.getInt(KEY_BACKGROUND, BACKGROUND1);
        switch (theme) {
            case BACKGROUND1:
                backgroundButton.setVisibility(View.VISIBLE);
                backgroundButton.setImageResource(R.drawable.setupback);
                break;
            case BACKGROUND2:
                backgroundButton.setVisibility(View.VISIBLE);
                backgroundButton.setImageResource(R.drawable.background2);
                break;
            case BACKGROUND3:
                backgroundButton.setVisibility(View.VISIBLE);
                backgroundButton.setImageResource(R.drawable.background3);
                break;
            case BACKGROUND4:
                backgroundButton.setVisibility(View.INVISIBLE);
                break;
            case BACKGROUND_CUSTOM:
            default:
                String uriString = prefs.getString(KEY_IMAGE_URI, null);
                if (uriString != null) {
                    Uri savedUri = Uri.parse(uriString);
                    backgroundButton.setVisibility(View.VISIBLE);
                    backgroundButton.setImageURI(savedUri);
                } else {
                    backgroundButton.setVisibility(View.VISIBLE);
                    backgroundButton.setImageResource(R.drawable.setupback);
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();

            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

            // 永続的アクセス権を取得
            final int takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            ContentResolver resolver = getContentResolver();
            resolver.takePersistableUriPermission(imageUri, takeFlags);

            // URI を保存
            prefs.edit().putString(KEY_IMAGE_URI, imageUri.toString()).apply();
            recreate();
        }
    }
    
}
