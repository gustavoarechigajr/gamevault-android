# GameVault Android — Architecture & Developer Guide

This document is for collaborators (human or AI) who want to understand how the app is structured, where to find things, and the reasoning behind key decisions.

---

## What This App Does

GameVault is a native Android client for a personal ROM library server. The server (Flask/Python) hosts ROM files organized by console platform. The app lets users:

- Browse consoles and their ROM lists
- Search within a platform
- Download ROMs directly into the correct EmulationStation subfolder on the device
- Automatically switch between a local IP (home WiFi) and a DuckDNS URL (remote) with no user input

---

## Repository Layout

```
gamevault-android/
├── app/src/main/kotlin/com/gamevault/android/
│   ├── MainActivity.kt              # Entry point; session check + auto-login on launch
│   ├── data/
│   │   ├── api/
│   │   │   ├── ApiClient.kt         # OkHttp/Retrofit clients + smart URL selection
│   │   │   ├── GameVaultApi.kt      # Retrofit interface (all API endpoints)
│   │   │   └── PersistentCookieJar.kt # In-memory cookie store for session persistence
│   │   └── model/
│   │       └── Models.kt            # All data classes (Platform, GameItem, GameMeta, MetadataRow, etc.)
│   ├── ui/
│   │   ├── login/
│   │   │   ├── LoginScreen.kt       # Login form (hides server URL field if already saved)
│   │   │   └── LoginViewModel.kt    # Handles login, remember-me, credential persistence
│   │   ├── platforms/
│   │   │   ├── PlatformsScreen.kt   # Console grid; each card shows the console icon
│   │   │   └── PlatformsViewModel.kt
│   │   ├── games/
│   │   │   ├── GamesScreen.kt       # ROM list with box art, search, download progress
│   │   │   └── GamesViewModel.kt    # Loads game list + metadata; manages download state
│   │   ├── settings/
│   │   │   ├── SettingsScreen.kt    # Remote URL, local URL, ROMs folder picker
│   │   │   └── SettingsViewModel.kt # Saves settings; creates ES subfolders on folder pick
│   │   └── theme/
│   │       └── Theme.kt             # GVBackground, GVRed, GVSurface color tokens
│   └── util/
│       ├── DownloadHelper.kt        # Streams ROM files from server → SAF OutputStream
│       ├── PlatformMapper.kt        # Maps platform IDs → ES folder names + drawable resources
│       └── Prefs.kt                 # DataStore keys and helper read/write functions
├── app/src/main/res/
│   ├── drawable/
│   │   └── ic_platform_*.png        # Console icons (256×256 RGBA), one per platform ID
│   ├── mipmap-*/
│   │   └── ic_launcher*.png         # App launcher icon at all densities
│   └── values/
│       ├── strings.xml
│       └── themes.xml
├── Assets/
│   └── App Logo.png                 # Source logo (600×600). Re-run the Pillow resize
│                                    # script to regenerate mipmaps if this changes.
├── CHANGELOG.md
└── ARCHITECTURE.md                  # This file
```

---

## Key Files in Detail

### `ApiClient.kt`
Three OkHttp clients:
- `probeClient` — 3s timeout, no cookies. Used only to test local URL reachability.
- `mainClient` — standard client with session cookies for all API calls.
- `downloadClient` — no read/write timeout, with cookies. Used for ROM downloads.

**`getApiSmart(remoteUrl, localUrl)`** — the smart URL selector. Sends a HEAD to `{localUrl}/login` on `Dispatchers.IO`. If it gets any response (even 405), the local server is reachable and gets used. Otherwise falls back to `remoteUrl`. Returns `Pair<GameVaultApi, String>` — the second value is the chosen base URL, needed for constructing cover art URLs.

> **Why HEAD to /login?** The /login endpoint requires no auth, so it works regardless of session state. Earlier versions probed an authenticated endpoint, which always failed because session cookies are domain-bound (a DuckDNS cookie won't be sent to a LAN IP).

> **Why not DownloadManager?** Android's `DownloadManager.setDestinationUri()` does not accept SAF `content://` URIs — it crashes. All downloads go through `DownloadHelper` instead.

---

### `PlatformMapper.kt`
Two responsibilities:

1. **`getFolderName(platformId)`** — maps server platform IDs (e.g. `"gba"`) to EmulationStation folder names (e.g. `"gba"`). Used when creating subfolders and routing downloaded files.

2. **`getDrawableRes(platformId)`** — returns the `R.drawable.ic_platform_*` resource ID for a platform's console icon, or `null` for unmapped platforms. The UI falls back to a colored accent bar when `null`.

3. **`allFolderNames()`** — returns all ES folder names (used by Settings to pre-create the full directory tree).

---

### `DownloadHelper.kt`
Downloads a ROM using OkHttp streaming and writes it directly into the SAF-managed folder tree:

1. Resolves `{romsRoot}/{esFolderName}/` using `DocumentFile`
2. Creates the platform subfolder if it doesn't exist
3. Deletes any existing file with the same name (clean restart)
4. Streams the response body into a `contentResolver.openOutputStream()` in 32KB chunks
5. Reports progress via `onProgress(0–100)` callback; sends `-1` if `Content-Length` is unknown

---

### `GamesViewModel.kt` — metadata + cover art
After fetching the game list, the VM fetches `/api/platform/{id}/metadata` which returns a `Map<String, MetadataRow>`. The key is either `game.metaKey` or `game.name`. Cover art URL is built as:

```
{baseUrl}/static/{MetadataRow.boxArtPath}
```

The DB stores `box_art_path` without the `/static/` prefix, so it must be prepended here.

---

### `Prefs.kt` — persistent storage (DataStore)
| Key | Type | Purpose |
|---|---|---|
| `SERVER_URL` | String | Remote DuckDNS URL |
| `LOCAL_URL` | String | Local network IP:port (optional) |
| `USERNAME` | String | Logged-in username |
| `SAVED_PASSWORD` | String | Password (only stored if Remember Me is on) |
| `REMEMBER_ME` | Boolean | Whether to persist password and auto-login |
| `ROMS_ROOT_URI` | String | SAF tree URI for the ROMs root folder |

---

## Server API Reference

All endpoints are relative to the base URL. Session cookie is required after login.

| Method | Path | Description |
|---|---|---|
| POST | `/login` | `{username, password}` → `{ok, error}` |
| GET | `/logout` | Clears server session |
| GET | `/api/me` | Returns `{id, username, role}` — used to check session validity |
| GET | `/api/platforms` | Returns `List<Platform>` |
| GET | `/api/platform/{id}` | Returns `{platform, items[]}` — full ROM list |
| GET | `/api/platform/{id}/metadata` | Returns `Map<filename, MetadataRow>` |
| GET | `/download?path={filePath}` | Streams the ROM file |

Box art is served as a static file:
```
GET /static/boxart/{platform_id}/{filename}.jpg
```

---

## Console Icon Assets

Source PNGs (256×256 RGBA, named by platform ID) live at:
```
game-browser_old/static/console-icons/
```

These are copied into `app/src/main/res/drawable/` with an `ic_platform_` prefix during setup. If you add a new platform, copy its icon here and add an entry to `PlatformMapper.getDrawableRes()`.

A larger set of `.ico` assets (one subfolder per console) may be available locally.
Use Python/Pillow to extract from `.ico` if the flat PNG set is missing a console.

---

## Building

Requirements: Android Studio installed at `~/android-studio/`, Android SDK at `~/Android/Sdk/`.

```bash
# Debug APK
JAVA_HOME=~/android-studio/jbr ANDROID_HOME=~/Android/Sdk ./gradlew assembleDebug

# Output
app/build/outputs/apk/debug/app-debug.apk
```

To cut a release:
```bash
cp app/build/outputs/apk/debug/app-debug.apk gamevault-v{X.Y.Z}.apk
git add gamevault-v{X.Y.Z}.apk
git commit -m "chore: add vX.Y.Z APK"
git push
gh release create vX.Y.Z gamevault-v{X.Y.Z}.apk --title "..." --notes "..."
```

---

## Known Gotchas

| Problem | Root Cause | Fix |
|---|---|---|
| ANR dialogs | Blocking OkHttp call on main thread | `getApiSmart` probe is wrapped in `withContext(Dispatchers.IO)` |
| Download crash | `DownloadManager` rejects SAF `content://` URIs | Use `DownloadHelper` (OkHttp + SAF OutputStream) |
| Box art 404 | `box_art_path` in DB has no `/static/` prefix | Prepend `/static/` when building the URL |
| Local URL probe fails | Probing authed endpoint; session cookie is domain-bound | Probe `/login` with HEAD (no auth required) |
| NAT hairpinning | Router can't loop DuckDNS traffic back to LAN | Local URL field in Settings — app probes it first |
