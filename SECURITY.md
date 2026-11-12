# Security Policy

## Scope

Nocturne Kernel Manager targets Samsung Galaxy M11 (SM-M115) and Galaxy A11
(SM-A115) devices based on Qualcomm Snapdragon 450 / SDM450. Hardware and
writable-interface availability depend on the running firmware, ROM, kernel
and root manager. A documented control is not a claim that every M11/A11
variant has been physically tested.

The app obtains elevated access only through the installed root manager's
standard `su` interface. It reuses a serialized interactive session with
bounded command timeouts. The UI does not provide an arbitrary shell-command
field.

## Current writable interfaces

Every write is gated by root access and live interface/value validation.
Read-back verification is used where applicable; multi-step updates attempt
best-effort rollback if a write or verification fails.

- **CPU cpufreq:** per-policy governor and minimum/maximum frequency, selected
  from the policy's live governor and frequency lists.
- **CPU hotplug:** only present secondary CPU IDs 1–7. CPU0 is protected and
  the app refuses to take a core offline when that would leave fewer than two
  cores online.
- **GPU/KGSL:** maximum power-level selection mapped to the running kernel's
  existing `gpu_available_frequencies` table. The app writes only
  `max_pwrlevel`; it does not create or modify OPP entries.
- **CCI/DDR fabric devfreq:** minimum/maximum limits only when the detected
  fabric node exposes readable bounds and a live `available_frequencies`
  table. Selected values must exist in that table.
- **Schedutil:** exposed `hispeed_load`, `hispeed_freq`,
  `up_rate_limit_us`, `down_rate_limit_us` and optional WALT PL flag,
  subject to bounded values and live CPU OPP validation.
- **Dynamic Fsync:** optional 0/1 toggle through the fixed
  `/sys/kernel/dyn_fsync/Dyn_fsync_active` node, when readable.
- **Block I/O scheduler:** whole physical devices from a fixed device-name
  allow-list; the selected scheduler must be present in the live list.
- **VM parameters:** swappiness (0–100) and VFS cache pressure (10–200).
- **TCP congestion control:** selected algorithm must appear in the kernel's
  current available-algorithm list.

These are runtime controls. The app does not guarantee that changes persist
after reboot.

## Read-only interfaces

The following remain monitoring-only:

- Regulator voltage, voltage limits and regulator state.
- Thermal-zone readings and thermal policy/trip points.
- CPU topology telemetry, except the guarded secondary-core hotplug control.
- GPU telemetry other than the KGSL maximum power-level selector.
- CCI/DDR telemetry other than the conditional min/max controls above.
- ZRAM compression algorithm, KSM status/statistics and entropy readings.
- Battery, storage, network counters, uptime, kernel release and kernel logs.

## Explicit non-features and safety limits

The app does not expose:

- Arbitrary shell-command input in the UI.
- GPU OPP-table modification, creation of new OPPs, or a frequency outside
  the live kernel OPP table.
- GPU/CPU voltage overrides, regulator writes, undervolting, or CPR/corner
  scaling writes.
- DDR/RAM overclocking, new devfreq OPP creation, or changes to kernel-owned
  bandwidth-boost policy.
- Thermal trip-point changes or thermal-protection bypass.
- CPU0 hotplug, or disabling cores when fewer than two cores would remain
  online.
- Apply-on-boot behavior or a promise of persistence across reboot.

Dynamic Fsync can defer `fsync`/`fdatasync` while the display is on. A kernel
crash or forced power loss before deferred writes complete can lose recent
data or corrupt the filesystem. GPU, CPU, schedutil and fabric tuning can
increase heat or power use and may cause instability, freezes, reboots or
data loss. Keep thermal protection enabled and make gradual changes.

## Reporting

Do not publish exploitable details in public issues. Use GitHub private
vulnerability reporting if enabled; otherwise contact the repository
maintainer privately.

Include app version, device model, Android version, root manager/version,
kernel release, impact and a safe reproduction description.
