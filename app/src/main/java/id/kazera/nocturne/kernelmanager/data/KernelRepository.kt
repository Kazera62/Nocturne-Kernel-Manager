package id.kazera.nocturne.kernelmanager.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import id.kazera.nocturne.kernelmanager.core.kernel.KernelParsers
import id.kazera.nocturne.kernelmanager.core.privilege.RootAccess
import id.kazera.nocturne.kernelmanager.core.privilege.RootShell
import id.kazera.nocturne.kernelmanager.core.privilege.ShellResult
import id.kazera.nocturne.kernelmanager.domain.model.BatteryInfo
import id.kazera.nocturne.kernelmanager.domain.model.CpuCoreInfo
import id.kazera.nocturne.kernelmanager.domain.model.CpuPolicyInfo
import id.kazera.nocturne.kernelmanager.domain.model.DeviceSnapshot
import id.kazera.nocturne.kernelmanager.domain.model.DevfreqInfo
import id.kazera.nocturne.kernelmanager.domain.model.GpuInfo
import id.kazera.nocturne.kernelmanager.domain.model.IoSchedulerInfo
import id.kazera.nocturne.kernelmanager.domain.model.IoUsage
import id.kazera.nocturne.kernelmanager.domain.model.KernelExtras
import id.kazera.nocturne.kernelmanager.domain.model.MemoryInfo
import id.kazera.nocturne.kernelmanager.domain.model.StorageInfo
import id.kazera.nocturne.kernelmanager.domain.model.ThermalSensor
import java.io.File
import java.util.concurrent.TimeUnit

data class ControlResult(
    val success: Boolean,
    val message: String
)

/**
 * Reads only kernel interfaces present on the running device.
 * Writable operations are restricted to validated CPU, I/O, VM and TCP controls.
 */
class KernelRepository(
    private val rootShell: RootShell = RootShell()
) {
    @Volatile
    private var cachedRoot: RootAccess? = null

    @Volatile
    private var rootCheckedAt: Long = 0L

    fun snapshot(context: Context): DeviceSnapshot {
        val root = rootAccess()
        val cpuCores = discoverCpuCores()
        val cpuPolicies = discoverCpuPolicies()
        val devfreq = discoverDevfreq()
        val gpu = discoverGpu(devfreq)
        val thermal = discoverThermalSensors()
        val ioSchedulers = discoverIoSchedulers()
        val memory = readMemory()
        val kernelExtras = readKernelExtras()
        val battery = readBattery(context)
        val storage = readStorage()
        val ioUsage = readIoUsage()
        val tcpCongestion = read("/proc/sys/net/ipv4/tcp_congestion_control")
        val availableTcpCongestion = read("/proc/sys/net/ipv4/tcp_available_congestion_control")
            .orEmpty().split(Regex("\\s+")).filter { it.matches(Regex("[A-Za-z0-9_-]{1,32}")) }.distinct()
        val uptime = read("/proc/uptime")?.substringBefore(' ')?.toDoubleOrNull()?.toLong()

        val warnings = mutableListOf<String>()
        if (!root.available) warnings += "Root unavailable; public telemetry only."
        if (cpuPolicies.isEmpty()) warnings += "No CPU frequency policy found."
        if (gpu.currentFrequencyHz == null && gpu.utilizationPercent == null) {
            warnings += "GPU telemetry unavailable; controls are read-only."
        }
        if (thermal.isEmpty()) warnings += "No thermal zone found."
        if (ioSchedulers.isEmpty()) warnings += "No supported physical block scheduler found."

        val deviceCode = Build.DEVICE.orEmpty().ifBlank { "Unknown" }
        val model = Build.MODEL.orEmpty().ifBlank { "Unknown Android device" }
        val platform = when {
            deviceCode.contains("m11", ignoreCase = true) ||
                model.contains("SM-M115", ignoreCase = true) ->
                "Samsung M11 · Qualcomm Snapdragon 450 target"
            deviceCode.contains("a11", ignoreCase = true) ||
                model.contains("SM-A115", ignoreCase = true) ->
                "Samsung A11 · Qualcomm Snapdragon 450 target"
            else -> Build.HARDWARE.orEmpty().ifBlank { "Platform not identified" }
        }

        return DeviceSnapshot(
            model = model,
            deviceCode = deviceCode,
            platform = platform,
            kernelVersion = read("/proc/sys/kernel/osrelease")
                ?: System.getProperty("os.version").orEmpty().ifBlank { "Unknown" },
            uptimeSeconds = uptime,
            root = root,
            cpuCores = cpuCores,
            cpuPolicies = cpuPolicies,
            gpu = gpu,
            devfreq = devfreq,
            ioSchedulers = ioSchedulers,
            thermalSensors = thermal,
            memory = memory,
            kernelExtras = kernelExtras,
            battery = battery,
            storage = storage,
            networkRxBytes = TrafficStats.getTotalRxBytes().takeIf { it >= 0L },
            networkTxBytes = TrafficStats.getTotalTxBytes().takeIf { it >= 0L },
            ioUsage = ioUsage,
            tcpCongestionControl = tcpCongestion,
            availableTcpCongestionControls = availableTcpCongestion,
            warnings = warnings
        )
    }

    fun applyCpuPolicy(
        policy: CpuPolicyInfo,
        governor: String?,
        minFrequencyKHz: Long,
        maxFrequencyKHz: Long
    ): ControlResult {
        if (!rootAccess().available) return ControlResult(false, "Root permission is required.")
        if (!Regex("^/sys/devices/system/cpu/(cpufreq/policy[0-9]+|cpu[0-7]/cpufreq)$").matches(policy.path)) {
            return ControlResult(false, "Rejected unknown CPU policy path.")
        }
        if (minFrequencyKHz <= 0L || maxFrequencyKHz <= 0L ||
            minFrequencyKHz > maxFrequencyKHz) {
            return ControlResult(false, "Invalid CPU frequency range.")
        }
        val minPath = policy.path + "/scaling_min_freq"
        val maxPath = policy.path + "/scaling_max_freq"
        val governorPath = policy.path + "/scaling_governor"

        // Re-read live capability lists; never trust a stale UI snapshot for a write.
        val liveFrequencyText = read(policy.path + "/scaling_available_frequencies")
            ?.takeIf { it.isNotBlank() }
            ?: read(policy.path + "/cpuinfo_available_frequencies")
        val liveFrequencies = KernelParsers.frequencyList(liveFrequencyText)
        if (liveFrequencies.isEmpty() || minFrequencyKHz !in liveFrequencies ||
            maxFrequencyKHz !in liveFrequencies) {
            return ControlResult(false, "Selected frequencies are no longer in the live kernel OPP table.")
        }
        val liveGovernors = read(policy.path + "/scaling_available_governors")
            .orEmpty().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (governor != null && governor !in liveGovernors) {
            return ControlResult(false, "The selected governor is no longer supported by this policy.")
        }
        val oldMin = read(minPath)?.toLongOrNull()
        val oldMax = read(maxPath)?.toLongOrNull()
        val oldGovernor = read(governorPath)
        if (oldMin == null || oldMax == null) {
            return ControlResult(false, "CPU frequency limit nodes are unavailable.")
        }

        fun rollback() {
            if (oldMin > maxFrequencyKHz) {
                rootShell.writeCpuNode(minPath, oldMin.toString())
                rootShell.writeCpuNode(maxPath, oldMax.toString())
            } else {
                rootShell.writeCpuNode(maxPath, oldMax.toString())
                rootShell.writeCpuNode(minPath, oldMin.toString())
            }
            oldGovernor?.takeIf { it.isNotBlank() }
                ?.let { rootShell.writeCpuNode(governorPath, it) }
        }

        if (governor != null) {
            val result = rootShell.writeCpuNode(governorPath, governor)
            if (!result.success) return ControlResult(false, result.error ?: "Governor update failed.")
        }

        // Avoid transient min > max states while moving the operating range.
        val minFirst = maxFrequencyKHz < oldMin
        val first = if (minFirst) {
            rootShell.writeCpuNode(minPath, minFrequencyKHz.toString())
        } else {
            rootShell.writeCpuNode(maxPath, maxFrequencyKHz.toString())
        }
        if (!first.success) {
            rollback()
            return ControlResult(false, first.error ?: "Could not update CPU limits.")
        }

        val second = if (minFirst) {
            rootShell.writeCpuNode(maxPath, maxFrequencyKHz.toString())
        } else {
            rootShell.writeCpuNode(minPath, minFrequencyKHz.toString())
        }
        if (!second.success) {
            rollback()
            return ControlResult(false, second.error ?: "CPU limit update failed; previous values were restored where possible.")
        }

        val verifiedMin = read(minPath)?.toLongOrNull()
        val verifiedMax = read(maxPath)?.toLongOrNull()
        val verifiedGovernor = if (governor != null) read(governorPath) else null
        if (verifiedMin != minFrequencyKHz || verifiedMax != maxFrequencyKHz ||
            (governor != null && verifiedGovernor != governor)) {
            rollback()
            return ControlResult(false, "Kernel read-back did not match the requested CPU policy; previous values were restored where possible.")
        }

        return ControlResult(true, "CPU policy updated. Values are not guaranteed to persist after reboot.")
    }

    fun applyVmSettings(swappiness: Int, cachePressure: Int): ControlResult {
        if (!rootAccess().available) return ControlResult(false, "Root permission is required.")
        if (swappiness !in 0..100 || cachePressure !in 10..200) {
            return ControlResult(false, "VM values are outside the allowed range.")
        }
        val oldSwappiness = read("/proc/sys/vm/swappiness")?.toIntOrNull()
        val oldCachePressure = read("/proc/sys/vm/vfs_cache_pressure")?.toIntOrNull()
        if (oldSwappiness == null || oldCachePressure == null) {
            return ControlResult(false, "VM tuning nodes are not available on this kernel.")
        }

        val swapResult = rootShell.writeVmSetting("swappiness", swappiness)
        if (!swapResult.success) {
            return ControlResult(false, swapResult.error ?: "Swappiness update failed.")
        }

        val cacheResult = rootShell.writeVmSetting("vfs_cache_pressure", cachePressure)
        if (!cacheResult.success) {
            rootShell.writeVmSetting("vfs_cache_pressure", oldCachePressure)
            rootShell.writeVmSetting("swappiness", oldSwappiness)
            return ControlResult(false, cacheResult.error ?: "VM update failed; previous values were restored where possible.")
        }

        val verifiedSwappiness = read("/proc/sys/vm/swappiness")?.toIntOrNull()
        val verifiedCachePressure = read("/proc/sys/vm/vfs_cache_pressure")?.toIntOrNull()
        if (verifiedSwappiness != swappiness || verifiedCachePressure != cachePressure) {
            rootShell.writeVmSetting("vfs_cache_pressure", oldCachePressure)
            rootShell.writeVmSetting("swappiness", oldSwappiness)
            return ControlResult(false, "VM read-back did not match; previous values were restored where possible.")
        }
        return ControlResult(true, "VM settings updated for the current boot session.")
    }

    fun applyTcpCongestionControl(selected: String): ControlResult {
        if (!rootAccess().available) return ControlResult(false, "Root permission is required.")
        val available = read("/proc/sys/net/ipv4/tcp_available_congestion_control")
            .orEmpty().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (selected !in available) {
            return ControlResult(false, "The selected TCP congestion algorithm is not exposed by this kernel.")
        }
        val result = rootShell.writeTcpCongestionControl(selected)
        if (!result.success) {
            return ControlResult(false, result.error ?: "TCP congestion control update failed.")
        }
        val active = read("/proc/sys/net/ipv4/tcp_congestion_control")
        return if (active == selected) {
            ControlResult(true, "TCP congestion control changed to " + selected + ".")
        } else {
            ControlResult(false, "TCP read-back did not match the requested algorithm.")
        }
    }

    fun applyIoScheduler(
        scheduler: IoSchedulerInfo,
        selected: String
    ): ControlResult {
        if (!rootAccess().available) return ControlResult(false, "Root permission is required.")
        if (!KernelParsers.isSupportedIoDevice(scheduler.deviceName) ||
            scheduler.path != "/sys/block/${scheduler.deviceName}/queue/scheduler") {
            return ControlResult(false, "Rejected unsupported physical I/O scheduler target.")
        }
        val liveSchedulers = KernelParsers.schedulerList(read(scheduler.path)).second
        if (liveSchedulers.isEmpty() || selected !in liveSchedulers) {
            return ControlResult(false, "The selected I/O scheduler is not exposed by the live kernel node.")
        }
        val result = rootShell.writeIoScheduler(scheduler.path, selected)
        if (!result.success) {
            return ControlResult(false, result.error ?: "I/O scheduler update failed.")
        }
        val active = KernelParsers.schedulerList(read(scheduler.path)).first
        return if (active == selected) {
            ControlResult(true, scheduler.deviceName + " scheduler changed to " + selected + ".")
        } else {
            ControlResult(false, "I/O scheduler read-back did not match the requested value.")
        }
    }

    fun kernelLogs(): ShellResult {
        if (!rootAccess().available) {
            return ShellResult(false, "", -1, "Root permission is required to read dmesg.")
        }
        return rootShell.kernelLogs()
    }

    fun checkRoot(): RootAccess = rootAccess()

    fun invalidateRootCache() {
        synchronized(this) {
            rootShell.invalidate()
            cachedRoot = null
            rootCheckedAt = 0L
        }
    }

    fun closeRootSession() {
        rootShell.close()
    }

    private fun rootAccess(): RootAccess {
        val now = System.nanoTime()
        val cached = cachedRoot
        if (cached != null && now - rootCheckedAt < TimeUnit.SECONDS.toNanos(45)) return cached
        synchronized(this) {
            val secondCached = cachedRoot
            if (secondCached != null &&
                System.nanoTime() - rootCheckedAt < TimeUnit.SECONDS.toNanos(45)) return secondCached
            val detected = rootShell.detect()
            cachedRoot = detected
            rootCheckedAt = System.nanoTime()
            return detected
        }
    }

    private fun read(path: String): String? {
        val file = File(path)
        if (file.isFile && file.canRead()) {
            try {
                return file.readText().trim()
            } catch (_: Exception) {
                // Retry through the approved root read bridge below.
            }
        }
        if (!rootAccess().available) return null
        return rootShell.read(path)
    }

    private fun discoverDirectories(path: String, pattern: Regex): List<File> {
        val direct = File(path).listFiles()
            ?.filter { it.isDirectory && pattern.matches(it.name) }
            .orEmpty()
        if (direct.isNotEmpty() || !rootAccess().available) return direct

        val listing = rootShell.execute(
            "for d in '$path'/*; do [ -d \"\$d\" ] && echo \"\$d\"; done",
            timeoutMs = 3_000
        )
        return listing.output.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() && it.startsWith(path + "/") }
            .filter { pattern.matches(it.substringAfterLast('/')) }
            .map { File(it) }
            .toList()
    }

    private fun discoverCpuCores(): List<CpuCoreInfo> {
        val coreIds = KernelParsers.cpuList(read("/sys/devices/system/cpu/present"))
        return coreIds.map { core ->
            val onlineValue = read("/sys/devices/system/cpu/cpu$core/online")?.toIntOrNull()
            CpuCoreInfo(
                coreId = core,
                online = onlineValue?.let { it == 1 } ?: if (core == 0) true else null
            )
        }
    }

    private fun discoverCpuPolicies(): List<CpuPolicyInfo> {
        val directories = discoverDirectories(
            "/sys/devices/system/cpu/cpufreq",
            Regex("policy[0-9]+")
        ).ifEmpty {
            val cpu0 = File("/sys/devices/system/cpu/cpu0/cpufreq")
            if (cpu0.isDirectory) listOf(cpu0) else emptyList()
        }

        return directories.sortedBy { it.name }.mapNotNull { directory ->
            val path = directory.absolutePath
            val current = read(path + "/scaling_cur_freq")
                ?: read(path + "/cpuinfo_cur_freq")
            val minimum = read(path + "/scaling_min_freq")
            val maximum = read(path + "/scaling_max_freq")
            val frequencyText = read(path + "/scaling_available_frequencies")
                ?.takeIf { it.isNotBlank() }
                ?: read(path + "/cpuinfo_available_frequencies")
            val frequencies = KernelParsers.frequencyList(frequencyText)
            val governors = read(path + "/scaling_available_governors")
                .orEmpty().split(Regex("\\s+")).filter { it.isNotBlank() }.distinct()
            CpuPolicyInfo(
                path = path,
                name = if (directory.name == "cpufreq") {
                    directory.parentFile?.name ?: directory.name
                } else {
                    directory.name
                },
                currentFrequencyKHz = KernelParsers.longValue(current),
                minFrequencyKHz = KernelParsers.longValue(minimum),
                maxFrequencyKHz = KernelParsers.longValue(maximum),
                availableFrequenciesKHz = frequencies,
                governor = read(path + "/scaling_governor"),
                availableGovernors = governors
            )
        }
    }

    private fun discoverDevfreq(): List<DevfreqInfo> {
        val directories = discoverDirectories(
            "/sys/class/devfreq",
            Regex("[A-Za-z0-9_.:@,-]+")
        )
        return directories.mapNotNull { directory ->
            val path = directory.absolutePath
            val name = read(path + "/name") ?: directory.name
            val lowered = (name + " " + directory.name).lowercase()
            if (listOf("cci", "cpubw", "bus", "ddr", "mem", "llcc").none { lowered.contains(it) }) {
                return@mapNotNull null
            }
            DevfreqInfo(
                name = name,
                path = path,
                currentFrequencyHz = KernelParsers.longValue(read(path + "/cur_freq")),
                governor = read(path + "/governor"),
                availableFrequenciesHz = KernelParsers.frequencyList(read(path + "/available_frequencies"))
            )
        }.distinctBy { it.path }
    }

    private fun discoverGpu(devfreq: List<DevfreqInfo>): GpuInfo {
        val kgslPaths = listOf(
            "/sys/class/kgsl/kgsl-3d0/gpuclk",
            "/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq",
            "/sys/class/devfreq/kgsl-3d0/cur_freq"
        )
        val gpuPath = kgslPaths.firstOrNull { !read(it).isNullOrBlank() }
        val gpuDevfreq = discoverDirectories(
            "/sys/class/devfreq",
            Regex("[A-Za-z0-9_.:@,-]+")
        ).firstOrNull {
            val name = (read(it.absolutePath + "/name") ?: it.name).lowercase()
            name.contains("gpu") || name.contains("kgsl")
        }

        val selectedPath = gpuPath ?: gpuDevfreq?.absolutePath?.plus("/cur_freq")
        val frequency = selectedPath?.let { KernelParsers.longValue(read(it)) }
        val busy = KernelParsers.gpuUtilization(read("/sys/class/kgsl/kgsl-3d0/gpubusy"))
        val available = if (gpuDevfreq != null) {
            KernelParsers.frequencyList(read(gpuDevfreq.absolutePath + "/available_frequencies"))
        } else {
            KernelParsers.frequencyList(read("/sys/class/kgsl/kgsl-3d0/gpu_available_frequencies"))
        }

        return GpuInfo(
            currentFrequencyHz = frequency,
            utilizationPercent = busy,
            availableFrequenciesHz = available,
            source = selectedPath ?: devfreq.firstOrNull { it.name.contains("gpu", true) }?.path
        )
    }

    private fun discoverThermalSensors(): List<ThermalSensor> {
        val zones = discoverDirectories("/sys/class/thermal", Regex("thermal_zone[0-9]+"))
        return zones.mapNotNull { zone ->
            val temperature = KernelParsers.thermalCelsius(read(zone.absolutePath + "/temp"))
                ?: return@mapNotNull null
            val type = read(zone.absolutePath + "/type")
                ?.takeIf { it.isNotBlank() } ?: zone.name
            ThermalSensor(type, temperature)
        }.sortedBy { it.name }
    }

    private fun discoverIoSchedulers(): List<IoSchedulerInfo> {
        val blockDevices = discoverDirectories(
            "/sys/block",
            Regex("(mmcblk[0-9]+|sd[a-c]|nvme[0-9]+n[0-9]+)")
        ).filter { KernelParsers.isSupportedIoDevice(it.name) }

        return blockDevices.mapNotNull { device ->
            val path = device.absolutePath + "/queue/scheduler"
            val content = read(path) ?: return@mapNotNull null
            val parsed = KernelParsers.schedulerList(content)
            if (parsed.second.isEmpty()) return@mapNotNull null
            IoSchedulerInfo(device.name, path, parsed.first, parsed.second)
        }.sortedBy { it.deviceName }
    }

    private fun readMemory(): MemoryInfo {
        val contents = read("/proc/meminfo").orEmpty()
        fun memValue(name: String): Long? {
            val line = contents.lineSequence().firstOrNull { it.startsWith(name + ":") }
            return line?.substringAfter(':')?.trim()?.substringBefore(' ')?.toLongOrNull()
        }

        val mmStat = read("/sys/block/zram0/mm_stat")
            .orEmpty().split(Regex("\\s+")).mapNotNull { it.toLongOrNull() }
        return MemoryInfo(
            totalKb = memValue("MemTotal"),
            availableKb = memValue("MemAvailable") ?: memValue("MemFree"),
            zramDiskBytes = KernelParsers.longValue(read("/sys/block/zram0/disksize")),
            zramOriginalBytes = mmStat.getOrNull(0),
            zramCompressedBytes = mmStat.getOrNull(1),
            zramMemoryBytes = mmStat.getOrNull(2),
            swappiness = read("/proc/sys/vm/swappiness")?.toIntOrNull(),
            vfsCachePressure = read("/proc/sys/vm/vfs_cache_pressure")?.toIntOrNull()
        )
    }

    private fun readKernelExtras(): KernelExtras {
        val zramAlgorithmText = read("/sys/block/zram0/comp_algorithm").orEmpty()
        val activeAlgorithm = Regex("\\[([^]]+)]").find(zramAlgorithmText)?.groupValues?.getOrNull(1)
        val availableAlgorithms = zramAlgorithmText
            .replace(Regex("\\[[^]]+]"), " ")
            .split(Regex("\\s+"))
            .filter { it.matches(Regex("[A-Za-z0-9_-]{1,32}")) }
            .distinct()

        val ksmRun = read("/sys/kernel/mm/ksm/run")?.toIntOrNull()
        val ksmPresent = ksmRun != null
        return KernelExtras(
            zramCompressionAlgorithm = activeAlgorithm,
            availableZramCompressionAlgorithms = availableAlgorithms,
            ksmSupported = ksmPresent,
            ksmEnabled = ksmRun?.let { it == 1 },
            ksmPagesShared = read("/sys/kernel/mm/ksm/pages_shared")?.toLongOrNull(),
            ksmPagesSharing = read("/sys/kernel/mm/ksm/pages_sharing")?.toLongOrNull(),
            entropyAvailable = read("/proc/sys/kernel/random/entropy_avail")?.toIntOrNull(),
            entropyReadWakeupThreshold = read("/proc/sys/kernel/random/read_wakeup_threshold")?.toIntOrNull(),
            entropyWriteWakeupThreshold = read("/proc/sys/kernel/random/write_wakeup_threshold")?.toIntOrNull()
        )
    }

    private fun readBattery(context: Context): BatteryInfo {
        val intent = try {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        } catch (_: Exception) {
            null
        } ?: return BatteryInfo(null, null, null, null, null)

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        return BatteryInfo(
            levelPercent = if (level >= 0 && scale > 0) (level * 100 / scale).coerceIn(0, 100) else null,
            temperatureCelsius = if (temperature >= 0) temperature / 10.0 else null,
            voltageMillivolts = voltage.takeIf { it > 0 },
            charging = charging,
            technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)
        )
    }

    private fun readStorage(): StorageInfo {
        return try {
            val stats = StatFs(Environment.getDataDirectory().absolutePath)
            StorageInfo(stats.totalBytes, stats.availableBytes)
        } catch (_: Exception) {
            StorageInfo(null, null)
        }
    }

    private fun readIoUsage(): IoUsage {
        val lines = read("/proc/diskstats").orEmpty().lines()
        var sectorsRead = 0L
        var sectorsWritten = 0L
        var hasData = false
        lines.forEach { line ->
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size >= 10) {
                val device = parts[2]
                val isPartition = File("/sys/class/block/$device/partition").exists()
                if (KernelParsers.isSupportedIoDevice(device) && !isPartition) {
                    sectorsRead += parts[5].toLongOrNull() ?: 0L
                    sectorsWritten += parts[9].toLongOrNull() ?: 0L
                    hasData = true
                }
            }
        }
        return IoUsage(
            sectorsRead = if (hasData) sectorsRead else null,
            sectorsWritten = if (hasData) sectorsWritten else null
        )
    }
}
