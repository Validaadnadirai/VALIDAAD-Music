# VALIDAAD MUSIC

An Android-first music platform focused on offline music, compliant online discovery, and a distinctive listening experience.

## Current milestone

**Phase 1 — Project foundation and starter UI.** The current app is a UI foundation only. Music scanning, playback, YouTube discovery, lyrics, and AI features are not implemented yet.

## Tech stack

- Kotlin
- Jetpack Compose and Material 3
- AndroidX Media3 dependencies prepared for the playback milestone
- Minimum Android version: 8.0 (API 26)
- Java 17

## Build

GitHub Actions builds a debug APK on pushes to the main branch and on manual workflow dispatch. Open the repository Actions tab, select Android Debug Build, and download the VALIDAAD-MUSIC-debug artifact after a successful run.

## Principles

- Respect copyright, licensing, and platform playback restrictions.
- Never mark a feature complete until implemented and tested on a real Android device.
- Keep API secrets out of the Android app.
