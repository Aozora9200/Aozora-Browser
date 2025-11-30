package com.aozora.aozora;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class asciiart extends Activity {

    private static final int STORAGE_PERMISSION_REQUEST_CODE = 100;
    private static final int REQUEST_CODE_PICK_IMAGE = 200;

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

    private Button convertButton;
    private TextView asciiTextView;
    private TextView selectedFilePath;
    private TextView savedFilePath;
    private String selectedImagePath = null;

    private static final String FILE_PREFIX = "_ascii_art.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applySavedTheme();
        setContentView(R.layout.asciiart);
        Background = findViewById(R.id.background);
        applySavedBackground();
        applyBackTheme();

        Button selectImageButton = findViewById(R.id.selectImageButton);
        convertButton = findViewById(R.id.convertButton);
        asciiTextView = findViewById(R.id.asciiTextView);
        selectedFilePath = findViewById(R.id.selectedFilePath);
        savedFilePath = findViewById(R.id.savedFilePath);

        if (!hasStoragePermission()) {
            requestStoragePermission();
        }

        // Action Bar が表示されているか確認
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        selectImageButton.setOnClickListener(v -> {
            if (hasStoragePermission()) {
                pickImageFromStorage();
            } else {
                Toast.makeText(this, getString(R.string.need_storage_permission), Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        convertButton.setOnClickListener(v -> {
            if (!selectedImagePath.isEmpty()) {
                Bitmap bitmap = BitmapFactory.decodeFile(selectedImagePath);
                if (bitmap != null) {
                    String asciiArt = convertToAscii(bitmap);
                    asciiTextView.setText(asciiArt);
                    if (((Switch) findViewById(R.id.saveText)).isChecked()) {
                        saveAsciiArt(asciiArt, bitmap); // カラー情報も保存
                    }
                } else {
                    Toast.makeText(this, getString(R.string.cannot_load_image), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // 権限があるか確認
    private boolean hasStoragePermission() {
        return checkSelfPermission(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    // 権限をリクエスト
    private void requestStoragePermission() {
        requestPermissions(
                new String[]{Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE},
                STORAGE_PERMISSION_REQUEST_CODE
        );
    }

    /** @noinspection NullableProblems*/
    // 権限リクエストの結果を処理
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this, getString(R.string.need_storage_permission), Toast.LENGTH_SHORT).show();
        }
    }

    // ストレージから画像を選択
    private void pickImageFromStorage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PICK_IMAGE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                Uri selectedImageUri = data.getData();
                selectedImagePath = getRealPathFromURI(selectedImageUri);
                selectedFilePath.setText(selectedImagePath != null ? selectedImagePath : getString(R.string.cannot_get_image_path));

                if (selectedImagePath != null) {
                    convertButton.setEnabled(true);
                } else {
                    convertButton.setEnabled(false);
                    savedFilePath.setText("");
                    Toast.makeText(this, getString(R.string.cannot_get_image_path), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    // URIから実際のパスを取得
    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        try (Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                return cursor.getString(column_index);
            }
        }
        return null;
    }

    // アスキーアートに変換
    private String convertToAscii(Bitmap bitmap) {
        StringBuilder asciiArt = new StringBuilder();
        int targetWidth = 200; // アスキーアートの幅を調整
        int targetHeight = (int) ((double) bitmap.getHeight() / bitmap.getWidth() * targetWidth);

        // 画像をリサイズ
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, false);

        // ASCII文字セット
        String asciiChars = "@#S%?*+;:,. ";

        // ピクセルごとに輝度を計算して文字にマッピング
        for (int y = 0; y < resizedBitmap.getHeight(); y++) {
            for (int x = 0; x < resizedBitmap.getWidth(); x++) {
                int pixel = resizedBitmap.getPixel(x, y);
                int red = (pixel >> 16) & 0xFF;
                int green = (pixel >> 8) & 0xFF;
                int blue = pixel & 0xFF;

                // 輝度値の計算
                int gray = (int) (0.2989 * red + 0.5870 * green + 0.1140 * blue);
                int charIndex = gray * (asciiChars.length() - 1) / 255;

                asciiArt.append(asciiChars.charAt(charIndex));
            }
            asciiArt.append("\n");
        }

        return asciiArt.toString();
    }

    // アスキーアートをファイルに保存 (カラー情報も保存)
    private void saveAsciiArt(String asciiArt, Bitmap bitmap) {
        File dir = getExternalFilesDir(null);
        if (dir != null) {


            SimpleDateFormat sdf = new SimpleDateFormat("MMddHHmmss");
            String currentDateTime = sdf.format(new Date());
            String baseFileName = currentDateTime + "_ascii_art.txt";

            File asciiFile = new File(dir, baseFileName);
            File colorFile = new File(dir, baseFileName.replace(".txt", ".dat"));

            try (FileOutputStream fos = new FileOutputStream(asciiFile)) {
                fos.write(asciiArt.getBytes());
                fos.flush();


                Switch saveColorSwitch = findViewById(R.id.saveColorSwitch); // Switchの参照取得
                if (saveColorSwitch.isChecked()) {
                    saveColorData(colorFile, bitmap);
                    Toast.makeText(this, "カラー情報の保存先: " + colorFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                }

                Toast.makeText(this, getString(R.string.file_save_success), Toast.LENGTH_SHORT).show();
                savedFilePath.setText(getString(R.string.file_saved_path) + asciiFile.getAbsolutePath());

            } catch (IOException e) {
                Toast.makeText(this, getString(R.string.file_save_failed), Toast.LENGTH_SHORT).show();
                savedFilePath.setText("");
                e.printStackTrace();
            }
        }
    }


    private void saveColorData(File file, Bitmap bitmap) {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            StringBuilder colorData = new StringBuilder();

            for (int y = 0; y < bitmap.getHeight(); y++) {
                for (int x = 0; x < bitmap.getWidth(); x++) {
                    int pixel = bitmap.getPixel(x, y);
                    int red = (pixel >> 16) & 0xFF;
                    int green = (pixel >> 8) & 0xFF;
                    int blue = pixel & 0xFF;


                    colorData.append(x).append(",").append(y).append(":")
                            .append(red).append(",")
                            .append(green).append(",")
                            .append(blue).append("\n");
                }
            }

            fos.write(colorData.toString().getBytes());
            fos.flush();
        } catch (IOException e) {
            Toast.makeText(this, getString(R.string.file_save_failed), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}