package com.gathin.tvhome;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;

/**
 * circle_list: lays the rows of list_mode on an arc and turns them. All the values come from {@link CircleList}; this
 * class only puts rows where it says and moves them. The rows stay laid out at one place (the selected one's) and are
 * moved with their translation, so a turn costs no layout; only when a row comes into the near window or leaves it is
 * the list laid out again. The list never scrolls (it has no ends): scrollToPosition only chooses the row.
 */
final class CircleLayoutManager extends RecyclerView.LayoutManager {
    private final float density;
    private final CircleList.Turner turner;
    private RecyclerView view;
    private int count;
    private int[] attachedNow = new int[0];
    private boolean frameQueued;
    private long lastFrameNs;
    /** The row of the chosen app must get focus once it is laid out (a jump brought it in from outside the window). */
    private boolean focusPending;

    CircleLayoutManager(float density, int durationMs) {
        this.density = density;
        this.turner = new CircleList.Turner(durationMs);
    }

    /** The chosen row (0 for an empty list). */
    int selected() {
        int n = getItemCount();
        return n <= 0 ? 0 : Math.floorMod(turner.target(), n);
    }

    /** One place up (-1) or down (+1) with the turn. False when nothing was done (fewer than two rows, or the press was dropped). */
    boolean step(int direction) {
        if (getItemCount() < 2) return false;
        boolean taken = turner.press(direction);
        if (taken) changed();
        return taken;
    }

    /** Chooses the row at {@code position}: a place next to the chosen one with the turn if {@code animate}, else at once. */
    void select(int position, boolean animate) {
        int n = getItemCount();
        if (n <= 0) return;
        int to = Math.max(0, Math.min(position, n - 1));
        int d = Math.floorMod(to - selected(), n);
        if (d > n / 2) d -= n;
        if (d == 0) return;
        if (animate && Math.abs(d) == 1) {
            if (!turner.press(d)) turner.jump(turner.target() + d);
        } else {
            turner.jump(turner.target() + d);
        }
        changed();
    }

    /** Focus goes to the chosen row's card if that row is laid out. False when it is not (yet). */
    boolean focusSelected() {
        View row = findViewByPosition(selected());
        if (row == null) return false;
        View card = row.findViewById(R.id.card);
        return card.isFocused() || card.requestFocus();
    }

    /** Lays the list out again and then focuses the chosen row; a row that is still missing then is an error. */
    void focusWhenLaidOut() {
        focusPending = true;
        if (view != null) view.requestLayout();
    }

    // ---- RecyclerView.LayoutManager ---------------------------------------------------------------------

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override public boolean canScrollVertically() { return false; }

    @Override public boolean canScrollHorizontally() { return false; }

    /** Nothing scrolls into view: the chosen row is always at its place. */
    @Override
    public boolean requestChildRectangleOnScreen(@NonNull RecyclerView parent, @NonNull View child,
                                                 @NonNull Rect rect, boolean immediate, boolean focusedChildVisible) {
        return false;
    }

    @Override public void scrollToPosition(int position) { select(position, false); }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
        select(position, false);
    }

    @Override
    public void onAttachedToWindow(RecyclerView recyclerView) {
        super.onAttachedToWindow(recyclerView);
        view = recyclerView;
    }

    @Override
    public void onDetachedFromWindow(RecyclerView recyclerView, RecyclerView.Recycler recycler) {
        super.onDetachedFromWindow(recyclerView, recycler);
        recyclerView.removeCallbacks(frame);
        frameQueued = false;
        view = null;
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        int n = state.getItemCount();
        boolean hadFocus = view != null && view.hasFocus();
        if (n != count) {
            count = n;
            turner.jump(n > 0 ? Math.floorMod(turner.target(), n) : 0);
        }
        detachAndScrapAttachedViews(recycler);
        attachedNow = new int[0];
        if (n == 0) return;
        attachedNow = CircleList.attached(turner.shown(), turner.target(), n);
        int rowWidth = Math.round(CircleList.ROW_WIDTH_DP * density);
        float middle = CircleList.arcCentreY(middleY() / density) * density;   // the selected row's centre (a little below the middle)
        // The arc is measured from the screen's left edge, the list starts where its parent's padding ends (56 dp in
        // activity_main.xml): the rows are laid out at the selected place in the list's own coordinates.
        int left = Math.round((CircleList.centreX(0) - CircleList.SYMBOL_HALF_DP) * density) - listLeftInWindow();
        for (int p : attachedNow) {
            View row = recycler.getViewForPosition(p);
            addView(row);
            row.measure(View.MeasureSpec.makeMeasureSpec(rowWidth, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            int height = row.getMeasuredHeight();
            int top = Math.round(middle - height / 2f);
            layoutDecoratedWithMargins(row, left, top, left + rowWidth, top + height);
        }
        applyTransforms();
        if (focusPending || hadFocus) {
            final boolean hard = focusPending;
            focusPending = false;
            view.post(() -> {
                if (view == null || getItemCount() == 0) return;
                if (!hard && view.getFocusedChild() != null) return;
                if (!focusSelected()) {
                    throw new IllegalStateException("circle_list: the row of the chosen app (position " + selected()
                            + ") is not laid out; attached " + Arrays.toString(attachedNow));
                }
            });
        }
        if (turner.running()) queueFrame();
    }

    // ---- the turn on screen -------------------------------------------------------------------------------

    /** The list's left edge in the window, in px. */
    private int listLeftInWindow() {
        int[] where = new int[2];
        view.getLocationInWindow(where);
        return where[0];
    }

    /** The screen's middle in the list's own coordinates, in px. */
    private float middleY() {
        int[] where = new int[2];
        view.getLocationInWindow(where);
        int rootHeight = view.getRootView().getHeight();
        // Before the first layout of the window the root has no height yet; the list's own middle stands in until then.
        float middle = rootHeight > 0 ? rootHeight / 2f : where[1] + view.getHeight() / 2f;
        return middle - where[1];
    }

    private final Runnable frame = new Runnable() {
        @Override public void run() {
            frameQueued = false;
            if (view == null) return;
            long now = System.nanoTime();
            float dt = Math.min(50f, (now - lastFrameNs) / 1_000_000f);
            lastFrameNs = now;
            turner.tick(dt);
            changed();
        }
    };

    private void queueFrame() {
        if (view == null || frameQueued) return;
        frameQueued = true;
        lastFrameNs = System.nanoTime();
        view.postOnAnimation(frame);
    }

    /** The shown phase or the target changed: move the rows, lay out again if the near window changed, keep running. */
    private void changed() {
        if (view == null) return;
        int n = getItemCount();
        if (!Arrays.equals(CircleList.attached(turner.shown(), turner.target(), n), attachedNow)) view.requestLayout();
        applyTransforms();
        if (turner.running()) queueFrame();
    }

    private void applyTransforms() {
        int n = getItemCount();
        double phase = turner.shown();
        float x0 = CircleList.centreX(0);
        float y0 = CircleList.centreY(0, 0f);
        for (int i = 0; i < getChildCount(); i++) {
            View row = getChildAt(i);
            double d = CircleList.delta(getPosition(row), phase, n);
            row.setTranslationX((CircleList.centreX(d) - x0) * density);
            row.setTranslationY((CircleList.centreY(d, 0f) - y0) * density);
            // Alpha on the two drawn parts, not on the row: a row with alpha would be drawn through a layer every frame.
            row.findViewById(R.id.icon).setAlpha(CircleList.alphaFor(d, n));
            ((TextView) row.findViewById(R.id.label)).setAlpha(CircleList.nameAlphaFor(d));
        }
    }
}
