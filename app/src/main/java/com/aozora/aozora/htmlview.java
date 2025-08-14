package com.aozora.aozora;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class htmlview extends Activity {

    private static final int TAG_COLOR = 0xFF0000FF;
    private static final int ATTRIBUTE_COLOR = 0xFF008000;
    private static final int VALUE_COLOR = 0xFFB22222;

    private static final int LARGE_TEXT_THRESHOLD = 700;
    private static final int REQUEST_PERMISSION_WRITE = 100;
    private static final int REQUEST_CODE_PICK_HTML = 101;

    private EditText urlInput;
    private Button loadButton, loadFromStorageButton, editButton, saveButton, searchButton;
    private EditText htmlEditText;
    private ImageButton revertButton;
    private RelativeLayout searchOverlay;
    private EditText searchQueryEditText;
    private TextView searchResultCountTextView;
    private Button searchNextButton, searchPrevButton, closeSearchButton;

    private String originalHtml = "";
    private final Stack<String> editHistory = new Stack<>();
    private boolean isEditing = false;
    private volatile boolean isUpdating = false;
    private volatile boolean isLoading = false;
    private long lastUndoTimestamp = 0;
    private static final long UNDO_THRESHOLD = 1000;

    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern ATTR_PATTERN = Pattern.compile("(\\w+)=\\\"([^\\\"]*)\\\"");
    private ArrayList<Integer> searchMatchPositions = new ArrayList<>();
    private int currentSearchIndex = -1;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler uiHandler = new Handler();

    private Runnable highlightRunnable;
    private static final int REQUEST_CODE_CREATE_FILE = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.htmlview);

        urlInput = findViewById(R.id.urlInput);
        loadButton = findViewById(R.id.loadButton);
        loadFromStorageButton = findViewById(R.id.loadFromStorageButton);
        editButton = findViewById(R.id.editButton);
        saveButton = findViewById(R.id.saveButton);
        htmlEditText = findViewById(R.id.htmlEditText);
        revertButton = findViewById(R.id.revertButton);
        searchButton = findViewById(R.id.searchButton);
        searchOverlay = findViewById(R.id.searchOverlay);
        searchQueryEditText = findViewById(R.id.searchQueryEditText);
        searchResultCountTextView = findViewById(R.id.searchResultCountTextView);
        searchNextButton = findViewById(R.id.searchNextButton);
        searchPrevButton = findViewById(R.id.searchPrevButton);
        closeSearchButton = findViewById(R.id.closeSearchButton);

        htmlEditText.setMovementMethod(new ScrollingMovementMethod());
        htmlEditText.setKeyListener(null);

        // Action Bar が表示されているか確認
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        loadButton.setOnClickListener(v -> {
            String urlStr = urlInput.getText().toString().trim();
            if (urlStr.startsWith("http://") || urlStr.startsWith("https://")) {
                if (!isLoading) {
                    fetchHtml(urlStr);
                } else {
                    Toast.makeText(htmlview.this, "読み込み中です。", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(htmlview.this, "正しいURLを入力してください", Toast.LENGTH_SHORT).show();
            }
        });

        loadFromStorageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("text/html");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "HTMLファイルを選択"), REQUEST_CODE_PICK_HTML);
        });

        editButton.setOnClickListener(v -> {
            if (!isEditing) {
                editHistory.clear();
                editHistory.push(htmlEditText.getText().toString());
                lastUndoTimestamp = System.currentTimeMillis();
                htmlEditText.setKeyListener(new EditText(htmlview.this).getKeyListener());
                htmlEditText.setFocusableInTouchMode(true);
                isEditing = true;
                Toast.makeText(htmlview.this, "編集モードに入りました", Toast.LENGTH_SHORT).show();
            }
        });

        htmlEditText.addTextChangedListener(new TextWatcher() {
            private String beforeChange;
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (!isUpdating && isEditing) {
                    beforeChange = s.toString();
                }
            }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (highlightRunnable != null) uiHandler.removeCallbacks(highlightRunnable);
            }
            @Override public void afterTextChanged(Editable s) {
                if (!isUpdating && isEditing) {
                    final String newText = s.toString();
                    long now = System.currentTimeMillis();
                    if (now - lastUndoTimestamp > UNDO_THRESHOLD) {
                        editHistory.push(beforeChange);
                        lastUndoTimestamp = now;
                    }
                    highlightRunnable = () -> {
                        if (!isUpdating) {
                            isUpdating = true;
                            executor.execute(() -> {
                                final int[][] spans = getHighlightSpans(newText);
                                uiHandler.post(() -> {
                                    applyHighlight(htmlEditText.getText(), spans);
                                    isUpdating = false;
                                });
                            });
                        }
                    };
                    uiHandler.postDelayed(highlightRunnable, 150);
                }
            }
        });

        revertButton.setOnClickListener(v -> {
            if (isEditing && !isUpdating) {
                if (!editHistory.isEmpty()) {
                    final String previousText = editHistory.pop();
                    isUpdating = true;
                    final Editable editable = htmlEditText.getText();
                    final int curPos = htmlEditText.getSelectionStart();
                    editable.replace(0, editable.length(), previousText);
                    executor.execute(() -> {
                        final int[][] spans = getHighlightSpans(previousText);
                        uiHandler.post(() -> {
                            applyHighlight(htmlEditText.getText(), spans);
                            int pos = Math.min(previousText.length(), curPos);
                            htmlEditText.setSelection(pos);
                            isUpdating = false;
                            Toast.makeText(htmlview.this, "変更を元に戻しました", Toast.LENGTH_SHORT).show();
                        });
                    });
                } else {
                    Toast.makeText(htmlview.this, "これ以上前はありません", Toast.LENGTH_SHORT).show();
                }
            }
        });

        saveButton.setOnClickListener(v -> {
            final String currentText = htmlEditText.getText().toString();
            final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            final String fileName = timeStamp + (!currentText.equals(originalHtml) ? "Edit.html" : ".html");
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/html");
            intent.putExtra(Intent.EXTRA_TITLE, fileName);
            startActivityForResult(intent, REQUEST_CODE_CREATE_FILE);
        });

        searchButton.setOnClickListener(v -> showSearchOverlay());
        closeSearchButton.setOnClickListener(v -> hideSearchOverlay());

        searchQueryEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString());
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        searchNextButton.setOnClickListener(v -> moveToNextSearchMatch());
        searchPrevButton.setOnClickListener(v -> moveToPreviousSearchMatch());
        Intent intent = getIntent();
        Uri uri = intent.getData();

        if (uri != null) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }

                htmlEditText.setText(stringBuilder.toString());
                reader.close();
            } catch (IOException e) {
                htmlEditText.setText("ファイルの読み込み中にエラーが発生しました。");
                e.printStackTrace();
            }
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // 前の画面に戻る
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSearchOverlay() {
        searchOverlay.setVisibility(View.VISIBLE);
        searchButton.setVisibility(View.INVISIBLE);
        searchQueryEditText.requestFocus();
        searchQueryEditText.setText("");
        searchResultCountTextView.setText("件数: 0");
        searchMatchPositions.clear();
        currentSearchIndex = -1;
    }

    private void hideSearchOverlay() {
        searchOverlay.setVisibility(View.GONE);
        searchButton.setVisibility(View.VISIBLE);
        Editable text = htmlEditText.getText();
        Object[] bgSpans = text.getSpans(0, text.length(), BackgroundColorSpan.class);
        for (Object span : bgSpans) {
            text.removeSpan(span);
        }
    }

    private void performSearch(final String query) {
        executor.execute(() -> {
            searchMatchPositions.clear();
            if (query != null && !query.isEmpty()) {
                String text = htmlEditText.getText().toString();
                int index = text.indexOf(query);
                while (index >= 0) {
                    searchMatchPositions.add(index);
                    index = text.indexOf(query, index + query.length());
                }
            }
            final int count = searchMatchPositions.size();
            uiHandler.post(() -> {
                searchResultCountTextView.setText("件数: " + count);
                if (count > 0) {
                    currentSearchIndex = 0;
                    highlightCurrentSearchMatch();
                }
            });
        });
    }

    private void highlightCurrentSearchMatch() {
        Editable text = htmlEditText.getText();
        Object[] bgSpans = text.getSpans(0, text.length(), BackgroundColorSpan.class);
        for (Object span : bgSpans) {
            text.removeSpan(span);
        }
        if (currentSearchIndex >= 0 && currentSearchIndex < searchMatchPositions.size()) {
            final int start = searchMatchPositions.get(currentSearchIndex);
            int end = start + searchQueryEditText.getText().toString().length();
            if (start >= 0 && end <= text.length()) {
                text.setSpan(new BackgroundColorSpan(Color.YELLOW), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                htmlEditText.setSelection(start, end);
            }
        }
    }

    private void moveToNextSearchMatch() {
        if (!searchMatchPositions.isEmpty()) {
            currentSearchIndex = (currentSearchIndex + 1) % searchMatchPositions.size();
            highlightCurrentSearchMatch();
        }
    }

    private void moveToPreviousSearchMatch() {
        if (!searchMatchPositions.isEmpty()) {
            currentSearchIndex = (currentSearchIndex - 1 + searchMatchPositions.size()) % searchMatchPositions.size();
            highlightCurrentSearchMatch();
        }
    }

    private void fetchHtml(final String urlString) {
        isLoading = true;
        executor.execute(() -> {
            final StringBuilder result = new StringBuilder();
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setUseCaches(false);
                connection.setInstanceFollowRedirects(true);
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line).append('\n');
                }
                reader.close();
                uiHandler.post(() -> {
                    originalHtml = result.toString();
                    isEditing = false;
                    editHistory.clear();
                    Editable editable = htmlEditText.getText();
                    editable.clear();
                    editable.append(originalHtml);
                    executor.execute(() -> {
                        final int[][] spans = getHighlightSpans(originalHtml);
                        uiHandler.post(() -> {
                            applyHighlight(htmlEditText.getText(), spans);
                            htmlEditText.setKeyListener(null);
                            isLoading = false;
                        });
                    });
                });
            } catch (final Exception e) {
                e.printStackTrace();
                uiHandler.post(() -> {
                    Toast.makeText(htmlview.this, "HTMLの取得に失敗しました: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    isLoading = false;
                });
            }
        });
    }

    private void saveHtmlToFile() {
        final String currentText = htmlEditText.getText().toString();
        final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        final String fileName = timeStamp + (!currentText.equals(originalHtml) ? "Edit.html" : ".html");
        executor.execute(() -> {
            try {
                File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File file = new File(downloadDir, fileName);
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
                bos.write(currentText.getBytes(StandardCharsets.UTF_8));
                bos.flush();
                bos.close();
                uiHandler.post(() -> Toast.makeText(htmlview.this, "保存しました: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show());
            } catch (final Exception e) {
                e.printStackTrace();
                uiHandler.post(() -> Toast.makeText(htmlview.this, "保存に失敗しました: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }
    private void saveHtmlToUri(final Uri uri) {
        final String content = htmlEditText.getText().toString();
        executor.execute(() -> {
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                if (out == null) throw new Exception("OutputStreamが取得できません");
                out.write(content.getBytes(StandardCharsets.UTF_8));
                out.flush();
                uiHandler.post(() -> Toast.makeText(this, "保存しました", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                uiHandler.post(() -> Toast.makeText(this, "保存に失敗: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private int[][] getHighlightSpans(String text) {
        ArrayList<int[]> spans = new ArrayList<>();
        Matcher tagMatcher = TAG_PATTERN.matcher(text);
        while (tagMatcher.find()) {
            int tagStart = tagMatcher.start();
            int tagEnd = tagMatcher.end();
            spans.add(new int[]{tagStart, tagEnd, TAG_COLOR});
            String tagText = text.substring(tagStart, tagEnd);
            Matcher attrMatcher = ATTR_PATTERN.matcher(tagText);
            while (attrMatcher.find()) {
                spans.add(new int[]{tagStart + attrMatcher.start(1), tagStart + attrMatcher.end(1), ATTRIBUTE_COLOR});
                spans.add(new int[]{tagStart + attrMatcher.start(2), tagStart + attrMatcher.end(2), VALUE_COLOR});
            }
        }
        int[][] result = new int[spans.size()][3];
        for (int i = 0; i < spans.size(); i++) result[i] = spans.get(i);
        return result;
    }

    private void applyHighlight(Editable editable, int[][] spans) {
        Object[] oldSpans = editable.getSpans(0, editable.length(), ForegroundColorSpan.class);
        for (Object span : oldSpans) editable.removeSpan(span);
        for (int[] span : spans) {
            if (span.length == 3) {
                int start = span[0];
                int end = span[1];
                int color = span[2];
                if (start >= 0 && end <= editable.length() && start < end) {
                    editable.setSpan(new ForegroundColorSpan(color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_WRITE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveHtmlToFile();
            } else {
                Toast.makeText(this, "書き込み権限が必要です", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_HTML && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) readHtmlFromUri(uri);
        } else if (requestCode == REQUEST_CODE_CREATE_FILE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                saveHtmlToUri(uri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void readHtmlFromUri(final Uri uri) {
        isLoading = true;
        executor.execute(() -> {
            final StringBuilder sb = new StringBuilder();
            try {
                ContentResolver resolver = getContentResolver();
                try (InputStream in = resolver.openInputStream(uri)) {
                    if (in == null) throw new Exception("InputStreamが取得できません");
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line).append('\n');
                    }
                }
                uiHandler.post(() -> {
                    originalHtml = sb.toString();
                    isEditing = false;
                    editHistory.clear();
                    Editable editable = htmlEditText.getText();
                    editable.clear();
                    editable.append(originalHtml);
                    executor.execute(() -> {
                        final int[][] spans = getHighlightSpans(originalHtml);
                        uiHandler.post(() -> {
                            applyHighlight(htmlEditText.getText(), spans);
                            htmlEditText.setKeyListener(null);
                            isLoading = false;
                        });
                    });
                });
            } catch (final Exception e) {
                e.printStackTrace();
                uiHandler.post(() -> {
                    Toast.makeText(htmlview.this, "HTMLファイルの読み込みに失敗しました: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    isLoading = false;
                });
            }
        });
    }
}