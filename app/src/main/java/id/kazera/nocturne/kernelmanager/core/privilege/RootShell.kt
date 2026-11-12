package id.kazera.nocturne.kernelmanager.core.privilege

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

data class ShellResult(
    val success: Boolean,
    val output: String,
    val exitCode: Int,
    val error: String? = null
)

data class RootAccess(
    val available: Boolean,
    val provider: String,
    val detail: String
)

/**
 * Minimal SU bridge shared by Magisk, KernelSU-family managers and APatch.
 * The app never accepts arbitrary shell input from the UI.
 */
class RootShell {

    fun detect(): RootAccess {
        val result = execute("id", timeoutMs = 4_000)
        if (!result.success || !result.output.contains("uid=0")) {
            return RootAccess(
                available = false,
                provider = "No root access",
                detail = result.error ?: result.output.ifBlank { "SU authorization unavailable" }
            )
        }

        val version = execute("su -v", timeoutMs = 2_000).output
            .ifBlank { execute("su -V", timeoutMs = 2_000).output }
            .trim()
        val provider = when {
            version.contains("magisk", ignoreCase = true) -> "Magisk"
            version.contains("apatch", ignoreCase = true) -> "APatch"
            version.contains("kernelsu", ignoreCase = true) -> "KernelSU-compatible"
            version.isNotBlank() -> version.take(42)
            else -> "SU-compatible root provider"
        }

        return RootAccess(
            available = true,
            provider = provider,
            detail = "Root shell verified (uid=0)"
        )
    }

    fun read(path: String): String? {
        if (!isAllowedReadPath(path)) return null
        val result = execute("cat " + quote(path), timeoutMs = 2_500)
        return result.output.takeIf { result.success }?.trim()
    }

    fun writeCpuNode(path: String, value: String): ShellResult {
        val allowedPath = Regex(
            "^/sys/devices/system/cpu/cpufreq/policy[0-9]+/scaling_(min_freq|max_freq|governor)$"
        ).matches(path)
        if (!allowedPath || !Regex("^[A-Za-z0-9_.-]{1,40}$").matches(value)) {
            return ShellResult(false, "", -1, "Rejected invalid CPU control target or value")
        }
        return writeNode(path, value)
    }

    fun writeIoScheduler(path: String, value: String): ShellResult {
        val allowedPath = Regex(
            "^/sys/block/[A-Za-z0-9_.-]+/queue/scheduler$"
        ).matches(path)
        if (!allowedPath || !Regex("^[A-Za-z0-9_.-]{1,40}$").matches(value)) {
            return ShellResult(false, "", -1, "Rejected invalid I/O scheduler target or value")
        }
        return writeNode(path, value)
    }

    fun kernelLogs(): ShellResult {
        return execute("dmesg | tail -n 160", timeoutMs = 5_000)
    }

    fun writeVmSetting(setting: String, value: Int): ShellResult {
        val target = when (setting) {
            "swappiness" -> if (value in 0..100) "/proc/sys/vm/swappiness" else null
            "vfs_cache_pressure" -> if (value in 10..200) "/proc/sys/vm/vfs_cache_pressure" else null
            else -> null
        } ?: return ShellResult(false, "", -1, "Rejected unsupported VM setting or value")
        return writeNode(target, value.toString())
    }

    fun writeTcpCongestionControl(value: String): ShellResult {
        if (!Regex("^[A-Za-z0-9_-]{1,32}$").matches(value)) {
            return ShellResult(false, "", -1, "Rejected invalid TCP congestion control value")
        }
        return writeNode("/proc/sys/net/ipv4/tcp_congestion_control", value)
    }

    private fun writeNode(path: String, value: String): ShellResult {
        // Both path and value are restricted by strict allow-lists above.
        return execute("printf '%s\\n' '$value' > '$path'", timeoutMs = 3_000)
    }

    fun execute(command: String, timeoutMs: Long = 4_000): ShellResult {
        val process = try {
            ProcessBuilder("su", "-c", command)
                .redirectErrorStream(true)
                .start()
        } catch (error: Exception) {
            return ShellResult(false, "", -1, error.message ?: "Unable to start su")
        }

        var output = ""
        val reader = Thread {
            output = try {
                BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            } catch (_: Exception) {
                ""
            }
        }.apply {
            name = "Nocturne-SU-output"
            isDaemon = true
            start()
        }

        val finished = try {
            process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
        } catch (error: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }

        if (!finished) {
            process.destroyForcibly()
            reader.join(300)
            return ShellResult(false, output.trim(), -1, "Root command timed out")
        }

        reader.join(1_000)
        val code = process.exitValue()
        return ShellResult(
            success = code == 0,
            output = output.trim(),
            exitCode = code,
            error = if (code == 0) null else output.trim().ifBlank { "Command failed ($code)" }
        )
    }

    private fun isAllowedReadPath(path: String): Boolean {
        if (path.contains("..") || path.contains('\u0000')) return false
        return path.startsWith("/sys/") || path.startsWith("/proc/") ||
            path.startsWith("/vendor/")
    }

    private fun quote(value: String): String =
        "'" + value.replace("'", "'\\''") + "'"
}
