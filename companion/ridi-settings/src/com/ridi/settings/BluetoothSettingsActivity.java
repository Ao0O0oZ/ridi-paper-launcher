package com.ridi.settings;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class BluetoothSettingsActivity extends BaseActivity {
    private BluetoothAdapter bluetooth;
    private Switch toggle;
    private TextView status;
    private ArrayAdapter<String> adapter;
    private final List<BluetoothDevice> devices = new ArrayList<BluetoothDevice>();
    private final Set<String> addresses = new HashSet<String>();
    private boolean updating;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                addDevice(device);
            }
            updateState();
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        bluetooth = BluetoothAdapter.getDefaultAdapter();
        title("蓝牙设置");
        toggle = new Switch(this);
        toggle.setText("蓝牙");
        toggle.setTextSize(19);
        content.addView(toggle, full(dp(54)));
        status = note("");
        Button scan = new Button(this);
        scan.setText("扫描设备");
        scan.setTextSize(17);
        content.addView(scan, full(dp(52)));
        ListView list = new ListView(this);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                new ArrayList<String>());
        list.setAdapter(adapter);
        content.addView(list, full(dp(620)));

        toggle.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                if (updating || bluetooth == null) return;
                if (toggle.isChecked()) bluetooth.enable(); else bluetooth.disable();
            }
        });
        scan.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                reloadDevices();
                if (bluetooth != null && bluetooth.isEnabled()) bluetooth.startDiscovery();
            }
        });
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < devices.size()) showDeviceAction(devices.get(position));
            }
        });
    }

    @Override protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(receiver, filter);
        updateState();
        reloadDevices();
    }

    @Override protected void onPause() {
        try { unregisterReceiver(receiver); } catch (Exception ignored) { }
        if (bluetooth != null) bluetooth.cancelDiscovery();
        super.onPause();
    }

    private void updateState() {
        updating = true;
        boolean available = bluetooth != null;
        toggle.setEnabled(available);
        toggle.setChecked(available && bluetooth.isEnabled());
        updating = false;
        status.setText(!available ? "本机没有可用的蓝牙适配器"
                : bluetooth.isEnabled() ? (bluetooth.isDiscovering() ? "正在扫描…" : "蓝牙已开启")
                : "蓝牙已关闭");
    }

    private void reloadDevices() {
        devices.clear(); addresses.clear(); adapter.clear();
        if (bluetooth != null) {
            for (BluetoothDevice device : bluetooth.getBondedDevices()) addDevice(device);
        }
        adapter.notifyDataSetChanged();
    }

    private void addDevice(BluetoothDevice device) {
        if (device == null || addresses.contains(device.getAddress())) return;
        addresses.add(device.getAddress());
        devices.add(device);
        String name = device.getName() == null ? "未命名设备" : device.getName();
        String bond = device.getBondState() == BluetoothDevice.BOND_BONDED ? "已配对" : "未配对";
        adapter.add(name + "\n" + device.getAddress() + "  " + bond);
        adapter.notifyDataSetChanged();
    }

    private void showDeviceAction(final BluetoothDevice device) {
        final boolean bonded = device.getBondState() == BluetoothDevice.BOND_BONDED;
        new AlertDialog.Builder(this)
                .setTitle(device.getName() == null ? device.getAddress() : device.getName())
                .setMessage(bonded ? "是否取消配对？" : "是否与该设备配对？")
                .setPositiveButton(bonded ? "取消配对" : "配对", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        try {
                            if (bonded) {
                                Method remove = device.getClass().getMethod("removeBond");
                                remove.invoke(device);
                            } else {
                                device.createBond();
                            }
                        } catch (Exception error) {
                            status.setText("配对操作失败：" + error.getClass().getSimpleName());
                        }
                    }
                }).setNegativeButton("取消", null).show();
    }
}
