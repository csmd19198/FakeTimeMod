package com.faketime;

import com.faketime.FakeTimeManager.TimeState;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class FakeTimeManagerTest {

    private AtomicLong now = new AtomicLong(1_000_000L); // 可控"现实毫秒"
    private FakeTimeManager fresh() {
        FakeTimeManager m = FakeTimeManager.getInstance();
        m.setClock(now::get);
        m.load(TimeState.FOLLOW, 0L, 0L, 0L, 18000L, now.get());
        return m;
    }

    private void advanceTicks(FakeTimeManager m, long ticks) {
        now.addAndGet(ticks * 50L);
    }

    @Test
    void follow_returnsRealTime() {
        FakeTimeManager m = fresh();
        assertEquals(18000L, m.getFakeDayTime(18000L));
        advanceTicks(m, 10);
        assertEquals(18010L, m.getFakeDayTime(0L)); // 按 20tps 现实速率流动
    }

    @Test
    void dragTo_entersIndependent_andFlows() {
        FakeTimeManager m = fresh();
        m.dragTo(1000L);
        assertEquals(TimeState.INDEPENDENT, m.getState());
        assertEquals(1000L, m.getFakeDayTime(999999L));
        advanceTicks(m, 1);
        assertEquals(1001L, m.getFakeDayTime(999999L));
        advanceTicks(m, 1);
        assertEquals(1002L, m.getFakeDayTime(999999L));
    }

    @Test
    void independent_wrapsAt24000() {
        FakeTimeManager m = fresh();
        m.dragTo(23999L);
        advanceTicks(m, 1);
        assertEquals(0L, m.getFakeDayTime(0L));
    }

    @Test
    void independent_ignoresServerTimeChanges() {
        FakeTimeManager m = fresh();
        m.dragTo(5000L);
        advanceTicks(m, 2);
        long fake = m.getFakeDayTime(18000L);
        assertEquals(fake, m.getFakeDayTime(0L));
    }

    @Test
    void lock_freezesTime() {
        FakeTimeManager m = fresh();
        m.dragTo(12000L);
        m.setLocked(true);
        assertEquals(TimeState.LOCKED, m.getState());
        assertEquals(12000L, m.getFakeDayTime(999999L));
        advanceTicks(m, 10);
        assertEquals(12000L, m.getFakeDayTime(999999L));
    }

    @Test
    void unlock_returnsToIndependentFromLockedValue() {
        FakeTimeManager m = fresh();
        m.dragTo(12000L);
        m.setLocked(true);
        m.setLocked(false);
        assertEquals(TimeState.INDEPENDENT, m.getState());
        assertEquals(12000L, m.getFakeDayTime(0L));
        advanceTicks(m, 1);
        assertEquals(12001L, m.getFakeDayTime(0L));
    }

    @Test
    void dragWhileLocked_changesLockedValue() {
        FakeTimeManager m = fresh();
        m.dragTo(1000L);
        m.setLocked(true);
        m.dragTo(18000L);
        assertEquals(18000L, m.getFakeDayTime(0L));
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
    void follow_flowsEvenWhenTicksFrozen() {
        FakeTimeManager m = fresh();
        // 模拟界面打开 tick 冻结：不调用 updateRealDayTime，仅现实时间流逝
        advanceTicks(m, 100);
        assertEquals(18100L, m.getFakeDayTime(18000L)); // 从最后校准点外推流动
    }

    @Test
    void updateRealDayTime_recals() {
        FakeTimeManager m = fresh();
        advanceTicks(m, 100);
        assertEquals(18100L, m.getFakeDayTime(18000L));
        m.updateRealDayTime(20000L); // 服务器校准
        assertEquals(20000L, m.getFakeDayTime(0L));
        advanceTicks(m, 10);
        assertEquals(20010L, m.getFakeDayTime(0L));
    }

    @Test
    void updateRealDayTime_frozenValue_keepsFlowing() {
        FakeTimeManager m = fresh();
        m.updateRealDayTime(18000L); // 界面打开时 levelData 冻结，传入相同值
        advanceTicks(m, 50);
        assertEquals(18050L, m.getFakeDayTime(18000L)); // 冻结校准不应抹平外推（FOLLOW 继续流动）
    }

    @Test
    void getDisplayTicks_usesApprox() {
        FakeTimeManager m = fresh();
        m.updateRealDayTime(20000L);
        assertEquals(20000L, m.getDisplayTicks());
        advanceTicks(m, 5);
        assertEquals(20005L, m.getDisplayTicks());
    }

    @Test
    void load_restoresIndependentState() {
        FakeTimeManager m = fresh();
        m.load(TimeState.INDEPENDENT, 5000L, 1_000_000L, 0L, 10000L, 1_000_000L);
        assertEquals(5000L, m.getFakeDayTime(0L));
        advanceTicks(m, 1);
        assertEquals(5001L, m.getFakeDayTime(0L));
    }

    @Test
    void getFakeFullDayTime_follow_returnsFullRealValue() {
        FakeTimeManager m = fresh();
        assertEquals(18000L, m.getFakeFullDayTime(80000L)); // FOLLOW 返回 getRealDayTimeApprox，忽略参数
    }

    @Test
    void getFakeFullDayTime_independent_returnsBasePlusElapsed() {
        FakeTimeManager m = fresh();
        m.dragTo(1000L);
        assertEquals(1000L, m.getFakeFullDayTime(80000L));
        advanceTicks(m, 1);
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
