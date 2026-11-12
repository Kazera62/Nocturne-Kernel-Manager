# Contributing

Terima kasih telah membantu mengembangkan Nocturne Kernel Manager.

## Tahap proyek

Repository saat ini sedang membangun fondasi aplikasi Android. Pertahankan perubahan tetap berfokus pada struktur proyek, keandalan build, dokumentasi, dan kualitas kode sampai ruang lingkup implementasi berikutnya ditentukan secara terpisah.

Jangan menambahkan perintah kernel, eksekusi root, akses sysfs, tuning perangkat, atau background service sebagai perubahan sampingan pada tahap fondasi.

## Lingkungan pengembangan

- JDK 17
- Android SDK Platform 36
- Android SDK Build Tools 36.0.0
- Android Studio yang mendukung AGP 9.4.0

## Build dan pemeriksaan

Jalankan perintah berikut sebelum membuka pull request:

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

## Pull request

Setiap pull request sebaiknya:
- Menjelaskan tujuan dan batas perubahan.
- Tidak mencampurkan refactor yang tidak berkaitan.
- Menyertakan screenshot untuk perubahan UI jika relevan.
- Memastikan task Gradle yang terkait berhasil.
- Menyebutkan asumsi kompatibilitas perangkat yang belum diverifikasi.

## Format commit

Gunakan prefix singkat dan imperatif:

- `chore:` pemeliharaan proyek atau tooling
- `build:` perubahan Gradle atau build system
- `ci:` otomatisasi
- `docs:` dokumentasi
- `feat:` fungsi aplikasi
- `fix:` perbaikan masalah
- `refactor:` perubahan struktur tanpa perubahan perilaku
- `test:` penambahan atau perbaikan pengujian
