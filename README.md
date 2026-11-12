# Nocturne Kernel Manager

Nocturne Kernel Manager adalah proyek aplikasi Android yang ditujukan untuk keluarga perangkat **Samsung Galaxy M11** dan **Samsung Galaxy A11**.

Tujuan jangka panjang proyek ini adalah menyediakan aplikasi pendamping untuk lingkungan custom kernel yang kompatibel pada perangkat tersebut. Dukungan model, firmware, ROM, dan kernel akan divalidasi secara bertahap menggunakan perangkat serta konfigurasi nyata.

## Status proyek

**Tahap saat ini: fondasi aplikasi Android.**

Tahap awal ini hanya menyiapkan struktur Android Studio, konfigurasi build, entry point aplikasi, identitas visual dasar, dokumentasi, dan CI otomatis.

Aplikasi saat ini masih berupa halaman awal. Belum ada akses atau perubahan ke interface kernel, eksekusi perintah root, pengaturan perangkat, instalasi modul, maupun service tuning di latar belakang. Tidak ada fitur pengelolaan kernel yang aktif pada tahap ini.

## Target perangkat

| Keluarga perangkat | Keluarga model | Target platform |
| --- | --- | --- |
| Samsung Galaxy M11 | SM-M115 series | Varian Qualcomm Snapdragon 450 |
| Samsung Galaxy A11 | SM-A115 series | Varian Qualcomm Snapdragon 450 |

Daftar tersebut adalah target awal proyek. Kompatibilitas semua SKU regional, firmware, versi Android, ROM, dan kernel belum diklaim.

## Teknologi

| Komponen | Versi / konfigurasi |
| --- | --- |
| Bahasa | Kotlin |
| Android Gradle Plugin | 9.4.0 |
| Kotlin Gradle Plugin | 2.4.21 |
| Gradle Wrapper | 9.6.0 |
| JDK | 17 |
| Compile SDK | Android API 36 |
| Target SDK | Android API 35 |
| Minimum SDK | Android API 26 |
| Application ID | `id.kazera.nocturne.kernelmanager` |

Kompilasi Kotlin menggunakan dukungan Kotlin bawaan Android Gradle Plugin 9.4.0. Tidak ada plugin Kotlin Android eksternal yang diterapkan.

## Build

Buka direktori repository melalui Android Studio dan tunggu proses Gradle Sync selesai.

Build APK debug melalui terminal:

```bash
./gradlew assembleDebug
```

Jalankan seluruh pemeriksaan yang tersedia:

```bash
./gradlew testDebugUnitTest lintDebug
```

Hasil APK:

```
app/build/outputs/apk/debug/app-debug.apk
```

Perangkat pengembangan yang dibutuhkan: JDK 17, Android SDK Platform 36, dan Android SDK Build Tools 36.0.0.

## Struktur repository

```text
.
├── .github/
│   ├── ISSUE_TEMPLATE/
│   ├── workflows/
│   └── pull_request_template.md
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/id/kazera/nocturne/kernelmanager/
│       └── res/
├── gradle/wrapper/
├── build.gradle.kts
├── gradle.properties
├── settings.gradle.kts
├── CHANGELOG.md
├── CONTRIBUTING.md
└── SECURITY.md
```

## Continuous Integration

GitHub Actions menjalankan build Android, unit-test task, dan Android lint pada push serta pull request. Jika build berhasil, APK debug akan disimpan sebagai workflow artifact.

## Batasan tahap awal

Repository ini saat ini hanya berisi fondasi aplikasi dan tooling pengembangan. Integrasi perangkat dan kernel akan dikerjakan pada perubahan terpisah setelah fondasi build stabil dan ruang lingkupnya ditentukan.

## Lisensi

Lisensi repository belum ditetapkan. Sampai file lisensi resmi ditambahkan, source code tetap mengikuti ketentuan hak cipta default.
