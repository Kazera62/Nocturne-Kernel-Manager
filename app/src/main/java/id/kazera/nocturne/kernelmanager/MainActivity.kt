package id.kazera.nocturne.kernelmanager

import android.app.Activity
import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ScrollView
import android.widget.Toast
import id.kazera.nocturne.kernelmanager.data.KernelRepository
import id.kazera.nocturne.kernelmanager.domain.model.CpuPolicyInfo
import id.kazera.nocturne.kernelmanager.domain.model.IoSchedulerInfo
import id.kazera.nocturne.kernelmanager.ui.home.HomeScreen
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : Activity() {

    private val repository = KernelRepository()
    private val worker = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val refreshInFlight = AtomicBoolean(false)
    private lateinit var screen: HomeScreen

    private val refreshLoop = object : Runnable {
        override fun run() {
            refresh()
            mainHandler.postDelayed(this, REFRESH_INTERVAL_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = 0xFF080B10.toInt()
        window.navigationBarColor = 0xFF080B10.toInt()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }

        screen = HomeScreen(
            context = this,
            onRefresh = { refresh(forceRender = true) },
            onCpuApply = { policy, governor, minimum, maximum ->
                applyCpu(policy, governor, minimum, maximum)
            },
            onIoApply = { scheduler, selected -> applyIoScheduler(scheduler, selected) },
            onShowLogs = { showKernelLogs() }
        )
        setContentView(screen)
    }

    override fun onResume() {
        super.onResume()
        mainHandler.removeCallbacks(refreshLoop)
        refreshLoop.run()
    }

    override fun onPause() {
        mainHandler.removeCallbacks(refreshLoop)
        super.onPause()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        worker.shutdownNow()
        super.onDestroy()
    }

    private fun refresh(forceRender: Boolean = false) {
        if (!refreshInFlight.compareAndSet(false, true)) return
        worker.execute {
            val result = try {
                repository.snapshot(applicationContext)
            } catch (_: Exception) {
                null
            }
            runOnUiThread {
                refreshInFlight.set(false)
                if (result != null && !isFinishing) screen.update(result, forceRender)
            }
        }
    }

    private fun applyCpu(
        policy: CpuPolicyInfo,
        governor: String?,
        minimum: Long,
        maximum: Long
    ) {
        worker.execute {
            val result = repository.applyCpuPolicy(policy, governor, minimum, maximum)
            runOnUiThread {
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                refresh(forceRender = true)
            }
        }
    }

    private fun applyIoScheduler(scheduler: IoSchedulerInfo, selected: String) {
        worker.execute {
            val result = repository.applyIoScheduler(scheduler, selected)
            runOnUiThread {
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                refresh()
            }
        }
    }

    private fun showKernelLogs() {
        worker.execute {
            val result = repository.kernelLogs()
            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                val body = android.widget.TextView(this).apply {
                    text = if (result.success) result.output.ifBlank { "Kernel log is empty." }
                    else result.error ?: "Unable to read kernel log."
                    textSize = 11f
                    setTextColor(0xFFE6EDF3.toInt())
                    setPadding(28, 24, 28, 24)
                    setTextIsSelectable(true)
                }
                val scroll = ScrollView(this).apply { addView(body) }
                AlertDialog.Builder(this)
                    .setTitle("Latest kernel log")
                    .setView(scroll)
                    .setPositiveButton("Close", null)
                    .show()
            }
        }
    }

    companion object {
        private const val REFRESH_INTERVAL_MS = 8_000L
    }
}
