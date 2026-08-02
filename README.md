# FakeTimeMod

A client-side fake time mod — adds a time slider to the ESC pause menu so you can adjust the world time display locally, without affecting server logic.

## Use Cases

- **Screenshots / Photography**: freely drag the time bar to get the ideal sky color and lighting angle.
- **Shader Debugging**: observe shader effects at different times in real time, with shader mods such as Iris.
- **Building Previews**: check how a build looks under specific lighting without waiting for the in-game time to pass naturally.
- **Independent Time**: don't want to follow the server's day/night cycle? Drag the slider into independent mode, or lock to a specific moment.

## Installation

1. Install Minecraft **1.21.1** and **NeoForge 21.1.x** (note: incompatible with the Forge build — pick the jar that matches your loader).
2. Place `faketimemod-1.0.0-1.21.1-neoforge.jar` in the `mods` folder of your game directory.
3. Launch the game.

This is a **client-only** mod — no server-side installation needed.

## Usage

Press **ESC** in-game to open the pause menu, then click the **「Time」** button in the top-right corner to open the FakeTimeMod panel. The panel contains:

| Control | Description |
|---------|-------------|
| **Time slider** | Horizontal slider, left end = 6:00 (0 ticks), right end = 5:59 (23999 ticks). Dragging instantly changes the world time display and automatically enters **independent mode**. |
| **Lock checkbox** | Check to **freeze** the current time — the sun/sky stops moving. Uncheck to return to independent mode. |
| **Sync button** | Click to **restore server time** (follow mode), discarding all local time adjustments. |

### The Three Time States

| State | Description |
|-------|-------------|
| **FOLLOW** | Default state. Time fully follows the server. |
| **INDEPENDENT** | From the moment you drag the slider, fake time advances at the client's own pace (+1 per tick), decoupled from the server but still flowing. |
| **LOCKED** | Time frozen at a fixed moment, never advances. Great for screenshots. |

### Config Persistence

The time state and slider position are saved automatically to the client config file (`faketimemod-client.toml`) when you leave a world or close the game, and restored on the next launch.

## Time Conversion Table

One Minecraft day = 24000 game ticks; 1000 ticks = 1 game hour (for the clock conversion; about 50 real seconds). 0 ticks is sunrise (6:00), and the display uses a 24-hour clock: ticks 0-12000 are daytime, ticks 12000-24000 are night.

| Ticks | Time | Description |
|-------|------|-------------|
| 0 | 6:00 | Sunrise (default start of day) |
| 6000 | 12:00 | Noon, sun at its highest |
| 12000 | 18:00 | Sunset |
| 18000 | 0:00 | Midnight |
| 24000 | 6:00 | Next sunrise (same as 0 ticks) |

## Known Behavior

The following are intentional design choices of this mod:

- **Mob spawning and sleeping**: mob spawning and player sleeping are **always based on the server's real time**. Even if you drag the client time to day while the server is at night, mobs will still spawn and beds will judge sleepability by server time (verifiable on a LAN world).
- **Clock item**: the vanilla clock item's hands **always show the server's real time**, unaffected by fake time. This is because the clock renders via `ItemProperties` rather than `Level#getDayTime()`.
- **Iris compatibility**: Iris only calls `Level#getDayTime()` / `Level#getTimeOfDay()` to read the time; this mod injects both methods via Mixin, so the sun position, sky color, and ambient lighting in shaders all follow the fake time.
- **Create's cuckoo clock**: Create's cuckoo clock reads the time via `ClientLevel#getDayTime()`, which this mod also injects, so its hands follow the fake time — which matches the mod's intent (unifying all client-rendered time). If you want the cuckoo clock to always show real time, that's a feature request, not a bug.
- **Server `/time set` command**: a server-side `/time set` does not affect an already-adjusted fake time (fake time won't auto-sync to server changes unless you click the sync button manually).

## Known Limitations

- **moonBrightness / moonPhase in shaders**: shader packs usually obtain the moon phase through paths other than `Level#getDayTime()` (e.g. `Level#getMoonPhase()`); this mod does not inject those paths, so moonlight brightness (moonBrightness) and moon phase (moonPhase) in shaders are **still computed from the server's real time**.
- **Moon phase in LOCKED mode**: when time is locked, `getFakeFullDayTime()` returns a fixed value, so the moon phase stays at the phase of that fixed moment. To have the moon phase follow fake time, use INDEPENDENT mode instead of LOCKED mode.

## Building

Prerequisites: JDK 21, Gradle (use the bundled Gradle Wrapper).

```bash
git clone <repo-url>
cd FakeTimeMod
./gradlew build
```

The build artifact is at `build/libs/faketimemod-1.0.0-1.21.1-neoforge.jar`.

Run the dev environment client:

```bash
./gradlew runClient
```

## License

MIT
