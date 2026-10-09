package com.ridi.bottombar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (context.getSharedPreferences("bottom_bar", Context.MODE_PRIVATE)
                .getBoolean(BottomBarService.PREF_ENABLED, true)) {
            context.startService(new Intent(context, BottomBarService.class));
        }
    }
}
