package id.kazera.nocturne.kernelmanager.data

import id.kazera.nocturne.kernelmanager.core.kernel.KernelParsers
import id.kazera.nocturne.kernelmanager.domain.model.BatteryInfo
import id.kazera.nocturne.kernelmanager.domain.model.DeviceSnapshot

/**
 * Formatting helpers for the presentation layer.
 * Values are always sourced from the live snapshot or marked unsupported.
 */
object DeviceInfo {

    fun memoryUsedPercent(snapshot: DeviceSnapshot): Int? {
        val total = snapshot.memory.totalKb ?: return null
        val available = snapshot.memory.availableKb ?: return null
        if (total <= 0L) return null
        return (((total - available).coerceAtLeast(0L) * 100L) / total)
            .toInt().coerceIn(0, 100)
    }

    fun batteryLabel(battery: BatteryInfo): String {
        val level = battery.levelPercent?.let { "$it%" } ?: "Unsupported"
        val state = when (battery.charging) {
            true -> "Charging"
            false -> "Not charging"
            null -> "Unknown"
        }
        return "$level · $state"
    }

    fun frequency(value: Long?): String = KernelParsers.formatFrequency(value)

    fun frequencyHz(value: Long?): String = KernelParsers.formatFrequencyHz(value)

    fun bytes(value: Long?): String = KernelParsers.formatBytes(value)

    fun uptime(value: Long?): String = KernelParsers.formatUptime(value)
}
