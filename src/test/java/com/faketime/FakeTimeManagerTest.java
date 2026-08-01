package com.faketime;

import com.faketime.FakeTimeManager.TimeState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FakeTimeManagerTest {

    private FakeTimeManager fresh() {
        // 每个测试用干净实例：通过 load 重置
        FakeTimeManager m = FakeTimeManager.getInstance();
        m.load(TimeState.FOLLOW, 0L, 0L, 0L, 0L, 18000L);
        return m;
    }

    @Test
    void follow_returnsRealTime() {
        FakeTimeManager m = fresh();
        assertEquals(18000L, m.getFakeDayTime(18000L));
        assertEquals(0L, m.getFakeDayTime(0L));
    }

    @Test
    void dragTo_entersIndependent_andFlows() {
        FakeTimeManager m = fresh();
        m.dragTo(1000L);
        assertEquals(TimeState.INDEPENDENT, m.getState());
        assertEquals(1000L, m.getFakeDayTime(999999L)); // 拖到 1000，基准时刻 1000
        m.onTick();
        assertEquals(1001L, m.getFakeDayTime(999999L)); // 本地走表 +1
        m.onTick();
        assertEquals(1002L, m.getFakeDayTime(999999L));
    }

    @Test
    void independent_wrapsAt24000() {
        FakeTimeManager m = fresh();
        m.dragTo(23999L);
        m.onTick();
        assertEquals(0L, m.getFakeDayTime(0L)); // 23999 -> 0 取模
    }

    @Test
    void independent_ignoresServerTimeChanges() {
        FakeTimeManager m = fresh();
        m.dragTo(5000L);
        m.onTick(); m.onTick();
        long fake = m.getFakeDayTime(18000L);
        assertEquals(fake, m.getFakeDayTime(0L)); // 服务器时间变化不影响假时间
    }

    @Test
    void lock_freezesTime() {
        FakeTimeManager m = fresh();
        m.dragTo(12000L);
        m.setLocked(true);
        assertEquals(TimeState.LOCKED, m.getState());
        assertEquals(12000L, m.getFakeDayTime(999999L));
        m.onTick();
        assertEquals(12000L, m.getFakeDayTime(999999L)); // 冻结
    }

    @Test
    void unlock_returnsToIndependentFromLockedValue() {
        FakeTimeManager m = fresh();
        m.dragTo(12000L);
        m.setLocked(true);
        m.setLocked(false);
        assertEquals(TimeState.INDEPENDENT, m.getState());
        assertEquals(12000L, m.getFakeDayTime(0L));
        m.onTick();
        assertEquals(12001L, m.getFakeDayTime(0L)); // 从锁定值继续走
    }

    @Test
    void dragWhileLocked_changesLockedValue() {
        FakeTimeManager m = fresh();
        m.dragTo(1000L);
        m.setLocked(true);
        m.dragTo(18000L);
        assertEquals(18000L, m.getFakeDayTime(0L)); // 锁定中拖动 = 改锁定值
        assertEquals(TimeState.LOCKED, m.getState());
    }

    @Test
    void syncToServer_returnsToFollow() {
        FakeTimeManager m = fresh();
        m.dragTo(1000L);
        m.syncToServer();
        assertEquals(TimeState.FOLLOW, m.getState());
        assertEquals(18000L, m.getFakeDayTime(18000L));
    }

    @Test
    void getDisplayTicks_usesLastRealDayTime() {
        FakeTimeManager m = fresh();
        m.updateRealDayTime(20000L);
        assertEquals(20000L, m.getDisplayTicks()); // FOLLOW
        m.dragTo(1000L);
        assertEquals(1000L, m.getDisplayTicks());
    }

    @Test
    void load_restoresIndependentState() {
        FakeTimeManager m = fresh();
        m.load(TimeState.INDEPENDENT, 5000L, 42L, 0L, 42L, 10000L);
        assertEquals(5000L, m.getFakeDayTime(0L)); // clientTicks == anchorTicks -> base
        m.onTick();
        assertEquals(5001L, m.getFakeDayTime(0L));
    }

    @Test
    void getFakeFullDayTime_follow_returnsFullRealValue() {
        FakeTimeManager m = fresh();
        assertEquals(80000L, m.getFakeFullDayTime(80000L));
    }

    @Test
    void getFakeFullDayTime_independent_returnsBasePlusElapsed() {
        FakeTimeManager m = fresh();
        m.dragTo(1000L);
        assertEquals(1000L, m.getFakeFullDayTime(80000L));
        m.onTick();
        assertEquals(1001L, m.getFakeFullDayTime(80000L));
    }

    @Test
    void getFakeFullDayTime_locked_returnsLockedValue() {
        FakeTimeManager m = fresh();
        m.dragTo(1000L);
        m.setLocked(true);
        assertEquals(1000L, m.getFakeFullDayTime(80000L));
    }
}
