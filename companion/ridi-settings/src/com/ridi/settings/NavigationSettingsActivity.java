package com.ridi.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

public final class NavigationSettingsActivity extends BaseActivity {
    private static final String PREFS = "navigation_settings";
    private Switch enabled;
    private SeekBar height;
    private TextView value;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        title("底部导航栏设置");
        final SharedPreferences preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        enabled = new Switch(this);
        enabled.setText("显示底部导航栏");
        enabled.setTextSize(19);
        enabled.setChecked(preferences.getBoolean("enabled", true));
        content.addView(enabled, full(dp(54)));
        value = note("");
        height = new SeekBar(this);
        height.setMax(20);
        height.setProgress(preferences.getInt("height_dp", 46) - 38);
        content.addView(height, full(dp(58)));
        updateLabel(height.getProgress() + 38);
        note("输入法弹出时导航栏会自动隐藏，防止遮挡键盘；退出输入后自动恢复。");

        enabled.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { apply(); }
        });
        height.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateLabel(progress + 38);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { apply(); }
        });
    }

    private void apply() {
        int heightDp = height.getProgress() + 38;
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putBoolean("enabled", enabled.isChecked())
                .putInt("height_dp", heightDp).commit();
        Intent intent = new Intent("com.ridi.bottombar.APPLY_SETTINGS");
        intent.setClassName("com.ridi.bottombar", "com.ridi.bottombar.SettingsReceiver");
        intent.putExtra("enabled", enabled.isChecked());
        intent.putExtra("height_dp", heightDp);
        sendBroadcast(intent);
    }

    private void updateLabel(int heightDp) { value.setText("导航栏高度：" + heightDp + " dp"); }
}
