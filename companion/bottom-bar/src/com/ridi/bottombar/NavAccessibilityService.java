package com.ridi.bottombar;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.io.FileInputStream;
import java.io.IOException;

public final class NavAccessibilityService extends AccessibilityService {
    private static final String TAG = "RidiNavKeys";
    private static volatile NavAccessibilityService instance;
    private final Handler handler = new Handler();
    private volatile boolean listening;
    private FileInputStream keyStream;

    @Override
    protected void onServiceConnected() {
        instance = this;
        startLeftKeyListener();
        startService(new Intent(this, BottomBarService.class));
    }

    private void startLeftKeyListener() {
        if (listening) return;
        listening = true;
        Thread reader = new Thread(new Runnable() {
            @Override
            public void run() {
                // RIDI PAPER is 32-bit ARM: Linux input_event is 16 bytes.
                // /dev/input/event0 is world-readable on this firmware.
                byte[] event = new byte[16];
                while (listening) {
                    try {
                        keyStream = new FileInputStream("/dev/input/event0");
                        Log.i(TAG, "Listening to left physical key");
                        while (listening) {
                            int offset = 0;
                            while (offset < event.length) {
                                int count = keyStream.read(event, offset, event.length - offset);
                                if (count < 0) throw new IOException("Input stream closed");
                                offset += count;
                            }
                            int type = (event[8] & 255) | ((event[9] & 255) << 8);
                            int code = (event[10] & 255) | ((event[11] & 255) << 8);
                            int value = (event[12] & 255) | ((event[13] & 255) << 8)
                                    | ((event[14] & 255) << 16) | ((event[15] & 255) << 24);
                            if (type == 1 && code == 59 && value == 0) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        openHomeFromLeftKey();
                                    }
                                });
                            }
                        }
                    } catch (IOException error) {
                        if (listening) Log.e(TAG, "Left key listener failed", error);
                    } finally {
                        closeKeyStream();
                    }
                    if (listening) {
                        try { Thread.sleep(5000); } catch (InterruptedException ignored) { }
                    }
                }
            }
        }, "RidiLeftKey");
        reader.setDaemon(true);
        reader.start();
    }

    private void openHomeFromLeftKey() {
        Intent home = new Intent(Intent.ACTION_MAIN);
        home.addCategory(Intent.CATEGORY_HOME);
        home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            startActivity(home);
            Log.i(TAG, "Left key opened home");
        } catch (RuntimeException error) {
            Log.e(TAG, "Left key could not open home", error);
        }
    }

    private void closeKeyStream() {
        FileInputStream stream = keyStream;
        keyStream = null;
        if (stream != null) {
            try { stream.close(); } catch (IOException ignored) { }
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // The bar stays below the IME in the window stack. Accessibility focus
        // events do not reliably describe keyboard visibility in custom views.
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public boolean onUnbind(Intent intent) {
        listening = false;
        closeKeyStream();
        instance = null;
        return super.onUnbind(intent);
    }

    static boolean performBack() {
        NavAccessibilityService service = instance;
        return service != null && service.performGlobalAction(GLOBAL_ACTION_BACK);
    }
}
