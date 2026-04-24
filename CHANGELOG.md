# Changelog

All notable changes to GameVault Android will be documented here.

## [Unreleased]

## [0.3.2] - 2026-04-24

### Fixed
- **Scrollbar now consistent across all consoles** — was previously hidden when the sort order wasn't set to Alphabetical; now always visible (hidden only during search)
- **Scrollbar letters now readable on large libraries** — consoles with many numbered/special-character titles (e.g. GameCube) no longer show dots; labels adapt to the available space and remain legible

### Added
- **Floating letter indicator** — while dragging the scrollbar, the current letter appears in a red bubble to the left of the bar; disappears on release

## [0.3.1] - 2026-04-24

### Fixed
- **Dual-screen crash on back press** — resolved `ViewTreeLifecycleOwner` lifecycle 2.8 API change that caused a crash when navigating back while the second screen was active
- **Controller key events and scrollbar** — D-pad scrolling and scroll bar interaction no longer conflict; key events are consumed correctly

### Added
- **Controller UX improvements** — search bar no longer auto-opens on D-pad focus; requires A/Enter or touch to activate. X button opens search from anywhere in the list. Y button downloads the focused game. Left/Right D-pad cycles through sort orders on a game row. Controller hint bar at the bottom shows A/B/X/Y bindings
- **Rescan games** — Settings now has a Rescan button that scans all ROMs subfolders and marks found files as detected across all platform screens; fixes previously-downloaded games not showing as owned after reinstall or first-time setup

## [0.2.0] - 2026-04-23

### Fixed
- **Downloads screen no longer jumps** — entries kept a stable position by preserving the original timestamp across progress updates; previously each tick generated a new timestamp causing constant reordering with multiple concurrent downloads

## [0.1.9] - 2026-04-23

### Fixed
- **Downloads survive navigation** — backing out of a platform screen no longer cancels in-progress downloads; they continue in the background and the progress UI re-syncs when you return
- **Remote URL broken when local URL is set** — the app now probes the explicit local URL before using it; if unreachable (e.g. away from home), falls back to the remote URL automatically
- **Login URL field not editable** — server URL field is now always visible on the login screen so it can be changed at any time

## [0.1.8] - 2026-04-23

### Added
- **Downloads screen** — new button in the top bar (with active-count badge) opens a screen showing all in-progress and recently completed downloads across all platforms; individual dismiss and "Clear completed" actions

## [0.1.7] - 2026-04-23

### Added
- **Local file detection** — games already downloaded to the ROMs folder show a green checkmark instead of the download button; detected automatically on screen load via SAF
- **Delete game** — tap the checkmark to open a confirmation dialog; deleting removes the file from the device and restores the download button
- **In-app updates** — Settings → App → Check for Updates fetches the latest GitHub release, shows the version if newer, and downloads + installs the APK directly in-app

## [0.1.3] - 2026-04-21

### Fixed
- **Download crash** — replaced DownloadManager (incompatible with SAF content:// URIs) with OkHttp streaming directly into a SAF OutputStream; no intermediate copy, no crash
- **Box art not showing** — cover URL was missing the `/static/` prefix; now built as `{baseUrl}/static/{box_art_path}`
- **Local URL probe always failing** — probe previously called an authenticated endpoint; session cookie is domain-bound so it was never sent to the local IP. Now uses a no-auth HEAD to `/login` which any reachable server accepts
- **Local URL not reverting after WiFi switch** — probing is now done on every API call, not just at login, so the app re-picks local vs remote each time
- **DuckDNS URL shown in login field** — server URL field is now hidden on the login screen when a URL is already saved in settings

### Added
- **Download progress bar** — real-time progress bar and percentage shown inline in the game list while a file is downloading
- **Download state indicators** — green checkmark on success, red error icon with retry on failure
- **Downloads prefer local URL** — download requests use the local IP when on home WiFi for faster transfers
- Typed `MetadataRow` data class — fixes silent Gson parsing failure for platform metadata

## [0.1.0] - 2026-04-22

### Added
- Initial project scaffold — Kotlin + Jetpack Compose
- Login screen with server URL, username, and password fields
- Platform grid screen — lists all consoles with game counts fetched from the server
- Games list screen — shows all ROMs for a platform with box art, file size, and download button
- In-list search/filter for the games screen
- Settings screen — server URL editor and ROMs root folder picker (Storage Access Framework)
- Download routing — files saved to `{romsRoot}/{esFolderName}/filename` automatically matching the EmulationStation directory layout
- Platform mapper covering 30+ consoles (server platform ID → ES folder name)
- Session cookie persistence across requests (no repeated logins)
- DataStore preferences for server URL and ROMs root URI (persists across app restarts)
- App icon at all mipmap densities (mdpi → xxxhdpi)
- Dark navy/red theme matching the GameVault web app
- `gradlew assembleDebug` CLI build support (no Android Studio required after initial SDK setup)

### Fixed
- Theme parent updated to `Theme.DeviceDefault.NoActionBar` for compatibility with newer SDK versions
- Added missing `androidx.documentfile` dependency for Storage Access Framework folder writes
