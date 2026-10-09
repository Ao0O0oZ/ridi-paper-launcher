package com.ridi.bottombar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public final class SettingsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences preferences = context.getSharedPreferences("bottom_bar", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        if (intent.hasExtra("enabled")) {
            editor.putBoolean(BottomBarService.PREF_ENABLED,
                    intent.getBooleanExtra("enabled", true));
        }
        if (intent.hasExtra("height_dp")) {
            int height = intent.getIntExtra("height_dp", 46);
            editor.putInt(BottomBarService.PREF_HEIGHT_DP,
                    Math.max(38, Math.min(58, height)));
        }
        editor.commit();
        Intent service = new Intent(context, BottomBarService.class);
        service.setAction(BottomBarService.ACTION_APPLY_SETTINGS);
        context.startService(service);
    }
}
