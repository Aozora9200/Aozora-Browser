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

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
            @Override
            public void onChildViewAdded(View parent, View child) {
                String targetKey = "getpropinfo";
                Preference pref = findPreference("getpropinfo");
                if (pref != null && child.findViewById(android.R.id.title) != null) {
                    TextView title = (TextView) child.findViewById(android.R.id.title);
                    title.setTextColor(Color.GRAY);
                }
                if (pref != null && child.findViewById(android.R.id.summary) != null) {
                    TextView title = (TextView) child.findViewById(android.R.id.summary);
                    title.setTextColor(Color.GRAY);
                }

                // child が Preference の 1行分のレイアウト
                TextView title = (TextView) child.findViewById(android.R.id.title);
                TextView summary = (TextView) child.findViewById(android.R.id.summary);

                // タイトルが一致している場合に色を変える
                Preference targetPref = findPreference(targetKey);
                if (targetPref != null && title != null &&
                        targetPref.getTitle().equals(title.getText())) {
                    title.setTextColor(Color.WHITE);
                }

                if (targetPref != null && summary != null &&
                        targetPref.getSummary() != null &&
                        targetPref.getSummary().equals(summary.getText())) {
                    summary.setTextColor(Color.GRAY);
                }
            }

            @Override
            public void onChildViewRemoved(View parent, View child) {}
        });
    }

    private void showDeviceInfo() {
        StringBuilder result = new StringBuilder();
        try {
            Process process = Runtime.getRuntime().exec("getprop");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }
            reader.close();
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
