# Changelog

## [0.4.0] - 2026-11-12

### Added
- Optional Advanced kernel menu, disabled by default and enabled from Settings.
- KGSL GPU maximum-OPP selection using the running kernel's live gpu_available_frequencies table.
- Secondary CPU hotplug controls for present CPU IDs 1–7; CPU0 remains protected.
- CCI/DDR min/max devfreq controls when writable bounds and a live available-frequency table are exposed.
- Regulator voltage, limits and state telemetry where the regulator sysfs nodes are readable.
- Runtime GPU OPP mapping and CPU hotplug target validation tests.

### Changed
- Bump app version to 0.4.0 (versionCode 4).
- Include regulator voltage and optional devfreq min/max values in device telemetry.
- Keep CPU tuning within the live cpufreq OPP tables.
- Keep all advanced controls behind live capability checks and root validation.

### Safety
- GPU control writes only KGSL max_pwrlevel; it does not add OPPs, change voltage, or modify thermal_pwrlevel.
- CPU/GPU voltage and CPR remain read-only; no runtime undervolt interface is exposed.
- CCI/DDR bounds are writable only for discovered fabric nodes and exact live OPP values; regulator, thermal policy and DDR bandwidth boost controls remain read-only.
- Higher GPU limits can increase heat and power and may cause artifacts, freezes, reboots or data loss.

## [0.3.0] - 2026-11-12

### Added
- Startup root preflight before full hardware telemetry scanning.
- Persistent interactive SU session with serialized commands, output markers and timeout cleanup.
- Dedicated Settings page opened from the top-right gear icon.
- English and Bahasa Indonesia selection, persisted locally.
- Configurable telemetry refresh interval: 5, 8, 15 or 30 seconds.
- Redesigned bottom navigation: Overview, Performance, Monitor and System.
- CPU core present/online telemetry.
- ZRAM compression algorithm and supported-algorithm discovery.
- KSM status and shared-page telemetry.
- Entropy pool and wakeup-threshold telemetry.
- CPU topology parser unit tests.

### Changed
- Root badge updates immediately after preflight, independently from the later device scan.
- Refresh reuses the cached SU process instead of launching a new su command for each sysfs/procfs read.
- Manual refresh invalidates the cached session and re-checks root access.
- Main UI groups related features into four sections and preserves controller selections during background refresh.
- Application version bumped to 0.3.0.

### Safety
- CPU core hotplug remains read-only.
- ZRAM algorithm, KSM, entropy, GPU, CCI/DDR and thermal data remain monitoring-only.
- No GPU overclock/cap, voltage/regulator, CPR/corner, DDR overclock or thermal-bypass controls were added.
- SmartPack concepts were reimplemented independently; no SmartPack source file was copied.
