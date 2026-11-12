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
| CPU | Per-policy governor, OPP table, current/min/max frequency; optional schedutil tunables when exposed |
| CPU topology | Present/online monitoring; CPU0 protected and optional hotplug for secondary cores 1–7 |
| GPU | Adreno/KGSL telemetry and maximum power-level selection from the live OPP table (Advanced menu) |
| CPU tuning | Live schedutil hispeed load/frequency and rate-limit tunables when exposed |
| Storage policy | Optional Dynamic Fsync runtime toggle when its kernel node exists |
| CCI / DDR | Devfreq min/max tuning only when writable nodes and matching live OPPs are exposed; otherwise read-only |
| Memory | RAM, ZRAM statistics, swappiness, VFS cache pressure |
| Kernel extras | ZRAM compression algorithm, KSM status/statistics, entropy readings, optional Dynamic Fsync |
| Thermal | Detected thermal-zone temperatures |
| I/O | Disk statistics; scheduler controls for whole-device `mmcblk*`, `sda`–`sdc` and `nvme*` only |
| Network | RX/TX counters and kernel-supported TCP congestion algorithms |
| System | Battery, storage, uptime, kernel release and dmesg |
| Settings | English / Bahasa Indonesia, refresh interval and default-off Advanced menu toggle |

## Application layout

Four primary sections are always visible. Advanced is added to the navigation only while its Settings toggle is ON:

- **Overview** — device identity, root status and live summary.
- **Performance** — CPU policy, CPU core topology, VM parameters and I/O scheduler.
- **Monitor** — GPU/KGSL, CCI/DDR devfreq and thermal sensors.
- **System** — battery, storage, network, TCP congestion and kernel logs.
- **Advanced** — hidden by default; GPU maximum OPP, secondary CPU hotplug, validated CCI/DDR limits, schedutil tunables, Dynamic Fsync and read-only regulator telemetry.

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
| GPU maximum OPP | Exact live KGSL OPP mapped to max_pwrlevel; min_pwrlevel and num_pwrlevels rechecked, with read-back and rollback |
| CPU hotplug | CPU0 protected; secondary CPU IDs 1–7 only, present/online verification and at least two online cores |
| CCI/DDR devfreq limits | Only detected fabric paths; min/max must be present in the live available_frequencies table, with ordered writes and rollback |
| Schedutil | Per-policy or global schedutil nodes; hispeed_load 0–100, hispeed_freq from the common live CPU OPPs, rate limits 0–1,000,000 µs; read-back and rollback |
| Dynamic Fsync | Exact runtime node, 0/1 values, read-back and rollback; may defer fsync/fdatasync and risk recent writes |

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
- DDR/RAM overclocking or new devfreq OPP creation;
- thermal trip-point changes or thermal-protection bypass;
- CPU0 hotplug or disabling cores when only two cores would not remain online;
- arbitrary shell commands from the UI.

GPU maximum-OPP selection is the only GPU write. CCI/DDR min/max limits are available only when the exact live nodes and OPPs are writable; regulator voltage, KSM, ZRAM algorithm and entropy remain monitoring-only. Schedutil and Dynamic Fsync are conditional runtime controls, shown only when their kernel nodes exist. Schedutil changes can alter ramp-up/ramp-down latency, boost frequency, power draw and thermal load. Dynamic Fsync is opt-in and carries a data-integrity risk if power is lost while writes are deferred. Kernel-side Zygote DDR bandwidth boost, when provided by the running kernel, remains kernel-owned and is not recreated by app polling.

## Advanced tuning and KonaBess

Schedutil tunables appear only while a policy uses `schedutil`; discovery supports per-policy and global kernel tunables directories. Global `hispeed_freq` is restricted to the intersection of live OPPs across active schedutil policies. Hispeed load is 0–100, rate limits are bounded to 0–1,000,000 µs, and writes use read-back with rollback. Dynamic Fsync appears only when `/sys/kernel/dyn_fsync/Dyn_fsync_active` is available. It may defer `fsync`/`fdatasync` while the display is on; a kernel crash or forced power loss during that period can lose recent writes or corrupt data. Leave it disabled when storage integrity is more important than write-latency behavior.

Advanced is disabled by default and enabled from Settings. GPU control maps an existing KGSL OPP to the driver's max_pwrlevel index; it does not change the OPP table, write voltage, or touch thermal_pwrlevel. CPU frequency limits continue to use the live cpufreq OPP list. Secondary CPU hotplug (IDs 1–7) is available with CPU0 protected and a two-core minimum. CCI/DDR limits can be written only when the device exposes readable min_freq/max_freq nodes and the selected values match its live OPP table; otherwise the card remains telemetry-only. Regulator voltage remains read-only.

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
