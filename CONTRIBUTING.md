# Contributing

Panduan ini berlaku untuk pengembangan Nocturne Kernel Manager, aplikasi Android
yang menargetkan Samsung Galaxy M11 (SM-M115 series) dan Galaxy A11
(SM-A115 series).

## Fokus tahap sekarang

Repository berada pada tahap **application foundation**. Perubahan saat ini
harus berfokus pada:
- struktur dan konfigurasi proyek Android;
- stabilitas build dan kualitas kode;
- UI dasar dan resource aplikasi;
- dokumentasi dan pengujian fondasi.

Jangan memasukkan integrasi root, perintah kernel, akses sysfs/procfs, tuning
perangkat, pemantauan hardware, atau background service sebagai perubahan
sampingan pada tahap ini.

## Lingkungan pengembangan

- JDK 17
- Android SDK Platform 36
- Android SDK Build Tools 36.0.0
- Android Studio yang mendukung Android Gradle Plugin 9.4.0

## Build dan verifikasi lokal

Jalankan dari root repository:

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

## Menjalankan CI

Workflow GitHub Actions bernama **Android CI** berjalan manual, bukan otomatis
saat push atau pull request.

1. Buka tab **Actions**.
2. Pilih **Android CI**.
3. Tekan **Run workflow**.
4. Pilih branch `main` untuk memverifikasi kondisi utama repository.

## Standar perubahan

- Satu perubahan harus memiliki tujuan yang jelas.
- Hindari mencampur refactor yang tidak berkaitan.
- Perbarui README atau dokumen arsitektur jika struktur berubah.
- Sertakan screenshot untuk perubahan UI yang terlihat.
- Jangan menyatakan perangkat telah diuji jika pengujian fisik tidak dilakukan.
- Jangan mengklaim dukungan semua model, firmware, ROM, atau kernel tanpa
  validasi.

## Format commit

Gunakan prefix singkat dan deskriptif:

- `build:` konfigurasi Gradle atau build system
- `ci:` GitHub Actions
- `docs:` dokumentasi
- `feat:` fungsi aplikasi
- `fix:` perbaikan
- `refactor:` perubahan struktur tanpa perubahan perilaku
- `test:` pengujian
- `chore:` pemeliharaan proyek
