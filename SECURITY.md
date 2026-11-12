# Security Policy

## Scope

Nocturne Kernel Manager targets Samsung Galaxy M11 / A11 SDM450 devices.

The app requires root permission only through the installed root manager's SU interface. It uses a cached interactive root session, a serialized command channel and bounded timeouts. The UI does not expose arbitrary shell command input.

## Current writable interfaces

- CPU cpufreq governor and min/max frequency, validated against the live policy OPP/governor lists.
- Block I/O scheduler, validated against the live scheduler list.
- VM swappiness (0–100).
- VM VFS cache pressure (10–200).
- TCP congestion algorithm, validated against the kernel's live available list.

All writes are checked by read-back where applicable. Multi-value changes attempt best-effort rollback on failure.

## Read-only interfaces

CPU core topology, GPU/KGSL, CCI/DDR devfreq, thermal zones, ZRAM algorithm, KSM, entropy, battery, storage, network counters and kernel logs are read-only.

## Explicit non-features

The app does not write GPU clocks, voltage, regulator, CPR/corner, DDR/RAM overclock, thermal trip-points or CPU hotplug nodes. It has no arbitrary shell command field.

## Reporting

Do not publish exploitable details in public issues. Use GitHub private vulnerability reporting if enabled, otherwise contact the repository maintainer privately.

Include app version, device model, Android version, root manager/version, kernel release, impact and a safe reproduction description.
