package com.aozora.aozora;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import org.openjdk.tools.javac.comp.Check;

public class DebugActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);
        Button clearCache = findViewById(R.id.buttonCache);
        CheckBox securityAlert = findViewById(R.id.securityAlert);
        CheckBox acceptCookies = findViewById(R.id.acceptCookies);
        Button delCookie = findViewById(R.id.delcookie);
        CheckBox geoLocation = findViewById(R.id.geolocation);
        Button delgeolocation = findViewById(R.id.delgeolocation);
        CheckBox javaScript = findViewById(R.id.javascript);
        Button websiteSettings = findViewById(R.id.websiteSettings);
        Button geoSettings = findViewById(R.id.geoSettings);
        CheckBox PopupBlock = findViewById(R.id.popupblock);
        CheckBox TouchEffect = findViewById(R.id.touchEffect);
        Button license = findViewById(R.id.license);
        clearCache.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WebViewUtils.clearWebViewCache(DebugActivity.this);
                Toast.makeText(DebugActivity.this, "キャッシュを削除しました", Toast.LENGTH_SHORT).show();
            }
        });
        SharedPreferences setupprefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean issecurityAlert = setupprefs.getBoolean("securityAlert", true);
        if (issecurityAlert) {
            securityAlert.setChecked(true);
        } else {
            securityAlert.setChecked(false);
        }
        securityAlert.setOnClickListener(v -> {
            // チェックステータス取得
            boolean check = securityAlert.isChecked();
            if (check) {
                SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("securityAlert", true);
                editor.apply();
            } else {
                SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("securityAlert", false);
                editor.apply();
            }
        });

        boolean isacceptCookies = setupprefs.getBoolean("acceptCookies", true);
        if (isacceptCookies) {
            acceptCookies.setChecked(true);
        } else {
            acceptCookies.setChecked(false);
        }
        acceptCookies.setOnClickListener(v -> {
            // チェックステータス取得
            boolean check = acceptCookies.isChecked();
            if (check) {
                SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("acceptCookies", true);
                editor.apply();
            } else {
                SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("acceptCookies", false);
                editor.apply();
            }
        });
        delCookie.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(DebugActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Cookieをすべて消去")
                        .setMessage("すべてのCookieを削除しますか？")
                        .setPositiveButton("OK", (dialog, which) -> {
                            CookieManager cm = CookieManager.getInstance();
                            cm.removeAllCookie(); // API 19 では removeAllCookie を使用
                            CookieSyncManager.getInstance().sync(); // flush の代わりに CookieSyncManager を使用
                        })
                        .setNegativeButton("キャンセル", null)
                        .show();
            }
        });
        boolean isgeoLocation = setupprefs.getBoolean("geoLocation", true);
        if (isgeoLocation) {
            geoLocation.setChecked(true);
        } else {
            geoLocation.setChecked(false);
        }
        geoLocation.setOnClickListener(v -> {
            // チェックステータス取得
            boolean check = geoLocation.isChecked();
            if (check) {
                SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("geoLocation", true);
                editor.apply();
            } else {
                SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("geoLocation", false);
                editor.apply();
            }
        });
        delgeolocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(DebugActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("位置情報アクセスをクリア")
                        .setMessage("ウェブサイトの現在地情報へのアクセスをクリアしますか？")
                        .setPositiveButton("OK", (dialog, which) -> {
                            SharedPreferences geoPrefs = getSharedPreferences("GeoPermissionStore", MODE_PRIVATE);
                            geoPrefs.edit().clear().apply();
                        })
                        .setNegativeButton("キャンセル", null)
                        .show();
            }
        });
        boolean isjavaScript = setupprefs.getBoolean("javaScript", true);
        if (isjavaScript) {
            javaScript.setChecked(true);
        } else {
            javaScript.setChecked(false);
        }
        javaScript.setOnClickListener(v -> {
            // チェックステータス取得
            boolean check = javaScript.isChecked();
            if (check) {
                SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("javaScript", true);
                editor.apply();
            } else {
                SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("javaScript", false);
                editor.apply();
            }
        });
        websiteSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DebugActivity.this, WebSiteSettingsActivity.class);
                startActivity(intent);
            }
        });
        geoSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DebugActivity.this, geoSettingsActivity.class);
                startActivity(intent);
            }
        });
        boolean ispopupBlock = setupprefs.getBoolean("popupBlock", false);
        if (ispopupBlock) {
            PopupBlock.setChecked(false);
        } else {
            PopupBlock.setChecked(true);
        }
        PopupBlock.setOnClickListener(v -> {
            // チェックステータス取得
            boolean check = PopupBlock.isChecked();
            if (check) {
                SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("popupBlock", false);
                editor.apply();
            } else {
                SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("popupBlock", true);
                editor.apply();
            }
        });
        boolean istouchEffect = setupprefs.getBoolean("touchEffect", true);
        if (istouchEffect) {
            TouchEffect.setChecked(true);
        } else {
            TouchEffect.setChecked(false);
        }
        TouchEffect.setOnClickListener(v -> {
            // チェックステータス取得
            boolean check = TouchEffect.isChecked();
            if (check) {
                SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("touchEffect", true);
                editor.apply();
            } else {
                SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("touchEffect", false);
                editor.apply();
            }
        });
        license.setOnClickListener(v -> {

        });
    }
}
