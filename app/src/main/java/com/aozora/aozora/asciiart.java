package com.aozora.aozora;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

public class asciiart extends Activity {

    private static final int REQUEST_CODE_PICK_IMAGE = 200;
    private Button convertButton;
    private TextView asciiTextView;
    private TextView selectedFilePath;
    private TextView savedFilePath;
    private Uri selectedImageUri = null;
    private static final String FILE_PREFIX = "_ascii_art.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.asciiart);

        Button selectImageButton = findViewById(R.id.selectImageButton);
        convertButton = findViewById(R.id.convertButton);
        asciiTextView = findViewById(R.id.asciiTextView);
        selectedFilePath = findViewById(R.id.selectedFilePath);
        savedFilePath = findViewById(R.id.savedFilePath);

        selectImageButton.setOnClickListener(v -> pickImageFromStorage());

        // Action Bar が表示されているか確認
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        convertButton.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                try (InputStream imageStream = getContentResolver().openInputStream(selectedImageUri)) {
                    if (imageStream != null) {
                        Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
                        String asciiArt = convertToAscii(bitmap);
                        asciiTextView.setText(asciiArt);
                        if (((Switch) findViewById(R.id.saveText)).isChecked()) {
                            saveAsciiArt(asciiArt, bitmap);
                        }
                    } else {
                        Toast.makeText(this, R.string.cannot_load_image, Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    Toast.makeText(this, R.string.cannot_load_image, Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // 前の画面に戻る
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void pickImageFromStorage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                selectedFilePath.setText(getFileName(selectedImageUri));
                convertButton.setEnabled(true);
            } else {
                selectedFilePath.setText(R.string.cannot_get_image_path);
                convertButton.setEnabled(false);
                savedFilePath.setText("");
                Toast.makeText(this, R.string.cannot_get_image_path, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getFileName(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
            }
        }
        return getString(R.string.unknown_file);
    }

    private String convertToAscii(Bitmap bitmap) {
        StringBuilder asciiArt = new StringBuilder();
        int targetWidth = 200;
        int targetHeight = bitmap.getHeight() * targetWidth / bitmap.getWidth();
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, false);
        int[] pixels = new int[targetWidth * targetHeight];
        resizedBitmap.getPixels(pixels, 0, targetWidth, 0, 0, targetWidth, targetHeight);

        String asciiChars = "@#S%?*+;:,. ";

        for (int y = 0; y < targetHeight; y++) {
            for (int x = 0; x < targetWidth; x++) {
                int pixel = pixels[y * targetWidth + x];
                int gray = (int) (0.2989 * ((pixel >> 16) & 0xFF) + 0.5870 * ((pixel >> 8) & 0xFF) + 0.1140 * (pixel & 0xFF));
                asciiArt.append(asciiChars.charAt(gray * (asciiChars.length() - 1) / 255));
            }
            asciiArt.append("\n");
        }

        return asciiArt.toString();
    }

    private void saveAsciiArt(String asciiArt, Bitmap bitmap) {
        File dir = getExternalFilesDir(null);
        if (dir != null) {
            String fileName = new SimpleDateFormat("MMddHHmmss").format(new Date()) + FILE_PREFIX;
            File asciiFile = new File(dir, fileName);
            File colorFile = new File(dir, fileName.replace(".txt", ".dat"));

            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(asciiFile), StandardCharsets.UTF_8))) {
                writer.write(asciiArt);
                Toast.makeText(this, R.string.file_save_success, Toast.LENGTH_SHORT).show();
                savedFilePath.setText(getString(R.string.file_saved_path) + asciiFile.getAbsolutePath());

                if (((Switch) findViewById(R.id.saveColorSwitch)).isChecked()) {
                    saveColorData(colorFile, bitmap);
                    Toast.makeText(this, "カラー情報の保存先: " + colorFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                Toast.makeText(this, R.string.file_save_failed, Toast.LENGTH_SHORT).show();
                savedFilePath.setText("");
                e.printStackTrace();
            }
        }
    }

    private void saveColorData(File file, Bitmap bitmap) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = pixels[y * width + x];
                    writer.write(String.format("%d,%d:%d,%d,%d%n", x, y, (pixel >> 16) & 0xFF, (pixel >> 8) & 0xFF, pixel & 0xFF));
                }
            }
            writer.flush();
        } catch (IOException e) {
            Toast.makeText(this, R.string.file_save_failed, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}
