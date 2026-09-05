package com.faketime;

import net.minecraft.network.chat.Component;
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
        // 现在返回本地化 Component，按 key+参数 比较（纯逻辑，不依赖语言加载）
        assertEquals(Component.translatable("gui.faketimemod.ticks", 1000L),
                FakeTimeFormatter.formatTicks(1000L));
        assertEquals(Component.translatable("gui.faketimemod.ticks", 23999L),
                FakeTimeFormatter.formatTicks(23999L));
    }

    /** 复刻 vanilla DimensionType.getTimeOfDay 平滑曲线（非线性）的关键锚点。
     *  线性 skyTicks/24000 只在 6:00(6000刻) 与 18:00(18000刻) 相等，其余时刻偏差最大 ~840 刻
     *  （黎明/黄昏）——正是太阳位置与 vanilla 不一致的根因。这些值取自 1.20.1 的 Mth.frac 公式。 */
    @Test
    void vanillaTimeOfDaySmoothedCurve() {
        // 6:00(0刻)：d0=frac(-0.25)=0.75, d1=0.5-cos(0.75π)/2=0.8536, (2*0.75+0.8536)/3=0.7845
        assertEquals(0.7845F, FakeTimeManager.getTimeOfDay(0L), 0.001F);
        // 正午(6000刻)：0.0
        assertEquals(0.0F, FakeTimeManager.getTimeOfDay(6000L), 0.001F);
        // 18:00(12000刻)：d0=frac(0.25)=0.25, d1=0.5-cos(0.25π)/2=0.1464, (0.5+0.1464)/3=0.2155
        assertEquals(0.2155F, FakeTimeManager.getTimeOfDay(12000L), 0.001F);
        // 午夜(18000刻)：0.5
        assertEquals(0.5F, FakeTimeManager.getTimeOfDay(18000L), 0.001F);
        // 逐日取模后同值
        assertEquals(FakeTimeManager.getTimeOfDay(12000L), FakeTimeManager.getTimeOfDay(24000L + 12000L), 0.0001F);
    }
}
