package com.gathin.tvhome;

import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * list_mode's soft black shadow for things that are not text (list symbols, the line between the clock and the date, the
 * signs of the two round buttons): the bitmap with the shadow and the shape on top is made once, from the shape's own
 * alpha (extractAlpha with a BlurMaskFilter), and drawn centred on the view that holds it, past that view's edges.
 * The cost is ListMode.shadowBitmapBytes for each shape (ARGB_8888); nothing is made again per row or per frame.
 */
final class ShadowIcons {
    private ShadowIcons() { }

    /** A shape of this size ({@code w} x {@code h} px) with its shadow; {@code scale} is the density over 2.0. */
    static ShadowDrawable make(Drawable shadowShape, Drawable top, int w, int h, float scale) {
        int radius = Math.max(1, Math.round(ListMode.SHADOW_RADIUS_PX * scale));
        int dy = Math.round(ListMode.SHADOW_DY_PX * scale);
        int margin = ListMode.shadowMarginPx(radius, dy);
        int width = w + 2 * margin, height = h + 2 * margin;
        Bitmap out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        Bitmap mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        shadowShape.setBounds(margin, margin, margin + w, margin + h);
        shadowShape.draw(new Canvas(mask));
        Paint blur = new Paint();
        blur.setMaskFilter(new BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL));
        int[] offset = new int[2];
        Bitmap alpha = mask.extractAlpha(blur, offset);
        Paint color = new Paint();
        color.setColor(ListMode.SHADOW_COLOR);
        canvas.drawBitmap(alpha, offset[0], offset[1] + dy, color);
        top.setBounds(margin, margin, margin + w, margin + h);
        top.draw(canvas);
        mask.recycle();
        alpha.recycle();
        return new ShadowDrawable(out, w, h);
    }

    /** The made bitmap, centred on whatever bounds it is given, and as large as the shape (not the shadow) for layout. */
    static final class ShadowDrawable extends Drawable {
        private final Bitmap bitmap;
        private final int width, height;
        private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);

        ShadowDrawable(Bitmap bitmap, int width, int height) {
            this.bitmap = bitmap;
            this.width = width;
            this.height = height;
        }

        @Override
        public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            canvas.drawBitmap(bitmap, bounds.exactCenterX() - bitmap.getWidth() / 2f,
                    bounds.exactCenterY() - bitmap.getHeight() / 2f, paint);
        }

        @Override public int getIntrinsicWidth() { return width; }
        @Override public int getIntrinsicHeight() { return height; }
        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
        @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }

        @Override
        public ConstantState getConstantState() {
            return new ConstantState() {
                @Override public Drawable newDrawable() { return new ShadowDrawable(bitmap, width, height); }
                @Override public int getChangingConfigurations() { return 0; }
            };
        }
    }
}
