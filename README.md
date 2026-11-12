# Nocturne Kernel Manager

Nocturne Kernel Manager adalah proyek aplikasi Android yang ditujukan untuk
keluarga perangkat **Samsung Galaxy M11 (SM-M115)** dan
**Samsung Galaxy A11 (SM-A115)** dengan platform **Qualcomm Snapdragon 450
(SDM450)**.

Proyek ini dikembangkan sebagai aplikasi pendamping untuk perangkat M11/A11.
Pada tahap awal, fokusnya adalah membangun fondasi aplikasi Android yang rapi,
mudah dikembangkan, dan memiliki proses build serta pengujian yang konsisten.

## Status proyek

**Tahap saat ini: Android application foundation.**

Yang sudah tersedia:
- proyek Android berbasis Kotlin;
- konfigurasi Android Gradle Plugin dan Gradle Wrapper;
- entry point dan halaman awal aplikasi;
- identitas visual dasar dengan tema gelap;
- pemisahan awal antara Activity, UI, dan metadata ruang lingkup proyek;
- unit test untuk menjaga batas tahap fondasi;
- dokumentasi arsitektur dan kontribusi;
- GitHub Actions untuk build dan verifikasi manual.

Belum ada fitur pengelolaan kernel, integrasi root, pemantauan perangkat,
pengaturan CPU/GPU, kontrol thermal, atau service latar belakang. Aplikasi
belum menjalankan perintah privileged dan belum membaca maupun mengubah
interface kernel.

## Target perangkat

| Perangkat | Keluarga model | Platform target |
| --- | --- | --- |
| Samsung Galaxy M11 | SM-M115 series | Qualcomm Snapdragon 450 |
| Samsung Galaxy A11 | SM-A115 series | Qualcomm Snapdragon 450 |

Daftar tersebut adalah target awal proyek, bukan klaim bahwa seluruh SKU
regional, firmware, ROM, dan versi kernel telah tervalidasi.

## Teknologi dan konfigurasi

| Komponen | Versi / konfigurasi |
| --- | --- |
| Bahasa | Kotlin |
| Android Gradle Plugin | 9.4.0 |
| Gradle Wrapper | 9.6.0 |
| JDK | 17 |
| Compile SDK | Android API 36 |
| Target SDK | Android API 35 |
| Minimum SDK | Android API 26 |
| Application ID | `id.kazera.nocturne.kernelmanager` |
| Unit test | JUnit 4 |

## Struktur repository

```text
.
├── .editorconfig
├── .github/
│   └── workflows/
│       └── android-ci.yml
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/id/kazera/nocturne/kernelmanager/
│       │   │   ├── MainActivity.kt
│       │   │   ├── core/project/ProjectScope.kt
│       │   │   └── ui/home/HomeScreen.kt
│       │   └── res/
│       │       ├── drawable/ic_launcher.xml
│       │       └── values/
│       │           ├── colors.xml
│       │           ├── strings.xml
│       │           └── themes.xml
│       └── test/
│           └── java/id/kazera/nocturne/kernelmanager/
│               └── core/project/ProjectScopeTest.kt
├── docs/
│   └── ARCHITECTURE.md
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── .gitignore
├── build.gradle.kts
├── gradle.properties
├── settings.gradle.kts
├── gradlew
├── gradlew.bat
├── CHANGELOG.md
├── CONTRIBUTING.md
└── SECURITY.md
```

Penjelasan batas tanggung jawab package dan arah arsitektur tahap berikutnya
tersedia di [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Build lokal

Buka repository melalui Android Studio dan tunggu Gradle Sync selesai.

Untuk membangun APK debug:

```bash
./gradlew assembleDebug
```

Untuk menjalankan seluruh pemeriksaan:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

APK debug dihasilkan di:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Lingkungan build memerlukan JDK 17, Android SDK Platform 36, dan Android SDK
Build Tools 36.0.0.

## GitHub Actions — manual

Workflow **Android CI** hanya dijalankan secara manual. Push dan pull request
tidak memulai workflow secara otomatis.

Untuk menjalankan:
1. Buka tab **Actions** pada repository.
2. Pilih workflow **Android CI**.
3. Tekan **Run workflow**.
4. Pilih branch `main`, lalu jalankan workflow.

Workflow menjalankan unit test, Android lint, dan build APK debug. APK yang
berhasil dibuat tersedia sebagai artifact selama 14 hari.

## Dokumentasi proyek

- [Architecture](docs/ARCHITECTURE.md) — susunan package, batas tanggung jawab,
  dan arah arsitektur.
- [Contributing](CONTRIBUTING.md) — standar perubahan dan verifikasi.
- [Changelog](CHANGELOG.md) — catatan perubahan.
- [Security](SECURITY.md) — pelaporan isu keamanan.

## Lisensi

Lisensi repository belum ditetapkan. Sampai lisensi resmi ditambahkan, source
code mengikuti ketentuan hak cipta default.
