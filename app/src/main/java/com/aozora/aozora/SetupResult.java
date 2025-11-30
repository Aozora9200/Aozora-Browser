package com.aozora.aozora;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class SetupResult extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setupresult);
        TextView resultView = findViewById(R.id.resultsetup);

        Button startaozorabutton = findViewById(R.id.startaozora);
        startaozorabutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start();
            }
        });

        // 渡された速度を表示
        String speed = getIntent().getStringExtra("speed_result");
        resultView.setText("お使いの端末のダウンロード速度: " + speed);
    }
    private void start() {
        // セットアップ完了 → フラグ更新
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isFirstRun", false);
        editor.apply();
        startActivity(new Intent(this, BootingActivity.class));
        finish();
    }
}
