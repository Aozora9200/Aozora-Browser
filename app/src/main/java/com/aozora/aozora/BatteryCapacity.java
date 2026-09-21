package com.aozora.aozora;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.BatteryManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Locale;

public class BatteryCapacity extends Activity {

    private TextView textHealthPercent;
    private ProgressBar progressHealth;
    private TextView textHealthChip;
    private TextView textDesignCapacity;
    private TextView textCurrentCapacity;

    private TextView labelLevel, valueLevel;
    private TextView labelVoltage, valueVoltage;
    private TextView labelTemperature, valueTemperature;
    private TextView labelTechnology, valueTechnology;
    private TextView labelStatus, valueStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battery_capacity);
        TouchEffectView.attach(getWindow());
        textHealthPercent = findViewById(R.id.text_health_percent);
        progressHealth = findViewById(R.id.progress_health);
        textHealthChip = findViewById(R.id.text_health_chip);
        textDesignCapacity = findViewById(R.id.text_design_capacity);
        textCurrentCapacity = findViewById(R.id.text_current_capacity);

        View rowLevel = findViewById(R.id.row_level);
        View rowVoltage = findViewById(R.id.row_voltage);
        View rowTemperature = findViewById(R.id.row_temperature);
        View rowTechnology = findViewById(R.id.row_technology);
        View rowStatus = findViewById(R.id.row_status);

        labelLevel = rowLevel.findViewById(R.id.text_label);
        valueLevel = rowLevel.findViewById(R.id.text_value);
        labelVoltage = rowVoltage.findViewById(R.id.text_label);
        valueVoltage = rowVoltage.findViewById(R.id.text_value);
        labelTemperature = rowTemperature.findViewById(R.id.text_label);
        valueTemperature = rowTemperature.findViewById(R.id.text_value);
        labelTechnology = rowTechnology.findViewById(R.id.text_label);
        valueTechnology = rowTechnology.findViewById(R.id.text_value);
        labelStatus = rowStatus.findViewById(R.id.text_label);
        valueStatus = rowStatus.findViewById(R.id.text_value);

        labelLevel.setText(R.string.label_level);
        labelVoltage.setText(R.string.label_voltage);
        labelTemperature.setText(R.string.label_temperature);
        labelTechnology.setText(R.string.label_technology);
        labelStatus.setText(R.string.label_status);

        findViewById(R.id.btn_refresh).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshBatteryInfo();
            }
        });

        refreshBatteryInfo();
    }

    private void refreshBatteryInfo() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, filter);
        if (batteryStatus == null) {
            return;
        }

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int voltageMv = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
        int temperatureTenths = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
        String technology = batteryStatus.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

        double currentCapacityMah = getCurrentCapacityMah(level, scale);
        double designCapacityMah = getDesignCapacityMah();

        updateHealthUi(currentCapacityMah, designCapacityMah);
        updateCapacityUi(currentCapacityMah, designCapacityMah);
        updateStatusRows(level, scale, voltageMv, temperatureTenths, technology, status);
    }

    /**
     * BatteryManager の CHARGE_COUNTER（残量の絶対値・µAh）と
     * 現在の充電率（EXTRA_LEVEL / EXTRA_SCALE）から、
     * 「満充電時に相当する容量」＝現在の推定最大容量を逆算する。
     */
    private double getCurrentCapacityMah(int level, int scale) {
        if (level <= 0 || scale <= 0) {
            return -1;
        }

        BatteryManager batteryManager = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
        if (batteryManager == null) {
            return -1;
        }

        int chargeCounterUah = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
        if (chargeCounterUah == Integer.MIN_VALUE || chargeCounterUah <= 0) {
            return -1;
        }

        double fraction = level / (double) scale;
        if (fraction <= 0) {
            return -1;
        }

        double chargeCounterMah = chargeCounterUah / 1000.0;
        return chargeCounterMah / fraction;
    }

    /**
     * 端末の出荷時（設計）容量を PowerProfile からリフレクションで取得する。
     * 端末によっては取得できない場合がある。
     */
    private double getDesignCapacityMah() {
        try {
            Class<?> powerProfileClass = Class.forName("com.android.internal.os.PowerProfile");
            Object powerProfile = powerProfileClass
                    .getConstructor(Context.class)
                    .newInstance(this);
            Object value = powerProfileClass
                    .getMethod("getAveragePower", String.class)
                    .invoke(powerProfile, "battery.capacity");
            if (value instanceof Double) {
                double capacity = (Double) value;
                return capacity > 0 ? capacity : -1;
            }
        } catch (Exception e) {
            // この端末では取得不可
        }
        return -1;
    }

    private void updateHealthUi(double currentCapacityMah, double designCapacityMah) {
        if (currentCapacityMah <= 0 || designCapacityMah <= 0) {
            textHealthPercent.setText("--%");
            progressHealth.setProgress(0);
            textHealthChip.setText(R.string.value_unavailable);
            tintChip(Color.parseColor("#9AA3AF"));
            return;
        }

        double healthRatio = (currentCapacityMah / designCapacityMah) * 100.0;
        int healthDisplay = (int) Math.round(healthRatio);
        int healthForBar = Math.max(0, Math.min(100, healthDisplay));

        textHealthPercent.setText(String.format(Locale.JAPAN, "%d%%", healthDisplay));
        progressHealth.setProgress(healthForBar);

        int color;
        String label;
        if (healthDisplay >= 80) {
            color = getColorCompat(R.color.health_good);
            label = "良好";
        } else if (healthDisplay >= 60) {
            color = getColorCompat(R.color.health_warn);
            label = "やや劣化";
        } else {
            color = getColorCompat(R.color.health_bad);
            label = "劣化が進行";
        }
        textHealthChip.setText(label);
        tintChip(color);
        progressHealth.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }

    private void tintChip(int color) {
        textHealthChip.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }

    private void updateCapacityUi(double currentCapacityMah, double designCapacityMah) {
        textDesignCapacity.setText(designCapacityMah > 0
                ? String.format(Locale.JAPAN, "%d mAh", Math.round(designCapacityMah))
                : getString(R.string.value_unavailable));

        textCurrentCapacity.setText(currentCapacityMah > 0
                ? String.format(Locale.JAPAN, "%d mAh", Math.round(currentCapacityMah))
                : getString(R.string.value_unavailable));
    }

    private void updateStatusRows(int level, int scale, int voltageMv, int temperatureTenths,
                                  String technology, int status) {
        valueLevel.setText((level >= 0 && scale > 0)
                ? String.format(Locale.JAPAN, "%d%%", Math.round(level * 100f / scale))
                : getString(R.string.value_unavailable));

        valueVoltage.setText(voltageMv > 0
                ? String.format(Locale.JAPAN, "%.2f V", voltageMv / 1000.0)
                : getString(R.string.value_unavailable));

        valueTemperature.setText(temperatureTenths > -1000
                ? String.format(Locale.JAPAN, "%.1f ℃", temperatureTenths / 10.0)
                : getString(R.string.value_unavailable));

        valueTechnology.setText((technology != null && !technology.isEmpty())
                ? technology
                : getString(R.string.value_unavailable));

        valueStatus.setText(mapStatus(status));
    }

    private String mapStatus(int status) {
        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING:
                return getString(R.string.status_charging);
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                return getString(R.string.status_discharging);
            case BatteryManager.BATTERY_STATUS_FULL:
                return getString(R.string.status_full);
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                return getString(R.string.status_not_charging);
            default:
                return getString(R.string.status_unknown);
        }
    }

    private int getColorCompat(int colorRes) {
        return getResources().getColor(colorRes);
    }

    @Override
    public void onBackPressed() {
        finish(); // 前の画面に戻る
        overridePendingTransition(R.anim.no_animation,  R.anim.slide_out_down_low);
    }
}
