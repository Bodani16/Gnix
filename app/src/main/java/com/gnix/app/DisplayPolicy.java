package com.gnix.app;

/** Layout and refresh rules use available space, never manufacturer names. */
public final class DisplayPolicy {
    /** Partial failure keeps a recent-looking stamp that ages out before the full TTL. */
    private static final long PARTIAL_FAILURE_BACKDATE = 480_000L;
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
    public static long refreshTimestamp(long now, int successCount, int totalSources) {
        if (totalSources <= 0) return 0L;
        if (successCount >= totalSources) return now;
        if (successCount <= 0) return 0L;
        return Math.max(1L, now - PARTIAL_FAILURE_BACKDATE);
    }
    public static long refreshTimestamp(long now, boolean allSourcesSucceeded) {
        return allSourcesSucceeded ? now : 0L;
    }
}
