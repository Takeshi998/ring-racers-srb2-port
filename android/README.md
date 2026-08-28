# Ring Racers Android

Android port scaffold for the official Ring Racers `v2.4` source. The app targets
64-bit Android devices (API 28 or newer) and uses SDL2 with the engine's existing
GLES2 renderer.

## Fresh data

The APK contains only core runtime assets from the neighboring Ring Racers 2.4
installation. It does not package `ringdata.dat`, `ringprofiles.prf`,
`ringconfig.cfg`, backups, replays, logs, addons, passwords, or server data.

On first launch, core assets are extracted to the app-private `game` directory.
New profiles, settings, progress, and replays are created separately below the
app-private `user/.ringracers` directory. Uninstalling the app clears that new
Android data.

## Touch controls

- D-pad: steer; up/down also aim items and choose trick directions
- `GO` (`A`): accelerate and menu accept
- `LOOK` (`B`): look backward
- `SPIN` (`C`): spindash, fastfall, and menu extra action
- `BRAKE` (`X`): brake and menu back
- `BAIL` (`Y`): Ring Bail
- `VOTE` (`Z`): multiplayer vote action
- `ITEM` (`L`): use items and Rings
- `DRIFT` (`R`): drift
- Pause icon: Start/pause

The overlay supports simultaneous fingers. SDL's normal USB and Bluetooth
controller handling remains enabled.

## Build

Required local tools:

- JDK 17
- Android SDK 34, Build Tools 34.0.0
- Android NDK 26.3.11579264 and CMake 3.22.1
- vcpkg at `C:/vcpkg` with the `arm64-android` dependencies installed

From this directory:

```powershell
.\gradlew.bat assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.
