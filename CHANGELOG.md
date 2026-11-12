# Changelog

Perubahan penting proyek dicatat di sini.

## [Unreleased]

### Added
- Dashboard perangkat dengan halaman Overview, CPU, GPU, CCI/DDR, Memory,
  Thermal, I/O, dan System.
- SU-compatible root bridge dengan timeout, output capture, dan write allow-list.
- Deteksi CPU cpufreq policy, governor, dan OPP table secara dinamis.
- Kontrol CPU governor dan frequency min/max dengan validasi dan rollback best-effort.
- Telemetry KGSL/GPU, devfreq, thermal zone, RAM/ZRAM, battery, storage,
  network, disk I/O, dan uptime.
- Kontrol I/O scheduler berdasarkan pilihan yang diekspos oleh block device.
- Tampilan dmesg terbaru untuk diagnosis (root diperlukan).
- Unit tests untuk parser kernel.

### Changed
- README dan architecture documentation diperbarui sesuai implementasi.
- Project scope kini menandai monitoring, root bridge, CPU dan I/O controls aktif.

### Safety
- GPU, voltage, regulator, CPR/corner, DDR overclock, dan thermal protection
  tetap tidak dapat diubah melalui aplikasi.
- Interface yang tidak ditemukan ditampilkan sebagai Unsupported.
- Tidak ada telemetry contoh atau dummy controls.
