# Changelog

All notable changes to GameVault Android will be documented here.

## [Unreleased]

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
