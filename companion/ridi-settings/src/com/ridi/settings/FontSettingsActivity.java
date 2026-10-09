package com.ridi.settings;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;

public final class FontSettingsActivity extends BaseActivity {
    private Spinner chinese;
    private Spinner western;
    private SeekBar scale;
    private TextView scaleLabel;
    private TextView preview;
    private Button apply;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        title("字体设置");
        note("系统界面字体。中文与英文、数字可以分别选择。");

        note("中文字体");
        chinese = spinner(new String[] {"思源宋体（粗体）", "原系统中文字体"});
        note("英文与数字字体");
        western = spinner(new String[] {"Times New Roman（粗体）", "原系统字体"});
        chinese.setSelection(contains("/system/etc/fallback_fonts.xml", "RidiSourceHanSerifSCBold.otf") ? 0 : 1);
        western.setSelection(contains("/system/etc/system_fonts.xml", "RidiTimesNewRomanBold.ttf") ? 0 : 1);

        scaleLabel = note("");
        scale = new SeekBar(this);
        scale.setMax(12);
        Configuration config = getResources().getConfiguration();
        int initial = Math.round((config.fontScale - 0.8f) * 20f);
        scale.setProgress(Math.max(0, Math.min(12, initial)));
        content.addView(scale, full(dp(54)));
        preview = note("字号预览：中文 ABC 123");
        preview.setTextColor(Color.BLACK);
        updatePreview();
        scale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) { updatePreview(); }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });

        note("字号范围为 80%–140%。系统字体需要重启后生效；KOReader 书页字号由阅读器单独管理。");
        apply = action("保存字体与字号并重启", FontSettingsActivity.class);
        apply.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { confirmApply(); }
        });
    }

    private Spinner spinner(String[] choices) {
        Spinner result = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, choices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        result.setAdapter(adapter);
        content.addView(result, full(dp(56)));
        return result;
    }

    private int percent() { return 80 + scale.getProgress() * 5; }

    private void updatePreview() {
        int value = percent();
        scaleLabel.setText("系统界面字号：" + value + "%");
        preview.setTextSize(18f * value / 100f);
    }

    private void confirmApply() {
        new AlertDialog.Builder(this).setTitle("应用字体设置")
                .setMessage("将保存所选字体及字号，随后重启电纸书。")
                .setNegativeButton("取消", null)
                .setPositiveButton("保存并重启", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) { applyChanges(); }
                }).show();
    }

    private void applyChanges() {
        apply.setEnabled(false);
        final String systemAsset = western.getSelectedItemPosition() == 0
                ? "system_times_bold.xml" : "system_original.xml";
        final String fallbackAsset = chinese.getSelectedItemPosition() == 0
                ? "fallback_song_bold.xml" : "fallback_original.xml";
        final String fontScale = String.format(Locale.US, "%.2f", percent() / 100f);
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    File dir = getFilesDir();
                    File system = copyAsset(systemAsset, new File(dir, "system_fonts.xml"));
                    File fallback = copyAsset(fallbackAsset, new File(dir, "fallback_fonts.xml"));
                    String shell = "set -e\n"
                            + "mkdir -p /data/local/tmp/ridi-font-settings\n"
                            + "cp /system/etc/system_fonts.xml /data/local/tmp/ridi-font-settings/system_fonts.previous.xml\n"
                            + "cp /system/etc/fallback_fonts.xml /data/local/tmp/ridi-font-settings/fallback_fonts.previous.xml\n"
                            + "mount -o remount,rw /system\n"
                            + "trap 'cp /data/local/tmp/ridi-font-settings/system_fonts.previous.xml /system/etc/system_fonts.xml; cp /data/local/tmp/ridi-font-settings/fallback_fonts.previous.xml /system/etc/fallback_fonts.xml; mount -o remount,ro /system' 0\n"
                            + "cp " + system.getAbsolutePath() + " /system/etc/system_fonts.xml.new\n"
                            + "cp " + fallback.getAbsolutePath() + " /system/etc/fallback_fonts.xml.new\n"
                            + "chmod 0644 /system/etc/system_fonts.xml.new /system/etc/fallback_fonts.xml.new\n"
                            + "chown root:root /system/etc/system_fonts.xml.new /system/etc/fallback_fonts.xml.new\n"
                            + "mv /system/etc/system_fonts.xml.new /system/etc/system_fonts.xml\n"
                            + "mv /system/etc/fallback_fonts.xml.new /system/etc/fallback_fonts.xml\n"
                            + "settings put system font_scale " + fontScale + "\n"
                            + "sync\n"
                            + "mount -o remount,ro /system\n"
                            + "trap - 0\n";
                    File script = new File(dir, "apply-fonts.sh");
                    FileOutputStream output = new FileOutputStream(script);
                    output.write(shell.getBytes("UTF-8"));
                    output.close();
                    Process process = Runtime.getRuntime().exec(new String[] {
                            "su", "-c", "sh " + script.getAbsolutePath()});
                    int code = process.waitFor();
                    if (code != 0) throw new Exception("写入失败，代码 " + code);
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            Toast.makeText(FontSettingsActivity.this, "设置已保存，正在重启", Toast.LENGTH_LONG).show();
                        }
                    });
                    Thread.sleep(600);
                    Runtime.getRuntime().exec(new String[] {"su", "-c", "reboot"});
                } catch (final Exception error) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            apply.setEnabled(true);
                            Toast.makeText(FontSettingsActivity.this,
                                    "字体设置失败：" + error.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }, "font-settings").start();
    }

    private File copyAsset(String name, File target) throws Exception {
        InputStream input = getAssets().open(name);
        FileOutputStream output = new FileOutputStream(target);
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        output.close();
        input.close();
        return target;
    }

    private boolean contains(String path, String needle) {
        try {
            InputStream input = new FileInputStream(path);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            input.close();
            return output.toString("UTF-8").contains(needle);
        } catch (Exception ignored) { return false; }
    }
}
