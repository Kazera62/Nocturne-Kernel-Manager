package id.kazera.nocturne.kernelmanager.domain.model

import id.kazera.nocturne.kernelmanager.core.privilege.RootAccess

data class CpuPolicyInfo(
    val path: String,
    val name: String,
    val currentFrequencyKHz: Long?,
    val minFrequencyKHz: Long?,
    val maxFrequencyKHz: Long?,
    val availableFrequenciesKHz: List<Long>,
    val governor: String?,
    val availableGovernors: List<String>
)

data class GpuInfo(
    val currentFrequencyHz: Long?,
    val utilizationPercent: Int?,
    val availableFrequenciesHz: List<Long>,
    val source: String?
)

data class DevfreqInfo(
    val name: String,
    val path: String,
    val currentFrequencyHz: Long?,
    val governor: String?,
    val availableFrequenciesHz: List<Long>
)

data class ThermalSensor(
    val name: String,
    val temperatureCelsius: Double
)

data class IoSchedulerInfo(
    val deviceName: String,
    val path: String,
    val activeScheduler: String?,
    val availableSchedulers: List<String>
)

data class MemoryInfo(
    val totalKb: Long?,
    val availableKb: Long?,
    val zramDiskBytes: Long?,
    val zramOriginalBytes: Long?,
    val zramCompressedBytes: Long?,
    val zramMemoryBytes: Long?,
    val swappiness: Int?,
    val vfsCachePressure: Int?
)

data class BatteryInfo(
    val levelPercent: Int?,
    val temperatureCelsius: Double?,
    val voltageMillivolts: Int?,
    val charging: Boolean?,
    val technology: String?
)

data class StorageInfo(
    val totalBytes: Long,
    val availableBytes: Long
)

data class IoUsage(
    val sectorsRead: Long?,
    val sectorsWritten: Long?
)

data class DeviceSnapshot(
    val model: String,
    val deviceCode: String,
    val platform: String,
    val kernelVersion: String,
    val uptimeSeconds: Long?,
    val root: RootAccess,
    val cpuPolicies: List<CpuPolicyInfo>,
    val gpu: GpuInfo,
    val devfreq: List<DevfreqInfo>,
    val ioSchedulers: List<IoSchedulerInfo>,
    val thermalSensors: List<ThermalSensor>,
    val memory: MemoryInfo,
    val battery: BatteryInfo,
    val storage: StorageInfo,
    val networkRxBytes: Long?,
    val networkTxBytes: Long?,
    val ioUsage: IoUsage,
    val tcpCongestionControl: String?,
    val availableTcpCongestionControls: List<String>,
    val warnings: List<String>
)
