<p align="center"><img src="assets/icon.svg" width="96" height="96" alt="Remink icon"></p>
<h1 align="center">Remink</h1>
<p align="center">A reminder app for the Mudita Kompact.</p>

<p align="center">
  <a href="https://github.com/shano/remink/actions/workflows/release.yml"><img src="https://github.com/shano/remink/actions/workflows/release.yml/badge.svg" alt="Build status"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-GPL--3.0-blue.svg" alt="License: GPL-3.0"></a>
</p>

---

Remink sets one-off reminders that fire as a full-screen alarm, even over the lock
screen — built for the Mudita Kompact's e-ink display, where a silent notification is
too easy to miss.

## Features

- Set a reminder for any date and time
- Full-screen alarm that wakes and unlocks the display when it fires
- Survives reboot — alarms are rescheduled automatically
- Overdue reminders are called out clearly in the list
- No accounts, no network access, no tracking

## Requirements

- A Mudita Kompact, or any Android 7.0+ (API 24) device

## Installation

Grab the latest APK from [Releases](https://github.com/shano/remink/releases) and
sideload it — Remink isn't on the Play Store.

## Building from source

Remink consumes Mudita's [MMD](https://github.com/mudita/MMD) e-ink component library
as a Gradle composite build. Clone it as a sibling directory before building:

```sh
git clone https://github.com/mudita/MMD ../MMD
./gradlew assembleDebug
```

The APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

## License

GPL-3.0 — see [LICENSE](LICENSE).

## Other apps for the Kompact

| [Navink](https://github.com/shano/navink) | [Remink](https://github.com/shano/remink) | Zendo |
|:---:|:---:|:---:|
| <a href="https://github.com/shano/navink"><img src="https://raw.githubusercontent.com/shano/navink/master/assets/icon.svg" width="64" height="64"></a> | <a href="https://github.com/shano/remink"><img src="assets/icon.svg" width="64" height="64"></a> | 🚧 |
| Navidrome / Subsonic player | Reminders with alarms | Meditation timer — in progress |
