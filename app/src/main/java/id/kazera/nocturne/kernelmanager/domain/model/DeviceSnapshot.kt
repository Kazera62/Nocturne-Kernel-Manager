package id.kazera.nocturne.kernelmanager.domain.model

import id.kazera.nocturne.kernelmanager.core.privilege.RootAccess

data class CpuCoreInfo(
    val coreId: Int,
    val online: Boolean?
)

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
    val source: String?,
    val maximumPowerLevel: Int? = null,
    val minimumPowerLevel: Int? = null,
    val numberOfPowerLevels: Int? = null,
    val maximumClockHz: Long? = null,
    val availableFrequenciesMHz: List<Int> = emptyList(),
    val minimumClockMHz: Int? = null,
    val maximumClockMHz: Int? = null
)

data class DevfreqInfo(
    val name: String,
    val path: String,
    val currentFrequencyHz: Long?,
    val governor: String?,
    val availableFrequenciesHz: List<Long>,
    val minimumFrequencyHz: Long? = null,
    val maximumFrequencyHz: Long? = null
)

data class RegulatorTelemetry(
    val name: String,
    val voltageMicrovolts: Long?,
    val minimumMicrovolts: Long?,
    val maximumMicrovolts: Long?,
    val state: String?
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

data class KernelExtras(
    val zramCompressionAlgorithm: String?,
    val availableZramCompressionAlgorithms: List<String>,
    val ksmSupported: Boolean,
    val ksmEnabled: Boolean?,
    val ksmPagesShared: Long?,
    val ksmPagesSharing: Long?,
    val entropyAvailable: Int?,
    val entropyReadWakeupThreshold: Int?,
    val entropyWriteWakeupThreshold: Int?
)

data class BatteryInfo(
    val levelPercent: Int?,
    val temperatureCelsius: Double?,
    val voltageMillivolts: Int?,
    val charging: Boolean?,
    val technology: String?
)

data class StorageInfo(
    val totalBytes: Long?,
    val availableBytes: Long?
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
    val cpuCores: List<CpuCoreInfo>,
    val cpuPolicies: List<CpuPolicyInfo>,
    val gpu: GpuInfo,
    val regulators: List<RegulatorTelemetry> = emptyList(),
    val devfreq: List<DevfreqInfo>,
    val ioSchedulers: List<IoSchedulerInfo>,
    val thermalSensors: List<ThermalSensor>,
    val memory: MemoryInfo,
    val kernelExtras: KernelExtras,
    val battery: BatteryInfo,
    val storage: StorageInfo,
    val networkRxBytes: Long?,
    val networkTxBytes: Long?,
    val ioUsage: IoUsage,
    val tcpCongestionControl: String?,
    val availableTcpCongestionControls: List<String>,
    val warnings: List<String>
)
