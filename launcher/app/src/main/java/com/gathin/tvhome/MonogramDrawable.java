package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

/**
 * list_mode symbol for an app without a logo: a white ring with the first letter of its name in the middle, 36 dp
 * square (the size of the square logos, drawn without scaling). ShadowIcons gives it the same soft shadow as the logos.
 */
final class MonogramDrawable extends Drawable {
    private final String letter;
    private final int sizePx;
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);

    MonogramDrawable(String letter, float density) {
        this.letter = letter;
        this.sizePx = Math.round(ListMode.MONOGRAM_DP * density);
        ring.setColor(0xFFFFFFFF);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(ListMode.MONOGRAM_RING_DP * density);
        text.setColor(0xFFFFFFFF);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setTextSize(sizePx * 0.5f);
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        float cx = b.exactCenterX(), cy = b.exactCenterY();
        float radius = Math.min(b.width(), b.height()) / 2f - ring.getStrokeWidth() / 2f;
        canvas.drawCircle(cx, cy, radius, ring);
        // Centre the letter on the ring, not on its baseline.
        float baseline = cy - (text.ascent() + text.descent()) / 2f;
        canvas.drawText(letter, cx, baseline, text);
    }

    @Override public int getIntrinsicWidth() { return sizePx; }
    @Override public int getIntrinsicHeight() { return sizePx; }
    @Override public void setAlpha(int alpha) { ring.setAlpha(alpha); text.setAlpha(alpha); }
    @Override public void setColorFilter(ColorFilter filter) { ring.setColorFilter(filter); text.setColorFilter(filter); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
