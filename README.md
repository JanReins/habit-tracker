# Habitude

A small, good-looking Android app for building the habits you want and quitting the ones you don't. Streaks, progress charts and gentle reminders, all stored on your phone.

## What's here so far

- Kotlin + Jetpack Compose app with a soft sage-and-clay Material 3 theme (light and dark)
- Three tabs: **Today**, **Progress** and **Settings** (placeholders until habits land)
- A GitHub Actions build that runs the unit tests and produces a debug APK

## Get the APK

Open the latest run under **Actions → Android build** and download the `habitude-debug-apk` artifact. Unzip it and install `app-debug.apk` on your phone (you may need to allow installs from unknown sources).

## Build locally

Needs JDK 17 and the Android SDK (Android Studio sets both up).

```sh
./gradlew assembleDebug
```

## Roadmap

1. App skeleton and theme ✅
2. Habits to build and break, with streaks
3. Charts and progress
4. Reminders and polish
