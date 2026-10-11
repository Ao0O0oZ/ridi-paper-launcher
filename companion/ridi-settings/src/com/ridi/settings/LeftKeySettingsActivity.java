package com.ridi.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public final class LeftKeySettingsActivity extends BaseActivity {
    private static final String[] ACTIONS = {"back", "home", "light", "wifi", "refresh"};
    private static final String[] LABELS = {"返回", "返回主页", "背光开关", "Wi-Fi 开关", "刷新屏幕"};

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        title("左侧实体按键设置");
        final SharedPreferences preferences = getSharedPreferences("left_key_settings", MODE_PRIVATE);
        String selected = preferences.getString("action", "home");
        RadioGroup choices = new RadioGroup(this);
        choices.setOrientation(RadioGroup.VERTICAL);
        for (int i = 0; i < ACTIONS.length; i++) {
            RadioButton button = new RadioButton(this);
            button.setId(i + 1);
            button.setText(LABELS[i]);
            button.setTextSize(21);
            button.setTextColor(Color.BLACK);
            button.setGravity(android.view.Gravity.CENTER_VERTICAL);
            choices.addView(button, new RadioGroup.LayoutParams(
                    RadioGroup.LayoutParams.MATCH_PARENT, dp(60)));
            if (ACTIONS[i].equals(selected)) choices.check(i + 1);
        }
        content.addView(choices, full(android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
        choices.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(RadioGroup group, int checkedId) {
                int index = checkedId - 1;
                if (index < 0 || index >= ACTIONS.length) return;
                String action = ACTIONS[index];
                preferences.edit().putString("action", action).commit();
                Intent apply = new Intent("com.ridi.bottombar.APPLY_SETTINGS");
                apply.setClassName("com.ridi.bottombar", "com.ridi.bottombar.SettingsReceiver");
                apply.putExtra("left_key_action", action);
                sendBroadcast(apply);
            }
        });
    }
}
