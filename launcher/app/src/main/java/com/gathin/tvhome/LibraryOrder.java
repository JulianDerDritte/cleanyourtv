package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The order of the app library in list_mode, everything that can be decided without a TV: the apps not on
 * Home first (the owner's saved order, then the rest by name), the apps on Home last in the order Home shows them;
 * moving one app inside the first block; the saved text. The dialog (AppLibraryDialog) only calls this.
 */
final class LibraryOrder {
    private LibraryOrder() { }

    /** The saved order holds at most this many packages; the front ones fall away first. */
    static final int MAX_SAVED = 128;

    /** The new order and the grouping apply in list_mode only, and never in the selection mode (OK toggles there). */
    static boolean useListOrder(boolean listMode, boolean selectionMode) {
        return listMode && !selectionMode;
    }

    /**
     * The whole library: {@code byName} is every package in name order; {@code chosen} those on Home; {@code saved}
     * the owner's order; {@code homeOrder} the packages in the order Home shows them. Result: not chosen, saved first
     * (only those still not chosen), the rest in name order; then the chosen (see {@link #chosenInHomeOrder}).
     */
    static List<String> arrange(List<String> byName, Set<String> chosen, List<String> saved, List<String> homeOrder) {
        List<String> notChosen = new ArrayList<>();
        List<String> onHome = new ArrayList<>();
        for (String pkg : byName) (chosen.contains(pkg) ? onHome : notChosen).add(pkg);
        Set<String> inBlock = new HashSet<>(notChosen);
        List<String> result = new ArrayList<>();
        Set<String> placed = new HashSet<>();
        for (String pkg : saved) if (inBlock.contains(pkg) && placed.add(pkg)) result.add(pkg);
        for (String pkg : notChosen) if (placed.add(pkg)) result.add(pkg);
        result.addAll(chosenInHomeOrder(onHome, homeOrder));
        return result;
    }

    /** The chosen apps in the order Home shows them; one Home does not show goes last, in the order given. */
    static List<String> chosenInHomeOrder(List<String> chosen, List<String> homeOrder) {
        Set<String> wanted = new HashSet<>(chosen);
        List<String> result = new ArrayList<>();
        Set<String> placed = new HashSet<>();
        for (String pkg : homeOrder) if (wanted.contains(pkg) && placed.add(pkg)) result.add(pkg);
        for (String pkg : chosen) if (placed.add(pkg)) result.add(pkg);
        return result;
    }

    /** Each package once, the first place kept (Home can hold two tiles of one package). */
    static List<String> distinct(List<String> packages) {
        return new ArrayList<>(new java.util.LinkedHashSet<>(packages));
    }

    // ---- moving ------------------------------------------------------------------------------------------

    /**
     * {@code all} with the app at {@code index} one place further ({@code delta} is -1 or +1). Only inside the first
     * {@code blockSize} entries (the apps not on Home): from outside the block, or to a place outside it, nothing moves.
     */
    static List<String> moveBy(List<String> all, int blockSize, int index, int delta) {
        List<String> result = new ArrayList<>(all);
        int target = index + delta;
        if (index < 0 || index >= blockSize || target < 0 || target > blockSize - 1) return result;
        result.add(target, result.remove(index));
        return result;
    }

    /** Left or right key to a step: LTR left is -1, right +1; the row is mirrored in RTL. */
    static int deltaForKey(boolean left, boolean rtl) {
        int delta = left ? -1 : 1;
        return rtl ? -delta : delta;
    }

    static int indexOf(List<String> order, String pkg) {
        return order.indexOf(pkg);
    }

    /** One move of one app: OK confirms the new order, Back (or closing the library) gives the old one back. */
    static final class MoveSession {
        private final List<String> original;
        private List<String> current;
        private boolean confirmed;

        MoveSession(List<String> order) {
            this.original = Collections.unmodifiableList(new ArrayList<>(order));
            this.current = new ArrayList<>(order);
        }

        List<String> current() {
            return new ArrayList<>(current);
        }

        /** Moves {@code pkg} one place inside the block; returns its place afterwards. */
        int move(String pkg, int blockSize, int delta) {
            current = moveBy(current, blockSize, current.indexOf(pkg), delta);
            return current.indexOf(pkg);
        }

        /** OK: the order as it is now. */
        List<String> confirm() {
            confirmed = true;
            return current();
        }

        /** Back or closing: the order before moving, unless OK already confirmed (the window closes after OK too). */
        List<String> cancel() {
            if (!confirmed) current = new ArrayList<>(original);
            return current();
        }
    }

    // ---- what choosing and un-choosing do to the saved order -----------------------------------------------

    /** An app put on Home leaves the saved order. */
    static List<String> afterChosen(List<String> saved, String pkg) {
        List<String> next = new ArrayList<>(saved);
        next.removeAll(Collections.singleton(pkg));
        return next;
    }

    /** An app taken off Home: the order the apps not on Home have now, and the app last. */
    static List<String> afterRemoved(List<String> notChosenNow, String pkg) {
        List<String> next = afterChosen(notChosenNow, pkg);
        next.add(pkg);
        return next;
    }

    // ---- the saved text ------------------------------------------------------------------------------------

    /** One package per line, only valid names, each once, the last {@link #MAX_SAVED} kept. Never throws. */
    static String encode(List<String> order) {
        List<String> kept = clean(order);
        StringBuilder text = new StringBuilder();
        for (String pkg : kept) text.append(pkg).append('\n');
        return text.toString();
    }

    /** The packages in a saved text: invalid lines skipped, each once, the last {@link #MAX_SAVED} kept. Never throws. */
    static List<String> decode(String text) {
        List<String> lines = new ArrayList<>();
        if (text != null) Collections.addAll(lines, text.split("\n", -1));
        return clean(lines);
    }

    private static List<String> clean(List<String> names) {
        List<String> valid = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String pkg : names) if (LibraryApps.validPackage(pkg) && seen.add(pkg)) valid.add(pkg);
        int extra = valid.size() - MAX_SAVED;
        return extra > 0 ? new ArrayList<>(valid.subList(extra, valid.size())) : valid;
    }
}
