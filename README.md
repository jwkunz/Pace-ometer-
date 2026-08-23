# Pace-ometer

A personal, local-only Android running tracker: GPS + accelerometer distance tracking, BLE
heart-rate monitoring, spoken (TTS) run announcements, post-run analysis with a map and charts,
personal records, and equipment-usage tracking (e.g. running shoes) — all in a crimson/gold/black
theme, with no accounts and no cloud.

## Install

Pace-ometer isn't on the Play Store — it's distributed as a signed APK straight from
[GitHub Releases](https://github.com/jwkunz/Pace-ometer-/releases/latest). No account, no store,
no review process.

**On your phone:**

1. Open the [latest release](https://github.com/jwkunz/Pace-ometer-/releases/latest) in your
   phone's browser and download `app-release.apk`.
2. Tap the downloaded file. Android will ask permission to install from your browser/file manager
   the first time — this is normal for any app installed outside the Play Store. Allow it.
3. Tap **Install**. Once it's done, open Pace-ometer and complete the first-launch onboarding.

**Updating later:** repeat the same steps with a newer release's APK — installing over an
existing copy keeps your local data (runs, records, settings, equipment). If you'd rather not
check back manually, install [Obtainium](https://github.com/ImranR98/Obtainium) and add this repo
as a source — it watches GitHub Releases and notifies you when a new version is out.

> **Why the warning when installing?** Android shows an "unknown sources" prompt for any APK not
> installed via the Play Store, regardless of who built it — it's not specific to this app. The
> APK is signed with a dedicated release key (see `.github/workflows/release.yml`), so Android
> will always recognize updates as coming from the same source.

## Features

- **Run tracking** — start/pause/resume/stop, with a foreground service that keeps tracking
  accurate with the screen off or the phone in a pocket.
- **Sensor fusion** — GPS (`FusedLocationProviderClient`) smoothed with an EMA filter and outlier
  rejection, falling back to stride-calibrated accelerometer dead reckoning during GPS gaps.
- **Live metrics** — distance, elapsed time, current segment pace, split/projected pace,
  elevation and last-segment elevation change, heart rate, cadence, calories, and clock time are
  all shown live during a run, regardless of which are configured for voice announcements.
- **BLE sensors** — scans for nearby athletic devices (heart rate, running/cycling speed &
  cadence, cycling power, fitness machines) over standard GATT services, with live BPM feedback
  in Settings once a heart-rate monitor is connected.
- **Bluetooth headset controls** — the inline media play/pause button on connected headphones
  pauses and resumes the run via a standard `MediaSession`.
- **Configurable TTS announcements** — spoken updates at a configurable distance interval, with
  a toggle per metric for what gets read aloud.
- **Post-run analysis** — an OpenStreetMap (osmdroid) route map plus metric-over-time charts for
  pace, heart rate, elevation, and cadence.
- **Personal records** — automatically evaluated on save, tracked both all-time and per
  user-defined season.
- **Equipment tracking** — optionally log gear like running shoes, assign saved runs to it, see
  cumulative distance, and retire it once it's worn out.
- **JSON export** — export all saved runs to a JSON file via the system's document picker; no
  data ever leaves the device unless you explicitly export it.
- **First-launch onboarding** — collects birthdate, gender, unit system, body weight, and height
  up front (used for calorie/heart-rate-zone estimates), with an optional equipment step.
- **Local only** — everything is stored on-device in a Room database and DataStore; there is no
  backend and no account system.

## Tech stack

Kotlin, Jetpack Compose (Material3), Navigation Compose, Room (KSP), DataStore Preferences,
kotlinx.serialization, osmdroid, AndroidX Media3, FusedLocationProviderClient, Android BLE GATT,
Android TextToSpeech, and a foreground service (`android:foregroundServiceType="location"`).

## Requirements

- Android Studio (current stable) with an SDK platform matching `compileSdk`/`targetSdk` below.
- A device or emulator running **API 24+** (min SDK); GPS/BLE testing requires a physical device.
- `compileSdk`/`targetSdk`: see `app/build.gradle.kts`.

## Building

```bash
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Install it with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Cutting a release (maintainers)

Releases are built and published automatically by
[`.github/workflows/release.yml`](.github/workflows/release.yml). Bump `versionCode` and
`versionName` in `app/build.gradle.kts`, commit, then push a tag matching `v*.*.*`:

```bash
git tag v1.0.1
git push origin v1.0.1
```

That builds a signed release APK and publishes it as a GitHub Release with the APK attached —
the same link from the [Install](#install) section above. This needs the `RELEASE_KEYSTORE_BASE64`,
`RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD` repository secrets set,
matching the release keystore used for local builds (`keystore/pace-ometer-release.jks` +
`keystore.properties` at the repo root — both gitignored, so keep your own backup).
**Losing that keystore means future releases can no longer install over copies already on
people's phones.**

## Permissions

Location (fine + background), Bluetooth (scan/connect), notifications, and activity recognition
are requested contextually — background location is required for accurate tracking and the app
will block starting a run until it's granted.

## Privacy

Pace-ometer stores all run data, sensor readings, and settings exclusively on-device in a local
database. Nothing is transmitted to or stored on any remote server. The only way data leaves the
device is a user-initiated JSON export. Bluetooth and GPS are used solely to record runs locally.

## License

© 2026 Numerius Engineering LLC. All rights reserved.
