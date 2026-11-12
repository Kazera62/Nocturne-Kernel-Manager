package id.kazera.nocturne.kernelmanager

import android.app.Activity
import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowInsets
import android.widget.ScrollView
import android.widget.Toast
import id.kazera.nocturne.kernelmanager.data.KernelRepository
import id.kazera.nocturne.kernelmanager.domain.model.CpuPolicyInfo
import id.kazera.nocturne.kernelmanager.domain.model.IoSchedulerInfo
import id.kazera.nocturne.kernelmanager.ui.home.HomeScreen
import id.kazera.nocturne.kernelmanager.ui.settings.UiPreferences
import id.kazera.nocturne.kernelmanager.ui.settings.UiText
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : Activity() {

    private val repository = KernelRepository()
    // All sysfs/procfs reads and SU commands are serialized on this dedicated I/O thread.
    private val ioExecutor = Executors.newSingleThreadExecutor { task ->
        Thread(task, "Nocturne-IO").apply {
            priority = Thread.NORM_PRIORITY - 1
            isDaemon = true
        }
    }
    private val mainHandler = Handler(Looper.getMainLooper())
    private val refreshInFlight = AtomicBoolean(false)
    private val rootRecheckInFlight = AtomicBoolean(false)
    private lateinit var screen: HomeScreen

    @Volatile
    private var rootInitialized = false

    @Volatile
    private var activityResumed = false

    @Volatile
    private var refreshIntervalMs = 8_000L

    private val refreshLoop = object : Runnable {
        override fun run() {
            if (!activityResumed) return
            refresh()
            mainHandler.postDelayed(this, refreshIntervalMs)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        refreshIntervalMs = UiPreferences.refreshIntervalSeconds(this) * 1_000L

        window.statusBarColor = 0xFF080B10.toInt()
        window.navigationBarColor = 0xFF080B10.toInt()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }

        screen = HomeScreen(
            context = this,
            onRefresh = { recheckRootAndRefresh() },
            onCpuApply = { policy, governor, minimum, maximum ->
                applyCpu(policy, governor, minimum, maximum)
            },
            onIoApply = { scheduler, selected -> applyIoScheduler(scheduler, selected) },
            onVmApply = { swappiness, cachePressure -> applyVmSettings(swappiness, cachePressure) },
            onTcpApply = { selected -> applyTcpCongestionControl(selected) },
            onShowLogs = { showKernelLogs() },
            onRootRefresh = { recheckRootAndRefresh() },
            onLanguageChanged = { language ->
                UiPreferences.setLanguage(this, language)
            },
            onRefreshIntervalChanged = { seconds ->
                UiPreferences.setRefreshIntervalSeconds(this, seconds)
                refreshIntervalMs = seconds * 1_000L
                if (activityResumed && rootInitialized) startRefreshLoop()
            }
        )
        setContentView(screen)
        installWindowInsets(screen)

        // Root preflight runs first and updates the UI before the broader node scan.
        // KernelRepository reuses this cached root session for subsequent sysfs reads.
        ioExecutor.execute {
            val access = try {
                repository.checkRoot()
            } catch (error: Exception) {
                id.kazera.nocturne.kernelmanager.core.privilege.RootAccess(
                    available = false,
                    provider = "No root access",
                    detail = error.message ?: "Root preflight failed."
                )
            }
            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                rootInitialized = true
                screen.updateRootStatus(access)
                if (activityResumed) startRefreshLoop()
            }
        }
    }

    private fun installWindowInsets(view: HomeScreen) {
        view.setOnApplyWindowInsetsListener { target, insets ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bars = insets.getInsets(
                    WindowInsets.Type.statusBars() or
                        WindowInsets.Type.navigationBars() or
                        WindowInsets.Type.displayCutout()
                )
                target.setPadding(
                    target.paddingLeft,
                    bars.top,
                    target.paddingRight,
                    bars.bottom
                )
            } else {
                @Suppress("DEPRECATION")
                val topInset = insets.systemWindowInsetTop
                @Suppress("DEPRECATION")
                val bottomInset = insets.systemWindowInsetBottom
                target.setPadding(
                    target.paddingLeft,
                    topInset,
                    target.paddingRight,
                    bottomInset
                )
            }
            insets
        }
        view.requestApplyInsets()
    }

    override fun onResume() {
        super.onResume()
        activityResumed = true
        if (rootInitialized) startRefreshLoop()
    }

    override fun onPause() {
        activityResumed = false
        mainHandler.removeCallbacks(refreshLoop)
        super.onPause()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        ioExecutor.shutdownNow()
        repository.closeRootSession()
        super.onDestroy()
    }

    private fun startRefreshLoop() {
        mainHandler.removeCallbacks(refreshLoop)
        refreshLoop.run()
    }

    private fun recheckRootAndRefresh() {
        if (!rootInitialized || !rootRecheckInFlight.compareAndSet(false, true)) return
        ioExecutor.execute {
            repository.invalidateRootCache()
            val access = try {
                repository.checkRoot()
            } catch (error: Exception) {
                id.kazera.nocturne.kernelmanager.core.privilege.RootAccess(
                    available = false,
                    provider = "No root access",
                    detail = error.message ?: "Root check failed."
                )
            }
            runOnUiThread {
                rootRecheckInFlight.set(false)
                if (isFinishing) return@runOnUiThread
                screen.updateRootStatus(access)
                refresh(forceRender = true)
            }
        }
    }

    private fun refresh(forceRender: Boolean = false) {
        if (!refreshInFlight.compareAndSet(false, true)) return
        ioExecutor.execute {
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
        ioExecutor.execute {
            val result = repository.applyCpuPolicy(policy, governor, minimum, maximum)
            runOnUiThread {
                Toast.makeText(this, UiText.translate(this, result.message), Toast.LENGTH_LONG).show()
                refresh(forceRender = true)
            }
        }
    }

    private fun applyIoScheduler(scheduler: IoSchedulerInfo, selected: String) {
        ioExecutor.execute {
            val result = repository.applyIoScheduler(scheduler, selected)
            runOnUiThread {
                Toast.makeText(this, UiText.translate(this, result.message), Toast.LENGTH_LONG).show()
                refresh(forceRender = true)
            }
        }
    }

    private fun applyVmSettings(swappiness: Int, cachePressure: Int) {
        ioExecutor.execute {
            val result = repository.applyVmSettings(swappiness, cachePressure)
            runOnUiThread {
                Toast.makeText(this, UiText.translate(this, result.message), Toast.LENGTH_LONG).show()
                refresh(forceRender = true)
            }
        }
    }

    private fun applyTcpCongestionControl(selected: String) {
        ioExecutor.execute {
            val result = repository.applyTcpCongestionControl(selected)
            runOnUiThread {
                Toast.makeText(this, UiText.translate(this, result.message), Toast.LENGTH_LONG).show()
                refresh(forceRender = true)
            }
        }
    }

    private fun showKernelLogs() {
        ioExecutor.execute {
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
                    .setTitle(UiText.translate(this, "Latest kernel log"))
                    .setView(scroll)
                    .setPositiveButton(UiText.translate(this, "Close"), null)
                    .show()
            }
        }
    }
}
