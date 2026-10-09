package com.ridi.bottombar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;

final class StatusIconView extends View {
    static final int BACK = 1;
    static final int WIFI = 3;
    static final int BATTERY = 4;
    static final int HOME = 5;
    static final int REFRESH = 6;

    private final int type;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Bitmap[] icons;
    private final Rect[] iconBounds;
    private int level;
    private boolean active;
    private boolean charging;

    StatusIconView(Context context, int type) {
        super(context);
        this.type = type;
        int[] resources = resourcesFor(type);
        icons = new Bitmap[resources.length];
        iconBounds = new Rect[resources.length];
        for (int i = 0; i < resources.length; i++) {
            icons[i] = decodeIcon(resources[i]);
            iconBounds[i] = visibleBounds(icons[i]);
        }
        setBackgroundColor(Color.WHITE);
    }

    void setState(boolean active) {
        this.active = active;
        invalidate();
    }

    void setLevel(int level) {
        this.level = Math.max(0, Math.min(100, level));
        invalidate();
    }

    void setBattery(int level, boolean charging) {
        this.level = Math.max(0, Math.min(100, level));
        this.charging = charging;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int index = iconIndex();
        if (index < 0 || index >= icons.length || icons[index] == null) return;

        if (type == BATTERY) {
            float iconSize = Math.min(d(25), getHeight() * 0.58f);
            float totalWidth = iconSize + d(25);
            float left = (getWidth() - totalWidth) * 0.5f + d(4);
            drawBitmap(canvas, index, left, (getHeight() - iconSize) * 0.5f, iconSize, iconSize);
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setTextSize(d(9.5f));
            canvas.drawText(level + "%", left + iconSize + d(2), getHeight() * 0.62f, paint);
        } else {
            float iconSize = Math.min(d(29), getHeight() * 0.70f);
            drawBitmap(canvas, index, (getWidth() - iconSize) * 0.5f,
                    (getHeight() - iconSize) * 0.5f, iconSize, iconSize);
        }
    }

    private int iconIndex() {
        if (type == WIFI) {
            if (!active) return 0;
            return Math.max(1, Math.min(3, level + 1));
        }
        if (type == BATTERY) {
            if (charging) return 5;
            return Math.max(0, Math.min(4, (Math.max(1, level) - 1) / 20));
        }
        return 0;
    }

    private int[] resourcesFor(int iconType) {
        switch (iconType) {
            case BACK:
                return new int[] {R.drawable.nav_return};
            case HOME:
                return new int[] {R.drawable.nav_home};
            case REFRESH:
                return new int[] {R.drawable.nav_refresh};
            case WIFI:
                return new int[] {R.drawable.nav_wifi_off, R.drawable.nav_wifi_1,
                        R.drawable.nav_wifi_2, R.drawable.nav_wifi_3};
            case BATTERY:
                return new int[] {R.drawable.nav_battery_1, R.drawable.nav_battery_2,
                        R.drawable.nav_battery_3, R.drawable.nav_battery_4,
                        R.drawable.nav_battery_5, R.drawable.nav_battery_charging};
            default:
                return new int[0];
        }
    }

    private void drawBitmap(Canvas canvas, int index, float left, float top,
                            float width, float height) {
        Rect source = iconBounds[index];
        float sourceRatio = source.width() / (float) Math.max(1, source.height());
        float boxRatio = width / Math.max(1f, height);
        float drawWidth = width;
        float drawHeight = height;
        if (sourceRatio > boxRatio) {
            drawHeight = width / sourceRatio;
        } else {
            drawWidth = height * sourceRatio;
        }
        float drawLeft = left + (width - drawWidth) * 0.5f;
        float drawTop = top + (height - drawHeight) * 0.5f;
        RectF destination = new RectF(drawLeft, drawTop,
                drawLeft + drawWidth, drawTop + drawHeight);
        canvas.drawBitmap(icons[index], source, destination, paint);
    }

    private Rect visibleBounds(Bitmap bitmap) {
        if (bitmap == null) return new Rect(0, 0, 1, 1);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        int left = width;
        int top = height;
        int right = -1;
        int bottom = -1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if ((pixels[y * width + x] >>> 24) > 12) {
                    if (x < left) left = x;
                    if (x > right) right = x;
                    if (y < top) top = y;
                    if (y > bottom) bottom = y;
                }
            }
        }
        if (right < left || bottom < top) return new Rect(0, 0, width, height);
        int padding = Math.max(2, Math.min(width, height) / 40);
        return new Rect(Math.max(0, left - padding), Math.max(0, top - padding),
                Math.min(width, right + padding + 1), Math.min(height, bottom + padding + 1));
    }

    private Bitmap decodeIcon(int resource) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeResource(getResources(), resource, options);
    }

    private float d(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
