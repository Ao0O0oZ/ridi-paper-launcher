package com.ridi.settings;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

abstract class BaseActivity extends Activity {
    protected LinearLayout content;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.WHITE);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(18), dp(24), dp(64));
        scroll.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);
    }

    protected void title(String value) {
        TextView title = new TextView(this);
        title.setText(value);
        title.setTextSize(24);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(0, 0, 0, dp(14));
        content.addView(title, full(dp(58)));
    }

    protected TextView note(String value) {
        TextView note = new TextView(this);
        note.setText(value);
        note.setTextSize(16);
        note.setTextColor(Color.DKGRAY);
        note.setPadding(dp(4), dp(8), dp(4), dp(12));
        content.addView(note, full(LinearLayout.LayoutParams.WRAP_CONTENT));
        return note;
    }

    protected Button action(String label, final Class<?> target) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(18);
        button.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        button.setAllCaps(false);
        button.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                startActivity(new android.content.Intent(BaseActivity.this, target));
            }
        });
        LinearLayout.LayoutParams params = full(dp(60));
        params.setMargins(0, dp(4), 0, dp(4));
        content.addView(button, params);
        return button;
    }

    protected LinearLayout.LayoutParams full(int height) {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height);
    }

    protected int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
