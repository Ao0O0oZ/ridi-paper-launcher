package com.ridi.settings;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class InputSettingsActivity extends BaseActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        // Fullscreen windows are panned instead of resized on Android 4.4 when an IME opens.
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(Color.WHITE);
        page.setPadding(dp(24), dp(18), dp(24), dp(18));
        page.setFocusableInTouchMode(true);

        TextView heading = new TextView(this);
        heading.setText("输入法设置与测试");
        heading.setTextSize(24);
        heading.setTextColor(Color.BLACK);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(heading, full(dp(58)));

        TextView description = new TextView(this);
        description.setText("当前使用手心输入法。键盘弹出时，底部导航栏会位于键盘后方。");
        description.setTextSize(16);
        description.setTextColor(Color.DKGRAY);
        description.setPadding(dp(4), dp(8), dp(4), dp(12));
        page.addView(description, full(LinearLayout.LayoutParams.WRAP_CONTENT));

        Button choose = new Button(this);
        choose.setText("启用或切换输入法");
        choose.setTextSize(18);
        page.addView(choose, full(dp(56)));
        choose.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
            }
        });
        EditText test = new EditText(this);
        test.setHint("点击这里测试中文输入");
        test.setTextSize(19);
        test.setMinLines(4);
        test.setGravity(Gravity.TOP);
        page.addView(test, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(page);
        page.requestFocus();
    }
}
