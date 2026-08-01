# FakeTimeMod Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 一个 Forge 1.20.1 纯客户端模组：在暂停界面通过拖动条、指针时钟、锁定复选框和同步按钮，让玩家单独控制自己客户端显示的虚假世界时间，并完全兼容 Oculus 光影。

**Architecture:** Mixin 注入 `Level#getDayTime()`/`getTimeOfDay()` 让渲染管线（天空、光影）读到假时间，同时覆盖钟表物品的 `ItemProperties` 让钟表读服务器真实时间。核心状态机 `FakeTimeManager` 为纯 Java（无 MC 依赖，可 JUnit 测试）：FOLLOW（跟随）/ INDEPENDENT（独立流动）/ LOCKED（冻结）。GUI 通过 Mixin 注入 `PauseScreen#init()` 添加原生控件。

**Tech Stack:** Forge 1.20.1 (47.3.0) + Mixin (Forge 内置加载) + Gradle wrapper (MDK 自带) + Java 17 + JUnit 5（仅测纯逻辑层）

## Global Constraints

- Java 17（本机 `E:\_java\jdk-17.0.12` 已验证）
- 项目根目录 `E:\Claude Code files\FakeTimeMod`（已 git init）
- Forge 1.20.1-47.3.0，official mappings（代码中的方法名即游戏反混淆名，如 `getDayTime`）
- 包名 `com.faketime`，modid `faketimemod`，显示名 `FakeTimeMod`
- **`FakeTimeManager` 与 `FakeTimeFormatter` 不得引用任何 `net.minecraft.*` 类**（保证 JUnit 在纯 JVM 运行）
- Mixin 配置 `faketimemod.mixins.json` 位于 resources 根目录，refmap `faketimemod.refmap.json`，Mixin 类包名 `com.faketime.mixin`
- 所有客户端专用代码放 `src/main/java`，无需 `@OnlyIn` 标注（服务端不加载 mixin json 的 `client` 数组）
- Windows 10 + git bash；所有 gradle 命令用 `./gradlew`（wrapper）；网络为国内环境，依赖下载失败时重试或配置镜像（实现时处理，不在本计划阻塞）
- 时间换算基准：0 刻 = 6:00 AM，1000 刻 = 7:00 AM（1 刻 = 0.864 现实秒，1000 刻 = 1 现实小时）
- 每次任务结束必须 commit；每个 `runClient` 手动验证完成后杀掉进程（Ctrl+C / `taskkill`）

---

### Task 1: 项目脚手架（Forge MDK + Gradle + 主类 + Mixin 配置）

**Files:**
- Create: `build.gradle`
- Create: `settings.gradle`
- Create: `gradle.properties`
- Create: `src/main/resources/META-INF/mods.toml`
- Create: `src/main/resources/pack.mcmeta`
- Create: `src/main/resources/faketimemod.mixins.json`
- Create: `src/main/java/com/faketime/FakeTimeMod.java`
- Create: `run/` 目录（gitignore）
- Create: `.gitignore`

**Interfaces:**
- Consumes: 无
- Produces: 可编译运行的空模组框架；主类常量 `FakeTimeMod.MODID = "faketimemod"` 供后续所有类引用

- [ ] **Step 1: 下载并解压 Forge MDK**

```bash
cd "E:/Claude Code files/FakeTimeMod"
curl -L -o /tmp/forge-mdk.zip https://maven.minecraftforge.net/net/minecraftforge/forge/1.20.1-47.3.0/forge-1.20.1-47.3.0-mdk.zip
jar xf /tmp/forge-mdk.zip    # JDK 自带 jar 命令，解压 MDK 模板到项目根
rm -f /tmp/forge-mdk.zip
ls -la                    # 应看到 gradlew、gradlew.bat、build.gradle、gradle/ 等
```

若 curl 失败（网络问题）：重试两次；仍失败则手动下载 zip 到项目目录用 PowerShell 解压：
```powershell
Expand-Archive -Path .\forge-mdk.zip -DestinationPath .
```
解压后删除 MDK 自带的示例代码：`rm -rf src/main/java/com/example src/main/resources/META-INF/mods.toml src/main/resources/pack.mcmeta`（示例文件将被下面步骤覆盖）。

- [ ] **Step 2: 覆盖 build.gradle**

```gradle
plugins {
    id 'eclipse'
    id 'idea'
    id 'net.minecraftforge.gradle' version '[6.0,6.2)'
    id 'org.spongepowered.mixin' version '0.7.+'
}

version = '1.0.0'
group = 'com.faketime'
base { archivesName = 'faketimemod' }

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

minecraft {
    mappings channel: 'official', version: '1.20.1'
    runs {
        client {
            workingDirectory project.file('run')
            property 'forge.logging.markers', 'REGISTRIES'
            property 'forge.logging.console.level', 'debug'
            mods { faketimemod { source sourceSets.main } }
        }
        server {
            workingDirectory project.file('run')
            property 'forge.logging.markers', 'REGISTRIES'
            property 'forge.logging.console.level', 'debug'
            mods { faketimemod { source sourceSets.main } }
        }
    }
}

mixin {
    add sourceSets.main, 'faketimemod.refmap.json'
    config 'faketimemod.mixins.json'
}

repositories { mavenCentral() }

dependencies {
    minecraft 'net.minecraftforge:forge:1.20.1-47.3.0'
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

test { useJUnitPlatform() }
```

删除 MDK 自带 build.gradle 中 `data` run（本模组无 data generation）、`jar`/`publish` 块（保持最小）。

- [ ] **Step 3: 覆盖 settings.gradle、gradle.properties**

`settings.gradle` 保持 MDK 原样（`pluginManagement { repositories { gradlePluginPortal(); maven { url 'https://maven.minecraftforge.net/' } } }` 等）。`gradle.properties` 改为：

```properties
org.gradle.jvmargs=-Xmx2G
org.gradle.daemon=true
```

- [ ] **Step 4: 覆盖 mods.toml**

`src/main/resources/META-INF/mods.toml`：

```toml
modLoader="javafml"
loaderVersion="[47,)"
license="All rights reserved"

[[mods]]
modId="faketimemod"
version="1.0.0"
displayName="FakeTimeMod"
authors="xll"
description="Client-side fake world time display and adjustment. Pause-screen time slider, analog clock, time lock, server-sync button. Oculus compatible."

[[dependencies.faketimemod]]
modId="forge"
mandatory=true
versionRange="[47,)"
ordering="NONE"
side="CLIENT"

[[dependencies.faketimemod]]
modId="minecraft"
mandatory=true
versionRange="[1.20.1,1.21)"
ordering="NONE"
side="CLIENT"
```

- [ ] **Step 5: 创建 pack.mcmeta、mixin 配置、主类**

`src/main/resources/pack.mcmeta`：
```json
{
  "pack": {
    "description": "faketimemod resources",
    "pack_format": 15
  }
}
```

`src/main/resources/faketimemod.mixins.json`：
```json
{
  "required": true,
  "package": "com.faketime.mixin",
  "compatibilityLevel": "JAVA_17",
  "refmap": "faketimemod.refmap.json",
  "client": [
    "LevelMixin",
    "ItemPropertiesMixin",
    "PauseScreenMixin",
    "CuckooClockMixin"
  ],
  "injectors": {
    "defaultRequire": 1
  }
}
```

`src/main/java/com/faketime/FakeTimeMod.java`：
```java
package com.faketime;

import net.minecraftforge.fml.common.Mod;

@Mod(FakeTimeMod.MODID)
public class FakeTimeMod {
    public static final String MODID = "faketimemod";

    public FakeTimeMod() {
    }
}
```

`.gitignore`：
```
run/
build/
.gradle/
out/
```

（mixin json 中列出的 4 个 Mixin 类在 Task 3-7 才创建——此时构建会因 json 引用缺失类而失败吗？不会：Mixin json 只在运行时加载，编译期不校验。但为稳妥，此任务先建一个空占位 `LevelMixin` 在 Task 3 填充，其余 3 个类暂从 json 移除、在各自任务中加回。**修正：json 中暂时只保留 `LevelMixin`，Task 4/5/7 各自把新 Mixin 类名加回 json 的 client 数组。**）

- [ ] **Step 6: 构建验证**

```bash
cd "E:/Claude Code files/FakeTimeMod"
./gradlew build --no-daemon --console=plain 2>&1 | tail -20
```
Expected: `BUILD SUCCESSFUL`。首次运行会下载 Gradle 发行版与依赖（国内网络可能 5-30 分钟；失败则重试或配置阿里云镜像 `repositories`）。同时创建空 Mixin 占位类：

`src/main/java/com/faketime/mixin/LevelMixin.java`：
```java
package com.faketime.mixin;

import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Level.class)
public class LevelMixin {
}
```

- [ ] **Step 7: 启动验证**

```bash
./gradlew runClient --no-daemon --console=plain 2>&1 | tail -30
```
Expected: 游戏窗口正常打开到主菜单，无 mixin 报错；手动关闭窗口。看到主菜单即通过（`BUILD SUCCESSFUL` + 游戏窗口）。

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "chore: scaffold Forge 1.20.1 mod project (MDK, gradle, mixin config)"
```

---

### Task 2: FakeTimeManager 状态机 + FakeTimeFormatter（纯 Java，TDD）

**Files:**
- Create: `src/main/java/com/faketime/FakeTimeManager.java`
- Create: `src/main/java/com/faketime/FakeTimeFormatter.java`
- Create: `src/test/java/com/faketime/FakeTimeManagerTest.java`
- Create: `src/test/java/com/faketime/FakeTimeFormatterTest.java`

**Interfaces:**
- Consumes: 无（纯 Java，零 MC 依赖）
- Produces:
  - `FakeTimeManager.getInstance()` — 单例
  - `enum FakeTimeManager.TimeState { FOLLOW, INDEPENDENT, LOCKED }`
  - `void onTick()` — 客户端每 tick 调用，推进本地时钟
  - `void updateRealDayTime(long realDayTime)` — 记录服务器真实时间（锁定/拖动时读取基准）
  - `long getFakeDayTime(long realDayTime)` — 渲染用假时间（0~23999）
  - `long getDisplayTicks()` — 面板显示的当前时刻（0~23999，使用内部 lastRealDayTime）
  - `void dragTo(long ticks)` — 拖动时间条（未锁定→INDEPENDENT；已锁定→改锁定值）
  - `void setLocked(boolean locked)` — 勾选/取消锁定
  - `void syncToServer()` — 回到 FOLLOW
  - `TimeState getState()`、`boolean isLocked()`、`long getBaseTicks()`、`long getLockedTicks()`、`long getClientTicks()`
  - `void load(TimeState state, long baseTicks, long anchorTicks, long lockedTicks, long clientTicks, long lastRealDayTime)` — 配置恢复
  - `FakeTimeFormatter.formatTicks(long)` → `"1000 刻"`；`FakeTimeFormatter.formatClock(long)` → `"7:00 AM"`

- [ ] **Step 1: 写失败的测试**

`src/test/java/com/faketime/FakeTimeManagerTest.java`：
```java
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
}
```

`src/test/java/com/faketime/FakeTimeFormatterTest.java`：
```java
package com.faketime;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FakeTimeFormatterTest {
    @Test
    void clockConversions() {
        assertEquals("6:00 AM", FakeTimeFormatter.formatClock(0L));
        assertEquals("7:00 AM", FakeTimeFormatter.formatClock(1000L));
        assertEquals("12:00 PM", FakeTimeFormatter.formatClock(6000L));
        assertEquals("6:00 PM", FakeTimeFormatter.formatClock(12000L));
        assertEquals("12:00 AM", FakeTimeFormatter.formatClock(18000L));
        assertEquals("5:59 AM", FakeTimeFormatter.formatClock(23999L));
        assertEquals("6:00 AM", FakeTimeFormatter.formatClock(24000L)); // 取模
    }

    @Test
    void minutesRoundToHalfHours() {
        assertEquals("7:30 AM", FakeTimeFormatter.formatClock(1500L)); // 1000刻=1小时
    }

    @Test
    void formatTicks() {
        assertEquals("1000 刻", FakeTimeFormatter.formatTicks(1000L));
        assertEquals("23999 刻", FakeTimeFormatter.formatTicks(23999L));
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd "E:/Claude Code files/FakeTimeMod"
./gradlew test --no-daemon --console=plain 2>&1 | tail -10
```
Expected: FAIL —— `FakeTimeManager` / `FakeTimeFormatter` 类不存在（编译错误）。

- [ ] **Step 3: 实现 FakeTimeManager**

`src/main/java/com/faketime/FakeTimeManager.java`：
```java
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
```

`src/main/java/com/faketime/FakeTimeFormatter.java`：
```java
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
```

- [ ] **Step 4: 运行测试确认通过**

```bash
./gradlew test --no-daemon --console=plain 2>&1 | tail -10
```
Expected: PASS，全部 12 个测试绿。

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add FakeTimeManager state machine and time formatter with tests"
```

---

### Task 3: 事件驱动 + 渲染时间注入（LevelMixin）

**Files:**
- Modify: `src/main/java/com/faketime/mixin/LevelMixin.java`（填充）
- Create: `src/main/java/com/faketime/FakeTimeClient.java`（`@Mod.EventBusSubscriber`）

**Interfaces:**
- Consumes: `FakeTimeManager#onTick()`、`#updateRealDayTime(long)`
- Produces:
  - `FakeTimeClient` 类 — `@Mod.EventBusSubscriber(modid = FakeTimeMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)`，静态方法 `onClientTick`；此后 Mixin 与 GUI 都通过它驱动 manager
  - `LevelMixin` 注入完成：`Level#getDayTime()` 在客户端返回假时间（F3/isNight 等）
  - `LevelTimeAccessMixin` 注入完成：`LevelTimeAccess#dayTime()` 在客户端返回假时间（渲染链唯一根——天空/太阳/月亮/光影/Oculus）

- [ ] **Step 1: 实现 FakeTimeClient（事件驱动）**

`src/main/java/com/faketime/FakeTimeClient.java`：
```java
package com.faketime;

import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FakeTimeMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FakeTimeClient {
    private FakeTimeClient() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        FakeTimeManager manager = FakeTimeManager.getInstance();
        manager.onTick();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            // LevelData 不被注入，永远存服务器真实时间
            manager.updateRealDayTime(mc.level.getLevelData().getDayTime());
        }
    }
}
```

- [ ] **Step 2: 实现渲染时间注入（LevelMixin + LevelTimeAccessMixin）**

**1.20.1 渲染时间链（控制器已从官方源码确认，2026-08-01）：** `getTimeOfDay(float)` 定义在**接口** `net.minecraft.world.level.LevelTimeAccess`（default 方法，返回 `dimensionType().timeOfDay(dayTime())`），不在 `Level` 类；`dayTime()` 定义在接口 `net.minecraft.world.level.LevelAccessor`（default 方法，返回 `getLevelData().getDayTime()`）。`getSunAngle`（Level 类）、`getSkyDarken`/`getSkyColor`/`getCloudColor`/`getStarBrightness`（ClientLevel 类）、`getMoonBrightness`/`getMoonPhase`（LevelTimeAccess）全部从这两条接口方法派生——**完全绕过 `Level#getDayTime()`**。

因此需要两个注入点：
1. `LevelMixin` 注入 `Level#getDayTime()`（覆盖 F3 调试显示、`isNight()` 等 Level 方法调用者）
2. `LevelTimeAccessMixin` 注入 `LevelTimeAccess#dayTime()`（**渲染时间唯一根**——覆盖天空/太阳/月亮/光影/Oculus）

`src/main/java/com/faketime/mixin/LevelMixin.java`：
```java
package com.faketime.mixin;

import com.faketime.FakeTimeManager;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelMixin {

    /** 仅客户端：getDayTime 返回假时间（F3、isNight 等 Level 方法调用者）。 */
    @Inject(method = "getDayTime", at = @At("HEAD"), cancellable = true)
    private void faketime_getDayTime(CallbackInfoReturnable<Long> cir) {
        if (((Level) (Object) this).isClientSide) {
            cir.setReturnValue(FakeTimeManager.getInstance().getFakeDayTime(this.getLevelData().getDayTime()));
        }
    }
}
```

`src/main/java/com/faketime/mixin/LevelTimeAccessMixin.java`：
```java
package com.faketime.mixin;

import com.faketime.FakeTimeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelTimeAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelTimeAccess.class)
public interface LevelTimeAccessMixin {

    /** 渲染时间根：dayTime() 是 getTimeOfDay/getMoonBrightness/getMoonPhase 的唯一时间来源，
     *  注入它即可让天空/太阳/月亮/光影全部使用假时间。
     *  真实时间读取（getLevelData().getDayTime()）为 LevelData 接口方法，不受注入影响。 */
    @Inject(method = "dayTime", at = @At("HEAD"), cancellable = true)
    default void faketime_dayTime(CallbackInfoReturnable<Long> cir) {
        LevelTimeAccess self = (LevelTimeAccess) (Object) this;
        if (self instanceof Level level && level.isClientSide) {
            long real = self.getLevelData().getDayTime();
            cir.setReturnValue(FakeTimeManager.getInstance().getFakeDayTime(real));
        }
    }
}
```

注意：目标为接口时 Mixin 类声明为 `interface`，注入方法必须带 `default` 实现；若编译或运行报错，可改为 abstract class + 抽象注入方法（Mixin 对接口注入两种都支持，以编译为准）。`isClientSide` 判断保证服务端不劫持。

- [ ] **Step 3: 构建 + 回归启动验证**

```bash
./gradlew build --no-daemon --console=plain 2>&1 | tail -10
./gradlew runClient --no-daemon --console=plain 2>&1 | tail -30
```
Expected: BUILD SUCCESSFUL；游戏启动无 mixin 报错（默认 FOLLOW 模式，行为与未装模组完全一致——这是回归基线）。进单人世界（创造）确认天空正常。关闭游戏。

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: drive FakeTimeManager from client ticks; inject fake time into Level getDayTime/getTimeOfDay"
```

---

### Task 4: 钟表物品修复（ItemPropertiesMixin）

**Files:**
- Create: `src/main/java/com/faketime/mixin/ItemPropertiesMixin.java`
- Modify: `src/main/resources/faketimemod.mixins.json`（client 数组加 `"ItemPropertiesMixin"`）

**Interfaces:**
- Consumes: `LevelMixin` 已注入（此后 `getTimeOfDay` 返回假时间）
- Produces: 钟表物品（`Items.CLOCK`）的 `time` 物品属性函数被覆盖为读取 `level.getLevelData().getDayTime()` —— 钟表指针永远显示服务器真实时间

- [ ] **Step 1: 实现 ItemPropertiesMixin**

`src/main/java/com/faketime/mixin/ItemPropertiesMixin.java`：
```java
package com.faketime.mixin;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemProperties.class)
public class ItemPropertiesMixin {

    /** 静态块末尾覆盖 CLOCK 的 time 属性函数：钟表指针改读服务器真实时间。
     *  原函数读 level.getTimeOfDay()（已被注入为假时间），此处覆盖为读 LevelData 真值。 */
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void faketime_overrideClock(CallbackInfo ci) {
        ItemProperties.register(Items.CLOCK, new ResourceLocation("time"),
                (stack, level, entity, seed) -> {
                    if (level == null) return 0.0F;
                    long realTicks = level.getLevelData().getDayTime();
                    return Math.floorMod(realTicks, 24000L) / 24000.0F;
                });
    }
}
```

`ItemPropertyFunction` 的函数式签名（1.20.1）：`float call(ItemStack, @Nullable ClientLevel, @Nullable LivingEntity, int)`。原函数中 `entity.getUseItem()` 特判（手持使用中指针停 0）在覆盖中省略——钟表不被使用，无影响；若控制台报 lambda 类型不匹配，按反编译的 `ItemProperties` 源码核对 `call` 签名。

- [ ] **Step 2: 更新 mixin json**

`faketimemod.mixins.json` 的 `client` 数组变为：
```json
"client": [
    "LevelMixin",
    "ItemPropertiesMixin"
],
```

- [ ] **Step 3: 构建 + 回归启动验证**

```bash
./gradlew build --no-daemon --console=plain 2>&1 | tail -10
./gradlew runClient --no-daemon --console=plain 2>&1 | tail -30
```
Expected: BUILD SUCCESSFUL；启动无报错。单人世界物品栏放一个钟表（`/give @s clock`），指针正常转动（回归基线：此时 fake=FOLLOW 与真实一致，无法区分真假——行为验证在 Task 5 完成后统一做）。关闭游戏。

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: override clock item time property to show real server time"
```

---

### Task 5: 暂停界面面板（GUI）

**Files:**
- Create: `src/main/java/com/faketime/client/gui/FakeTimePanelBackground.java`
- Create: `src/main/java/com/faketime/client/gui/TimeSlider.java`
- Create: `src/main/java/com/faketime/client/gui/FakeTimeCheckbox.java`
- Create: `src/main/java/com/faketime/client/gui/FakeTimeSyncButton.java`
- Create: `src/main/java/com/faketime/mixin/PauseScreenMixin.java`
- Modify: `src/main/resources/faketimemod.mixins.json`（client 数组加 `"PauseScreenMixin"`）

**Interfaces:**
- Consumes: `FakeTimeManager`（`getDisplayTicks()`、`getLastRealDayTime()`、`dragTo`、`setLocked`、`syncToServer`、`isLocked`）、`FakeTimeFormatter`、`FakeTimeClient`
- Produces: 暂停界面底部面板 —— 时钟表盘（用 MC 原生 `textures/item/clock_%02d.png` 64 帧动画贴图按假时间选帧）、时间条（`SliderWidget` 子类）、锁定复选框、同步按钮、服务器真实时间文字；`PauseScreenMixin` 在 `init()` 末尾注册所有组件

- [ ] **Step 1: 实现面板背景 widget（时钟 + 文字）**

`src/main/java/com/faketime/client/gui/FakeTimePanelBackground.java`：
```java
package com.faketime.client.gui;

import com.faketime.FakeTimeFormatter;
import com.faketime.FakeTimeManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class FakeTimePanelBackground extends AbstractWidget {
    private static final ResourceLocation CLOCK_FRAME = new ResourceLocation("textures/item/clock_%02d.png");

    private final FakeTimeManager manager;

    public FakeTimePanelBackground(int x, int y, int width, int height) {
        super(x, y, width, height, Component.literal("FakeTimePanel"));
        this.manager = FakeTimeManager.getInstance();
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // 半透明深色底 + 边框
        g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xC0202020);
        g.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + getHeight() - 1, 0xC0282828);

        long fake = this.manager.getDisplayTicks();
        long real = this.manager.getLastRealDayTime();

        // 时钟：MC 钟表 64 帧动画贴图，按假时间选帧（帧 0 = 6:00 AM，帧 32 = 6:00 PM）
        int frame = (int) (fake / (float) FakeTimeManager.DAY_LENGTH * 64.0F) % 64;
        g.blit(ResourceLocation.withDefaultNamespace(String.format("textures/item/clock_%02d.png", frame)),
                getX() + 10, getY() + 10, 0, 0, 32, 32, 32, 32);

        // 当前时间（假时间）
        g.drawString(this.font, Component.literal(
                "当前: " + FakeTimeFormatter.formatTicks(fake) + "  (" + FakeTimeFormatter.formatClock(fake) + ")"),
                getX() + 50, getY() + 12, 0xFFFFFF);

        // 服务器真实时间
        g.drawString(this.font, Component.literal(
                "服务器真实时间: " + FakeTimeFormatter.formatTicks(real) + "  (" + FakeTimeFormatter.formatClock(real) + ")"),
                getX() + 50, getY() + 24, 0x808080);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
    }
}
```

注意：`ResourceLocation.withDefaultNamespace` 若在 1.20.1 不存在（它是 1.21 加入），改用 `new ResourceLocation("textures/item/clock_%02d.png")`。实现时以编译报错为准二选一。

- [ ] **Step 2: 实现时间条 TimeSlider**

`src/main/java/com/faketime/client/gui/TimeSlider.java`：
```java
package com.faketime.client.gui;

import com.faketime.FakeTimeManager;
import net.minecraft.client.gui.components.SliderWidget;
import net.minecraft.network.chat.Component;

public class TimeSlider extends SliderWidget {
    private final FakeTimeManager manager = FakeTimeManager.getInstance();
    private boolean dragging = false;

    public TimeSlider(int x, int y, int width, int height) {
        super(x, y, width, height, Component.literal(""), 0.0D);
        syncValue();
    }

    private void syncValue() {
        this.value = this.manager.getDisplayTicks() / (double) FakeTimeManager.DAY_LENGTH;
        this.updateMessage();
    }

    @Override
    public void renderWidget(net.minecraft.client.gui.GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (!this.dragging) this.syncValue(); // 未拖动时滑块随时间流动
        super.renderWidget(g, mouseX, mouseY, partialTick);
    }

    @Override
    protected void updateMessage() {
        long ticks = this.manager.getDisplayTicks();
        this.setMessage(Component.literal(com.faketime.FakeTimeFormatter.formatTicks(ticks)));
    }

    @Override
    protected void applyValue() {
        this.dragging = true;
        this.manager.dragTo((long) (this.value * FakeTimeManager.DAY_LENGTH));
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        this.dragging = false;
        this.syncValue();
        super.onRelease(mouseX, mouseY);
    }
}
```

- [ ] **Step 3: 实现复选框与同步按钮**

`src/main/java/com/faketime/client/gui/FakeTimeCheckbox.java`：
```java
package com.faketime.client.gui;

import com.faketime.FakeTimeManager;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.network.chat.Component;

public class FakeTimeCheckbox extends Checkbox {
    private final FakeTimeManager manager = FakeTimeManager.getInstance();

    public FakeTimeCheckbox(int x, int y, int width, int height) {
        super(x, y, width, height, Component.translatable("gui.faketimemod.lock"), manager.isLocked(),
                false, (cb, selected) -> manager.setLocked(selected), Checkbox.DEFAULT_TEXTURE_LOCATION, false, 8);
    }
}
```
注意：1.20.1 `Checkbox` 构造签名为 `Checkbox(int, int, int, int, Component, boolean, boolean, OnValueChange, ResourceLocation, boolean, int)`（private 构造，公开入口是 `Checkbox.builder(...)`）。**若构造不可用，改用 builder**：
```java
super(Checkbox.builder(Component.translatable("gui.faketimemod.lock"), Minecraft.getInstance().font)
        .pos(x, y).width(width).height(height)
        .selected(manager.isLocked())
        .onValueChange((cb, selected) -> manager.setLocked(selected))
        .build()); // Checkbox.builder().build() 返回 Checkbox
```
`Checkbox` 有 `protected Checkbox(int x, int y, int width, int height, Component message, boolean selected, boolean showLabel, OnValueChange onValueChange, ResourceLocation spriteLocation, boolean drawShadow, int gap)`——保护构造子类可用。实现时二选一（编译为准）。

`src/main/java/com/faketime/client/gui/FakeTimeSyncButton.java`：
```java
package com.faketime.client.gui;

import com.faketime.FakeTimeManager;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class FakeTimeSyncButton extends Button {
    private final FakeTimeManager manager = FakeTimeManager.getInstance();

    public FakeTimeSyncButton(int x, int y, int width, int height) {
        super(x, y, width, height, Component.translatable("gui.faketimemod.sync"),
                btn -> manager.syncToServer(), Button.DEFAULT_NARRATION);
    }
}
```
（1.20.1 `Button` 公开构造 `Button(int, int, int, int, Component, OnPress, CreateNarration)`；若不可用则用 `Button.builder(...).bounds(...).build()`。）

- [ ] **Step 4: 实现 PauseScreenMixin 注册面板**

`src/main/java/com/faketime/mixin/PauseScreenMixin.java`：
```java
package com.faketime.mixin;

import com.faketime.client.gui.FakeTimeCheckbox;
import com.faketime.client.gui.FakeTimePanelBackground;
import com.faketime.client.gui.FakeTimeSyncButton;
import com.faketime.client.gui.TimeSlider;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Widget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin {

    @Shadow
    protected abstract <T extends GuiEventListener & Widget & NarratableEntry> T addRenderableWidget(T widget);

    @Inject(method = "init", at = @At("TAIL"))
    private void faketime_addPanel(CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        int panelWidth = 220;
        int panelHeight = 116;
        int x = (screen.width - panelWidth) / 2;
        int y = screen.height - panelHeight - 12;

        this.addRenderableWidget((T) new FakeTimePanelBackground(x, y, panelWidth, panelHeight));
        this.addRenderableWidget((T) new TimeSlider(x + 10, y + 46, panelWidth - 20, 20));
        this.addRenderableWidget((T) new FakeTimeCheckbox(x + 10, y + 72, 14, 20));
        this.addRenderableWidget((T) new FakeTimeSyncButton(x + 128, y + 72, 82, 20));
    }
}
```
（`addRenderableWidget` 的泛型签名在 1.20.1 为 `protected <T extends GuiEventListener & Widget & NarratableEntry> T addRenderableWidget(T)`；`@Shadow` 泛型方法需按实际签名写，编译报错时去掉泛型用 `Object` 强转。若 shadow 泛型麻烦，改用 `@Invoker("addRenderableWidget")` 接口 + `(Screen)(Object)this` 无法调用 protected —— 用 shadow 正确实现即可。）

- [ ] **Step 5: 更新 mixin json + 添加 lang 文件**

`faketimemod.mixins.json` 的 `client` 数组：
```json
"client": [
    "LevelMixin",
    "ItemPropertiesMixin",
    "PauseScreenMixin"
],
```

`src/main/resources/assets/faketimemod/lang/zh_cn.json`：
```json
{
  "gui.faketimemod.lock": "锁定时间",
  "gui.faketimemod.sync": "同步到服务器"
}
```
`src/main/resources/assets/faketimemod/lang/en_us.json`（同一结构，英文 `Lock time` / `Sync to server`）。

- [ ] **Step 6: 构建 + 完整手动功能验证**

```bash
./gradlew build --no-daemon --console=plain 2>&1 | tail -10
./gradlew runClient --no-daemon --console=plain 2>&1 | tail -30
```

手动验证（单人创造世界，按 ESC）：
1. 面板出现在暂停界面底部，显示时钟 + 文字
2. 拖动时间条 → 天空/太阳立即变化；松开后时间条继续流动（未锁定）
3. 勾选锁定 → 时间条冻结，太阳不动
4. 锁定中拖动 → 时间跳变后冻结在新位置
5. 点同步按钮 → 恢复真实时间（FOLLOW）
6. 物品栏放钟表 → 钟表指针跟随服务器时间（与假时间不同步）
7. F3 界面时间显示 = 假时间
8. 全部通过后关闭游戏

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: add pause-screen time panel (slider, clock, lock, sync) with PauseScreen mixin"
```

---

### Task 6: 配置持久化

**Files:**
- Create: `src/main/java/com/faketime/FakeTimeConfig.java`
- Modify: `src/main/java/com/faketime/FakeTimeMod.java`（构造中注册配置）
- Modify: `src/main/java/com/faketime/FakeTimeClient.java`（GUI 操作后保存）

**Interfaces:**
- Consumes: `FakeTimeManager#load(...)`、`#getState()`、`#getBaseTicks()`、`#getAnchorTicks()`、`#getLockedTicks()`、`#getClientTicks()`、`#getLastRealDayTime()`
- Produces: `FakeTimeConfig.save()` / `FakeTimeConfig.load()` —— Forge ModConfigSpec（CLIENT 侧）持久化五项状态；启动时恢复到 manager

- [ ] **Step 1: 实现 FakeTimeConfig**

`src/main/java/com/faketime/FakeTimeConfig.java`：
```java
package com.faketime;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class FakeTimeConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.EnumValue<FakeTimeManager.TimeState> STATE =
            BUILDER.comment("Time state: FOLLOW (follow server), INDEPENDENT (free-running), LOCKED")
                    .defineEnum("state", FakeTimeManager.TimeState.FOLLOW);
    private static final ForgeConfigSpec.LongValue BASE_TICKS = BUILDER.define("baseTicks", 0L);
    private static final ForgeConfigSpec.LongValue ANCHOR_TICKS = BUILDER.define("anchorTicks", 0L);
    private static final ForgeConfigSpec.LongValue LOCKED_TICKS = BUILDER.define("lockedTicks", 0L);
    private static final ForgeConfigSpec.LongValue CLIENT_TICKS = BUILDER.define("clientTicks", 0L);
    private static final ForgeConfigSpec.LongValue LAST_REAL_DAY_TIME = BUILDER.define("lastRealDayTime", 0L);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private FakeTimeConfig() {}

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SPEC);
    }

    public static void save(FakeTimeManager m) {
        STATE.set(m.getState());
        BASE_TICKS.set(m.getBaseTicks());
        ANCHOR_TICKS.set(m.getAnchorTicks());
        LOCKED_TICKS.set(m.getLockedTicks());
        CLIENT_TICKS.set(m.getClientTicks());
        LAST_REAL_DAY_TIME.set(m.getLastRealDayTime());
        SPEC.save();
    }

    public static void load(FakeTimeManager m) {
        m.load(STATE.get(), BASE_TICKS.get(), ANCHOR_TICKS.get(), LOCKED_TICKS.get(),
                CLIENT_TICKS.get(), LAST_REAL_DAY_TIME.get());
    }
}
```

- [ ] **Step 2: 挂接加载与保存**

`FakeTimeMod.java` 构造改为：
```java
public FakeTimeMod() {
    FakeTimeConfig.register();
}
```

`FakeTimeClient.java` 增加两个事件：
```java
@Mod.EventBusSubscriber(modid = FakeTimeMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FakeTimeClient {
    ...
    @SubscribeEvent
    public static void onConfigLoad(net.minecraftforge.fml.event.config.ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == FakeTimeConfig.SPEC) {
            FakeTimeConfig.load(FakeTimeManager.getInstance());
        }
    }

    @SubscribeEvent
    public static void onConfigReload(net.minecraftforge.fml.event.config.ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == FakeTimeConfig.SPEC) {
            FakeTimeConfig.load(FakeTimeManager.getInstance());
        }
    }
}
```

GUI 交互后保存：在 `FakeTimePanelBackground` 不便挂——改为在 `FakeTimeClient.onClientTick` 内检测状态快照变化后保存（简单可靠，避免改 GUI 类）：
```java
// FakeTimeClient 静态字段
private static FakeTimeManager.TimeState lastSavedState = null;
private static long lastSavedTicks = Long.MIN_VALUE;
private static long lastSavedBase = Long.MIN_VALUE;

// onClientTick 内追加：
if (manager.getState() != lastSavedState
        || manager.getLockedTicks() != lastSavedTicks
        || manager.getBaseTicks() != lastSavedBase) {
    lastSavedState = manager.getState();
    lastSavedTicks = manager.getLockedTicks();
    lastSavedBase = manager.getBaseTicks();
    FakeTimeConfig.save(manager);
}
```
（`clientTicks` 每次 tick 都变，不纳入变更检测；`anchorTicks` 随 dragTo 设置故由 `lastSavedBase` 变化间接触发保存。）

- [ ] **Step 3: 构建 + 手动验证持久化**

```bash
./gradlew build --no-daemon --console=plain 2>&1 | tail -10
./gradlew runClient --no-daemon --console=plain 2>&1 | tail -30
```
手动验证：
1. 单人世界，拖动时间条到 1000 刻 → 退出世界 → 重进 → 时间仍显示 1000 刻起（INDEPENDENT 保持）
2. 勾选锁定 → 退出游戏 → 重开游戏 → 进入世界 → 仍锁定
3. 点同步 → 状态保存为 FOLLOW；重启后仍 FOLLOW
4. 检查 `run/config/faketimemod-client.toml` 存在且数值合理

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: persist fake time state via forge client config"
```

---

### Task 7: 机械动力布谷鸟钟兼容（可选 Mixin）

**Files:**
- Create: `src/main/java/com/faketime/mixin/CuckooClockMixin.java`
- Modify: `src/main/resources/faketimemod.mixins.json`（client 数组加 `"CuckooClockMixin"`）

**Interfaces:**
- Consumes: 无新接口（`@Pseudo` + `@Require(0)` 可选注入）
- Produces: 布谷鸟钟 `clientTick` 中读取的时间改为服务器真实时间（`level.getLevelData().getDayTime()`）；未安装 Create 时静默跳过

- [ ] **Step 1: 确认 Create 布谷鸟钟类路径与时间来源**

Create 1.20.1 版本为 0.5.1（`create-1.20.1-0.5.1.*.jar`）。**需要用户提供 Create jar 或从 CurseForge 下载**（`https://www.curseforge.com/minecraft/mc-mods/create` → Files → 1.20.1 → 0.5.1），放入 `run/mods/` 后运行一次 `runClient`，然后从 `.gradle/caches` 或 `run/mods` 中用 `jar tf` 查找：
```bash
cd "E:/Claude Code files/FakeTimeMod"
jar tf run/mods/create-*.jar | grep -i cuckoo
```
Expected: 找到类似 `com/simibubi/create/content/contraptions/clock/CuckooClockBlockEntity.class` 的类（0.5.1 实际路径以 jar 内容为准）。

用 `javap -c -p` 反编译该类确认 `clientTick`（若存在）是否调用 `getDayTime()`：
```bash
javap -c -p -classpath run/mods/create-*.jar com.simibubi.create.content.contraptions.clock.CuckooClockBlockEntity 2>/dev/null | grep -A3 -B3 getDayTime | head -30
```

- [ ] **Step 2: 实现可选 Mixin**

`src/main/java/com/faketime/mixin/CuckooClockMixin.java`（类路径以 Step 1 实测为准）：
```java
package com.faketime.mixin;

import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.throwables.CompatibilityLevel;

/**
 * 可选适配：布谷鸟钟读取的时间改为服务器真实时间。
 * 未安装 Create 时 @Pseudo 保证静默跳过；@Require(0) 防止因目标缺失导致崩溃。
 */
@Pseudo
@Mixin(targets = "com.simibubi.create.content.contraptions.clock.CuckooClockBlockEntity", remap = false)
public class CuckooClockMixin {

    // 注入点取决于 Step 1 反编译结果：
    // 若 clientTick 中为 long t = this.level.getDayTime() 之类的赋值：
    // @Inject(method = "clientTick", at = @At("HEAD")) 记录 or 用 ModifyVariable 替换局部变量
}
```
**若 Step 1 反编译发现布谷鸟钟不读 `getDayTime()`（例如时间快照存于服务端 BlockEntity 数据，客户端只是播放动画）→ 本任务无需任何注入，直接删除占位类并在 json 中不加此项，跳至 Step 4。** 以实测为准，不为注入而注入。

若确实需要注入（`clientTick` 内调用 `getDayTime()`），标准做法：
```java
@Inject(method = "clientTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getDayTime()J", remap = false), cancellable = true)
```
（Create 的字节码不经混淆重映射，`remap = false`；具体 target 以 `javap` 输出为准。）

- [ ] **Step 3: 验证（无 Create / 有 Create 两种环境）**

```bash
./gradlew build --no-daemon --console=plain 2>&1 | tail -10
```
无 Create 环境：`run/mods/` 不放置 Create → runClient 启动无报错，日志无 mixin 异常（`@Require(0)` 生效）。
有 Create 环境：`run/mods/` 放置 Create 0.5.1 → 世界内放置布谷鸟钟 → 指针显示**服务器真实时间**（先把假时间拖到明显不同，如拖到 1000 刻白天，服务器时间设晚上 → 布谷鸟钟显示晚上时间）。

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: optional Create cuckoo-clock compatibility (real server time)"
```

---

### Task 8: 最终验证 + 文档 + 发布

**Files:**
- Create: `README.md`
- Modify: 无（若手动测试发现问题则修复）

**Interfaces:**
- Consumes: 全部已完成功能
- Produces: 可发布的 mod jar + 使用文档

- [ ] **Step 1: 完整手动测试清单**

运行 `./gradlew runClient`，逐项验证（本地局域网服务器 + 单人世界两种）：
- [ ] 拖动时间条 → 天空/太阳立即变化
- [ ] 未锁定 → 时间条自动前进
- [ ] 锁定 → 时间冻结，太阳不动
- [ ] 同步按钮 → 恢复真实时间
- [ ] 服务器晚上、客户端白天 → 怪物正常生成、床正常可睡（局域网服验证：把假时间拖到白天，服务器保持夜晚，确认僵尸照常生成、床能入睡）
- [ ] 服务器 `/time set day` → 客户端已调整的假时间不受影响
- [ ] 钟表物品指针 = 服务器时间
- [ ] 安装 Oculus (1.20.1) + 光影包（如 Complementary Reimagined）→ 光影下太阳位置、光照色随假时间变化；无报错
- [ ] F3 显示假时间
- [ ] 重启游戏 → 状态保持
- [ ] 无 Create 时启动无报错

全部通过后关闭游戏。

- [ ] **Step 2: 写 README.md**

`README.md` 内容：模组用途、安装方法（Forge 1.20.1 + 拖入 mods 文件夹）、使用方法（ESC 面板各控件说明）、时间换算表（0刻=6:00AM / 6000刻=12:00PM / 12000刻=6:00PM / 18000刻=12:00AM）、已知行为（刷怪/睡觉按服务器时间、钟表按服务器时间、Oculus 兼容）、构建方法（`./gradlew build`）。

- [ ] **Step 3: 构建发布 jar**

```bash
./gradlew build --no-daemon --console=plain 2>&1 | tail -10
ls build/libs/faketimemod-1.0.0.jar
```
Expected: jar 存在。用 `jar tf` 抽查 jar 内含 `faketimemod.mixins.json`、`faketimemod-refmap.json`（注意 refmap 文件名实际为 `faketimemod.refmap.json`，以 build 目录中 mixin 插件输出为准）、`META-INF/mods.toml`。

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "docs: add README; release build"
```

---

## Self-Review（计划编写者自查记录）

**1. Spec 覆盖：**
- §1 需求 1-4（面板/拖动/锁定/服务器时间不影响）→ Task 2 + Task 5 + Task 6
- §1 需求 5（Oculus 兼容）→ Task 3（注入 getDayTime/getTimeOfDay 即 Oculus 唯一时间入口）
- §1 需求 6（刷怪/睡觉不处理）→ 明确不做，Task 8 手动验证确认不受影响
- §1 需求 7（钟表/第三方时钟真实时间）→ Task 4 + Task 7
- §2 架构 → 对应各 Task
- §3 状态机（修订版：FOLLOW/INDEPENDENT/LOCKED）→ Task 2
- §4 Mixin 表 5 项 → Task 3（#1#2）、Task 4（#3）、Task 7（#4）、#5 为不注入约定
- §5 GUI → Task 5
- §6 边界 → Task 6（重进保持）、Task 8（/time set 验证）
- §7 测试 → Task 2 单测 + Task 5/8 手动清单
- §8 构建 → Task 1

**2. 占位符扫描：** 无 TBD/TODO；Task 7 反编译未知点已给出明确决策规则（以 javap 实测为准），非占位。

**3. 类型一致性：** `dragTo/setLocked/syncToServer/load/getDisplayTicks/updateRealDayTime` 各 Task 使用一致；`FakeTimeFormatter.formatTicks/formatClock` 一致；`TimeState` 枚举一致。Task 6 配置字段与 `load` 签名（6 参数）一致。Task 5 中 `Checkbox`/`Button` 构造与 `addRenderableWidget` shadow 的不确定性已注明二选一规则（编译为准）。
