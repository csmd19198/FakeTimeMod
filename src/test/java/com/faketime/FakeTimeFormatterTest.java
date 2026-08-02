package com.faketime;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FakeTimeFormatterTest {
    @Test
    void clockConversions() {
        assertEquals("6:00", FakeTimeFormatter.formatClock(0L));
        assertEquals("7:00", FakeTimeFormatter.formatClock(1000L));
        assertEquals("12:00", FakeTimeFormatter.formatClock(6000L));
        assertEquals("18:00", FakeTimeFormatter.formatClock(12000L));
        assertEquals("0:00", FakeTimeFormatter.formatClock(18000L));
        assertEquals("5:59", FakeTimeFormatter.formatClock(23999L));
        assertEquals("6:00", FakeTimeFormatter.formatClock(24000L)); // 取模
    }

    @Test
    void minutesRoundToHalfHours() {
        assertEquals("7:30", FakeTimeFormatter.formatClock(1500L)); // 1000刻=1小时
    }

    @Test
    void formatTicks() {
        assertEquals("1000 刻", FakeTimeFormatter.formatTicks(1000L));
        assertEquals("23999 刻", FakeTimeFormatter.formatTicks(23999L));
    }
}
