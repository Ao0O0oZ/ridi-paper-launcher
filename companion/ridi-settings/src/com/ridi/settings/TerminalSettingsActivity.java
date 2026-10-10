package com.ridi.settings;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TerminalSettingsActivity extends BaseActivity {
    private static final String PACKAGE = "jackpal.androidterm";
    private static final String PREFS = "/data/data/" + PACKAGE
            + "/shared_prefs/" + PACKAGE + "_preferences.xml";
    private Button apply;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        title("终端模拟器配色");
        note("将终端内容区设为纯白背景、默认文字设为黑色。保留工具栏；已打开的终端会关闭，重新打开后生效。");
        apply = action("应用白底黑字", TerminalSettingsActivity.class);
        apply.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { applyColors(); }
        });
    }

    private void applyColors() {
        apply.setEnabled(false);
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    File local = new File(getFilesDir(), "terminal_preferences.xml");
                    File updated = new File(getFilesDir(), "terminal_preferences_updated.xml");
                    runRoot("set -e; am force-stop " + PACKAGE + "; test -f " + PREFS
                            + "; cp " + PREFS + " " + local.getAbsolutePath()
                            + "; chmod 0644 " + local.getAbsolutePath());
                    String xml = read(local);
                    // Jackpal Terminal Emulator uses color scheme 0 for black on white.
                    Pattern color = Pattern.compile("<string\\s+name=\"color\"\\s*>[^<]*</string>");
                    Matcher match = color.matcher(xml);
                    if (match.find()) {
                        xml = match.replaceFirst("<string name=\"color\">0</string>");
                    } else if (xml.contains("</map>")) {
                        xml = xml.replace("</map>", "    <string name=\"color\">0</string>\n</map>");
                    } else if (xml.matches("(?s).*<map\\s*/>.*")) {
                        xml = xml.replaceFirst("<map\\s*/>", "<map>\n    <string name=\"color\">0</string>\n</map>");
                    } else {
                        throw new Exception("终端设置文件格式无法识别");
                    }
                    write(updated, xml);
                    runRoot("set -e; cp " + PREFS + " " + PREFS + ".ridi-backup"
                            + "; cat " + updated.getAbsolutePath() + " > " + PREFS + "; sync");
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            apply.setEnabled(true);
                            Toast.makeText(TerminalSettingsActivity.this,
                                    "已设置白底黑字，重新打开终端即可", Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (final Exception error) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            apply.setEnabled(true);
                            Toast.makeText(TerminalSettingsActivity.this,
                                    "设置失败：请先打开终端一次并确认已授予 root 权限（"
                                            + error.getMessage() + "）", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }, "terminal-colors").start();
    }

    private void runRoot(String command) throws Exception {
        Process process = Runtime.getRuntime().exec(new String[] {"su", "-c", command});
        int code = process.waitFor();
        if (code != 0) throw new Exception("root 命令返回 " + code);
    }

    private String read(File file) throws Exception {
        FileInputStream input = new FileInputStream(file);
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return output.toString("UTF-8");
        } finally {
            input.close();
        }
    }

    private void write(File file, String value) throws Exception {
        FileOutputStream output = new FileOutputStream(file);
        try {
            output.write(value.getBytes("UTF-8"));
        } finally {
            output.close();
        }
    }
}
