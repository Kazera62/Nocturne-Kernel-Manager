# Changelog

Perubahan penting proyek dicatat di sini.

## [Unreleased]

### Added
- Dashboard perangkat dengan halaman Overview, CPU, GPU, CCI/DDR, Memory,
  Thermal, I/O, dan System.
- SU-compatible root bridge dengan timeout, output capture, dan write allow-list.
- Deteksi CPU cpufreq policy, governor, dan OPP table secara dinamis.
- Kontrol CPU governor dan frequency min/max dengan validasi, read-back, dan rollback best-effort.
- Telemetry KGSL/GPU, devfreq, thermal zone, RAM/ZRAM, battery, storage,
  network, disk I/O, dan uptime.
- Kontrol I/O scheduler berdasarkan daftar live yang diekspos block device.
- Kontrol VM swappiness dan VFS cache pressure dengan batas nilai dan rollback best-effort.
- Kontrol TCP congestion control berdasarkan daftar algoritma live dari kernel.
- Tampilan dmesg terbaru untuk diagnosis (root diperlukan).
- Unit tests untuk parser kernel.

### Changed
- README dan architecture documentation diperbarui sesuai implementasi.
- CPU fallback path legacy hanya dapat ditulis jika cocok dengan allow-list per-CPU yang ditetapkan.
- Nilai storage yang gagal dibaca ditampilkan sebagai Unsupported.
- Project scope menandai monitoring, root bridge, CPU, I/O, VM, dan TCP controls aktif.

### Safety
- GPU, voltage, regulator, CPR/corner, DDR overclock, dan thermal protection
  tetap tidak dapat diubah melalui aplikasi.
- Interface yang tidak ditemukan ditampilkan sebagai Unsupported.
- Controller tidak otomatis memilih opsi pertama bila nilai aktif tidak diketahui.
- Tidak ada telemetry contoh atau dummy controls.
