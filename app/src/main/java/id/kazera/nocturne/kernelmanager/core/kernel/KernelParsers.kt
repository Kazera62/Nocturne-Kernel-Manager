package id.kazera.nocturne.kernelmanager.core.kernel

import java.util.Locale

object KernelParsers {

    fun longValue(text: String?): Long? =
        text?.trim()?.toLongOrNull()?.takeIf { it >= 0L }

    fun frequencyList(text: String?): List<Long> =
        text.orEmpty()
            .trim()
            .split(Regex("\\s+"))
            .mapNotNull { it.toLongOrNull() }
            .filter { it > 0L }
            .distinct()
            .sorted()

    fun cpuList(text: String?): List<Int> {
        val result = sortedSetOf<Int>()
        text.orEmpty().trim().split(",").forEach { token ->
            val part = token.trim()
            if (part.isBlank()) return@forEach
            val range = part.split("-", limit = 2)
            val first = range.firstOrNull()?.toIntOrNull() ?: return@forEach
            val last = range.getOrNull(1)?.toIntOrNull() ?: first
            if (first < 0 || last < first || last > 255) return@forEach
            for (cpu in first..last) result += cpu
        }
        return result.toList()
    }

    fun gpuUtilization(text: String?): Int? {
        val values = text.orEmpty().trim().split(Regex("\\s+"))
            .mapNotNull { it.toLongOrNull() }
        if (values.size < 2 || values[1] <= 0L) return null
        return ((values[0].toDouble() / values[1].toDouble()) * 100.0)
            .toInt()
            .coerceIn(0, 100)
    }

    fun thermalCelsius(text: String?): Double? {
        val value = longValue(text) ?: return null
        val temperature = if (value > 1_000L) value / 1_000.0 else value.toDouble()
        return temperature.takeIf { it in -20.0..150.0 }
    }

    /**
     * Restrict scheduler controls to whole physical storage devices.
     * Partition nodes and virtual block devices are intentionally excluded.
     */
    fun isSupportedCpuHotplugCore(coreId: Int): Boolean = coreId in 1..7

    fun isSupportedIoDevice(name: String): Boolean =
        Regex("^mmcblk[0-9]+$").matches(name) ||
            Regex("^sd[a-c]$").matches(name) ||
            Regex("^nvme[0-9]+n[0-9]+$").matches(name)

    fun schedulerList(text: String?): Pair<String?, List<String>> {
        val tokens = text.orEmpty().trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val active = tokens.firstOrNull { it.startsWith("[") && it.endsWith("]") }
            ?.removePrefix("[")?.removeSuffix("]")
        val available = tokens.map { it.removePrefix("[").removeSuffix("]") }
            .filter { it.matches(Regex("[A-Za-z0-9_.-]{1,40}")) }
            .distinct()
        return active to available
    }

    fun formatFrequency(value: Long?): String {
        if (value == null) return "Unsupported"
        val mhz = when {
            value >= 1_000_000L -> value / 1_000.0
            value >= 10_000L -> value / 1_000.0
            else -> value.toDouble()
        }
        return String.format(Locale.US, "%.0f MHz", mhz)
    }

    fun formatFrequencyHz(value: Long?): String {
        if (value == null) return "Unsupported"
        return if (value >= 1_000_000L) {
            String.format(Locale.US, "%.0f MHz", value / 1_000_000.0)
        } else {
            String.format(Locale.US, "%.0f Hz", value.toDouble())
        }
    }

    fun formatBytes(bytes: Long?): String {
        if (bytes == null || bytes < 0L) return "Unsupported"
        val units = listOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var unit = 0
        while (value >= 1024.0 && unit < units.lastIndex) {
            value /= 1024.0
            unit++
        }
        return if (unit == 0) bytes.toString() + " " + units[unit]
        else String.format(Locale.US, "%.1f %s", value, units[unit])
    }

    fun formatUptime(seconds: Long?): String {
        if (seconds == null) return "Unsupported"
        val days = seconds / 86_400
        val hours = (seconds % 86_400) / 3_600
        val minutes = (seconds % 3_600) / 60
        return if (days > 0) days.toString() + "d " + hours + "h " + minutes + "m"
        else hours.toString() + "h " + minutes + "m"
    }
}
