# Architecture

## Purpose and target

Nocturne Kernel Manager is an Android application for Samsung Galaxy M11
(SM-M115 family) and Galaxy A11 (SM-A115 family), targeting the Qualcomm
Snapdragon 450 / SDM450 platform.

The app uses capability-driven discovery. It reads only interfaces found on the
running device, reports unavailable nodes as unsupported, and does not assume
that every kernel or regional firmware exposes identical paths.

## Runtime layers

```text
app/src/main/java/id/kazera/nocturne/kernelmanager/
├── MainActivity.kt
├── core/
│   ├── kernel/
│   │   └── KernelParsers.kt
│   ├── privilege/
│   │   └── RootShell.kt
│   └── project/
│       └── ProjectScope.kt
├── data/
│   ├── DeviceInfo.kt
│   └── KernelRepository.kt
├── domain/
│   └── model/
│       └── DeviceSnapshot.kt
└── ui/
    └── home/
        └── HomeScreen.kt
```

### MainActivity

Owns the Android lifecycle, background executor, periodic refresh while the
activity is resumed, controller callbacks, and kernel-log dialog.

### core/privilege

RootShell is the only component that starts `su`. It uses the common
`su -c` interface, captures command output, enforces timeouts, and restricts
write operations to explicit path/value allow-lists.

The UI never receives a general-purpose shell input field.

### core/kernel

KernelParsers parses frequency tables, KGSL busy counters, thermal readings,
I/O scheduler lists, and display formatting.

### data

KernelRepository discovers cpufreq policies, KGSL/devfreq nodes, thermal
zones, block I/O schedulers, memory data, battery state, storage, network
counters, and kernel logs. It owns all read/write validation.

DeviceInfo contains formatting and presentation calculations.

### domain/model

DeviceSnapshot and its child models hold one consistent device sample for
the UI. Missing values use nullable fields and are rendered as Unsupported.

### ui/home

The native Android view layer presents Overview, CPU, GPU, CCI/DDR, Memory,
Thermal, I/O, and System pages. UI code does not execute shell commands or
read sysfs directly.

## Implemented interfaces

| Function | Discovery source | Write policy |
| --- | --- | --- |
| CPU policy | `/sys/devices/system/cpu/cpufreq/policy*` or legacy `cpu*/cpufreq` | Governor and min/max from live exposed lists only |
| GPU telemetry | KGSL and matching devfreq nodes | Read-only |
| CCI / DDR telemetry | Matching `/sys/class/devfreq/*` names | Read-only |
| Thermal | `/sys/class/thermal/thermal_zone*/` | Read-only |
| Memory / ZRAM | `/proc/meminfo`, `/sys/block/zram0/` | Read-only |
| I/O scheduler | `/sys/block/*/queue/scheduler` | Selected scheduler must be in live node list |
| VM parameters | `/proc/sys/vm/swappiness`, `/proc/sys/vm/vfs_cache_pressure` | swappiness 0–100; cache pressure 10–200 |
| TCP congestion | `/proc/sys/net/ipv4/tcp_congestion_control` | Selected algorithm must be in `tcp_available_congestion_control` |
| Battery | Android `ACTION_BATTERY_CHANGED` | Read-only |
| Network | Android `TrafficStats` | Read-only counters |
| Kernel log | `dmesg` | Read-only |

Paths are discovery candidates, not assumptions. A node that cannot be read is
not represented as supported. A legacy CPU policy outside the explicit cpufreq
path allow-list is shown read-only.

## CPU write transaction

1. Require a verified root shell.
2. Verify the policy path against the cpufreq allow-list.
3. Re-read the live available-frequency and governor lists at apply time.
4. Verify min/max values are members of the live frequency table and
   `min <= max`.
5. Read and retain the old min/max/governor values.
6. Apply the governor and CPU limits in an order that avoids transient
   `min > max`.
7. Read back the updated values. On a failed write or mismatch, attempt to
   restore the previous values in a safe order.

Frequency controls remain hidden if the current bounds are missing or do not
match the live OPP list. If the active governor cannot be identified, the UI
offers **Keep current** rather than silently selecting the first governor.

Runtime changes are not guaranteed to survive a reboot.

## Other writable controls

- **I/O scheduler:** the node is re-read immediately before writing, and the
  selected scheduler must still appear in its live available list.
- **VM:** swappiness is limited to 0–100; VFS cache pressure is limited to
  10–200. Previous values are captured and best-effort rollback is attempted
  if the second write fails.
- **TCP congestion:** a new value is accepted only if it is still present in
  the live `tcp_available_congestion_control` list.

These changes are runtime-only and are not persisted by the app.

## Root compatibility

The bridge targets root implementations exposing `su -c`. Intended
compatibility includes Magisk, KernelSU, KernelSU Next, BakaSU, and APatch.
Provider identification is informational and is not required for standard SU
command execution. Real-device verification remains required for each manager
version and ROM.

## Explicit safety boundaries

- No GPU overclock, GPU clock cap, or GPU frequency writes.
- No voltage overrides, regulator changes, CPR/corner scaling, or power-rail writes.
- No DDR/RAM overclock.
- No thermal trip-point, cooling-device, or thermal-protection writes.
- No arbitrary commands entered by the user.
- No recreation of kernel-owned Zygote DDR bandwidth boost by application polling.
- Unknown interfaces remain unsupported/read-only.

CPU policy, I/O scheduler, VM, and TCP controls use explicit path allow-lists,
live capability checks, and bounded values. Future write features must have a
documented path allow-list, value validation, failure handling, and device
testing.

## Telemetry and refresh

The dashboard reads CPU, GPU/KGSL, CCI/DDR devfreq, thermal zones, RAM/ZRAM,
battery, storage, network counters, disk statistics, and uptime from available
system interfaces. Storage failures are represented as Unsupported rather
than zero. Disk counters exclude partition entries to avoid counting a whole
disk and its partitions together.

The UI samples device state on a background executor while the Activity is
resumed. It refreshes every 8 seconds and stops scheduling new refreshes when
the Activity is paused. This is not a persistent background monitoring service.

## Build configuration

- Kotlin
- Android Gradle Plugin 9.4.0
- Gradle Wrapper 9.6.0
- JDK 17
- Compile SDK 36
- Target SDK 35
- Minimum SDK 26
- Application ID `id.kazera.nocturne.kernelmanager`

## Verification

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

GitHub Actions runs only through
**Actions → Android CI → Run workflow**, selecting `main`.
