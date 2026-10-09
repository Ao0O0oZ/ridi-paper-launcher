package com.ridi.bottombar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;

public final class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        startService(new Intent(this, BottomBarService.class));

        TextView message = new TextView(this);
        message.setTextSize(22);
        message.setPadding(40, 40, 40, 40);
        message.setText("底部导航栏已启动。\n\n日期格式：YYYY/MM/DD\n时间格式：HH:mm\n\n返回键服务：" +
                (Settings.Secure.getInt(getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1 ? "已启用" : "未启用"));
        setContentView(message);
    }
}
