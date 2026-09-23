# Ring Racers Android

Android port scaffold for the official Ring Racers `v2.4` source. The app targets
64-bit Android devices (API 28 or newer) and uses SDL2 with the engine's existing
GLES2 renderer.

## Data folders (hybrid storage)

- Game `.pk3` core assets: extracted on first launch to the app-private
  `files/game` directory.
- User data (`-home`): the engine appends `RingRacers` (Android `DEFAULTDIR`,
  see `src/doomdef.h`), so passing the storage root yields:
  - Shared mode (visible): `/sdcard/RingRacers` with `addons/` and
    `addons/downloads/` pre-created. Requires All-files access on Android 11+
    (`MANAGE_EXTERNAL_STORAGE`, requested from the loading screen). This is the
    folder to open in the file manager / over USB to copy mods.
  - Fallback (private): app-specific external dir, then internal files dir.
    The game and mod auto-downloads still work, but the folder is not browsable.
- Optional: the loading screen has "Elegir carpeta" to pick a different storage
  root (primary storage only — the native engine needs a real filesystem path,
  so SD/OTG without OS path mapping won't work). Default stays `/sdcard`.
  Pick the folder that will CONTAIN `RingRacers` (e.g. `/sdcard` itself): if
  you pick `RingRacers` — or a folder with `bios.pk3` directly inside — the
  app normalizes it so the game folder is never nested twice.
- No-assets builds: copy the 16 files over USB either as
  `RingRacers/game/<file>` or PC-style `RingRacers/<file>`; the app imports
  them on first launch and refuses to start the engine until `bios.pk3`
  is present (instead of a cryptic "Expected in /").

The APK only bundles core runtime assets (see `stageGameAssets` in
`app/build.gradle`). It never packages `ringconfig.cfg`, profiles, replays,
logs, addons or server data.

## Touch controls

Base buttons map 1:1 to the engine keyboard defaults (`src/g_input.c`):

- D-pad: steer; up/down also aim items and choose trick directions
- `GO` (`A`): accelerate and menu accept
- `LOOK` (`B`): look backward
- `SPIN` (`C`): spindash, fastfall, and menu extra action
- `BRAKE` (`X`): brake and menu back
- `BAIL` (`Y`): Ring Bail
- `VOTE` (`Z`): multiplayer vote action
- `ITEM` (`L`): use items and Rings
- `DRIFT` (`R`): drift
- `CHAT` (`T`): talk + opens the keyboard
- Pause icon: Start/pause

Extra buttons (left cluster, draggable in EDIT mode):

- `RANK` (`TAB`): rankings, works out of the box (`gc_rankings` default).
- `CON` (`` ` ``): console. In release builds `gc_console` has no default
  (`src/g_input.c` only binds it under `DEVELOP`), so bind it once in
  Options > Controls if needed.
- `LUA1/2/3` (`1`/`2`/`3`): mod actions `gc_lua1..3`, no keyboard default —
  bind them in Options > Controls per mod (keys `1`..`3` are free by default).
- `CRUISE` (`AUTO`, bottom-right): latching GO. One tap holds acceleration
  (`KEYCODE_A` ref-counted, coexists with the physical GO button), another tap
  releases. Auto-releases on pause/focus loss. Positions are per-`id` in
  `SharedPreferences`, so old layouts survive the update.

The overlay supports simultaneous fingers. SDL's normal USB and Bluetooth
controller handling remains enabled.

## Servers with mods

Mod downloads go to `RingRacers/addons/downloads/` (engine `DOWNLOADDIR_PART`).
The app now pre-creates `addons/downloads` plus a write-probe before launching,
so a read-only `-home` shows a message on the Retry screen instead of an engine
`I_Error` crash. If a modded server still crashes, grab:

```sh
adb logcat -d | grep -i -E "SRB2|ringracers|SDL" > crash.txt
```

and check for `Couldn't write game config`, OOM (`largeHeap` is on), or a
specific `.pk3` hash error.

## Build

Required local tools:

- JDK 17
- Android SDK 34, Build Tools 34.0.0
- Android NDK 26.3.11579264 and CMake 3.22.1
- vcpkg with the `arm64-android` dependencies installed
  (default `C:/vcpkg`, override with `VCPKG_ROOT`; NDK via `ANDROID_NDK_HOME`)

Required game assets (proprietary, not in git): a Ring Racers 2.4 install or the
official `Dr.Robotnik.s-Ring-Racers-v2.4-Assets.zip` — all 16 files verified:
`bios.pk3`, `gamecontrollerdb.txt`, `data/{altmusic,chars,followers,gfx,maps,
music,scripts,shaders,sounds,staffghosts,unlocks}.pk3` plus
`data/textures_{General,OriginalZones,SEGAZones}.pk3` (staged lowercased, as the
engine expects). Without them `stageGameAssets` fails with
"Missing Ring Racers assets". Layout options:

- Clone this repo *inside* the game folder (default `installRoot`), or
- unzip the Assets zip anywhere and set `RINGRACERS_ASSETS` to that folder.

From this directory (Linux: `./gradlew`, Windows: `.\gradlew.bat`):

```sh
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.
