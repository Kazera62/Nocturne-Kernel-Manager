package id.kazera.nocturne.kernelmanager.ui.settings

import android.content.Context

object UiPreferences {
    private const val FILE_NAME = "nocturne_ui_preferences"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_REFRESH_INTERVAL = "refresh_interval_seconds"

    fun language(context: Context): String =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, "en")
            ?.takeIf { it == "en" || it == "id" } ?: "en"

    fun setLanguage(context: Context, language: String) {
        if (language != "en" && language != "id") return
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language)
            .apply()
    }

    fun refreshIntervalSeconds(context: Context): Int {
        val saved = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_REFRESH_INTERVAL, 8)
        return if (saved in ALLOWED_INTERVALS) saved else 8
    }

    fun setRefreshIntervalSeconds(context: Context, seconds: Int) {
        if (seconds !in ALLOWED_INTERVALS) return
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_REFRESH_INTERVAL, seconds)
            .apply()
    }

    private val ALLOWED_INTERVALS = setOf(5, 8, 15, 30)
}

object UiText {
    private val id = mapOf(
        "Refresh" to "Segarkan",
        "Settings" to "Pengaturan",
        "Overview" to "Ringkasan",
        "Performance" to "Performa",
        "Monitor" to "Pemantauan",
        "System" to "Sistem",
        "Device overview" to "Ringkasan perangkat",
        "Live readings from interfaces available on this device." to "Data langsung dari interface yang tersedia pada perangkat.",
        "Initializing device interface" to "Menyiapkan interface perangkat",
        "Checking root access before scanning kernel interfaces…" to "Memeriksa akses root sebelum memindai interface kernel…",
        "Reading device information and probing available kernel nodes…" to "Membaca informasi perangkat dan memeriksa node kernel…",
        "No sample or placeholder telemetry is displayed." to "Tidak ada data contoh atau telemetry palsu.",
        "Device" to "Perangkat",
        "Hardware identity" to "Identitas hardware",
        "Model" to "Model",
        "Device code" to "Kode perangkat",
        "Platform" to "Platform",
        "Kernel" to "Kernel",
        "Uptime" to "Waktu aktif",
        "Root access" to "Akses root",
        "ROOT CHECK" to "CEK ROOT",
        "ROOT CHECK" to "CEK ROOT",
        "CPU CORE TOPOLOGY" to "TOPOLOGI CORE CPU",
        "Read-only core online status" to "Status core aktif (hanya baca)",
        "Cores online" to "Core aktif",
        "Online" to "Aktif",
        "Offline" to "Nonaktif",
        "CPU core topology" to "Topologi core CPU",
        "Root ready" to "Root aktif",
        "DEVICE" to "PERANGKAT",
        "ROOT ACCESS" to "AKSES ROOT",
        "Uses the standard su command interface. Manager-specific APIs are not required for basic commands." to "Menggunakan antarmuka perintah su standar. API khusus root manager tidak diperlukan untuk perintah dasar.",
        "CPU cpufreq policies" to "CPU cpufreq policy",
        "Open CPU for available governor and frequency controls." to "Buka halaman Performa untuk kontrol governor dan frekuensi CPU.",
        "CPU, virtual memory and storage scheduler controls." to "Kontrol CPU, memori virtual, dan scheduler penyimpanan.",
        "Live hardware telemetry. Unsupported nodes are not simulated." to "Telemetry hardware langsung. Node yang tidak didukung tidak disimulasikan.",
        "Application preferences and root diagnostics." to "Preferensi aplikasi dan diagnostik root.",
        "Select a valid refresh interval." to "Pilih interval penyegaran yang valid.",
        "Refresh interval updated." to "Interval penyegaran diperbarui.",
        "Provider" to "Penyedia",
        "Root access required" to "Akses root diperlukan",
        "Read-only values remain available where Android permits. Grant root through your installed root manager to apply supported controls." to "Data yang hanya dibaca tetap tersedia sesuai izin Android. Berikan akses root melalui root manager untuk menerapkan kontrol yang didukung.",
        "Apply governor and limits" to "Terapkan governor dan batas frekuensi",
        "RAM" to "RAM",
        "Read: " to "Dibaca: ",
        "Core hotplug control is not enabled in this build. This page only observes the kernel state." to "Kontrol hotplug core belum diaktifkan pada build ini. Halaman ini hanya memantau status kernel."

        "Root off" to "Root tidak aktif",
        "Root status unknown" to "Status root belum diketahui",
        "Privileged shell verified" to "Shell dengan akses root terverifikasi",
        "Root permission not available" to "Izin root tidak tersedia",
        "Root session is ready and cached." to "Sesi root siap dan tersimpan.",
        "Grant root access through your root manager, then tap Check root." to "Berikan izin root melalui root manager, lalu tekan Periksa root.",
        "Check root now" to "Periksa root sekarang",
        "PROCESSOR" to "PROSESOR",
        "Detected CPU frequency policies" to "CPU frequency policy terdeteksi",
        "Open Performance for CPU controls." to "Buka Performa untuk kontrol CPU.",
        "GRAPHICS" to "GRAFIS",
        "Adreno / KGSL telemetry" to "Telemetry Adreno / KGSL",
        "GPU clock" to "Clock GPU",
        "GPU utilization" to "Utilisasi GPU",
        "GPU overclock and voltage controls are intentionally not provided." to "Kontrol overclock GPU dan tegangan memang tidak disediakan.",
        "MEMORY" to "MEMORI",
        "RAM and compressed swap" to "RAM dan swap terkompresi",
        "RAM usage" to "Penggunaan RAM",
        "RAM total" to "Total RAM",
        "ZRAM size" to "Ukuran ZRAM",
        "THERMAL" to "TERMAL",
        "Sensor summary" to "Ringkasan sensor",
        "Sensors detected" to "Sensor terdeteksi",
        "Highest temperature" to "Suhu tertinggi",
        "Thermal protection remains under kernel control." to "Proteksi termal tetap dikendalikan kernel.",
        "DETECTION NOTES" to "CATATAN DETEKSI",
        "Performance controls" to "Kontrol performa",
        "CPU controls" to "Kontrol CPU",
        "Only values exposed by each cpufreq policy are selectable." to "Hanya nilai yang disediakan setiap cpufreq policy yang dapat dipilih.",
        "CPU interface unavailable" to "Interface CPU tidak tersedia",
        "No readable cpufreq policy was found." to "Tidak ditemukan cpufreq policy yang dapat dibaca.",
        "Current frequency" to "Frekuensi saat ini",
        "Current governor" to "Governor saat ini",
        "Current minimum" to "Batas minimum saat ini",
        "Current maximum" to "Batas maksimum saat ini",
        "Available OPP entries" to "Jumlah OPP tersedia",
        "Governor" to "Governor",
        "Keep current" to "Pertahankan saat ini",
        "Minimum frequency" to "Frekuensi minimum",
        "Maximum frequency" to "Frekuensi maksimum",
        "Apply CPU settings" to "Terapkan pengaturan CPU",
        "Root permission is required." to "Izin root diperlukan.",
        "Kernel frequency values are unavailable." to "Nilai frekuensi kernel tidak tersedia.",
        "This policy uses a non-standard path. Telemetry is read-only for safety." to "Policy ini memakai path non-standar. Telemetry hanya dapat dibaca demi keamanan.",
        "Frequency controls hidden because current min/max are missing or do not match the live OPP table." to "Kontrol frekuensi disembunyikan karena batas saat ini tidak cocok dengan tabel OPP.",
        "Available governor list" to "Daftar governor yang tersedia",
        "Grant root access to apply changes." to "Berikan izin root untuk menerapkan perubahan.",
        "Control boundary" to "Batas kontrol",
        "Frequency values are loaded from the active kernel OPP table. The app rejects values outside that table and restores previous limits when a write fails. No voltage or regulator node is modified." to "Nilai frekuensi diambil dari tabel OPP kernel yang aktif. Nilai di luar tabel ditolak dan batas sebelumnya akan dipulihkan bila penulisan gagal. Node tegangan dan regulator tidak diubah.",
        "GPU telemetry" to "Telemetry GPU",
        "Read-only Adreno / KGSL and devfreq readings." to "Pembacaan Adreno / KGSL dan devfreq saja.",
        "GPU STATUS" to "STATUS GPU",
        "No KGSL / GPU node detected" to "Node KGSL / GPU tidak ditemukan",
        "Utilization" to "Utilisasi",
        "GPU SAFETY POLICY" to "KEBIJAKAN KEAMANAN GPU",
        "This application does not expose GPU overclocking, maximum-clock caps, voltage overrides, regulator changes, or CPR/corner scaling. Existing kernel GPU operating points are left unchanged." to "Aplikasi ini tidak menyediakan overclock GPU, pembatas clock maksimum, override tegangan, perubahan regulator, atau CPR/corner scaling. Operating point GPU kernel tidak diubah.",
        "Telemetry is shown only when the device exposes a readable KGSL or devfreq node." to "Telemetry hanya ditampilkan bila perangkat menyediakan node KGSL atau devfreq yang dapat dibaca.",
        "CCI & DDR fabric" to "Interkoneksi CCI & DDR",
        "Current devfreq values from detected interconnect and memory-bus nodes." to "Nilai devfreq saat ini dari node interkoneksi dan memory bus yang terdeteksi.",
        "Fabric telemetry unavailable" to "Telemetry interkoneksi tidak tersedia",
        "No matching CCI, cpubw, bus, DDR, memory or LLCC devfreq node was detected." to "Node devfreq CCI, cpubw, bus, DDR, memory, atau LLCC tidak ditemukan.",
        "Available frequency entries" to "Daftar frekuensi tersedia",
        "KERNEL-OWNED BOOST" to "BOOST DIKELOLA KERNEL",
        "Kernel-side transient boosts, including Zygote-triggered DDR bandwidth behavior when implemented by the running kernel, are not recreated by this app. This page reports exposed devfreq state only; it does not poll or inject a replacement boost." to "Boost sementara yang dikelola kernel, termasuk boost bandwidth DDR saat Zygote berjalan, tidak dibuat ulang oleh aplikasi. Halaman ini hanya membaca status devfreq dan tidak menyuntikkan boost pengganti.",
        "Memory" to "Memori",
        "RAM and ZRAM data read from system memory interfaces." to "Data RAM dan ZRAM dari interface memori sistem.",
        "Total" to "Total",
        "Available" to "Tersedia",
        "Used" to "Terpakai",
        "ZRAM" to "ZRAM",
        "Configured disk size" to "Ukuran disk terkonfigurasi",
        "Original data" to "Data asli",
        "Compressed data" to "Data terkompresi",
        "Memory consumed" to "Memori terpakai",
        "ZRAM configuration is shown read-only; no swap or compression policy is changed." to "Konfigurasi ZRAM hanya dibaca; kebijakan swap dan kompresi tidak diubah.",
        "VIRTUAL MEMORY TUNING" to "PENYESUAIAN MEMORI VIRTUAL",
        "Runtime VM parameters · current boot session" to "Parameter VM runtime · sesi boot saat ini",
        "Current swappiness" to "Swappiness saat ini",
        "Current VFS cache pressure" to "VFS cache pressure saat ini",
        "Swappiness" to "Swappiness",
        "VFS cache pressure" to "VFS cache pressure",
        "Apply VM settings" to "Terapkan pengaturan VM",
        "Apply virtual memory settings" to "Terapkan pengaturan memori virtual",
        "VM tuning nodes or current values outside supported ranges" to "Node VM tidak tersedia atau nilai di luar rentang dukungan",
        "Root permission is required to apply changes." to "Izin root diperlukan untuk menerapkan perubahan.",
        "KERNEL OPTIMIZATION TELEMETRY" to "TELEMETRY OPTIMASI KERNEL",
        "ZRAM compression algorithm" to "Algoritma kompresi ZRAM",
        "KSM status" to "Status KSM",
        "Enabled" to "Aktif",
        "Disabled" to "Nonaktif",
        "KSM shared pages" to "Halaman KSM yang dibagikan",
        "KSM sharing pages" to "Halaman KSM hasil sharing",
        "Entropy available" to "Entropy tersedia",
        "Entropy read threshold" to "Ambang baca entropy",
        "Entropy write threshold" to "Ambang tulis entropy",
        "Kernel optimization interfaces are shown as read-only telemetry." to "Interface optimasi kernel ditampilkan sebagai telemetry saja.",
        "Thermal sensors" to "Sensor termal",
        "All detected thermal zones. No thermal policy writes are exposed." to "Semua zona termal yang terdeteksi. Tidak ada perubahan kebijakan termal.",
        "Thermal interface unavailable" to "Interface termal tidak tersedia",
        "No readable thermal zone temperature node was found." to "Node suhu thermal zone yang dapat dibaca tidak ditemukan.",
        "THERMAL SAFETY" to "KEAMANAN TERMAL",
        "Thermal throttling, trip points, cooling devices and shutdown protection remain controlled by the kernel and Android thermal framework. This app does not disable or raise thermal limits." to "Thermal throttling, trip point, cooling device, dan proteksi shutdown tetap dikendalikan kernel dan framework termal Android. Aplikasi tidak menonaktifkan atau menaikkan batas termal.",
        "I/O scheduler" to "Scheduler I/O",
        "Select only schedulers reported by each block device." to "Hanya scheduler yang dilaporkan setiap block device yang dapat dipilih.",
        "Cumulative disk sectors" to "Sektor disk kumulatif",
        "I/O scheduler interfaces unavailable" to "Interface scheduler I/O tidak tersedia",
        "No /sys/block/*/queue/scheduler node was readable." to "Tidak ada node /sys/block/*/queue/scheduler yang dapat dibaca.",
        "Active scheduler" to "Scheduler aktif",
        "Apply scheduler" to "Terapkan scheduler",
        "Apply I/O scheduler" to "Terapkan scheduler I/O",
        "System & diagnostics" to "Sistem & diagnostik",
        "Battery, storage, network counters and kernel logs." to "Baterai, penyimpanan, counter jaringan, dan log kernel.",
        "BATTERY" to "BATERAI",
        "Level" to "Level",
        "State" to "Status",
        "Charging / full" to "Mengisi / penuh",
        "Not charging" to "Tidak mengisi",
        "Temperature" to "Suhu",
        "Voltage" to "Tegangan",
        "Technology" to "Teknologi",
        "STORAGE" to "PENYIMPANAN",
        "Data partition total" to "Total partisi data",
        "Available space" to "Ruang tersedia",
        "NETWORK" to "JARINGAN",
        "Total RX" to "Total RX",
        "Total TX" to "Total TX",
        "Android TrafficStats counters; cumulative since the system counter reset." to "Counter Android TrafficStats; akumulatif sejak counter sistem direset.",
        "TCP congestion control" to "TCP congestion control",
        "Current algorithm" to "Algoritma saat ini",
        "Available TCP congestion algorithms" to "Algoritma TCP congestion tersedia",
        "Apply TCP algorithm" to "Terapkan algoritma TCP",
        "KERNEL LOG" to "LOG KERNEL",
        "Read the latest kernel ring-buffer entries. Root access is required." to "Baca entri terbaru kernel ring buffer. Memerlukan akses root.",
        "View dmesg" to "Lihat dmesg",
        "Latest kernel log" to "Log kernel terbaru",
        "Close" to "Tutup",
        "BUILD INFORMATION" to "INFORMASI BUILD",
        "Kernel release" to "Versi kernel",
        "Settings" to "Pengaturan",
        "Application preferences" to "Preferensi aplikasi",
        "Language" to "Bahasa",
        "Choose the language used throughout the application." to "Pilih bahasa yang digunakan di seluruh aplikasi.",
        "Refresh interval" to "Interval penyegaran",
        "Choose how often live kernel telemetry is refreshed." to "Pilih frekuensi pembaruan telemetry kernel.",
        "5 seconds" to "5 detik",
        "8 seconds" to "8 detik",
        "15 seconds" to "15 detik",
        "30 seconds" to "30 detik",
        "Apply refresh interval" to "Terapkan interval penyegaran",
        "Root diagnostics" to "Diagnostik root",
        "Root access is checked before kernel telemetry is scanned. A cached SU session is reused for subsequent reads." to "Akses root diperiksa sebelum telemetry kernel dipindai. Sesi SU tersimpan digunakan ulang untuk pembacaan berikutnya.",
        "ABOUT" to "TENTANG",
        "Nocturne Kernel Manager" to "Nocturne Kernel Manager",
        "Target: Samsung Galaxy M11 / A11 · SDM450" to "Target: Samsung Galaxy M11 / A11 · SDM450",
        "Version 0.3.0" to "Versi 0.2.0",
        "Unsupported" to "Tidak didukung",
        "Unknown" to "Tidak diketahui",
        "Root permission is required to read dmesg." to "Izin root diperlukan untuk membaca dmesg.",
        "TCP read-back did not match the requested algorithm." to "Hasil baca ulang TCP tidak sesuai algoritma yang diminta.",
        "VM read-back did not match; previous values were restored where possible." to "Hasil baca ulang VM tidak sesuai; nilai sebelumnya telah dicoba untuk dipulihkan.",
        "I/O scheduler read-back did not match the requested value." to "Hasil baca ulang scheduler I/O tidak sesuai nilai yang diminta.",
        "CPU policy updated. Values are not guaranteed to persist after reboot." to "CPU policy diperbarui. Nilai belum tentu bertahan setelah reboot."
    )

    fun translate(context: Context, source: String): String {
        if (UiPreferences.language(context) != "id") return source
        if (source.startsWith("Core ")) return "Core " + source.removePrefix("Core ")
        return id[source] ?: source
    }
}
