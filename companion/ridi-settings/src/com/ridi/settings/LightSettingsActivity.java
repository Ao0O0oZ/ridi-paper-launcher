package com.ridi.settings;

import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.view.View;

public final class LightSettingsActivity extends BaseActivity {
    private static final String FRONT_LIGHT_MODE = "front_light_mode";
    private Switch toggle;
    private SeekBar brightness;
    private TextView value;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        title("背光设置");
        toggle = new Switch(this);
        toggle.setText("开启前光");
        toggle.setTextSize(19);
        content.addView(toggle, full(dp(54)));
        value = note("");
        brightness = new SeekBar(this);
        brightness.setMax(244);
        content.addView(brightness, full(dp(58)));
        note("调节范围 11–255；关闭背光时会保留当前亮度。");

        int current = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 100);
        brightness.setProgress(Math.max(0, current - 11));
        toggle.setChecked(Settings.System.getInt(getContentResolver(), FRONT_LIGHT_MODE, 0) != 0);
        updateLabel(current);

        toggle.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { applyLight(); }
        });
        brightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateLabel(progress + 11);
                if (fromUser && toggle.isChecked()) setBrightness(progress + 11);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                if (toggle.isChecked()) setBrightness(seekBar.getProgress() + 11);
            }
        });
    }

    private void applyLight() {
        ContentResolver resolver = getContentResolver();
        Intent intent = new Intent("android.intent.action.FL_SW_CHANGED");
        if (toggle.isChecked()) {
            Settings.System.putInt(resolver, FRONT_LIGHT_MODE, 1);
            setBrightness(brightness.getProgress() + 11);
            intent.putExtra("FL_STATE", "ON");
        } else {
            Settings.System.putInt(resolver, FRONT_LIGHT_MODE, 0);
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, 0);
            intent.putExtra("FL_STATE", "OFF");
        }
        sendBroadcast(intent);
    }

    private void setBrightness(int amount) {
        Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, amount);
    }

    private void updateLabel(int amount) { value.setText("亮度：" + amount + " / 255"); }
}
