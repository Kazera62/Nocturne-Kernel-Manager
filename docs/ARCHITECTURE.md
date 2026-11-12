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
| CPU topology | /sys/devices/system/cpu/present, cpu*/online | CPU0 protected; IDs 1–7 only; at least two online cores retained; read-back required |
| CPU cpufreq | /sys/devices/system/cpu/cpufreq/policy*, legacy per-CPU cpufreq | Governor and min/max from live lists only |
| GPU | /sys/class/kgsl/kgsl-3d0/gpu_available_frequencies, max_pwrlevel, min_pwrlevel, num_pwrlevels | Maximum power-level selector using a live OPP only; thermal_pwrlevel is never written |
| CCI / DDR | Matching /sys/class/devfreq/*/available_frequencies, min_freq, max_freq | Writes only when both bound nodes and selected live OPPs exist; path/name allowlist, ordered writes, read-back and rollback |
| Regulator | /sys/class/regulator/regulator*/ | Voltage/range/state telemetry only |
| Thermal | /sys/class/thermal/thermal_zone*/ | Read-only |
| Memory / ZRAM | /proc/meminfo, /sys/block/zram0/ | Read-only |
| KSM | /sys/kernel/mm/ksm/ | Read-only |
| Entropy | /proc/sys/kernel/random/ | Read-only |
| I/O scheduler | /sys/block/{mmcblk[0-9]+, sd[a-c], nvme[0-9]+n[0-9]+}/queue/scheduler | Whole physical devices only; selected value must appear in the live node list |
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

The repository validates the physical device name and canonical scheduler path, re-reads its available values and checks that the selected scheduler remains available before writing. RootShell enforces the same physical-device allowlist again. It then reads back the active scheduler.

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

The Settings page is opened from the gear icon in the upper-right header. It manages language (en / id), refresh interval, root diagnostics and a default-off Advanced menu toggle. Enabling it adds an Advanced navigation page.

UI preferences are stored locally with Android SharedPreferences.

The root view applies status-bar, navigation-bar and display-cutout insets from WindowInsets. This keeps the header and bottom navigation inside the device's safe display area.

Overview, Performance, Monitor and System use bound TextViews for changing telemetry values. Snapshot updates are coalesced with postOnAnimation; the view tree is rebuilt only when device topology, capability lists or active controller values change. Settings is intentionally static between user actions.

## SmartPack reference

The archived SmartPack Kernel Manager repository was reviewed for its capability-driven feature organization and kernel-manager concepts. The implementation in this repository was written independently; no SmartPack source file was copied.

Read-only features inspired by that review include CPU core online topology, ZRAM algorithm discovery, KSM telemetry and entropy readings. CPU hotplug write controls, profiles and apply-on-boot are not part of this release.

Reference: https://github.com/SmartPack/SmartPack-Kernel-Manager
The reference repository is archived and declares GPL-3.0.

## Advanced runtime control boundary

The Advanced menu is default-off. Its GPU control maps a selected frequency to the index in the live descending KGSL OPP list, validates it against num_pwrlevels and min_pwrlevel, writes only /sys/class/kgsl/kgsl-3d0/max_pwrlevel, then reads it back and attempts rollback on mismatch. It does not create OPPs or write thermal_pwrlevel.

CPU hotplug accepts only present secondary cores 1–7; CPU0 cannot be disabled, and the repository rejects a change that would leave fewer than two cores online. CCI/DDR limits are exposed only when available_frequencies, min_freq and max_freq are all readable and the requested bounds exist in the live table. Writes are serialized, ordered to avoid invalid transient ranges, verified and rolled back best-effort.

The SDM450 source defines Adreno 506 OPPs in DTB data. This app can select only entries exposed by the running kernel. Regulator and CPR voltage data is read-only; no validated runtime undervolt write interface is provided. CCI/DDR limits are also read-only in this release because a generic write interface was not established for this kernel.

## Safety limits

- No GPU OPP-table modification or frequency above the live OPP table.
- No voltage overrides, regulator/CPR/corner or power-rail writes.
- No DDR/RAM overclock or new devfreq OPP creation; CCI/DDR min/max changes are limited to existing live OPP entries.
- No thermal trip-point or thermal-protection writes.
- No CPU0 hotplug; secondary CPU hotplug is restricted to IDs 1–7 and keeps at least two cores online.
- No arbitrary shell command field in the UI.
- No recreation of the kernel's Zygote DDR bandwidth boost by app polling.

Future writable interfaces require a documented path allow-list, value bounds, read-back, failure handling and device testing.

## Refresh behavior

The activity performs root preflight once during startup. All sysfs/procfs reads, SU commands, control writes, log retrieval and SU session cleanup run on a dedicated single-thread I/O executor; no kernel I/O is performed on the UI thread. Serial execution also protects the persistent SU stream from concurrent readers and writers.

Telemetry refreshes only while the activity is resumed. The interval can be set to 5, 8, 15 or 30 seconds. A completed snapshot is posted to the UI thread, coalesced to the next display frame and applied to existing value views. This is not a persistent background monitoring service.

## Build

- Kotlin
- Android Gradle Plugin 9.4.0
- Gradle Wrapper 9.6.0
- JDK 17
- Compile SDK 36
- Target SDK 35
- Minimum SDK 26
- App version 0.4.0

Run:

~~~bash
./gradlew testDebugUnitTest lintDebug assembleDebug
~~~

GitHub Actions is manually triggered from main only.
