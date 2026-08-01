package com.faketime;

public class FakeTimeManager {
    public enum TimeState { FOLLOW, INDEPENDENT, LOCKED }

    public static final long DAY_LENGTH = 24000L;

    private static final FakeTimeManager INSTANCE = new FakeTimeManager();

    private volatile TimeState state = TimeState.FOLLOW;
    private long baseTicks = 0;       // INDEPENDENT: 用户设定的时刻 (0~23999)
    private long anchorTicks = 0;     // INDEPENDENT: 进入独立时的 clientTicks
    private long lockedTicks = 0;     // LOCKED: 冻结时刻 (0~23999)
    private long clientTicks = 0;     // 客户端本地计数器，跨世界连续
    private long lastRealDayTime = 0; // 最近一次服务器真实时间

    private FakeTimeManager() {}

    public static FakeTimeManager getInstance() { return INSTANCE; }

    public void onTick() { this.clientTicks++; }

    public void updateRealDayTime(long realDayTime) { this.lastRealDayTime = realDayTime; }

    public long getFakeDayTime(long realDayTime) {
        return switch (this.state) {
            case FOLLOW -> realDayTime % DAY_LENGTH;
            case INDEPENDENT -> Math.floorMod(this.baseTicks + (this.clientTicks - this.anchorTicks), DAY_LENGTH);
            case LOCKED -> Math.floorMod(this.lockedTicks, DAY_LENGTH);
        };
    }

    /** 完整假时间（含天数，不取模）——用于月相等需要"第几天"的计算。 */
    public long getFakeFullDayTime(long realDayTime) {
        return switch (this.state) {
            case FOLLOW -> realDayTime;
            case INDEPENDENT -> this.baseTicks + (this.clientTicks - this.anchorTicks);
            case LOCKED -> this.lockedTicks;
        };
    }

    public long getDisplayTicks() { return this.getFakeDayTime(this.lastRealDayTime); }

    public void dragTo(long ticks) {
        if (this.state == TimeState.LOCKED) {
            this.lockedTicks = Math.floorMod(ticks, DAY_LENGTH);
        } else {
            this.baseTicks = Math.floorMod(ticks, DAY_LENGTH);
            this.anchorTicks = this.clientTicks;
            this.state = TimeState.INDEPENDENT;
        }
    }

    public void setLocked(boolean locked) {
        if (locked && this.state != TimeState.LOCKED) {
            this.lockedTicks = this.getDisplayTicks();
            this.state = TimeState.LOCKED;
        } else if (!locked && this.state == TimeState.LOCKED) {
            this.baseTicks = this.lockedTicks;
            this.anchorTicks = this.clientTicks;
            this.state = TimeState.INDEPENDENT;
        }
    }

    public void syncToServer() { this.state = TimeState.FOLLOW; }

    public TimeState getState() { return this.state; }
    public boolean isLocked() { return this.state == TimeState.LOCKED; }
    public long getBaseTicks() { return this.baseTicks; }
    public long getAnchorTicks() { return this.anchorTicks; }
    public long getLockedTicks() { return this.lockedTicks; }
    public long getClientTicks() { return this.clientTicks; }
    public long getLastRealDayTime() { return this.lastRealDayTime; }

    public void load(TimeState state, long baseTicks, long anchorTicks, long lockedTicks, long clientTicks, long lastRealDayTime) {
        this.state = state;
        this.baseTicks = baseTicks;
        this.anchorTicks = anchorTicks;
        this.lockedTicks = lockedTicks;
        this.clientTicks = clientTicks;
        this.lastRealDayTime = lastRealDayTime;
    }
}
