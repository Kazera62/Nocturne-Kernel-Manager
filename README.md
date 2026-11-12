# Nocturne Kernel Manager

Aplikasi Android untuk memantau dan mengelola interface kernel pada Samsung Galaxy M11 (SM-M115) dan Galaxy A11 (SM-A115), berbasis Qualcomm Snapdragon 450 / SDM450.

## Release status

**Version: 0.4.0 · Advanced runtime controls**

Aplikasi memakai model *capability-driven*: interface dicari pada perangkat yang sedang berjalan. Kontrol hanya ditampilkan saat node dan nilai yang dibutuhkan tersedia. Node yang tidak tersedia ditandai **Unsupported**, bukan diisi data contoh.

Target M11/A11 adalah fokus pengembangan. Seluruh varian regional, firmware, ROM dan kernel belum diklaim telah diuji secara fisik.

## What is implemented

| Area | Function |
| --- | --- |
| Root | Startup root preflight, persistent SU session, status chip, manual re-check |
| CPU | Per-policy governor, OPP table, current/min/max frequency |
| CPU topology | Present/online core monitoring |
| GPU | Adreno/KGSL telemetry and maximum power-level selection from the live OPP table (Advanced menu) |
| CCI / DDR | Devfreq telemetry and exposed min/max readings (read-only) |
| Memory | RAM, ZRAM statistics, swappiness, VFS cache pressure |
| Kernel extras | ZRAM compression algorithm, KSM status/statistics, entropy readings |
| Thermal | Detected thermal-zone temperatures |
| I/O | Disk statistics; scheduler controls for whole-device `mmcblk*`, `sda`–`sdc` and `nvme*` only |
| Network | RX/TX counters and kernel-supported TCP congestion algorithms |
| System | Battery, storage, uptime, kernel release and dmesg |
| Settings | English / Bahasa Indonesia, refresh interval and default-off Advanced menu toggle |

## Application layout

The navigation is divided into four main areas:

- **Overview** — device identity, root status and live summary.
- **Performance** — CPU policy, CPU core topology, VM parameters and I/O scheduler.
- **Monitor** — GPU/KGSL, CCI/DDR devfreq and thermal sensors.
- **System** — battery, storage, network, TCP congestion and kernel logs.
- **Advanced** — hidden by default; KGSL maximum OPP selection, regulator voltage telemetry and CCI/DDR status.

The gear icon in the top-right opens **Settings**. Language, refresh preferences and the Advanced menu toggle are stored locally. Advanced remains hidden until enabled.

The layout applies dynamic status-bar, navigation-bar and display-cutout insets. Kernel telemetry and all SU commands run on a dedicated background I/O executor. Overview, Performance, Monitor and System update bound values on the next display frame rather than rebuilding the full view tree on every poll.

## Root startup flow

1. The app starts a root preflight before scanning the broader kernel interface set.
2. The root status is rendered as soon as the preflight finishes.
3. The app keeps one interactive SU session and reuses it for subsequent reads and writes.
4. A manual refresh invalidates the session, re-checks authorization and then reloads device telemetry.

The bridge targets root managers that provide a standard SU interface, including Magisk, KernelSU-family managers, BakaSU and APatch. Actual behavior must still be checked with the installed manager and ROM.

## Available controls

| Controller | Validation |
| --- | --- |
| CPU governor and min/max frequency | Live governor and OPP lists, path allow-list, read-back and best-effort rollback |
| I/O scheduler | Physical-device whitelist, live scheduler re-read, exact path check and read-back |
| VM swappiness | 0–100 |
| VFS cache pressure | 10–200 |
| TCP congestion control | Selected algorithm must be present in the kernel's live available list |
| GPU maximum OPP | Exact KGSL gpu_available_frequencies entry mapped to a valid max_pwrlevel; current min_pwrlevel and num_pwrlevels are rechecked, with read-back and rollback |

These are runtime controls. The app does not guarantee that settings persist across reboot.

## SmartPack-inspired ideas

The archived [SmartPack Kernel Manager](https://github.com/SmartPack/SmartPack-Kernel-Manager) was reviewed for feature organization and capability discovery. No SmartPack source file was copied into this project.

The current implementation independently includes read-only views for:
- CPU core online topology;
- ZRAM compression algorithm and supported algorithms;
- KSM status and shared-page counters;
- entropy pool readings.

SmartPack Kernel Manager is archived and marked GPL-3.0. This project uses the repository as a feature reference, not as copied implementation code.

## Safety boundaries

The application exposes only a runtime GPU maximum-OPP selector using existing KGSL power levels. The control cannot add a new GPU OPP or exceed the live kernel table. Thermal power-level limits remain kernel-owned.

The application does not expose:
- GPU OPP table modification or a frequency above the live kernel table;
- voltage overrides, regulator writes or undervolting;
- CPR/corner scaling changes;
- DDR/RAM overclocking;
- thermal trip-point changes or thermal-protection bypass;
- CPU hotplug writes in this release;
- arbitrary shell commands from the UI.

GPU maximum-OPP selection is the only new GPU write. CCI/DDR, regulator voltage, KSM, ZRAM algorithm and entropy remain monitoring-only. Kernel-side Zygote DDR bandwidth boost, when provided by the running kernel, remains kernel-owned and is not recreated by app polling.

## Advanced tuning and KonaBess

Advanced is disabled by default and enabled from Settings. GPU control maps an existing KGSL OPP to the driver's max_pwrlevel index; it does not change the OPP table, write voltage, or touch thermal_pwrlevel. CPU frequency limits continue to use the live cpufreq OPP list. CCI/DDR and regulator values are shown only when readable and are not written by this release.

The Qualcomm CPR/DTB voltage-corner path in the SDM450 kernel has no validated runtime undervolt interface in this app. KonaBess is a boot-image DTB modification workflow and its published device list does not include SDM450. Its source is a reference only; do not use its generated image for M11/A11.

## Build

Requirements:
- JDK 17
- Android SDK Platform 36
- Android SDK Build Tools 36.0.0
- Gradle Wrapper 9.6.0

Run:

~~~bash
./gradlew testDebugUnitTest lintDebug assembleDebug
~~~

Debug APK output:

~~~text
app/build/outputs/apk/debug/app-debug.apk
~~~

## GitHub Actions

The **Android CI** workflow is manually triggered and restricted to main:

1. Open **Actions**.
2. Select **Android CI**.
3. Select **Run workflow** on main.

It runs unit tests, lint and assembleDebug, then uploads the debug APK artifact for 14 days.

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Contributing](CONTRIBUTING.md)
- [Changelog](CHANGELOG.md)
- [Security policy](SECURITY.md)

The project license has not yet been selected. Until a license is added, repository contents remain under their default copyright terms.
