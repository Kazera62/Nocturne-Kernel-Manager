# Contributing

Panduan ini berlaku untuk Nocturne Kernel Manager, aplikasi Android untuk
Samsung Galaxy M11 (SM-M115 series) dan Galaxy A11 (SM-A115 series).

## Current implementation scope

Fokus pengembangan:
- Android UI dan presentation logic;
- discovery serta parsing kernel interface;
- root bridge yang dibatasi;
- controller dengan validasi nilai dan rollback;
- tes parser, repository behavior, dan dokumentasi.

## Safety requirements

- Jangan menambahkan arbitrary shell command input ke UI.
- Jangan mengasumsikan sysfs/procfs path tersedia di semua kernel.
- Node yang tidak terdeteksi harus menjadi Unsupported, bukan dummy control.
- Semua write path harus memiliki allow-list dan validasi nilai.
- Jangan menambahkan GPU overclock/cap, voltage/regulator/CPR/corner writes,
  DDR overclock, atau thermal-protection bypass.
- Jangan membuat ulang fitur kernel yang sudah berjalan secara internal melalui
  polling aplikasi.
- Jangan mengklaim perangkat telah diuji jika pengujian fisik tidak dilakukan.

## Environment

- JDK 17
- Android SDK Platform 36
- Android SDK Build Tools 36.0.0
- Android Gradle Plugin 9.4.0
- Gradle Wrapper 9.6.0

## Local verification

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

## Manual CI

GitHub Actions workflow **Android CI** dijalankan melalui tab Actions:
**Android CI → Run workflow → main**. Push dan pull request tidak memicu CI.

## Commit prefixes

- build: Gradle dan build system
- ci: GitHub Actions
- docs: dokumentasi
- feat: fungsi aplikasi
- fix: perbaikan
- refactor: perubahan struktur tanpa perubahan perilaku
- test: pengujian
- chore: pemeliharaan
