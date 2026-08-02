# FakeTimeMod

A client-side fake-time mod — provides a time slider on the ESC pause screen, allowing local adjustment of the world-time display without affecting server logic.

## Purpose

- **Screenshots / Photography**: Freely drag the time bar to get the ideal sky color and lighting angle.
- **Shader Debugging**: Combined with shader mods like Oculus, observe lighting effects at different times in real time.
- **Build Previews**: Check the visual appearance of builds under specific lighting conditions without waiting for in-game time to pass naturally.
- **Independent Time**: Don't want to follow the server's day/night cycle? Drag the slider to enter independent mode, or lock onto a specific moment.

## Installation

1. Install Minecraft **1.20.1** and **Forge 47.x** (47.3.0 recommended).
2. Place `faketimemod-1.0.0.jar` into the `mods` folder in the game directory.
3. Launch the game.

This mod is **client-side only** — no server-side installation is required.

## Usage

In game, press **ESC** to open the pause menu; a FakeTimeMod control panel appears at the bottom of the screen. The panel contains the following controls:

| Control | Description |
|------|------|
| **Time Slider** | A horizontal slider; the left end = 6:00 AM (0 ticks), the right end = 5:59 AM (23999 ticks). Dragging the slider immediately changes the world-time display. Dragging automatically enters **independent mode**. |
| **Lock Checkbox** | When checked, the current time is **frozen** — the sun/sky stops moving. Unchecking restores independent mode. |
| **Sync Button** | Click to **restore server time** (follow mode), discarding all local time adjustments. |

### Three Time States

| State | Description |
|------|------|
| **FOLLOW** | Default state; time fully follows the server. |
| **INDEPENDENT** | From the moment you drag the slider, the fake time advances on the client's own local cadence (+1 per tick). Decoupled from the server, but time still flows. |
| **LOCKED** | Time is frozen at a specific moment and does not advance. Suitable for screenshot scenarios. |

### Configuration Persistence

The time state and slider position are automatically saved to a client config file (`faketimemod-client.toml`) when you leave the world / close the game, and restored automatically on the next launch.

## Time Conversion Table

A Minecraft day = 24000 game ticks; 1000 ticks = 1 in-game hour (used for in-game clock conversion; roughly 50 seconds of real time). Tick 0 is sunrise (6:00 AM).

| Game Ticks | Real Time | Description |
|--------|----------|------|
| 0 | 6:00 AM | Sunrise (default start of the day) |
| 6000 | 12:00 PM | Noon, sun at its highest |
| 12000 | 6:00 PM | Sunset |
| 18000 | 12:00 AM | Midnight |
| 24000 | 6:00 AM | Next day's sunrise (equivalent to tick 0) |

## Known Behavior

The following behaviors are design choices of this mod and are expected:

- **Mob spawning and sleeping**: Mob spawning and the player's ability to sleep are **always based on the server's real time**. Even if you drag the time to daytime on the client, if it is night on the server, mobs still spawn as usual, and the bed's sleepability is judged by server time (this can be verified on a LAN server).
- **Clock items**: The hands of vanilla clock items **always show the server's real time**, unaffected by fake time. This is because the clock item's rendering goes through `ItemProperties` rather than `Level#getDayTime()`.
- **Oculus compatibility**: The Oculus shaders only fetch time via `Level#getDayTime()` / `Level#getTimeOfDay()`. This mod injects into both methods via Mixin, so the shader's sun position, sky color, and ambient lighting all change with the fake time.
- **Create cuckoo clock**: The Create mod's cuckoo clock fetches time via `ClientLevel#getDayTime()`, which this mod injects, so the cuckoo clock's hands are affected by fake time — but this is expected behavior for this mod (making all client-rendered time consistent). If you want the cuckoo clock to always show real time, that is a feature request rather than a bug.
- **Server `/time set` command**: A server's `/time set` command does not affect the client's already-set fake time (i.e., fake time does not auto-sync with server changes unless you manually click the sync button).

## Known Limitations

- **Oculus shaders' moonBrightness / moonPhase**: Shader packs usually fetch the moon phase through means other than `Level#getDayTime()` (e.g., `Level#getMoonPhase()`). This mod does not inject those paths, so the moon brightness (moonBrightness) and moon phase (moonPhase) in shaders are **still computed from the server's real time**.
- **Moon phase in LOCKED mode**: When time is locked, `getFakeFullDayTime()` returns a fixed tick value, so the moon phase is always the phase corresponding to that fixed moment. If you want the moon phase to also follow the fake time, use INDEPENDENT mode rather than LOCKED mode.

## Building

Prerequisites: JDK 17, Gradle (use the project's bundled Gradle Wrapper).

```bash
git clone <repo-url>
cd FakeTimeMod
./gradlew build
```

The build artifact is located at `build/libs/faketimemod-1.0.0.jar`.

Run the dev-environment client:

```bash
./gradlew runClient
```

## License

MIT
