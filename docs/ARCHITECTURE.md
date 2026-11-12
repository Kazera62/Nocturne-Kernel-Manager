# Architecture

## Target

Nocturne Kernel Manager targets Samsung Galaxy M11 (SM-M115) and Galaxy A11 (SM-A115), based on Qualcomm Snapdragon 450 / SDM450.

Hardware capability is discovered from the running device. Regional firmware, ROM and kernel differences are expected. A missing node is represented as unsupported.

## Runtime structure

~~~text
app/src/main/java/id/kazera/nocturne/kernelmanager/
├── MainActivity.kt
├── core/
│   ├── kernel/KernelParsers.kt
│   ├── privilege/RootShell.kt
│   └── project/ProjectScope.kt
├── data/
│   ├── DeviceInfo.kt
│   └── KernelRepository.kt
├── domain/model/DeviceSnapshot.kt
├── ui/home/HomeScreen.kt
└── ui/settings/UiPreferences.kt
~~~

## Root lifecycle

MainActivity performs root preflight before starting the full telemetry scan.

RootShell:
- starts an interactive SU process;
- sends commands through one persistent session;
- serializes access to the process streams;
- uses a unique completion marker to capture each command's exit code and output;
- enforces a timeout and destroys an unresponsive session;
- exposes narrow allow-listed write adapters rather than accepting shell text from the UI.

KernelRepository.checkRoot() primes the cached root session. The same session is reused for subsequent sysfs and procfs reads. Manual refresh invalidates the session, checks authorization again and reloads the snapshot.

The UI reports the result of the initial root check separately from the larger telemetry scan so the root status does not wait for every hardware probe.

## Data and discovery

KernelRepository reads device information and discovers:
- CPU present/online topology and cpufreq policies;
- GPU KGSL and devfreq telemetry;
- CCI, bus, DDR and memory-related devfreq devices;
- thermal zones;
- block-device I/O schedulers and disk statistics;
- RAM and ZRAM data;
- ZRAM compression algorithm availability;
- KSM status/statistics;
- entropy readings;
- battery, storage, network, kernel release and uptime.

DeviceSnapshot contains a single sample for the UI. Nullable fields represent unavailable readings; zero is not used as a substitute for unknown data.

## Interface matrix

| Feature | Interface | Write policy |
| --- | --- | --- |
| CPU topology | /sys/devices/system/cpu/present, cpu*/online | Read-only |
| CPU cpufreq | /sys/devices/system/cpu/cpufreq/policy*, legacy per-CPU cpufreq | Governor and min/max from live lists only |
| GPU | KGSL and matching devfreq nodes | Read-only |
| CCI / DDR | Matching /sys/class/devfreq/* nodes | Read-only |
| Thermal | /sys/class/thermal/thermal_zone*/ | Read-only |
| Memory / ZRAM | /proc/meminfo, /sys/block/zram0/ | Read-only |
| KSM | /sys/kernel/mm/ksm/ | Read-only |
| Entropy | /proc/sys/kernel/random/ | Read-only |
| I/O scheduler | /sys/block/*/queue/scheduler | Selected value must appear in the live node list |
| VM | /proc/sys/vm/swappiness, /proc/sys/vm/vfs_cache_pressure | swappiness 0–100; cache pressure 10–200 |
| TCP congestion | /proc/sys/net/ipv4/tcp_congestion_control | Selected algorithm must be in the live available list |
| Battery | Android battery sticky broadcast | Read-only |
| Network | Android TrafficStats | Read-only |
| Kernel log | dmesg | Read-only |

Paths are discovery candidates. A node is not treated as supported until it can be read.

## Controller transaction model

### CPU

1. Verify root access and the CPU policy path.
2. Re-read the live OPP and governor lists immediately before applying.
3. Validate selected min/max values against the live OPP table and ensure min is not greater than max.
4. Save the existing min/max/governor values.
5. Apply the governor and frequency bounds in an order that avoids transient invalid ranges.
6. Read back the result and attempt to restore the previous values if the write fails or read-back differs.

Frequency controls are hidden when current min/max values are unavailable or do not match the live OPP table. Unknown active governor state is represented with a **Keep current** option to avoid an accidental selection.

### I/O scheduler

The repository validates the scheduler path, re-reads its available values and checks that the selected scheduler remains available before writing. It then reads back the active scheduler.

### VM parameters

Swappiness is restricted to 0–100 and VFS cache pressure to 10–200. Previous values are captured and best-effort rollback is attempted if a multi-value update fails. Values are runtime-only.

### TCP congestion

The selected algorithm must still appear in tcp_available_congestion_control. The repository checks the active algorithm after writing.

## UI

The main navigation contains:
- Overview
- Performance
- Monitor
- System

The Settings page is opened from the gear icon in the upper-right header. It manages language (en / id), refresh interval and root diagnostics.

UI preferences are stored locally with Android SharedPreferences.

## SmartPack reference

The archived SmartPack Kernel Manager repository was reviewed for its capability-driven feature organization and kernel-manager concepts. The implementation in this repository was written independently; no SmartPack source file was copied.

Read-only features inspired by that review include CPU core online topology, ZRAM algorithm discovery, KSM telemetry and entropy readings. CPU hotplug write controls, profiles and apply-on-boot are not part of this release.

Reference: https://github.com/SmartPack/SmartPack-Kernel-Manager
The reference repository is archived and declares GPL-3.0.

## Safety limits

- No GPU frequency writes, GPU overclock or maximum-clock cap.
- No voltage overrides, regulator/CPR/corner or power-rail writes.
- No DDR/RAM overclock.
- No thermal trip-point or thermal-protection writes.
- No CPU hotplug writes.
- No arbitrary shell command field in the UI.
- No recreation of the kernel's Zygote DDR bandwidth boost by app polling.

Future writable interfaces require a documented path allow-list, value bounds, read-back, failure handling and device testing.

## Refresh behavior

The activity performs root preflight once during startup. Telemetry runs on a single background executor and refreshes only while the activity is resumed. The interval can be set to 5, 8, 15 or 30 seconds. This is not a persistent background monitoring service.

## Build

- Kotlin
- Android Gradle Plugin 9.4.0
- Gradle Wrapper 9.6.0
- JDK 17
- Compile SDK 36
- Target SDK 35
- Minimum SDK 26
- App version 0.3.0

Run:

~~~bash
./gradlew testDebugUnitTest lintDebug assembleDebug
~~~

GitHub Actions is manually triggered from main only.
