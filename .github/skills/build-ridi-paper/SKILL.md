---
name: build-ridi-paper
description: Build and verify the RIDI Paper Launcher, bottom bar, settings, and calendar Android apps from this repository. Use when asked to set up the Android build environment, produce APKs, or diagnose build failures in these four components.
---

# Build RIDI Paper apps

Run commands from the repository root. Inspect the existing build files before changing dependency versions or build settings.

## Environment

- Use JDK 17 and set `JAVA_HOME`.
- Set `ANDROID_HOME` or `ANDROID_SDK_ROOT` to an Android SDK. The launcher needs Platform 36; the three companion apps need Platform 19. The Gradle wrapper uses Gradle 9.1.0.
- The companion scripts default to Build Tools 36.1.0. If another installed version contains `aapt`, `d8`, `zipalign`, and `apksigner`, set `RIDI_BUILD_TOOLS_VERSION` to that version before running them. Do not edit the scripts just to match a local SDK path.
- The companion scripts sign with `~/.android/debug.keystore` by default. If it does not exist, create a local debug keystore outside the repository or set `RIDI_SIGNING_KEYSTORE`, `RIDI_SIGNING_ALIAS`, `RIDI_SIGNING_STOREPASS`, and `RIDI_SIGNING_KEYPASS` to an existing local key. A new key cannot upgrade an APK signed by a different key.

## Build and verify

```sh
./gradlew :app:assembleDebug
sh companion/bottom-bar/build.sh
sh companion/ridi-settings/build.sh
sh companion/ridi-calendar/build.sh
sh companion/ridi-calendar/tests/run.sh
```

Set `ANDROID_HOME` for the calendar tests as well, so they include the weather tests. Build outputs are `app/build/outputs/apk/debug/app-debug.apk` and `companion/{bottom-bar,ridi-settings,ridi-calendar}/build/Ridi*.apk`. Check that each command exits successfully; the companion scripts also verify APK signatures.

If a build fails, report the failing command and error, fix the underlying cause, and rerun that build. Do not claim an APK was produced when its build failed. Device installation is separate from building and requires an explicitly selected device and compatible signing key.

## Keep local data out of Git

Do not add generated APKs, keystores, device screenshots, device backups, or lock-screen wallpapers to a commit. These are local artifacts, even when generated during build verification. Check the staged file list before any commit or publication.
