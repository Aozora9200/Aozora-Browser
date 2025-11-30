package com.aozora.aozora;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.viewpager.widget.ViewPager;

public class SetupActivity extends Activity {

    private ViewPager viewPager;
    private Button btnPrev, btnNext;
    private SetupPagerAdapter adapter;
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_IMAGE_URI = "image_uri";

    // 表示するレイアウトを指定
    private int[] layouts = {
            R.layout.setup_page1,
            R.layout.setup_page2,
            R.layout.setup_page3,
            R.layout.setup_page4,
            R.layout.setup_page5
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        viewPager = findViewById(R.id.viewPager);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);

        adapter = new SetupPagerAdapter(this, layouts);
        viewPager.setAdapter(adapter);

        // 最初のページでは「戻る」を非表示
        btnPrev.setVisibility(View.INVISIBLE);

        btnPrev.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem() - 1;
            if (current >= 0) {
                viewPager.setCurrentItem(current);
            }
        });

        btnNext.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem() + 1;
            if (current < layouts.length) {
                viewPager.setCurrentItem(current);
            } else {
                // 最終ページ → 測定画面へ遷移
                startActivity(new Intent(this, SetupProgress.class));
                finish();
            }
        });

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
            @Override public void onPageScrollStateChanged(int state) {}

            @Override
            public void onPageSelected(int position) {
                // 戻るボタン制御
                btnPrev.setVisibility(position == 0 ? View.INVISIBLE : View.VISIBLE);
                // 最後のページでは「次へ」を「開始」に変更
                if (position == layouts.length - 1) {
                    btnNext.setText("開始");
                } else {
                    btnNext.setText("＞");
                }
            }
        });
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