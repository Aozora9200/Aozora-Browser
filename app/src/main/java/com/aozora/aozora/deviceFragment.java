package com.aozora.aozora;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class deviceFragment extends PreferenceFragment {

    private BroadcastReceiver batteryReceiver, Battery;
    private BroadcastReceiver networkReceiver;
    private Preference networkPref;
    private ConnectivityManager connectivityManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.devicestatus);
        Preference openActivityPreference = findPreference("getpropinfo");
        if (openActivityPreference != null) {
            openActivityPreference.setOnPreferenceClickListener(preference -> {
                showDeviceInfo();
                return true;
            });
        }
        // 対象のPreferenceを取得
        Preference batterystatus = findPreference("batterystatus");
        if (batterystatus != null) {
            // summaryを変更
            // バッテリー残量監視
            batteryReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                    String chargeStatus;
                    if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
                        chargeStatus = "充電中";
                    } else if (status == BatteryManager.BATTERY_STATUS_FULL) {
                        chargeStatus = "充電完了";
                    } else {
                        chargeStatus = "放電中";
                    }

                    batterystatus.setSummary(chargeStatus);
                }
            };
        }

        // 対象のPreferenceを取得
        Preference battery = findPreference("battery");
        if (battery != null) {
            // summaryを変更
            // バッテリー残量監視
            Battery = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                    int batteryPct = (int) ((level / (float) scale) * 100);

                    battery.setSummary(batteryPct + "%");
                }
            };
        }

        networkPref = findPreference("networkstatus");
        connectivityManager =
                (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        // 初回表示
        updateNetworkSummary();

        networkReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateNetworkSummary();
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        if (batteryReceiver != null) {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            getActivity().registerReceiver(batteryReceiver, ifilter);
        }
        if (Battery != null) {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            getActivity().registerReceiver(Battery, ifilter);
        }
        // コールバック登録
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        getActivity().registerReceiver(networkReceiver, filter);
        // 念のため即更新
        updateNetworkSummary();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (batteryReceiver != null) {
            getActivity().unregisterReceiver(batteryReceiver);
        }
        if (Battery != null) {
            getActivity().unregisterReceiver(Battery);
        }
        getActivity().unregisterReceiver(networkReceiver);
    }

    private void updateNetworkSummary() {
        if (networkPref == null || connectivityManager == null) return;

        String status = getConnectionType(getActivity());
        networkPref.setSummary(status);
    }

    private String getConnectionType(Context context) {
        Network network = connectivityManager.getActiveNetwork();
        if (network == null) return "未接続";

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        if (capabilities == null) return "不明";

        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return "Wi-Fi";
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            return "モバイルデータ";
        } else {
            return "その他";
        }
    }

    private void showDeviceInfo() {
        SpannableStringBuilder result = new SpannableStringBuilder();
        try {
            Process process = Runtime.getRuntime().exec("getprop");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            String[][] props = {
                    {"ro.product.model", "Model"},
                    {"ro.product.manufacturer", "Manufacturer"},
                    {"ro.product.brand", "Carrier"},
                    {"ro.system.build.id", "Build Id"},
                    {"ro.system.build.version.release", "OS Version"},
                    {"ro.vndk.version", "VNDK"},
                    {"ro.system.build.version.sdk", "SDK"},
                    {"ro.hardware", "SOC"},
                    {"ro.build.type", "Build Type"},
                    {"ro.product.locale", "Language"},
                    {"ro.sf.lcd_density", "Density"},
                    {"ro.boot.baseband", "BaseBand"},
                    {"ro.boot.slot_suffix", "Slot"}
            };

            Map<String, String> propValues = new HashMap<>();

            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("[")) continue;
                int keyStart = line.indexOf('[') + 1;
                int keyEnd = line.indexOf(']');
                int valueStart = line.indexOf('[', keyEnd) + 1;
                int valueEnd = line.indexOf(']', valueStart);

                if (keyStart < 0 || keyEnd < 0 || valueStart < 0 || valueEnd < 0) continue;
                String key = line.substring(keyStart, keyEnd).trim();
                String value = line.substring(valueStart, valueEnd).trim();

                if (key.startsWith("ro.")) {
                    propValues.put(key, value);
                }
            }
            reader.close();

            for (String[] prop : props) {
                String label = prop[1] + "  ";
                String val = propValues.getOrDefault(prop[0], "不明");

                result.append(label);
                int start = result.length();
                result.append(val).append("\n");
                int end = result.length();
                result.setSpan(
                        new ForegroundColorSpan(0xFF448AFF),
                        start, end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
        } catch (Exception e) {
            result.append("取得失敗");
        }
        new AlertDialog.Builder(getActivity())
                .setTitle("端末情報")
                .setMessage(result.toString())
                .setPositiveButton("OK", (dialog, which) -> {
                })
                .show();
    }
}
