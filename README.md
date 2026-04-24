# GameVault Android

Android client for the GameVault personal ROM library server. Browse and download your game collection directly to your device, sorted automatically into the correct EmulationStation / EmuDeck folder structure.

## Features

- Browse your ROM library by console/platform
- Search within any platform
- Download games directly to your device's `Emulation/roms` folder — files land in the correct subfolder automatically (e.g. GBA → `gba/`, PS2 → `ps2/`)
- User login with session persistence
- Admin-approved account system (mirrors the web app)

## Supported Platforms

Nintendo Switch, PS1, PS2, PSP, PS Vita, GameCube, Wii, Wii U, 3DS, DS, N64, NES, SNES, Game Boy, GBA, GBC, Genesis / Mega Drive, Master System, Game Gear, Sega 32X, Sega CD, Saturn, Dreamcast, TurboGrafx-16, Atari 2600/7800, Atari Jaguar, Atari Lynx, Neo Geo, Arcade / MAME, PC

## Requirements

- A running GameVault server on your local network or accessible via the internet
- Android 8.0 (API 26) or higher
- An emulator frontend that follows the EmulationStation folder layout (e.g. [EmuDeck](https://www.emudeck.com/), [RetroDECK](https://retrodeck.net/))

## Setup

1. Install the APK (sideloading — enable Unknown Sources in Settings → Security)
2. Open the app and enter your server URL:
   - **Local:** `http://<your-server-ip>:<port>` (e.g. `http://10.0.0.10:5000`)
   - **Remote:** your DuckDNS or public URL (e.g. `https://yourserver.duckdns.org`)
3. Log in with your GameVault credentials
4. Go to **Settings** and pick your ROMs root folder (the `Emulation/roms` directory)
5. Browse, search, and download — files sort themselves automatically

## Building from Source

**Prerequisites:** Android Studio (or Android SDK + JDK 17+)

```bash
git clone git@github.com:gustavoarechigajr/gamevault-android.git
cd gamevault-android

# Build debug APK
JAVA_HOME=/path/to/jdk ANDROID_HOME=/path/to/sdk ./gradlew assembleDebug

# APK output
app/build/outputs/apk/debug/app-debug.apk
```

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Navigation:** Navigation Compose
- **Networking:** Retrofit + OkHttp
- **Image loading:** Coil
- **Storage:** DataStore Preferences
- **Downloads:** Android DownloadManager + Storage Access Framework
