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

    /** vanilla DimensionType.timeOfDay = frac(dayTime/24000 - 0.25) 的 -6000 刻偏移。
     *  天空/太阳/雾等渲染（getTimeOfDay/getSunAngle）都基于偏移后的时刻：6:00(0刻) 天亮、
     *  正午(6000刻) 最亮、18:00(12000刻) 天黑、午夜(18000刻) 最暗。 */
    public static long skyTicks(long dayTicks) {
        return Math.floorMod(dayTicks - 6000L, DAY_LENGTH);
    }

    /** 复刻 vanilla DimensionType.getTimeOfDay 的平滑曲线（非线性）。
     *  vanilla: d0 = frac(dayTime/24000 - 0.25); d1 = 0.5 - cos(d0*PI)/2; return (2*d0+d1)/3。
     *  仅用线性 skyTicks/24000 近似会与 vanilla 在黎明/黄昏相差最多 ~840 刻（约 35 分钟），
     *  导致同刻下太阳位置与 vanilla 不一致（模组端太阳更低、升起更晚）——这正是"0 刻渲染
     *  与实际 0 刻不同"的根因。所有 getTimeOfDay/getSunAngle 重定向都必须用本方法。 */
    public static float getTimeOfDay(long dayTicks) {
        double d0 = (double) dayTicks / (double) DAY_LENGTH - 0.25D;
        d0 = d0 - Math.floor(d0); // Mth.frac
        double d1 = 0.5D - Math.cos(d0 * Math.PI) / 2.0D;
        return (float) ((d0 * 2.0D + d1) / 3.0D);
    }

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

    /** 同步到服务器：未锁定时恢复跟随（FOLLOW，时间流动）；
     *  已锁定时将锁定值跳到服务器当前时刻并保持锁定（冻结在服务器时间）。 */
    public void syncToServer() {
        if (this.state == TimeState.LOCKED) {
            this.lockedTicks = Math.floorMod(this.getRealDayTimeApprox(), DAY_LENGTH);
        } else {
            this.state = TimeState.FOLLOW;
        }
    }

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
