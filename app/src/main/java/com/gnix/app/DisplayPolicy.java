package com.gnix.app;

/** Layout and refresh rules use available space, never manufacturer names. */
public final class DisplayPolicy {
    public static int topicColumns(int availableDp, float fontScale) {
        if (availableDp < 360 || fontScale >= 1.5f) return 1;
        return availableDp >= 600 ? 3 : 2;
    }
    public static int horizontalGutter(int availablePixels, int maxContentPixels) {
        return Math.max(0, (availablePixels - maxContentPixels) / 2);
    }
    public static boolean needsRefresh(long now, long lastRefresh, boolean emptyCache) {
        return emptyCache || lastRefresh <= 0 || now < lastRefresh || now - lastRefresh >= 600000L;
    }
    public static long refreshTimestamp(long now, boolean allSourcesSucceeded) { return allSourcesSucceeded ? now : 0L; }
}
