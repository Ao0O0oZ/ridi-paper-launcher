package com.ridi.settings;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public final class MainActivity extends BaseActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        title("设置");
        note("针对 RIDI PAPER 与当前精简桌面优化");
        action("Wi-Fi 设置", WifiSettingsActivity.class);
        action("蓝牙设置", BluetoothSettingsActivity.class);
        action("背光设置", LightSettingsActivity.class);
        action("字体设置", FontSettingsActivity.class);
        Button lockScreen = action("锁屏背景设置", MainActivity.class);
        lockScreen.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                Intent intent = new Intent();
                intent.setClassName("com.android.settings",
                        "com.android.settings.standby.StandbySettingsActivity");
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException error) {
                    Toast.makeText(MainActivity.this, "此设备未提供独立锁屏背景设置", Toast.LENGTH_LONG).show();
                }
            }
        });
        action("终端模拟器配色", TerminalSettingsActivity.class);
        action("底部导航栏设置", NavigationSettingsActivity.class);
        action("输入法设置与测试", InputSettingsActivity.class);
        Button advanced = action("高级系统设置", MainActivity.class);
        advanced.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                Intent intent = new Intent(Settings.ACTION_SETTINGS);
                startActivity(intent);
            }
        });
        Button restartAdb = action("恢复无线 ADB 连接", MainActivity.class);
        restartAdb.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                Toast.makeText(MainActivity.this, "正在重启 ADB 服务，请稍后重新连接", Toast.LENGTH_LONG).show();
                new Thread(new Runnable() {
                    @Override public void run() {
                        try {
                            Process process = Runtime.getRuntime().exec(new String[] {
                                    "su", "-c", "setprop ctl.restart adbd"});
                            final int result = process.waitFor();
                            if (result != 0) {
                                runOnUiThread(new Runnable() {
                                    @Override public void run() {
                                        Toast.makeText(MainActivity.this,
                                                "ADB 服务重启失败（" + result + "）", Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        } catch (final Exception error) {
                            runOnUiThread(new Runnable() {
                                @Override public void run() {
                                    Toast.makeText(MainActivity.this,
                                            "ADB 服务重启失败：" + error.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                }, "adb-restart").start();
            }
        });
        note("无线 ADB 卡住时使用；仅重启调试服务，不重启电纸书。");
    }
}
