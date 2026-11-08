# YourTube Android

An Android video-app project using Kotlin, Jetpack Compose and AndroidX Media3.

## Current status
This is the initial project scaffold and UI foundation. It is not yet a complete video client:
- Home, Shorts, Subscriptions, Downloads and account placeholder navigation
- Dark interface foundation
- AndroidX Media3 dependencies added for the playback implementation phase
- Codemagic debug APK workflow

## Build
Open the project in Android Studio with JDK 17 and Android SDK 35, or run:

```bash
gradle assembleDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Important
A working playback source, account integration, download provider, and real feeds still need to be implemented. This project does not bypass platform access controls or copyright restrictions.
