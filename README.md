# AppLock

A local-only Android distraction blocker for Android 13+.

## What it does

- Detects launchable apps installed on the phone.
- Auto-selects common distractions when present: TikTok, Instagram, Facebook, Messenger, Snapchat, X/Twitter, Reddit, YouTube, Netflix, Discord, Pinterest, and Twitch.
- Lets the user select any other installed launchable app.
- Manual lock switch.
- Daily start/end schedule, including schedules that cross midnight.
- Blocks selected apps with an Android Accessibility Service.
- Optional trusted-device trigger using Android Companion Device Manager presence callbacks.
- Restores trusted-device presence observation after reboot.
- Stores all settings locally in SharedPreferences; no server or account.

## Trusted-device limitation

The trusted-device mode is best effort. Another Android phone does not need this AppLock app installed, but it must be discoverable/associable over Bluetooth at pairing time, and Android must later be able to observe its BLE range or Bluetooth connection. Phones frequently rotate Bluetooth addresses or stop advertising in the background, so a watch, earbuds, or another dedicated Bluetooth accessory will generally produce more reliable presence events than an arbitrary phone.

The app intentionally does not use Google Home in this first build. Google Home's current Android APIs require OAuth, Home structure permission, and access to the user's Home data, while Companion Device presence is local and much simpler for this use case.

## Build on Windows

1. Install Android Studio.
2. Make sure Android SDK Platform 36 is available. The included script attempts to install it if `sdkmanager.bat` is available.
3. Double-click `build-app.cmd`.
4. The debug APK is created at:
   `app\build\outputs\apk\debug\app-debug.apk`

The script uses Android Studio's bundled JBR when `JAVA_HOME` is not set, downloads Gradle 9.5.0 locally into `.tools`, and builds without GitHub Actions.

## First run

1. Open AppLock.
2. Tap **Enable / manage Accessibility service** and enable **AppLock blocker**.
3. Choose apps to block.
4. Turn on manual lock, daily schedule, and/or trusted-device proximity.
5. For trusted-device mode, tap **Pair trusted Bluetooth device** and choose the known device in Android's system pairing UI.

## Security model

This is a self-control app, not a mobile-device-management product. The phone owner can still disable Accessibility, clear app data, or uninstall the app. Preventing those actions requires Device Owner / enterprise-managed-device provisioning, which is intentionally not part of this simple build.
