package com.faketime;

public final class FakeTimeFormatter {
    private FakeTimeFormatter() {}

    public static String formatTicks(long ticks) {
        return Math.floorMod(ticks, FakeTimeManager.DAY_LENGTH) + " 刻";
    }

    /** 游戏刻 -> 24 小时制现实时钟。0 刻 = 6:00（1000 刻 = 1 小时），18000 刻（午夜）= 0:00。 */
    public static String formatClock(long ticks) {
        long normalized = Math.floorMod(ticks, FakeTimeManager.DAY_LENGTH);
        long hours24 = Math.floorMod(6 + normalized / 1000L, 24L);
        long minutes = (normalized % 1000L) * 60L / 1000L;
        return String.format("%d:%02d", hours24, minutes);
    }
}
