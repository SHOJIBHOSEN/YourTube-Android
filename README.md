# YourTube Android

Product decisions, target architecture, performance guardrails, and the phased implementation plan are documented in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

Kotlin + Jetpack Compose Android app using AndroidX Media3.

## Build verification

A GitHub Actions workflow builds the debug APK on pushes, pull requests, and manual dispatch:
- Workflow: `.github/workflows/android-build.yml`
- JDK: 17
- Gradle: 8.9
- Android Gradle Plugin: 8.7.3
- Compile SDK: 35

Build locally with JDK 17, Android SDK 35, and Gradle 8.9:

```bash
gradle --no-daemon assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

## Implemented in this repository

- Dark Compose interface with Home, Shorts, Subscriptions, Downloads, You, Settings, History, Playlist, and Player screens.
- Media3 playback for direct HTTP(S) media URLs supported by the device.
- Local watch history and saved URL playlist.
- Android DownloadManager for authorized direct MP4, M4V, and WebM file URLs.
- Wi-Fi-only download preference.

## Not implemented yet

- YouTube Data API feed/search and subscription synchronization.
- Google OAuth sign-in.
- Background playback service / media notification controls.
- Mini-player, SponsorBlock, and automatic intro skipping.
- HLS segmented downloads, in-app pause/resume, batch queue, and quality/audio-only selection.

These are deliberately not represented as working features until their complete implementations and tests exist.

## Platform and rights

The app does not bypass DRM, access controls, or copyright restrictions. Use media URLs you are authorized to play or download. YouTube account data requires the appropriate official API scopes and OAuth consent configuration.
