# Changelog

All notable changes to GameVault Android will be documented here.

## [Unreleased]

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
