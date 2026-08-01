package com.faketime;

public final class FakeTimeFormatter {
    private FakeTimeFormatter() {}

    public static String formatTicks(long ticks) {
        return Math.floorMod(ticks, FakeTimeManager.DAY_LENGTH) + " 刻";
    }

    /** 游戏刻 -> 现实时钟。0 刻 = 6:00 AM（1000 刻 = 1 现实小时）。 */
    public static String formatClock(long ticks) {
        long normalized = Math.floorMod(ticks, FakeTimeManager.DAY_LENGTH);
        long hours24 = Math.floorMod(6 + normalized / 1000L, 24L);
        long minutes = (normalized % 1000L) * 60L / 1000L;
        String ap = hours24 < 12 ? "AM" : "PM";
        long h12 = hours24 % 12;
        if (h12 == 0) h12 = 12;
        return String.format("%d:%02d %s", h12, minutes, ap);
    }
}
