package com.ridi.bottombar;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class BottomBarService extends Service {
    private static final String TAG = "RidiBottomBar";
    static final String ACTION_APPLY_SETTINGS = "com.ridi.bottombar.APPLY_SETTINGS";
    static final String ACTION_BACK = "com.ridi.bottombar.LEFT_KEY_BACK";
    static final String ACTION_TOGGLE_LIGHT = "com.ridi.bottombar.LEFT_KEY_LIGHT";
    static final String ACTION_TOGGLE_WIFI = "com.ridi.bottombar.LEFT_KEY_WIFI";
    static final String ACTION_FULL_REFRESH = "com.ridi.bottombar.LEFT_KEY_REFRESH";
    private static final String FRONT_LIGHT_MODE = "front_light_mode";
    private static final String SCREEN_BRIGHTNESS = "screen_brightness";
    private static final String PREFS = "bottom_bar";
    private static final String LAST_BRIGHTNESS = "last_brightness";
    static final String PREF_ENABLED = "enabled";
    static final String PREF_HEIGHT_DP = "height_dp";
    private static final int[] BRIGHTNESS_LEVELS = {
            11, 25, 50, 75, 100, 125, 150, 175, 200, 225, 255
    };

    private final Handler handler = new Handler();
    private WindowManager windowManager;
    private View bar;
    private TextView dateTimeView;
    private StatusIconView batteryView;
    private StatusIconView wifiView;
    private LightControlView lightControlView;
    private Intent lastBatteryIntent;

    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action) || Intent.ACTION_SHUTDOWN.equals(action)) {
                hideBar();
                return;
            }
            if (Intent.ACTION_SCREEN_ON.equals(action) || Intent.ACTION_USER_PRESENT.equals(action)) {
                showBar();
                return;
            }
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                lastBatteryIntent = intent;
                updateBattery();
                return;
            }
            if (WifiManager.RSSI_CHANGED_ACTION.equals(action) ||
                    WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action) ||
                    WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                updateWifi();
                return;
            }
            updateDateTime();
        }
    };

    private final ContentObserver settingsObserver = new ContentObserver(handler) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            updateLightControls();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        applyGlobalStatusBarPolicy();
        registerStateReceiver();
        ContentResolver resolver = getContentResolver();
        resolver.registerContentObserver(Settings.System.getUriFor(FRONT_LIGHT_MODE), false,
                settingsObserver);
        resolver.registerContentObserver(Settings.System.getUriFor(SCREEN_BRIGHTNESS), false,
                settingsObserver);
        int current = Settings.System.getInt(resolver, SCREEN_BRIGHTNESS, 100);
        if (current > 0) {
            saveBrightness(current);
        }
        PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
        if (power == null || power.isScreenOn()) {
            showBar();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        if (ACTION_BACK.equals(action)) goBack();
        else if (ACTION_TOGGLE_LIGHT.equals(action)) toggleFrontLight();
        else if (ACTION_TOGGLE_WIFI.equals(action)) toggleWifi();
        else if (ACTION_FULL_REFRESH.equals(action)) requestFullRefresh();
        if (ACTION_APPLY_SETTINGS.equals(action)) {
            hideBar();
        }
        if (!isEnabled()) {
            hideBar();
            stopSelf();
            return START_NOT_STICKY;
        }
        PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
        if (power == null || power.isScreenOn()) {
            showBar();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        try {
            unregisterReceiver(stateReceiver);
        } catch (IllegalArgumentException ignored) {
        }
        getContentResolver().unregisterContentObserver(settingsObserver);
        hideBar();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void registerStateReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_SHUTDOWN);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        lastBatteryIntent = registerReceiver(stateReceiver, filter);
    }

    private void showBar() {
        if (!isEnabled()) return;
        if (bar != null) {
            updateAll();
            return;
        }
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        View topDivider = new View(this);
        topDivider.setBackgroundColor(Color.rgb(170, 170, 170));
        root.addView(topDivider, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundColor(Color.WHITE);
        row.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LOW_PROFILE |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        dateTimeView = textItem("", 17, 1.72f, false);
        row.addView(dateTimeView);

        batteryView = iconItem(StatusIconView.BATTERY, 0.72f, false, "电量");
        row.addView(batteryView);

        wifiView = iconItem(StatusIconView.WIFI, 0.50f, true, "Wi-Fi 开关和信号强度");
        wifiView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openWifiSettings();
            }
        });
        row.addView(wifiView);

        StatusIconView refresh = iconItem(StatusIconView.REFRESH, 0.50f, true, "全屏刷新");
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestFullRefresh();
            }
        });
        row.addView(refresh);

        StatusIconView home = iconItem(StatusIconView.HOME, 0.50f, true, "主页");
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goHome();
            }
        });
        row.addView(home);

        StatusIconView back = iconItem(StatusIconView.BACK, 0.50f, true, "返回");
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "back-click");
                goBack();
            }
        });
        row.addView(back);

        lightControlView = new LightControlView(this, new LightControlView.Listener() {
            @Override
            public void onToggle() {
                toggleFrontLight();
            }

            @Override
            public void onDecrease() {
                changeBrightness(-1);
            }

            @Override
            public void onIncrease() {
                changeBrightness(1);
            }
        });
        lightControlView.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, 1.45f));
        row.addView(lightControlView);

        int heightDp = barHeightDp();
        root.addView(row, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(heightDp - 1)));

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                dp(heightDp),
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_FULLSCREEN,
                PixelFormat.OPAQUE);
        params.gravity = Gravity.BOTTOM;
        params.setTitle("RIDI Bottom Bar");
        bar = root;
        windowManager.addView(bar, params);
        updateAll();
    }

    private void hideBar() {
        if (bar != null && windowManager != null) {
            windowManager.removeView(bar);
        }
        bar = null;
        dateTimeView = null;
        batteryView = null;
        wifiView = null;
        lightControlView = null;
    }

    private TextView textItem(String text, int sizeSp, float weight, boolean clickable) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(Color.BLACK);
        view.setTextSize(sizeSp);
        view.setGravity(Gravity.CENTER);
        view.setSingleLine(true);
        view.setBackgroundColor(Color.WHITE);
        view.setClickable(clickable);
        view.setPadding(dp(2), 0, dp(2), 0);
        view.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, weight));
        return view;
    }

    private StatusIconView iconItem(int type, float weight, boolean clickable, String description) {
        StatusIconView view = new StatusIconView(this, type);
        view.setClickable(clickable);
        view.setContentDescription(description);
        view.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, weight));
        return view;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private boolean isEnabled() {
        return getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(PREF_ENABLED, true);
    }

    private int barHeightDp() {
        int value = getSharedPreferences(PREFS, MODE_PRIVATE).getInt(PREF_HEIGHT_DP, 46);
        return Math.max(38, Math.min(58, value));
    }

    private void updateAll() {
        updateDateTime();
        updateBattery();
        updateWifi();
        updateLightControls();
    }

    private void updateDateTime() {
        if (dateTimeView != null) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd  HH:mm", Locale.getDefault());
            dateTimeView.setText(format.format(new Date()));
        }
    }

    private void updateBattery() {
        if (batteryView == null || lastBatteryIntent == null) {
            return;
        }
        int level = lastBatteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int scale = lastBatteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int status = lastBatteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS,
                BatteryManager.BATTERY_STATUS_UNKNOWN);
        int percent = scale > 0 ? Math.round(level * 100f / scale) : 0;
        boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;
        batteryView.setBattery(percent, charging);
    }

    private void updateWifi() {
        if (wifiView == null) {
            return;
        }
        WifiManager manager = (WifiManager) getSystemService(WIFI_SERVICE);
        boolean connected = false;
        int signal = 0;
        if (manager != null && manager.isWifiEnabled()) {
            WifiInfo info = manager.getConnectionInfo();
            if (info != null && info.getNetworkId() != -1) {
                connected = true;
                signal = WifiManager.calculateSignalLevel(info.getRssi(), 4);
            }
        }
        wifiView.setState(connected);
        wifiView.setLevel(signal);
    }

    private void openWifiSettings() {
        Intent intent = new Intent();
        intent.setClassName("com.ridi.settings", "com.ridi.settings.WifiSettingsActivity");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (Exception unavailable) {
            Intent fallback = new Intent(Settings.ACTION_WIFI_SETTINGS);
            fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(fallback);
        }
    }

    private void applyGlobalStatusBarPolicy() {
        try {
            Settings.Global.putString(getContentResolver(), "policy_control", "immersive.status=*");
        } catch (SecurityException error) {
            Log.w(TAG, "WRITE_SECURE_SETTINGS unavailable; policy is applied during installation");
        }
    }

    private boolean isFrontLightOn() {
        return Settings.System.getInt(getContentResolver(), FRONT_LIGHT_MODE, 0) != 0;
    }

    private int currentBrightness() {
        return Settings.System.getInt(getContentResolver(), SCREEN_BRIGHTNESS, 100);
    }

    private void saveBrightness(int value) {
        if (value > 0) {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putInt(LAST_BRIGHTNESS, value).apply();
        }
    }

    private int storedBrightness() {
        int current = currentBrightness();
        if (current > 0) {
            return current;
        }
        SharedPreferences preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        return preferences.getInt(LAST_BRIGHTNESS, 100);
    }

    private int nearestBrightnessIndex(int value) {
        int result = 0;
        int distance = Math.abs(value - BRIGHTNESS_LEVELS[0]);
        for (int i = 1; i < BRIGHTNESS_LEVELS.length; i++) {
            int candidate = Math.abs(value - BRIGHTNESS_LEVELS[i]);
            if (candidate < distance) {
                result = i;
                distance = candidate;
            }
        }
        return result;
    }

    private void updateLightControls() {
        if (lightControlView == null) {
            return;
        }
        boolean on = isFrontLightOn();
        int index = nearestBrightnessIndex(storedBrightness());
        lightControlView.setState(on, index * 10);
    }

    private void toggleFrontLight() {
        boolean turnOn = !isFrontLightOn();
        ContentResolver resolver = getContentResolver();
        Intent intent = new Intent("android.intent.action.FL_SW_CHANGED");
        if (turnOn) {
            int value = storedBrightness();
            Settings.System.putInt(resolver, FRONT_LIGHT_MODE, 1);
            intent.putExtra("FL_STATE", "ON");
            sendBroadcast(intent);
            Settings.System.putInt(resolver, SCREEN_BRIGHTNESS, value);
        } else {
            int value = currentBrightness();
            if (value > 0) {
                saveBrightness(value);
            }
            Settings.System.putInt(resolver, FRONT_LIGHT_MODE, 0);
            Settings.System.putInt(resolver, SCREEN_BRIGHTNESS, 0);
            intent.putExtra("FL_STATE", "OFF");
            sendBroadcast(intent);
        }
        updateLightControls();
    }

    private void changeBrightness(int delta) {
        int index = nearestBrightnessIndex(storedBrightness());
        index = Math.max(0, Math.min(BRIGHTNESS_LEVELS.length - 1, index + delta));
        int value = BRIGHTNESS_LEVELS[index];
        saveBrightness(value);
        if (isFrontLightOn()) {
            Settings.System.putInt(getContentResolver(), SCREEN_BRIGHTNESS, value);
        }
        updateLightControls();
    }

    private void goBack() {
        if (NavAccessibilityService.performBack()) {
            return;
        }
        try {
            Runtime.getRuntime().exec(new String[] {"su", "-c", "input keyevent 4"});
        } catch (Exception error) {
            Log.e(TAG, "Back action failed", error);
        }
    }

    private void toggleWifi() {
        WifiManager wifi = (WifiManager) getSystemService(WIFI_SERVICE);
        if (wifi == null) return;
        try {
            wifi.setWifiEnabled(!wifi.isWifiEnabled());
            updateWifi();
        } catch (SecurityException error) {
            Log.e(TAG, "Wi-Fi action failed", error);
        }
    }

    private void goHome() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        startActivity(intent);
    }

    /** Force a clean full-screen redraw through the RIDI IMX6 E-Ink View extension. */
    private void requestFullRefresh() {
        if (windowManager == null) {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        }
        if (windowManager == null) return;
        final View refreshLayer = new View(this);
        refreshLayer.setBackgroundColor(Color.TRANSPARENT);
        refreshLayer.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LOW_PROFILE |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        final WindowManager.LayoutParams refreshParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_FULLSCREEN,
                PixelFormat.TRANSLUCENT);
        refreshParams.gravity = Gravity.TOP | Gravity.LEFT;
        refreshParams.setTitle("RIDI Full Refresh");
        try {
            windowManager.addView(refreshLayer, refreshParams);
            requestEinkGc(refreshLayer);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        windowManager.removeView(refreshLayer);
                    } catch (Exception ignored) {
                    }
                    if (bar != null) bar.invalidate();
                }
            }, 350);
        } catch (Exception error) {
            Log.w(TAG, "Full refresh layer failed", error);
            if (bar != null) requestEinkGc(bar);
        }
    }

    private void requestEinkGc(View view) {
        try {
            int waveform = View.class.getField("EINK_WAVEFORM_MODE_GC16").getInt(null);
            int full = View.class.getField("EINK_UPDATE_MODE_FULL").getInt(null);
            int wait = View.class.getField("EINK_WAIT_MODE_WAIT").getInt(null);
            int mode = waveform | full | wait;
            View.class.getMethod("invalidate", Integer.TYPE).invoke(view, mode);
        } catch (Exception error) {
            view.invalidate();
        }
    }

    private void openDisplaySettings() {
        Intent intent = new Intent();
        intent.setClassName("com.example.ctm", "com.example.ctm.MainActivity");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (Exception error) {
            Log.w(TAG, "Display Settings is unavailable", error);
        }
    }
}
