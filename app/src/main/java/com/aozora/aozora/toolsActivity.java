package com.aozora.aozora;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.app.Activity;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class toolsActivity extends Activity {
    Button toolsClose;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applySavedTheme();
        setContentView(R.layout.activity_tools);
        Background = findViewById(R.id.background);
        applySavedBackground();
        applyBackTheme();
        toolsClose     = findViewById(R.id.tools_close);

        toolsClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                overridePendingTransition(R.anim.no_animation,  R.anim.slide_out_down_low);
            }
        });

        //------------------------------------------------------------------------------------

        RecyclerView recycler = findViewById(R.id.popupRecycler);

        // --- 🔹 画面幅に応じて列数を動的計算 ---
        int columnWidthDp = 100; // 1アイテムの目安サイズ（変更OK）
        float density = getResources().getDisplayMetrics().density;
        int screenWidthPx = getResources().getDisplayMetrics().widthPixels;
        int columnWidthPx = (int) (columnWidthDp * density);
        int spanCount = Math.max(1, screenWidthPx / columnWidthPx); // 最低3列確保
        // --------------------------------------

        recycler.setLayoutManager(new GridLayoutManager(this, spanCount));

        // --- 以下、あなたの既存処理をそのまま維持 ---
        List<PopupMenuItem> items = new ArrayList<>();
        items.add(new PopupMenuItem(R.drawable.txttophoto, "文字を画像に変換"));
        items.add(new PopupMenuItem(R.drawable.photototxt, "画像を文字に変換"));
        items.add(new PopupMenuItem(R.drawable.covergloobus, "ページ保存ツール"));
        items.add(new PopupMenuItem(R.drawable.calculator, "電卓"));
        items.add(new PopupMenuItem(R.drawable.qr_545582, "QRコード化"));
        items.add(new PopupMenuItem(R.mipmap.ior1orl, "I or 1 or l"));
        items.add(new PopupMenuItem(R.mipmap.ic_launcher_settings, "exec"));
        items.add(new PopupMenuItem(R.drawable.search_page, "grep-i/md5"));
        items.add(new PopupMenuItem(R.drawable.html, "HTML エディタ"));
        items.add(new PopupMenuItem(R.drawable.notepad, "メモ帳"));
        items.add(new PopupMenuItem(R.drawable.camera, "QRコードのスキャン"));
        items.add(new PopupMenuItem(R.drawable.files, "ファイルマネージャ"));
        items.add(new PopupMenuItem(R.drawable.negapoji, "ネガポジ"));
        items.add(new PopupMenuItem(R.mipmap.translate, "翻訳"));
        items.add(new PopupMenuItem(R.drawable.picture, "スクリーンショット"));

        PopupMenuAdapterWhite adapter = new PopupMenuAdapterWhite(items, position -> {
            switch (position) {
                case 0:
                    new Handler().postDelayed(() -> {
                        startActivity(new Intent(toolsActivity.this, txtphoto.class));
                        overridePendingTransition(R.anim.slide_in_up_low, R.anim.no_animation);
                    }, 0);
                    break;
                case 1:
                    new Handler().postDelayed(() -> {
                        startActivity(new Intent(toolsActivity.this, asciiart.class));
                        overridePendingTransition(R.anim.slide_in_up_low, R.anim.no_animation);
                    }, 0);
                    break;
                case 2:
                    new Handler().postDelayed(() -> {
                        startActivity(new Intent(toolsActivity.this, pagedl.class));
                        overridePendingTransition(R.anim.slide_in_up_low, R.anim.no_animation);
                    }, 0);
                    break;
                case 3:
                    new Handler().postDelayed(() -> {
                        startActivity(new Intent(toolsActivity.this, Calculator.class));
                        overridePendingTransition(R.anim.slide_in_up_low, R.anim.no_animation);
                    }, 0);
                    break;
                case 4:
                    new Handler().postDelayed(() -> {
                        startActivity(new Intent(toolsActivity.this, QrCodeActivity.class));
                        overridePendingTransition(R.anim.slide_in_up_low, R.anim.no_animation);
                    }, 0);
                    break;
                case 5:
                    new Handler().postDelayed(() -> {
                        startActivity(new Intent(toolsActivity.this, Ior1orl.class));
                        overridePendingTransition(R.anim.slide_in_up_low, R.anim.no_animation);
                    }, 0);
                    break;
                case 6:
                    new Handler().postDelayed(() -> {
                        startActivity(new Intent(toolsActivity.this, exec.class));
                        overridePendingTransition(R.anim.slide_in_up_low, R.anim.no_animation);
                    }, 0);
                    break;
                case 7:
                    new Handler().postDelayed(() -> {
                        startActivity(new Intent(toolsActivity.this, grepmd5appActivity.class));
                        overridePendingTransition(R.anim.slide_in_up_low, R.anim.no_animation);
                    }, 0);
                    break;
                case 8:
                    new Handler().postDelayed(() -> {
                        startActivity(new Intent(toolsActivity.this, htmlview.class));
                        overridePendingTransition(R.anim.slide_in_up_low, R.anim.no_animation);
                    }, 0);
                    break;
                case 9:
                    new Handler().postDelayed(() -> {
                        startActivity(new Intent(toolsActivity.this, notepad.class));
                        overridePendingTransition(R.anim.slide_in_up_low, R.anim.no_animation);
                    }, 0);
                    break;
                case 10:
                    new Handler().postDelayed(() -> {
                        startActivity(new Intent(toolsActivity.this, QrCameraActivity.class));
                        overridePendingTransition(R.anim.slide_in_up_low, R.anim.no_animation);
                    }, 0);
                    break;
                case 11:
                    new Handler().postDelayed(() -> {
                        startActivity(new Intent(toolsActivity.this, FileManager.class));
                        overridePendingTransition(R.anim.slide_in_up_low, R.anim.no_animation);
                    }, 0);
                    break;
                case 12:
                    new Handler().postDelayed(() -> {
                        Intent result = new Intent();
                        result.putExtra("action", "negapoji");
                        setResult(RESULT_OK, result);
                        finish();
                        overridePendingTransition(R.anim.no_animation,  R.anim.fadeout);
                    }, 0);
                    break;
                case 13:
                    new Handler().postDelayed(() -> {
                        Intent result = new Intent();
                        result.putExtra("action", "translate");
                        setResult(RESULT_OK, result);
                        finish();
                        overridePendingTransition(R.anim.no_animation,  R.anim.fadeout);
                    }, 0);
                    break;
                case 14:
                    new Handler().postDelayed(() -> {
                        Intent result = new Intent();
                        result.putExtra("action", "screenshot");
                        setResult(RESULT_OK, result);
                        finish();
                        overridePendingTransition(R.anim.no_animation,  R.anim.fadeout);
                    }, 0);
                    break;
            }
            //currentPopupWindow.dismiss();
        });

        recycler.setAdapter(adapter);

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

    private void applySavedTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int theme = prefs.getInt(KEY_THEME, THEME_SYSTEM);
        int black = getResources().getColor(R. color. backgroundBlack);
        int white = getResources().getColor(R. color. backgroundWhite);

        switch (theme) {
            case THEME_LIGHT:
                setTheme(android.R.style.Theme_Holo_Light_NoActionBar);
                break;
            case THEME_DARK:
                setTheme(android.R.style.Theme_Holo_NoActionBar);
                break;
            case THEME_SYSTEM:
            default:
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
    public void onBackPressed() {
        finish(); // 前の画面に戻る
        overridePendingTransition(R.anim.no_animation,  R.anim.slide_out_down_low);
    }

}
