# Task 9: GUI Refactor - Pause Screen Entry Button + FakeTimeScreen

## Status: DONE

**Commit:** `1a77f0f` on branch `feat/fake-time`

---

## Changes Summary

### 1. New File: `src/main/java/com/faketime/client/gui/FakeTimeScreen.java`

Independent Screen (extends `Screen`) containing the full time adjustment panel. All widgets are added via standard `addRenderableWidget()` -- no manual list manipulation:

- `TimeSlider` (slider for time adjustment) -- reused existing class
- `FakeTimeCheckbox` (lock time checkbox) -- reused existing class
- `FakeTimeSyncButton` (sync to server button) -- reused existing class
- Close button via `Button.builder()` calling `this.onClose()`

Rendering:
- Semi-transparent dark panel background (`g.fill`)
- 64-frame animated clock icon from vanilla `textures/item/clock_XX.png` atlas
- Current fake time and server real time display strings

Uses `this.font` (inherited from `Screen`) for text rendering.

### 2. Rewritten: `src/main/java/com/faketime/mixin/PauseScreenMixin.java`

**@Shadow approach used: `@Mixin(Screen.class)` + `instanceof PauseScreen` guard + manual list management (fallback method)**

Reason: Mixin AP 0.8.5 cannot resolve the inherited generic method `addRenderableWidget` through `PauseScreen`'s class hierarchy. This was the approach used in the original code and confirmed necessary after both the generic and non-generic `@Shadow` attempts on `@Mixin(PauseScreen.class)` produced AP warnings ("Cannot find target for @Shadow method").

Details:
- Injects at TAIL of `Screen.init`
- Guards with `instanceof PauseScreen` to only affect the pause screen
- Adds a single entry button (uses `Button.builder()`) at position `(width - 80, 5)` with size `75x20`
- Button text: translation key `gui.faketimemod.open` ("Time" / "时间")
- On click: `Minecraft.getInstance().setScreen(new FakeTimeScreen())`
- Button is added to `screen.renderables`, `this.children`, and `this.narratables`

### 3. Deleted: `src/main/java/com/faketime/client/gui/FakeTimePanelBackground.java`

The old `AbstractWidget` subclass that drew the time panel background and clock. Its rendering logic is now integrated directly into `FakeTimeScreen.render()`. The old three-list manual addition of this widget + slider + checkbox + sync button in PauseScreenMixin has been replaced by the single entry button approach.

### 4. Updated Lang Files

**zh_cn.json:**
```json
"gui.faketimemod.open": "时间",
"gui.faketimemod.close": "完成"
```

**en_us.json:**
```json
"gui.faketimemod.open": "Time",
"gui.faketimemod.close": "Done"
```

---

## Verification

### Build
```
./gradlew build --no-daemon --console=plain
BUILD SUCCESSFUL in 16s
13 actionable tasks: 9 executed, 4 up-to-date
```
Clean compilation with zero errors and zero warnings on the final build. The intermediate build with the `@Mixin(PauseScreen.class)` approach produced `@Shadow` mapping warnings (as expected), which was the trigger to switch to the `@Mixin(Screen.class)` fallback.

### Runtime (runClient)
- Client launched successfully, Forge 47.3.0 for MC 1.20.1
- Mixin injection confirmed in log:
  ```
  Mixing PauseScreenMixin from faketimemod.mixins.json into net.minecraft.client.gui.screens.Screen
  Renaming synthetic method lambda$faketime_addEntryButton$0(...)
  ```
- No mixin errors, no class loading failures, no exceptions related to FakeTimeMod
- Expected benign warnings only: Realms auth failure (no session), Forge version outdated notice

---

## In-Game User Testing Steps

1. Launch Minecraft 1.20.1 with Forge and FakeTimeMod installed
2. Enter a singleplayer world (or join a server)
3. Press `ESC` to open the pause screen
4. **Verify:** A "Time" / "时间" button appears at the top-right corner of the pause screen (position: right edge - 80px, y=5)
5. **Verify:** The button does NOT overlap with the "Disconnect" / "断开连接" button
6. Click the "Time" button
7. **Verify:** `FakeTimeScreen` opens centered on screen with:
   - Dark panel background with clock icon (animated, showing current fake time)
   - "Current: X ticks (HH:MM)" display (white text)
   - "Server time: X ticks (HH:MM)" display (gray text)
   - Time slider (draggable)
   - "Lock time" / "锁定时间" checkbox (clickable)
   - "Sync to server" / "同步到服务器" button
   - "Done" / "完成" button (closes screen back to pause menu)
8. **Verify slider interaction:** Drag the slider, check that the fake time display and clock icon update in real-time
9. **Verify checkbox interaction:** Check "Lock time", close and reopen pause screen, verify time is frozen
10. **Verify sync button:** Click "Sync to server", verify time resets to server time
11. Press `ESC` or click "Done" to return to pause screen, then resume game
