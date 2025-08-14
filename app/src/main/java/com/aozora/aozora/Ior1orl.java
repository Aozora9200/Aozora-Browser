package com.aozora.aozora;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.app.Activity;
import android.widget.Toast;

public class Ior1orl extends Activity {
    private EditText editText;
    private TextView textViewResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ior1orl);

        editText = findViewById(R.id.editIor1orl);
        Button buttonCheck = findViewById(R.id.buttonCheck);
        textViewResult = findViewById(R.id.textViewResult);

        buttonCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkCharacter();
            }
        });
        // Action Bar が表示されているか確認
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_ior1orl, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.ior1orl_close) {
            finish();
        }
        if (item.getItemId() == android.R.id.home) {
            finish(); // 前の画面に戻る
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkCharacter() {
        String input = editText.getText().toString().trim();
        if (input.length() != 1) {
            textViewResult.setText("1文字を入力してください");
            return;
        }

        char c = input.charAt(0);
        String result;

        switch (c) {
            case 'I':
                result = "アルファベットの大文字 I(ｱｲ) です";
                break;
            case 'l':
                result = "アルファベットの小文字 l(ｴﾙ) です";
                break;
            case '1':
                result = "数字の 1(ｲﾁ) です";
                break;
            default:
                result = "I, l, 1 のいずれかを入力してください";
        }

        textViewResult.setText(result);
    }
}
