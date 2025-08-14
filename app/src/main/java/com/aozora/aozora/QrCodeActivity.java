package com.aozora.aozora;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.v4.view.ViewCompat;
import android.util.Base64;
import android.util.Base64OutputStream;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class QrCodeActivity extends Activity {

    private static final int FILE_SELECT_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int MAX_QR_DATA_LENGTH = 2953;

    private Button selectFileButton;
    private Button generateTextQrButton;
    private EditText inputEditText;
    private volatile boolean isProcessing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Android 10 未満の場合、外部ストレージ書き込み権限のチェック
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
            }
        }

        // LinearLayoutCompat（旧サポートライブラリ）を利用してレイアウト生成
        LinearLayoutCompat layout = new LinearLayoutCompat(this);
        layout.setOrientation(LinearLayoutCompat.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        // Action Bar が表示されているか確認
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // `fitsSystemWindows=true` を適用
        ViewCompat.setFitsSystemWindows(layout, true);

        selectFileButton = new Button(this);
        selectFileButton.setText("ファイルを選択してQR生成");
        selectFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectFile();
            }
        });
        layout.addView(selectFileButton);

        inputEditText = new EditText(this);
        inputEditText.setHint("文字列やURLを入力");
        layout.addView(inputEditText);

        generateTextQrButton = new Button(this);
        generateTextQrButton.setText("QRコード保存");
        generateTextQrButton.setVisibility(View.GONE);
        generateTextQrButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                generateTextQrCode();
            }
        });
        layout.addView(generateTextQrButton);

        inputEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (s.toString().trim().isEmpty()) {
                    generateTextQrButton.setVisibility(View.GONE);
                } else {
                    generateTextQrButton.setVisibility(View.VISIBLE);
                }
            }
        });

        setContentView(layout);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // 前の画面に戻る
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void selectFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, FILE_SELECT_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_SELECT_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            if (fileUri != null) {
                final String originalFileName = getFileName(fileUri);
                final String qrContent = createDataUri(fileUri);
                if (qrContent == null) {
                    Toast.makeText(QrCodeActivity.this, "ファイルの変換に失敗しました", Toast.LENGTH_LONG).show();
                    return;
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        isProcessing = true;
                        Bitmap qrBitmap = generateQrCodeBitmap(qrContent, 500, 500);
                        if (qrBitmap != null) {
                            final String savedFileName = originalFileName + "_" + getCurrentTimeStamp() + ".png";
                            final boolean saved = saveBitmapToPictures(qrBitmap, savedFileName);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    isProcessing = false;
                                    if (saved) {
                                        Toast.makeText(QrCodeActivity.this,
                                                "QRコード画像保存: " + savedFileName, Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(QrCodeActivity.this,
                                                "保存に失敗しました", Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    isProcessing = false;
                                    Toast.makeText(QrCodeActivity.this,
                                            "QRコード生成に失敗しました", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                }).start();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private Bitmap generateQrCodeBitmap(String text, int width, int height) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null) cursor.close();
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result != null ? result : "unknown";
    }

    private String getCurrentTimeStamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        return sdf.format(new Date());
    }

    private boolean saveBitmapToPictures(Bitmap bitmap, String fileName) {
        OutputStream out = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
                Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri == null) return false;
                out = getContentResolver().openOutputStream(uri);
            } else {
                File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File file = new File(picturesDir, fileName);
                out = new FileOutputStream(file);
            }
            if (out != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    private void generateTextQrCode() {
        final String text = inputEditText.getText().toString().trim();
        if (!text.isEmpty()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    isProcessing = true;
                    Bitmap qrBitmap = generateQrCodeBitmap(text, 500, 500);
                    if (qrBitmap != null) {
                        final String savedFileName = getCurrentTimeStamp() + ".png";
                        final boolean saved = saveBitmapToPictures(qrBitmap, savedFileName);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                isProcessing = false;
                                if (saved) {
                                    Toast.makeText(QrCodeActivity.this,
                                            "QRコード画像保存: " + savedFileName, Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(QrCodeActivity.this,
                                            "保存に失敗しました", Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                isProcessing = false;
                                Toast.makeText(QrCodeActivity.this,
                                        "QRコード生成に失敗しました", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            }).start();
        }
    }

    private String createDataUri(Uri fileUri) {
        String mimeType = getContentResolver().getType(fileUri);
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        String base64Data = convertFileToBase64(fileUri);
        if (base64Data == null) {
            return null;
        }
        String dataUriPrefix;
        if (mimeType.startsWith("image/")) {
            dataUriPrefix = "data:" + mimeType + ";base64,";
        } else if (mimeType.equals("text/plain")) {
            dataUriPrefix = "data:" + mimeType + ";charset=utf-8;base64,";
        } else {
            dataUriPrefix = "data:" + mimeType + ";base64,";
        }
        String dataUri = dataUriPrefix + base64Data;
        if (dataUri.length() > MAX_QR_DATA_LENGTH) {
            Toast.makeText(QrCodeActivity.this,
                    "ファイル容量がQRコードのサイズを超えています", Toast.LENGTH_LONG).show();
            return null;
        }
        return dataUri;
    }

    private String convertFileToBase64(Uri fileUri) {
        InputStream inputStream = null;
        ByteArrayOutputStream byteArrayOutputStream = null;
        try {
            inputStream = getContentResolver().openInputStream(fileUri);
            if (inputStream == null) return null;
            BufferedInputStream bis = new BufferedInputStream(inputStream);
            byteArrayOutputStream = new ByteArrayOutputStream();
            Base64OutputStream base64OutputStream = new Base64OutputStream(byteArrayOutputStream, Base64.NO_WRAP);
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                base64OutputStream.write(buffer, 0, bytesRead);
            }
            base64OutputStream.flush();
            base64OutputStream.close();
            return byteArrayOutputStream.toString("UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (byteArrayOutputStream != null) {
                try { byteArrayOutputStream.close(); } catch (IOException e) { e.printStackTrace(); }
            }
            if (inputStream != null) {
                try { inputStream.close(); } catch (IOException e) { e.printStackTrace(); }
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (isProcessing) {
            Toast.makeText(this, "変換中はバックキーが無効です", Toast.LENGTH_SHORT).show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,
                        "外部ストレージへの書き込み権限が必要です", Toast.LENGTH_LONG).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}