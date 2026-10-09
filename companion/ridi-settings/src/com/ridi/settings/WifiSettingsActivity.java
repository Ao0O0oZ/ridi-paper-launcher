package com.ridi.settings;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class WifiSettingsActivity extends BaseActivity {
    private WifiManager wifi;
    private Switch toggle;
    private TextView status;
    private ArrayAdapter<String> adapter;
    private final List<ScanResult> results = new ArrayList<ScanResult>();
    private boolean updating;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            updateState();
            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) loadResults();
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        wifi = (WifiManager) getSystemService(WIFI_SERVICE);
        title("Wi-Fi 设置");
        toggle = new Switch(this);
        toggle.setText("Wi-Fi");
        toggle.setTextSize(19);
        content.addView(toggle, full(dp(54)));
        status = note("");
        Button scan = new Button(this);
        scan.setText("扫描附近网络");
        scan.setTextSize(17);
        scan.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                if (wifi != null && wifi.isWifiEnabled()) wifi.startScan();
            }
        });
        content.addView(scan, full(dp(52)));

        ListView list = new ListView(this);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                new ArrayList<String>());
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < results.size()) chooseNetwork(results.get(position));
            }
        });
        content.addView(list, full(dp(620)));
        toggle.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                if (!updating && wifi != null) wifi.setWifiEnabled(toggle.isChecked());
            }
        });
    }

    @Override protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(receiver, filter);
        updateState();
        if (wifi != null && wifi.isWifiEnabled()) wifi.startScan();
    }

    @Override protected void onPause() {
        try { unregisterReceiver(receiver); } catch (Exception ignored) { }
        super.onPause();
    }

    private void updateState() {
        if (wifi == null) return;
        updating = true;
        toggle.setChecked(wifi.isWifiEnabled());
        updating = false;
        WifiInfo info = wifi.getConnectionInfo();
        String name = info == null ? null : cleanSsid(info.getSSID());
        status.setText(wifi.isWifiEnabled()
                ? (info != null && info.getNetworkId() != -1 ? "已连接：" + name : "Wi-Fi 已开启")
                : "Wi-Fi 已关闭");
    }

    private void loadResults() {
        if (wifi == null) return;
        List<ScanResult> scans = wifi.getScanResults();
        if (scans == null) return;
        Collections.sort(scans, new Comparator<ScanResult>() {
            @Override public int compare(ScanResult a, ScanResult b) { return b.level - a.level; }
        });
        results.clear();
        adapter.clear();
        Set<String> seen = new HashSet<String>();
        for (ScanResult result : scans) {
            if (result.SSID == null || result.SSID.length() == 0 || seen.contains(result.SSID)) continue;
            seen.add(result.SSID);
            results.add(result);
            int bars = WifiManager.calculateSignalLevel(result.level, 4);
            adapter.add(result.SSID + "    " + signalText(bars) + securityText(result.capabilities));
        }
        adapter.notifyDataSetChanged();
    }

    private void chooseNetwork(final ScanResult result) {
        WifiConfiguration saved = findSaved(result.SSID);
        if (saved != null) {
            wifi.disconnect();
            wifi.enableNetwork(saved.networkId, true);
            wifi.reconnect();
            status.setText("正在连接：" + result.SSID);
            return;
        }
        boolean secured = result.capabilities.contains("WEP") || result.capabilities.contains("WPA");
        if (!secured) {
            connect(result, "");
            return;
        }
        final EditText password = new EditText(this);
        password.setHint("密码");
        password.setSingleLine(true);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        new AlertDialog.Builder(this).setTitle(result.SSID).setView(password)
                .setPositiveButton("连接", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        connect(result, password.getText().toString());
                    }
                }).setNegativeButton("取消", null).show();
    }

    private void connect(ScanResult result, String password) {
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = quote(result.SSID);
        if (result.capabilities.contains("WEP")) {
            config.wepKeys[0] = quote(password);
            config.wepTxKeyIndex = 0;
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
        } else if (result.capabilities.contains("WPA")) {
            config.preSharedKey = quote(password);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        } else {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        }
        int networkId = wifi.addNetwork(config);
        if (networkId >= 0) {
            wifi.saveConfiguration();
            wifi.disconnect();
            wifi.enableNetwork(networkId, true);
            wifi.reconnect();
            status.setText("正在连接：" + result.SSID);
        } else {
            status.setText("无法保存该网络，请检查密码后重试");
        }
    }

    private WifiConfiguration findSaved(String ssid) {
        List<WifiConfiguration> saved = wifi.getConfiguredNetworks();
        if (saved == null) return null;
        for (WifiConfiguration config : saved) {
            if (ssid.equals(cleanSsid(config.SSID))) return config;
        }
        return null;
    }

    private String quote(String value) { return "\"" + value + "\""; }
    private String cleanSsid(String value) {
        if (value == null) return "未知网络";
        return value.replace("\"", "");
    }
    private String signalText(int bars) {
        if (bars >= 3) return "信号强";
        if (bars == 2) return "信号中";
        return "信号弱";
    }
    private String securityText(String caps) {
        return (caps.contains("WEP") || caps.contains("WPA")) ? "  [加密]" : "  [开放]";
    }
}
