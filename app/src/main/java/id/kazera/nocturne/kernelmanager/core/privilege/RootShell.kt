package id.kazera.nocturne.kernelmanager.core.privilege

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

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
 * A cached interactive SU session prevents a new su process from being started
 * for every sysfs/procfs read. Calls are serialized and bounded by a timeout.
 *
 * All writable interfaces are exposed through narrow allow-listed methods.
 * The UI never receives an arbitrary shell command field.
 */
class RootShell : AutoCloseable {

    private val lock = Any()
    private val commandCounter = AtomicLong(0L)

    private var process: Process? = null
    private var input: BufferedReader? = null
    private var output: BufferedWriter? = null

    fun detect(): RootAccess {
        val result = execute("id", timeoutMs = 3_000)
        val granted = result.success &&
            Regex("(^|\\s)uid=0(?:\\(|\\s|$)").containsMatchIn(result.output)

        return if (granted) {
            RootAccess(
                available = true,
                provider = "SU-compatible root",
                detail = "Root session is ready and cached."
            )
        } else {
            RootAccess(
                available = false,
                provider = "No root access",
                detail = result.error ?: result.output.ifBlank {
                    "SU authorization was denied or timed out."
                }
            )
        }
    }

    fun read(path: String): String? {
        if (!isAllowedReadPath(path)) return null
        val result = execute("cat " + quote(path), timeoutMs = 2_000)
        return result.output.takeIf { result.success }?.trim()
    }

    fun writeCpuNode(path: String, value: String): ShellResult {
        val allowedPath = Regex(
            "^/sys/devices/system/cpu/(cpufreq/policy[0-9]+|cpu[0-7]/cpufreq)/scaling_(min_freq|max_freq|governor)$"
        ).matches(path)
        if (!allowedPath || !Regex("^[A-Za-z0-9_.-]{1,40}$").matches(value)) {
            return rejected("Rejected invalid CPU control target or value.")
        }
        return writeNode(path, value)
    }

    fun writeIoScheduler(path: String, value: String): ShellResult {
        val allowedPath = Regex(
            "^/sys/block/[A-Za-z0-9_.-]+/queue/scheduler$"
        ).matches(path)
        if (!allowedPath || !Regex("^[A-Za-z0-9_.-]{1,40}$").matches(value)) {
            return rejected("Rejected invalid I/O scheduler target or value.")
        }
        return writeNode(path, value)
    }

    fun writeVmSetting(setting: String, value: Int): ShellResult {
        val target = when (setting) {
            "swappiness" -> if (value in 0..100) "/proc/sys/vm/swappiness" else null
            "vfs_cache_pressure" -> if (value in 10..200) "/proc/sys/vm/vfs_cache_pressure" else null
            else -> null
        } ?: return rejected("Rejected unsupported VM setting or value.")
        return writeNode(target, value.toString())
    }

    fun writeTcpCongestionControl(value: String): ShellResult {
        if (!Regex("^[A-Za-z0-9_-]{1,32}$").matches(value)) {
            return rejected("Rejected invalid TCP congestion control value.")
        }
        return writeNode("/proc/sys/net/ipv4/tcp_congestion_control", value)
    }

    fun kernelLogs(): ShellResult =
        execute("dmesg | tail -n 160", timeoutMs = 5_000)

    private fun writeNode(path: String, value: String): ShellResult {
        // Both target and value are validated by the public write adapters.
        return execute("printf '%s\\n' '$value' > '$path'", timeoutMs = 3_000)
    }

    fun execute(command: String, timeoutMs: Long = 4_000): ShellResult {
        if (command.isBlank() || command.contains('\u0000')) {
            return rejected("Empty or invalid shell command.")
        }

        synchronized(lock) {
            val session = ensureSession()
                ?: return ShellResult(false, "", -1, "Unable to start SU session.")

            val marker = "__NOCTURNE_" + commandCounter.incrementAndGet() + "__"
            return try {
                session.second.write(command)
                session.second.newLine()
                session.second.write(
                    "__nocturne_status=$?; printf '\\n${marker}:%s\\n' \"\$__nocturne_status\""
                )
                session.second.newLine()
                session.second.flush()

                var result: ShellResult? = null
                var readFailure: String? = null
                val readerThread = Thread {
                    try {
                        val captured = StringBuilder()
                        while (true) {
                            val line = session.first.readLine()
                                ?: throw IOException("SU session closed unexpectedly.")
                            if (line.startsWith(marker + ":")) {
                                val code = line.substringAfter(':').trim().toIntOrNull() ?: 1
                                result = ShellResult(
                                    success = code == 0,
                                    output = captured.toString().trim(),
                                    exitCode = code,
                                    error = if (code == 0) null else captured.toString().trim()
                                        .ifBlank { "Root command failed ($code)." }
                                )
                                break
                            }
                            captured.append(line).append('\n')
                        }
                    } catch (error: Exception) {
                        readFailure = error.message ?: "Unable to read SU output."
                    }
                }.apply {
                    name = "Nocturne-SU-reader"
                    isDaemon = true
                    start()
                }

                readerThread.join(timeoutMs)
                if (readerThread.isAlive) {
                    clearSession(destroy = true)
                    return ShellResult(false, "", -1, "Root command timed out.")
                }

                result ?: run {
                    val error = readFailure ?: "No result returned by SU."
                    clearSession(destroy = true)
                    ShellResult(false, "", -1, error)
                }
            } catch (error: Exception) {
                clearSession(destroy = true)
                ShellResult(false, "", -1, error.message ?: "SU command failed.")
            }
        }
    }

    private fun ensureSession(): Pair<BufferedReader, BufferedWriter>? {
        val active = process
        val reader = input
        val writer = output
        if (active != null && active.isAlive && reader != null && writer != null) {
            return reader to writer
        }

        clearSession(destroy = true)
        return try {
            val started = ProcessBuilder("su")
                .redirectErrorStream(true)
                .start()
            process = started
            input = BufferedReader(InputStreamReader(started.inputStream))
            output = BufferedWriter(OutputStreamWriter(started.outputStream))
            Pair(input!!, output!!)
        } catch (_: Exception) {
            clearSession(destroy = true)
            null
        }
    }

    private fun clearSession(destroy: Boolean) {
        val oldProcess = process
        val oldInput = input
        val oldOutput = output

        // Detach the active references first so no new command can reuse this session.
        process = null
        input = null
        output = null

        // Kill first: closing a reader while another thread is blocked in readLine()
        // can itself block. Process termination unblocks that reader.
        if (destroy) {
            try {
                oldProcess?.destroyForcibly()
                oldProcess?.waitFor(250, TimeUnit.MILLISECONDS)
            } catch (_: Exception) {
                // The process may already have exited.
            }
        }

        try {
            oldOutput?.close()
        } catch (_: Exception) {
            // Session is already closed.
        }
        try {
            oldInput?.close()
        } catch (_: Exception) {
            // Session is already closed.
        }
    }

    fun invalidate() {
        synchronized(lock) {
            clearSession(destroy = true)
        }
    }

    override fun close() {
        synchronized(lock) {
            clearSession(destroy = true)
        }
    }

    private fun isAllowedReadPath(path: String): Boolean {
        if (path.contains("..") || path.contains('\u0000')) return false
        return path.startsWith("/sys/") ||
            path.startsWith("/proc/") ||
            path.startsWith("/vendor/")
    }

    private fun quote(value: String): String =
        "'" + value.replace("'", "'\\''") + "'"

    private fun rejected(message: String): ShellResult =
        ShellResult(false, "", -1, message)
}
