package com.gathin.tvhome;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LruCache;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * list_mode's Wallpaper window: the theme's pictures as tiles, three to a row, no names and no buttons. OK takes the
 * picture and closes the window, Back closes it unchanged. Previews are made from the files at the TV, off the UI
 * thread, shrunk while decoding (WallpaperGrid.sampleSize) and kept in a cache of WallpaperGrid.CACHE_BYTES.
 * The window is built new at every opening and remembers nothing: focus starts on the chosen picture.
 */
final class WallpaperTiles {
    private static final String TAG = "WallpaperTiles";
    /** The loader thread's name: it is the one thread this window starts, and `ps -T` finds it by it. */
    static final String THREAD_NAME = "wallpaper-tiles";

    interface OnPick {
        void picked(String fileName);
    }

    /** The tick "Random abspielen" was switched. */
    interface OnRandom {
        void changed(boolean on);
    }

    private final Activity activity;
    private final List<File> images;
    private final OnPick onPick;
    private final OnRandom onRandom;
    private final Dialog dialog;
    private final RecyclerView grid;
    private final ExecutorService loader = Executors.newSingleThreadExecutor(r -> new Thread(r, THREAD_NAME));
    private final Handler main = new Handler(Looper.getMainLooper());
    private final LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(WallpaperGrid.CACHE_BYTES) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };
    private final int tileWidth, tileHeight, cellWidth, cellHeight, gap;
    private final float density;
    private boolean dismissed;
    /** The tick above the tiles (top right of the title row) and the column Up left the tiles from. */
    /** The corners of a picture tile: the 14 dp of the rows and cards inside a panel. */
    static final int TILE_RADIUS_DP = 14;
    private final CheckBox toggle;
    private int toggleColumn;

    static WallpaperTiles show(Activity activity, List<File> images, String current, boolean randomOn,
                               OnRandom onRandom, OnPick onPick) {
        WallpaperTiles tiles = new WallpaperTiles(activity, images, randomOn, onRandom, onPick);
        tiles.dialog.show();
        tiles.focusStart(WallpaperGrid.startIndex(tiles.names(), current));
        return tiles;
    }

    /** Closes the window and ends the loader thread; the window's dismiss does the same, so closing twice is harmless. */
    void dismiss() {
        if (dialog.isShowing()) dialog.dismiss();
        else stopLoader();
    }

    private List<String> names() {
        List<String> names = new ArrayList<>();
        for (File image : images) names.add(image.getName());
        return names;
    }

    private WallpaperTiles(Activity activity, List<File> images, boolean randomOn, OnRandom onRandom, OnPick onPick) {
        this.activity = activity;
        this.images = images;
        this.onPick = onPick;
        this.onRandom = onRandom;
        density = activity.getResources().getDisplayMetrics().density;
        gap = Math.round(WallpaperGrid.GAP_DP * density);
        int gridWidth = Math.round((WallpaperGrid.WINDOW_WIDTH_DP - 2 * WallpaperGrid.PADDING_DP) * density);
        tileWidth = WallpaperGrid.tileWidthPx(gridWidth, WallpaperGrid.COLUMNS, gap);
        tileHeight = WallpaperGrid.tileHeightPx(tileWidth);
        cellWidth = WallpaperGrid.cellWidthPx(tileWidth, gap);
        cellHeight = WallpaperGrid.cellWidthPx(tileHeight, gap);

        dialog = new Dialog(activity, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);
        dialog.setOnDismissListener(d -> stopLoader());

        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(WallpaperGrid.PADDING_DP * density);
        panel.setPadding(pad, pad, pad, pad);
        panel.setBackgroundResource(R.drawable.glass_panel);   // the panel of the menus and cards: gradient, 1 dp frame, 20 dp corners

        TextView title = new TextView(activity);
        title.setText(R.string.wallpaper_label);
        title.setTextColor(Color.WHITE);
        title.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTypeface(activity.getResources().getFont(R.font.outfit_regular));
        title.setIncludeFontPadding(false);
        // The title row: the title, and the tick at its right, 24 dp (48 px) high so the row stays as tall as the title.
        toggle = new CheckBox(activity);
        toggle.setText(R.string.wallpaper_random);
        toggle.setTextColor(Color.WHITE);
        toggle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16);
        toggle.setTypeface(activity.getResources().getFont(R.font.outfit_regular));
        toggle.setButtonDrawable(R.drawable.random_box);
        toggle.setMinHeight(0);
        toggle.setMinimumHeight(0);
        toggle.setMinWidth(0);
        toggle.setMinimumWidth(0);
        toggle.setIncludeFontPadding(false);
        toggle.setGravity(Gravity.CENTER_VERTICAL);
        toggle.setPaddingRelative(Math.round(8 * density), 0, Math.round(10 * density), 0);
        toggle.setBackgroundResource(R.drawable.input_row_bg);
        toggle.setFocusable(true);
        toggle.setChecked(randomOn);
        toggle.setOnCheckedChangeListener((box, checked) -> onRandom.changed(checked));
        toggle.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    return true;   // the focus stays on the tick
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    focusTileAfterToggle();
                    return true;
                default:
                    return false;
            }
        });
        LinearLayout titleRow = new LinearLayout(activity);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        titleRow.addView(toggle, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                Math.round(24 * density)));
        panel.addView(titleRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        grid = new RecyclerView(activity);
        grid.setLayoutManager(new GridLayoutManager(activity, WallpaperGrid.COLUMNS));
        int gridPad = Math.round(WallpaperGrid.GRID_PADDING_DP * density);
        grid.setPadding(0, gridPad, 0, gridPad);
        // The tiles are cut at the grid's own edge (under the title, above the window's rim), but may scroll through its
        // 12 px padding, so the ring of a fully visible focus tile (it reaches 3 px past the tile, inside the cell's
        // 12 px of air) is not cut off. Neither the grid nor the panel may let children draw past their bounds.
        grid.setClipToPadding(false);
        grid.setOverScrollMode(View.OVER_SCROLL_NEVER);
        grid.setItemAnimator(null);
        grid.setAdapter(new TileAdapter());
        panel.addView(grid, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        dialog.setContentView(panel);
        Window window = dialog.getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = Math.round(WallpaperGrid.WINDOW_X_DP * density);
        lp.y = Math.round(WallpaperGrid.WINDOW_Y_DP * density);
        lp.width = Math.round(WallpaperGrid.WINDOW_WIDTH_DP * density);
        lp.height = Math.round(WallpaperGrid.WINDOW_HEIGHT_DP * density);
        window.setAttributes(lp);
    }

    private void stopLoader() {
        dismissed = true;
        loader.shutdownNow();
    }

    /** Scrolls the start tile into view and gives it focus once it is laid out. */
    private void focusStart(int position) {
        grid.scrollToPosition(position);
        focusWhenLaidOut(position, 3);
    }

    private void focusWhenLaidOut(int position, int tries) {
        grid.post(() -> {
            if (dismissed) return;
            RecyclerView.ViewHolder holder = grid.findViewHolderForAdapterPosition(position);
            if (holder != null) {
                ((TileAdapter.Holder) holder).tile.requestFocus();
            } else if (tries > 0) {
                focusWhenLaidOut(position, tries - 1);
            } else {
                Log.w(TAG, "tile " + position + " was never laid out; no focus set");
            }
        });
    }

    /** Down from the tick: the first row's tile in the column Up left from. */
    private void focusTileAfterToggle() {
        int to = WallpaperGrid.afterToggle(toggleColumn, images.size());
        RecyclerView.ViewHolder holder = grid.findViewHolderForAdapterPosition(to);
        if (holder != null) {
            ((TileAdapter.Holder) holder).tile.requestFocus();
        } else {
            grid.scrollToPosition(to);
            focusWhenLaidOut(to, 3);
        }
    }

    private void moveFocus(int from, int direction) {
        int to = WallpaperGrid.step(from, images.size(), WallpaperGrid.COLUMNS, direction);
        if (to == WallpaperGrid.TOGGLE) {
            toggleColumn = from % WallpaperGrid.COLUMNS;
            toggle.requestFocus();
            return;
        }
        if (to == from) return;
        RecyclerView.ViewHolder holder = grid.findViewHolderForAdapterPosition(to);
        if (holder != null) {
            ((TileAdapter.Holder) holder).tile.requestFocus();
        } else {
            grid.scrollToPosition(to);
            focusWhenLaidOut(to, 3);
        }
    }

    // ---- previews -----------------------------------------------------------------------------------------

    private void load(Tile tile, File file) {
        String path = file.getPath();
        Bitmap cached = cache.get(path);
        if (cached != null) {
            tile.setImageBitmap(cached);
            return;
        }
        tile.setImageDrawable(null);
        if (dismissed) return;
        loader.execute(() -> {
            // Gone from its tile (scrolled out and recycled) before its turn: nothing to do.
            if (!path.equals(tile.getTag())) return;
            Bitmap bitmap = decode(path);
            if (bitmap == null) return;   // unreadable: the tile stays empty
            cache.put(path, bitmap);
            main.post(() -> {
                if (!dismissed && path.equals(tile.getTag())) tile.setImageBitmap(bitmap);
            });
        });
    }

    private Bitmap decode(String path) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, bounds);
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                Log.w(TAG, "not a readable picture: " + new File(path).length() + " bytes");
                return null;
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = WallpaperGrid.sampleSize(bounds.outWidth, bounds.outHeight, tileWidth, tileHeight);
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap bitmap = BitmapFactory.decodeFile(path, options);
            if (bitmap == null) Log.w(TAG, "decoding failed: " + new File(path).length() + " bytes");
            return bitmap;
        } catch (RuntimeException | OutOfMemoryError e) {
            Log.w(TAG, "decoding failed: " + e);
            return null;
        }
    }

    // ---- views --------------------------------------------------------------------------------------------

    /** One tile: the preview, rounded, with the focus ring drawn around it (white inside, black outside). */
    private final class Tile extends ImageView {
        private final Paint white = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint black = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();

        Tile() {
            super(activity);
            setScaleType(ScaleType.CENTER_CROP);
            setFocusable(true);
            setClickable(true);
            setBackgroundColor(0xFF2A2D35);
            setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), TILE_RADIUS_DP * density);
                }
            });
            setClipToOutline(true);
            white.setStyle(Paint.Style.STROKE);
            white.setColor(Color.WHITE);
            white.setStrokeWidth(2.5f * density);
            black.setStyle(Paint.Style.STROKE);
            black.setColor(Color.BLACK);
            black.setStrokeWidth(1.5f * density);
        }

        void drawRing(Canvas canvas) {
            float w = getWidth(), h = getHeight(), corner = TILE_RADIUS_DP * density;
            float whiteWidth = white.getStrokeWidth(), blackWidth = black.getStrokeWidth();
            // White ring: 5 px wide, its outer edge on the tile's edge. Black ring: 3 px wide, outside the tile.
            rect.set(whiteWidth / 2, whiteWidth / 2, w - whiteWidth / 2, h - whiteWidth / 2);
            canvas.drawRoundRect(rect, corner - whiteWidth / 2, corner - whiteWidth / 2, white);
            rect.set(-blackWidth / 2, -blackWidth / 2, w + blackWidth / 2, h + blackWidth / 2);
            canvas.drawRoundRect(rect, corner + blackWidth / 2, corner + blackWidth / 2, black);
        }
    }

    /** A cell: the tile with the air around it. It draws the ring of its focused tile on top, past the tile's clip. */
    private final class Cell extends FrameLayout {
        final Tile tile = new Tile();

        Cell() {
            super(activity);
            setClipChildren(false);
            setClipToPadding(false);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(tileWidth, tileHeight);
            lp.gravity = Gravity.CENTER;
            addView(tile, lp);
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            super.dispatchDraw(canvas);
            if (!tile.isFocused()) return;
            canvas.save();
            canvas.translate(tile.getLeft(), tile.getTop());
            tile.drawRing(canvas);
            canvas.restore();
        }
    }

    private final class TileAdapter extends RecyclerView.Adapter<TileAdapter.Holder> {
        final class Holder extends RecyclerView.ViewHolder {
            final Tile tile;

            Holder(Cell cell) {
                super(cell);
                tile = cell.tile;
            }
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Cell cell = new Cell();
            cell.setLayoutParams(new RecyclerView.LayoutParams(cellWidth, cellHeight));
            Holder holder = new Holder(cell);
            holder.tile.setOnFocusChangeListener((v, focused) -> cell.invalidate());
            holder.tile.setOnKeyListener((v, keyCode, event) -> {
                int direction;
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_UP: direction = WallpaperGrid.UP; break;
                    case KeyEvent.KEYCODE_DPAD_DOWN: direction = WallpaperGrid.DOWN; break;
                    case KeyEvent.KEYCODE_DPAD_LEFT: direction = WallpaperGrid.LEFT; break;
                    case KeyEvent.KEYCODE_DPAD_RIGHT: direction = WallpaperGrid.RIGHT; break;
                    default: return false;
                }
                int position = holder.getBindingAdapterPosition();
                if (event.getAction() == KeyEvent.ACTION_DOWN && position != RecyclerView.NO_POSITION) {
                    moveFocus(position, direction);
                }
                return true;   // one tile per key; at an edge the focus stays
            });
            holder.tile.setOnClickListener(v -> {
                int position = holder.getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;
                onPick.picked(images.get(position).getName());
                dialog.dismiss();
            });
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            File file = images.get(position);
            holder.tile.setTag(file.getPath());
            holder.tile.setContentDescription(file.getName());
            load(holder.tile, file);
        }

        @Override
        public int getItemCount() {
            return images.size();
        }
    }
}
