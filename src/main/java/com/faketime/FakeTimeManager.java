package com.faketime;

import java.util.function.LongSupplier;

public class FakeTimeManager {
    public enum TimeState { FOLLOW, INDEPENDENT, LOCKED }

    public static final long DAY_LENGTH = 24000L;
    private static final long MS_PER_TICK = 50L; // 1 游戏刻 = 50 现实毫秒（20 tps）

    private static final FakeTimeManager INSTANCE = new FakeTimeManager();

    private volatile TimeState state = TimeState.FOLLOW;
    private long baseTicks = 0;       // INDEPENDENT: 用户设定的时刻 (0~23999)
    private long anchorMs = 0;        // INDEPENDENT: 进入独立时的现实毫秒
    private long lockedTicks = 0;     // LOCKED: 冻结时刻 (0~23999)
    private long lastRealDayTime = 0; // 最近校准的服务器真实时间（完整值）
    private long lastUpdateMs = 0;    // 校准时刻的现实毫秒
    private LongSupplier clock = System::currentTimeMillis;

    private FakeTimeManager() {
        this.lastUpdateMs = this.clock.getAsLong();
    }

    public static FakeTimeManager getInstance() { return INSTANCE; }

    /** 测试用：注入可控时钟。 */
    public void setClock(LongSupplier clock) {
        this.clock = clock;
        this.lastUpdateMs = clock.getAsLong();
    }

    /** 每 tick 校准（ClientTickEvent）：记录服务器真实时间基准。
     *  仅在真实时间值确实变化时更新校准点——若客户端 levelData 冻结（界面打开/暂停）
     *  而传入相同值，则保持旧校准点，让 getRealDayTimeApprox 按 20tps 外推继续流动。 */
    public void updateRealDayTime(long realDayTime) {
        if (realDayTime != this.lastRealDayTime) {
            this.lastRealDayTime = realDayTime;
            this.lastUpdateMs = this.clock.getAsLong();
        }
    }

    /** 当前服务器真实时间近似（校准点之间按 20tps 现实速率外推，tick 冻结时依然流动）。 */
    public long getRealDayTimeApprox() {
        return this.lastRealDayTime + (this.clock.getAsLong() - this.lastUpdateMs) / MS_PER_TICK;
    }

    public long getFakeDayTime(long realDayTime) {
        return switch (this.state) {
            case FOLLOW -> Math.floorMod(this.getRealDayTimeApprox(), DAY_LENGTH);
            case INDEPENDENT -> Math.floorMod(this.baseTicks + (this.clock.getAsLong() - this.anchorMs) / MS_PER_TICK, DAY_LENGTH);
            case LOCKED -> Math.floorMod(this.lockedTicks, DAY_LENGTH);
        };
    }

    /** 完整假时间（含天数，不取模）——月相计算用。 */
    public long getFakeFullDayTime(long realDayTime) {
        return switch (this.state) {
            case FOLLOW -> this.getRealDayTimeApprox();
            case INDEPENDENT -> this.baseTicks + (this.clock.getAsLong() - this.anchorMs) / MS_PER_TICK;
            case LOCKED -> this.lockedTicks;
        };
    }

    public long getDisplayTicks() { return this.getFakeDayTime(this.getRealDayTimeApprox()); }

    public void dragTo(long ticks) {
        if (this.state == TimeState.LOCKED) {
            this.lockedTicks = Math.floorMod(ticks, DAY_LENGTH);
        } else {
            this.baseTicks = Math.floorMod(ticks, DAY_LENGTH);
            this.anchorMs = this.clock.getAsLong();
            this.state = TimeState.INDEPENDENT;
        }
    }

    public void setLocked(boolean locked) {
        if (locked && this.state != TimeState.LOCKED) {
            this.lockedTicks = this.getDisplayTicks();
            this.state = TimeState.LOCKED;
        } else if (!locked && this.state == TimeState.LOCKED) {
            this.baseTicks = this.lockedTicks;
            this.anchorMs = this.clock.getAsLong();
            this.state = TimeState.INDEPENDENT;
        }
    }

    public void syncToServer() { this.state = TimeState.FOLLOW; }

    public TimeState getState() { return this.state; }
    public boolean isLocked() { return this.state == TimeState.LOCKED; }
    public long getBaseTicks() { return this.baseTicks; }
    public long getLockedTicks() { return this.lockedTicks; }
    public long getAnchorMs() { return this.anchorMs; }
    public long getLastRealDayTime() { return this.lastRealDayTime; }
    public long getLastUpdateMs() { return this.lastUpdateMs; }

    /** 配置恢复：anchorMs/lastUpdateMs 为现实毫秒。 */
    public void load(TimeState state, long baseTicks, long anchorMs, long lockedTicks, long lastRealDayTime, long lastUpdateMs) {
        this.state = state;
        this.baseTicks = baseTicks;
        this.anchorMs = anchorMs;
        this.lockedTicks = lockedTicks;
        this.lastRealDayTime = lastRealDayTime;
        this.lastUpdateMs = lastUpdateMs;
    }
}
