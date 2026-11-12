# Nocturne Kernel Manager

Nocturne Kernel Manager adalah aplikasi Android untuk membantu memantau perangkat dan mengelola interface kernel pada keluarga **Samsung Galaxy M11 (SM-M115)** dan **Samsung Galaxy A11 (SM-A115)** berbasis **Qualcomm Snapdragon 450 / SDM450**.

Aplikasi menggunakan pendekatan capability-driven: setiap nilai dan kontrol berasal dari interface yang benar-benar terdeteksi pada perangkat. Node yang tidak tersedia akan ditampilkan sebagai **Unsupported**. Aplikasi tidak membuat nilai contoh dan tidak menganggap semua varian firmware atau kernel memiliki layout sysfs yang sama.

## Status

**Tahap: core application implementation.**

Aplikasi saat ini menyediakan dashboard pemantauan, pembacaan interface kernel, root bridge berbasis perintah standard su, kontrol CPU cpufreq, I/O scheduler, VM sysctl, TCP congestion control, dan tampilan log kernel.

## Target perangkat

| Perangkat | Keluarga model | Target platform |
| --- | --- | --- |
| Samsung Galaxy M11 | SM-M115 series | Qualcomm Snapdragon 450 |
| Samsung Galaxy A11 | SM-A115 series | Qualcomm Snapdragon 450 |

Target perangkat adalah fokus pengembangan, bukan klaim bahwa semua SKU regional, firmware, ROM, atau kernel telah diuji secara fisik.

## Implementasi saat ini

| Area | Kemampuan |
| --- | --- |
| Device | Model, device code, platform, kernel release, uptime |
| Root | SU-compatible root check dan command bridge |
| CPU | Per-policy current/min/max frequency, available OPP, governor; apply governor dan frequency limits |
| GPU | KGSL/devfreq clock, utilization, available OPP bila diekspos; read-only |
| CCI / DDR | Devfreq current frequency, governor, available frequency entries; read-only |
| Memory | MemTotal, MemAvailable, ZRAM, swappiness, VFS cache pressure |
| Thermal | Semua thermal zone yang terbaca; read-only |
| I/O | Disk counters dan per-device scheduler; apply scheduler yang tersedia |
| Battery | Level, charging state, temperature, voltage, technology |
| Storage | Data partition total dan available |
| Network | Android TrafficStats RX/TX counters; TCP congestion selector dari daftar kernel |
| Diagnostics | Tampilan 160 baris terakhir dmesg; root diperlukan |

### CPU controls

- Menemukan cpufreq policy secara dinamis.
- Membaca governor dan frekuensi yang diekspos kernel.
- Pilihan frekuensi dibatasi pada daftar OPP yang dibaca dari perangkat.
- Validasi min <= max dan nilai harus menjadi anggota OPP table.
- Perubahan dilakukan melalui su dan berusaha mengembalikan nilai sebelumnya jika write gagal.
- Perubahan runtime tidak dijamin bertahan setelah reboot.

### I/O controls

- Menemukan /sys/block/*/queue/scheduler.
- Menampilkan scheduler aktif dan daftar scheduler yang benar-benar didukung node.
- Hanya nilai yang terdaftar yang dapat diterapkan.

### VM controls

- Swappiness dibatasi ke 0–100.
- VFS cache pressure dibatasi ke 10–200.
- Nilai lama dibaca terlebih dahulu; jika perubahan kedua gagal, aplikasi mencoba mengembalikan nilai sebelumnya.
- Perubahan berlaku untuk sesi boot saat ini dan tidak dipersistenkan.

### Network controls

- Membaca algoritma aktif dan daftar tcp_available_congestion_control.
- Hanya algoritma yang disediakan kernel yang dapat diterapkan.
- TrafficStats RX/TX tetap berupa counter Android read-only.

## Root compatibility

Root bridge menggunakan antarmuka standard su -c. Desain ini ditujukan agar dapat dipakai dengan root manager yang menyediakan command interface tersebut, seperti Magisk, KernelSU, KernelSU Next, BakaSU, dan APatch. Nama provider yang dilaporkan bergantung pada output versi su; deteksi provider tidak dijadikan syarat untuk menjalankan command.

Kompatibilitas aktual tetap harus diverifikasi pada manager, versi, ROM, dan perangkat yang digunakan. Aplikasi tidak menginstal atau mengganti root manager.

## Layout aplikasi

- **Overview** — ringkasan perangkat, root, CPU, GPU, RAM, dan thermal.
- **CPU** — governor dan frequency limits per policy.
- **GPU** — telemetry KGSL/devfreq.
- **CCI / DDR** — telemetry devfreq untuk interconnect dan memory bus.
- **Memory** — RAM, ZRAM, swappiness, dan VFS cache pressure.
- **Thermal** — seluruh sensor yang terdeteksi.
- **I/O** — disk counters dan scheduler.
- **System** — battery, storage, network, TCP congestion control, kernel information, dan dmesg.

UI menggunakan komponen Android native dan tema gelap, tanpa dependency UI eksternal.

## Safety boundaries

Aplikasi **tidak** menyediakan:
- GPU overclock atau GPU maximum-clock cap;
- voltage override;
- regulator, CPR, atau corner scaling changes;
- DDR/RAM overclock;
- thermal trip-point edits atau thermal protection disable;
- arbitrary shell command input dari UI.

GPU OPP dan batas GPU yang didefinisikan kernel dibiarkan apa adanya. Thermal protection tetap dikendalikan kernel dan Android thermal framework. Zygote-triggered DDR bandwidth boost, jika ada pada kernel yang terpasang, tetap menjadi tanggung jawab implementasi kernel dan tidak dibuat ulang oleh polling aplikasi.

Semua interface yang tidak dikenal atau tidak tervalidasi harus tetap read-only / Unsupported.

## Build

Requirements:
- JDK 17
- Android SDK Platform 36
- Android SDK Build Tools 36.0.0
- Gradle Wrapper 9.6.0

Build debug APK:

```bash
./gradlew assembleDebug
```

Run checks:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Output:
```text
app/build/outputs/apk/debug/app-debug.apk
```

## GitHub Actions

Workflow **Android CI** hanya dijalankan manual:
1. Buka tab **Actions**.
2. Pilih **Android CI**.
3. Tekan **Run workflow**.
4. Pilih branch main.

Workflow menjalankan unit tests, Android lint, dan debug APK build. APK yang berhasil dibuat disimpan sebagai artifact selama 14 hari. Push dan pull request tidak menjalankan CI secara otomatis.

## Repository documentation

- Architecture — docs/ARCHITECTURE.md
- Contributing — CONTRIBUTING.md
- Changelog — CHANGELOG.md
- Security — SECURITY.md

## License

Lisensi repository belum ditetapkan. Sampai lisensi resmi ditambahkan, source code mengikuti ketentuan hak cipta default.
