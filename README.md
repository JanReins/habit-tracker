# Habitude

A small, good-looking Android app for building the habits you want and quitting the ones you don't. Streaks, progress charts and gentle reminders, all stored on your phone.

## What's here so far

- Kotlin + Jetpack Compose app with a soft sage-and-clay Material 3 theme (light and dark)
- Three tabs: **Today**, **Progress** and **Settings** (Settings is a placeholder for now)
- **Build** habits: tick them off on their scheduled days to grow a 🔥 streak; rest days don't break it
- **Break** habits: count your days clean, and log a slip honestly when it happens
- Current and best streak on every habit, all stored on the phone (no account)
- Tap a habit for its stats: a 20-week calendar heatmap, weekly completion (or slips) bars and streak history
- **Progress** tab: today at a glance, an all-habits heatmap and a 30-day rate for each habit
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
2. Habits to build and break, with streaks ✅
3. Charts and progress ✅
4. Reminders and polish
