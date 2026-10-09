package com.ridi.bottombar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

final class LightControlView extends View {
    interface Listener {
        void onToggle();
        void onDecrease();
        void onIncrease();
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Listener listener;
    private final Bitmap lightOn;
    private final Bitmap lightOff;
    private final Rect lightOnBounds;
    private final Rect lightOffBounds;
    private boolean active;
    private int percent;

    LightControlView(Context context, Listener listener) {
        super(context);
        this.listener = listener;
        lightOn = decodeIcon(R.drawable.nav_light_on);
        lightOff = decodeIcon(R.drawable.nav_light_off);
        lightOnBounds = visibleBounds(lightOn);
        lightOffBounds = visibleBounds(lightOff);
        setBackgroundColor(Color.WHITE);
        setClickable(true);
        setContentDescription("背光开关与亮度调节");
    }

    void setState(boolean active, int percent) {
        this.active = active;
        this.percent = Math.max(0, Math.min(100, percent));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cy = getHeight() * 0.5f;
        float groupWidth = Math.min(getWidth(), d(116));
        float groupLeft = (getWidth() - groupWidth) * 0.5f;
        float iconSize = Math.min(d(29), getHeight() * 0.70f);
        Bitmap icon = active ? lightOn : lightOff;
        Rect source = active ? lightOnBounds : lightOffBounds;
        float iconLeft = groupLeft + groupWidth * 0.02f;
        float sourceRatio = source.width() / (float) Math.max(1, source.height());
        float drawWidth = sourceRatio >= 1f ? iconSize : iconSize * sourceRatio;
        float drawHeight = sourceRatio >= 1f ? iconSize / sourceRatio : iconSize;
        canvas.drawBitmap(icon, source, new RectF(
                iconLeft + (iconSize - drawWidth) * 0.5f, cy - drawHeight / 2f,
                iconLeft + (iconSize + drawWidth) * 0.5f, cy + drawHeight / 2f), paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(d(20));
        canvas.drawText("−", groupLeft + groupWidth * 0.38f, cy + d(6.5f), paint);
        paint.setTextSize(d(14));
        canvas.drawText(percent + "%", groupLeft + groupWidth * 0.64f, cy + d(5), paint);
        paint.setTextSize(d(20));
        canvas.drawText("+", groupLeft + groupWidth * 0.91f, cy + d(6.5f), paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) return true;
        float part = event.getX() / Math.max(1f, getWidth());
        if (part < 0.31f) {
            listener.onToggle();
        } else if (part < 0.53f) {
            listener.onDecrease();
        } else if (part >= 0.78f) {
            listener.onIncrease();
        }
        performClick();
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private Rect visibleBounds(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        int left = width, top = height, right = -1, bottom = -1;
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
