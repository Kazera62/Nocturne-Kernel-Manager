# Changelog

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
