# Habitude

A small, good-looking Android app for building the habits you want and quitting the ones you don't. Streaks, progress charts and gentle reminders, all stored on your phone.

## What's here so far

- Kotlin + Jetpack Compose app with a soft sage-and-clay Material 3 theme (light and dark)
- Three tabs: **Today**, **Progress** and **Settings**
- **Build** habits: tick them off on their scheduled days to grow a 🔥 streak; rest days don't break it
- Or give a build habit a **times a week** goal (say, gym 3 times a week on any days); its streak counts the weeks you hit it
- **Count habits**, like 8 glasses of water: tap the ring on Today once each time, and the day counts as done when it reaches the goal
- **Home-screen widget**: today's habits with a tap to tick each one off (or count one more)
- **Milestones**: a small celebration when a streak or clean run reaches 7, 30, 100 or 365 days (4, 12, 26 or 52 weeks for a times-a-week goal), and the ones reached on each habit's stats page
- **Break** habits: count your days clean, and log a slip honestly when it happens. Set a "clean since" date if you quit before you started using the app
- **Today** shows how many of today's habits are done, with the ones still to do at the top
- **Archive** a habit you're pausing: it keeps its history, leaves Today and stops its reminders, and can be restored from its edit screen
- Current and best streak on every habit, all stored on the phone (no account)
- Tap a habit for its stats: a 20-week calendar heatmap, weekly completion (or slips) bars and streak history
- Forgot to log a day? Tap it on the habit's heatmap to mark it done (or log or remove a slip)
- Changing a habit's days applies from today on, so past streaks stay as they were
- **Progress** tab: today at a glance, an all-habits heatmap and a 30-day rate for each habit
- **Reminders**: an optional daily reminder per habit, with a "Done ✓" button right on the notification
- **App lock**: an optional 6-digit PIN, asked for when you open the app or come back after a minute away
- **Backup**: export everything to a JSON file and import it again on a new phone or after reinstalling (Settings → Backup). Android's own cloud backup is off, so nothing leaves the phone unless you export it
- **Evening streak check** at 8pm if a streak of two days or more is about to break (can be turned off in Settings)
- A GitHub Actions build that runs the unit tests and produces a debug APK

## Get the APK

Open the latest run under **Actions → Android build** and download the `habitude-debug-apk` artifact. Unzip it and install `app-debug.apk` on your phone (you may need to allow installs from unknown sources).

Every build is signed with the same key (`app/debug.keystore`), so a new APK installs over the old one and keeps your data. Builds made before version 0.5.0 used a random key each time, so uninstall those once before installing 0.5.0 or later. Export a backup first if you want to keep what you have.

## Build locally

Needs JDK 17 and the Android SDK (Android Studio sets both up).

```sh
./gradlew assembleDebug
```

## Roadmap

1. App skeleton and theme ✅
2. Habits to build and break, with streaks ✅
3. Charts and progress ✅
4. Reminders and polish ✅
