package id.kazera.nocturne.kernelmanager.ui.home

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.content.res.ColorStateList
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import id.kazera.nocturne.kernelmanager.core.kernel.KernelParsers
import id.kazera.nocturne.kernelmanager.data.DeviceInfo
import id.kazera.nocturne.kernelmanager.domain.model.CpuPolicyInfo
import id.kazera.nocturne.kernelmanager.domain.model.DeviceSnapshot
import id.kazera.nocturne.kernelmanager.domain.model.IoSchedulerInfo
import id.kazera.nocturne.kernelmanager.core.privilege.RootAccess
import id.kazera.nocturne.kernelmanager.ui.settings.UiPreferences
import id.kazera.nocturne.kernelmanager.ui.settings.UiText
import java.util.Locale

class HomeScreen(
    context: Context,
    private val onRefresh: () -> Unit,
    private val onCpuApply: (CpuPolicyInfo, String?, Long, Long) -> Unit,
    private val onIoApply: (IoSchedulerInfo, String) -> Unit,
    private val onVmApply: (Int, Int) -> Unit,
    private val onTcpApply: (String) -> Unit,
    private val onShowLogs: () -> Unit,
    private val onRootRefresh: () -> Unit,
    private val onLanguageChanged: (String) -> Unit,
    private val onRefreshIntervalChanged: (Int) -> Unit
) : LinearLayout(context) {

    private enum class Page(val label: String, val icon: String) {
        OVERVIEW("Overview", "⌂"),
        PERFORMANCE("Performance", "⚡"),
        MONITOR("Monitor", "◉"),
        SYSTEM("System", "▦"),
        SETTINGS("Settings", "⚙")
    }

    private var selectedPage = Page.OVERVIEW
    private var snapshot: DeviceSnapshot? = null
    private var rootStatus: RootAccess? = null
    private var currentScroll: ScrollView? = null

    private fun t(value: String): String = UiText.translate(context, value)

    fun updateRootStatus(value: RootAccess) {
        rootStatus = value
        render(restoreScroll = true)
    }

    init {
        orientation = VERTICAL
        setBackgroundColor(BACKGROUND)
        render()
    }

    fun update(value: DeviceSnapshot, forceRender: Boolean = false) {
        snapshot = value
        if (forceRender || selectedPage == Page.OVERVIEW || selectedPage == Page.MONITOR) {
            render(restoreScroll = true)
        }
    }

    private fun render(restoreScroll: Boolean = false) {
        val previousScroll = if (restoreScroll) currentScroll?.scrollY ?: 0 else 0
        removeAllViews()

        addView(buildHeader(), LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val scroll = ScrollView(context).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(dp(16), dp(12), dp(16), dp(24))
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }
        currentScroll = scroll

        val content = LinearLayout(context).apply {
            orientation = VERTICAL
        }
        scroll.addView(content, ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        addView(scroll, LayoutParams(MATCH_PARENT, 0, 1f))
        addView(buildNavigation(), LayoutParams(MATCH_PARENT, dp(66)))

        val data = snapshot
        if (data == null && selectedPage != Page.SETTINGS) {
            addCard(content, "Initializing device interface") {
                val root = rootStatus
                if (root == null) {
                    addText(it, "Checking root access before scanning kernel interfaces…", SECONDARY, 14f)
                } else {
                    addText(
                        it,
                        if (root.available) "Privileged shell verified" else "Root permission not available",
                        if (root.available) ACCENT else WARNING,
                        14f,
                        bold = true
                    )
                    addText(it, root.detail, MUTED, 12f, top = 6)
                    addText(it, "Reading device information and probing available kernel nodes…", SECONDARY, 12f, top = 10)
                }
            }
        } else {
            when (selectedPage) {
                Page.OVERVIEW -> data?.let { renderOverview(content, it) }
                Page.PERFORMANCE -> data?.let { renderPerformance(content, it) }
                Page.MONITOR -> data?.let { renderMonitor(content, it) }
                Page.SYSTEM -> data?.let { renderSystem(content, it) }
                Page.SETTINGS -> renderSettings(content, data)
            }
        }

        if (previousScroll > 0) scroll.post { scroll.scrollTo(0, previousScroll) }
    }

    private fun buildHeader(): View {
        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(12), dp(16), dp(10))
            setBackgroundColor(BACKGROUND)
        }

        val titleStack = LinearLayout(context).apply { orientation = VERTICAL }
        val title = addText(titleStack, "NOCTURNE", TEXT, 21f, bold = true)
        title.letterSpacing = 0.14f
        addText(titleStack, "KERNEL MANAGER  ·  M11 / A11", SECONDARY, 10f, top = 2)
        header.addView(titleStack, LayoutParams(0, WRAP_CONTENT, 1f))

        val currentRoot = rootStatus ?: snapshot?.root
        val rootLabel = when {
            currentRoot == null -> "ROOT CHECK"
            currentRoot.available -> "Root ready"
            else -> "Root off"
        }
        val rootColor = if (currentRoot?.available == true) ACCENT else WARNING
        header.addView(chip(rootLabel, rootColor), LayoutParams(WRAP_CONTENT, dp(30)))

        val refresh = iconButton("↻", "Refresh") { onRefresh() }
        header.addView(refresh, LayoutParams(dp(40), dp(40)).apply { leftMargin = dp(6) })

        val settings = iconButton("⚙", "Settings") {
            selectedPage = Page.SETTINGS
            render()
        }
        header.addView(settings, LayoutParams(dp(40), dp(40)).apply { leftMargin = dp(4) })
        return header
    }

    private fun buildNavigation(): View {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(6), dp(8), dp(6))
            setBackgroundColor(SURFACE)
        }

        Page.values().filter { it != Page.SETTINGS }.forEach { page ->
            val selected = page == selectedPage
            val item = LinearLayout(context).apply {
                orientation = VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(4), dp(3), dp(4), dp(3))
                background = rounded(
                    if (selected) SURFACE_ALT else SURFACE,
                    if (selected) ACCENT else SURFACE,
                    12f
                )
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    selectedPage = page
                    render()
                }
            }
            val icon = TextView(context).apply {
                text = page.icon
                textSize = 19f
                gravity = Gravity.CENTER
                setTextColor(if (selected) ACCENT else MUTED)
            }
            val label = TextView(context).apply {
                text = t(page.label)
                textSize = 10f
                gravity = Gravity.CENTER
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                setTextColor(if (selected) TEXT else SECONDARY)
                maxLines = 1
            }
            item.addView(icon, LayoutParams(MATCH_PARENT, dp(25)))
            item.addView(label, LayoutParams(MATCH_PARENT, dp(16)))
            row.addView(item, LayoutParams(0, MATCH_PARENT, 1f).apply {
                leftMargin = dp(3)
                rightMargin = dp(3)
            })
        }
        return row
    }

    private fun iconButton(icon: String, description: String, action: () -> Unit): TextView =
        TextView(context).apply {
            text = icon
            contentDescription = t(description)
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(TEXT)
            background = rounded(SURFACE_ALT, BORDER, 12f)
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }

    private fun renderOverview(container: LinearLayout, data: DeviceSnapshot) {
        addPageHeading(container, "Device overview", "Live readings from interfaces available on this device.")

        addCard(container, "DEVICE", "Hardware identity") {
            addInfoRow(it, "Model", data.model)
            addInfoRow(it, "Device code", data.deviceCode)
            addInfoRow(it, "Platform", data.platform)
            addInfoRow(it, "Kernel", data.kernelVersion)
            addInfoRow(it, "Uptime", DeviceInfo.uptime(data.uptimeSeconds))
        }

        val root = rootStatus ?: data.root
        addCard(container, "ROOT ACCESS", root.provider) {
            addText(it, if (root.available) "Privileged shell verified" else "Root permission not available",
                if (root.available) ACCENT else WARNING, 15f, bold = true)
            addText(it, root.detail, SECONDARY, 12f, top = 5)
            addText(it, "Uses the standard su command interface. Manager-specific APIs are not required for basic commands.",
                MUTED, 11f, top = 9)
        }

        addCard(container, "PROCESSOR", "Detected CPU frequency policies") {
            val onlineCores = data.cpuCores.count { it.online == true }
            addInfoRow(it, "Cores online", onlineCores.toString() + "/" + data.cpuCores.size)
            if (data.cpuPolicies.isEmpty()) {
                addUnsupported(it, "CPU cpufreq policies")
            } else {
                data.cpuPolicies.forEach { policy ->
                    addInfoRow(it, policy.name + " · current", DeviceInfo.frequency(policy.currentFrequencyKHz))
                    addInfoRow(it, "Governor", policy.governor ?: "Unsupported")
                }
            }
            addText(it, "Open CPU for available governor and frequency controls.", MUTED, 11f, top = 8)
        }

        addCard(container, "GRAPHICS", "Adreno / KGSL telemetry") {
            addInfoRow(it, "GPU clock", DeviceInfo.frequencyHz(data.gpu.currentFrequencyHz))
            addInfoRow(it, "GPU utilization", data.gpu.utilizationPercent?.let { value -> "$value%" } ?: "Unsupported")
            addText(it, "GPU overclock and voltage controls are intentionally not provided.", MUTED, 11f, top = 7)
        }

        addCard(container, "MEMORY", "RAM and compressed swap") {
            val used = DeviceInfo.memoryUsedPercent(data)
            addInfoRow(it, "RAM usage", used?.let { value -> "$value%" } ?: "Unsupported")
            addInfoRow(it, "RAM total", data.memory.totalKb?.let { value -> formatKb(value) } ?: "Unsupported")
            addInfoRow(it, "ZRAM size", DeviceInfo.bytes(data.memory.zramDiskBytes))
        }

        addCard(container, "THERMAL", "Sensor summary") {
            val hottest = data.thermalSensors.maxByOrNull { it.temperatureCelsius }
            addInfoRow(it, "Sensors detected", data.thermalSensors.size.toString())
            addInfoRow(it, "Highest temperature", hottest?.let { sensor ->
                String.format(Locale.US, "%.1f °C · %s", sensor.temperatureCelsius, sensor.name)
            } ?: "Unsupported")
            addText(it, "Thermal protection remains under kernel control.", MUTED, 11f, top = 7)
        }

        if (data.warnings.isNotEmpty()) {
            addCard(container, "DETECTION NOTES") {
                data.warnings.forEach { warning -> addText(it, "•  $warning", SECONDARY, 12f, top = 5) }
            }
        }
    }

    private fun renderPerformance(container: LinearLayout, data: DeviceSnapshot) {
        addPageHeading(container, "Performance", "CPU, virtual memory and storage scheduler controls.")
        renderCpu(container, data)
        renderMemory(container, data)
        renderIo(container, data)
    }

    private fun renderMonitor(container: LinearLayout, data: DeviceSnapshot) {
        addPageHeading(container, "Monitor", "Live hardware telemetry. Unsupported nodes are not simulated.")
        renderGpu(container, data)
        renderFabric(container, data)
        renderThermal(container, data)
    }

    private fun renderSettings(container: LinearLayout, data: DeviceSnapshot?) {
        addPageHeading(container, "Settings", "Application preferences and root diagnostics.")

        addCard(container, "Application preferences") { card ->
            addText(card, "Language", SECONDARY, 13f, bold = true)
            addText(card, "Choose the language used throughout the application.", MUTED, 11f, top = 4, bottom = 10)
            val languageRow = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val currentLanguage = UiPreferences.language(context)
            languageRow.addView(
                choiceButton("English", currentLanguage == "en") {
                    onLanguageChanged("en")
                    render(restoreScroll = true)
                },
                LayoutParams(0, dp(44), 1f).apply { rightMargin = dp(5) }
            )
            languageRow.addView(
                choiceButton("Bahasa Indonesia", currentLanguage == "id") {
                    onLanguageChanged("id")
                    render(restoreScroll = true)
                },
                LayoutParams(0, dp(44), 1f).apply { leftMargin = dp(5) }
            )
            card.addView(languageRow, LayoutParams(MATCH_PARENT, dp(48)))
        }

        addCard(container, "Refresh interval") { card ->
            addText(card, "Choose how often live kernel telemetry is refreshed.", MUTED, 11f, bottom = 8)
            val currentInterval = UiPreferences.refreshIntervalSeconds(context)
            val options = listOf("5 seconds", "8 seconds", "15 seconds", "30 seconds")
            val picker = spinner(options, "$currentInterval seconds")
            card.addView(picker, LayoutParams(MATCH_PARENT, dp(46)))
            cardButton(card, "Apply refresh interval") {
                val seconds = picker.selectedItem?.toString()
                    ?.substringBefore(" ")
                    ?.toIntOrNull()
                if (seconds == null || seconds !in setOf(5, 8, 15, 30)) {
                    toast("Select a valid refresh interval.")
                } else {
                    onRefreshIntervalChanged(seconds)
                    toast("Refresh interval updated.")
                }
            }
        }

        addCard(container, "Root diagnostics") { card ->
            val root = rootStatus ?: data?.root
            addInfoRow(card, "Root access", when (root?.available) {
                true -> "Enabled"
                false -> "Disabled"
                null -> "Unknown"
            })
            addInfoRow(card, "Provider", root?.provider ?: "Unknown")
            addText(card, "Root access is checked before kernel telemetry is scanned. A cached SU session is reused for subsequent reads.",
                MUTED, 11f, top = 8)
            cardButton(card, "Check root now") {
                onRootRefresh()
            }
        }

        addCard(container, "ABOUT") { card ->
            addText(card, "Nocturne Kernel Manager", TEXT, 15f, bold = true)
            addText(card, "Target: Samsung Galaxy M11 / A11 · SDM450", SECONDARY, 12f, top = 5)
            addText(card, "Version 0.3.0", MUTED, 11f, top = 5)
            addText(card, "Inspired by established kernel-manager concepts. Hardware controls are exposed only when validated interfaces are present.",
                MUTED, 11f, top = 9)
        }
    }

    private fun choiceButton(label: String, selected: Boolean, action: () -> Unit): TextView =
        TextView(context).apply {
            text = label
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
            setTextColor(if (selected) BACKGROUND else TEXT)
            background = rounded(
                if (selected) ACCENT else SURFACE_ALT,
                if (selected) ACCENT else BORDER,
                12f
            )
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }

    private fun renderCpu(container: LinearLayout, data: DeviceSnapshot) {
        addPageHeading(container, "CPU controls", "Only values exposed by each cpufreq policy are selectable.")
        if (!data.root.available) addRootRequired(container)
        if (data.cpuPolicies.isEmpty()) {
            addCard(container, "CPU interface unavailable") { addUnsupported(it, "No readable cpufreq policy was found.") }
            return
        }
        data.cpuPolicies.forEach { policy -> addCpuPolicyCard(container, data, policy) }
        addCard(container, "CPU CORE TOPOLOGY", "Read-only core online status") { card ->
            if (data.cpuCores.isEmpty()) {
                addUnsupported(card, "CPU core topology")
            } else {
                data.cpuCores.forEach { core ->
                    addInfoRow(
                        card,
                        "Core " + core.coreId,
                        when (core.online) {
                            true -> "Online"
                            false -> "Offline"
                            null -> "Unknown"
                        }
                    )
                }
            }
            addText(card, "Core hotplug control is not enabled in this build. This page only observes the kernel state.",
                MUTED, 11f, top = 8)
        }
        addCard(container, "Control boundary") {
            addText(it, "Frequency values are loaded from the active kernel OPP table. The app rejects values outside that table and restores previous limits when a write fails. No voltage or regulator node is modified.",
                SECONDARY, 12f)
        }
    }

    private fun addCpuPolicyCard(container: LinearLayout, data: DeviceSnapshot, policy: CpuPolicyInfo) {
        addCard(container, policy.name.uppercase(Locale.US), policy.path) { card ->
            addInfoRow(card, "Current frequency", DeviceInfo.frequency(policy.currentFrequencyKHz))
            addInfoRow(card, "Current governor", policy.governor ?: "Unsupported")
            addInfoRow(card, "Current minimum", DeviceInfo.frequency(policy.minFrequencyKHz))
            addInfoRow(card, "Current maximum", DeviceInfo.frequency(policy.maxFrequencyKHz))
            addInfoRow(card, "Available OPP entries", policy.availableFrequenciesKHz.size.toString())

            val writablePolicyPath =
                Regex("^/sys/devices/system/cpu/(cpufreq/policy[0-9]+|cpu[0-7]/cpufreq)$").matches(policy.path)
            if (!writablePolicyPath) {
                addText(card, "This policy uses a non-standard path. Telemetry is read-only for safety.",
                    MUTED, 11f, top = 10)
            } else if (policy.availableGovernors.isNotEmpty()) {
                addText(card, "Governor", SECONDARY, 12f, bold = true, top = 12)
                val governorOptions = if (policy.governor != null && policy.governor in policy.availableGovernors) {
                    policy.availableGovernors
                } else {
                    listOf("Keep current") + policy.availableGovernors
                }
                val governorSpinner = spinner(governorOptions, policy.governor ?: "Keep current")
                card.addView(governorSpinner, LayoutParams(MATCH_PARENT, dp(46)))

                val frequencies = policy.availableFrequenciesKHz
                val currentMin = policy.minFrequencyKHz
                val currentMax = policy.maxFrequencyKHz
                val currentBoundsAreValid = currentMin != null && currentMax != null &&
                    currentMin in frequencies && currentMax in frequencies
                if (frequencies.isNotEmpty() && currentBoundsAreValid) {
                    addText(card, "Minimum frequency", SECONDARY, 12f, bold = true, top = 8)
                    val minSpinner = frequencySpinner(frequencies, currentMin!!)
                    card.addView(minSpinner, LayoutParams(MATCH_PARENT, dp(46)))
                    addText(card, "Maximum frequency", SECONDARY, 12f, bold = true, top = 8)
                    val maxSpinner = frequencySpinner(frequencies, currentMax!!)
                    card.addView(maxSpinner, LayoutParams(MATCH_PARENT, dp(46)))
                    card.addView(
                        actionButton("Apply CPU settings", "Apply governor and limits") {
                            if (!data.root.available) {
                                toast("Root permission is required.")
                            } else {
                                val min = selectedFrequency(minSpinner)
                                val max = selectedFrequency(maxSpinner)
                                val selectedGovernor = governorSpinner.selectedItem?.toString()
                                    ?.takeIf { it != t("Keep current") }
                                if (min == null || max == null) {
                                    toast("Kernel frequency values are unavailable.")
                                } else {
                                    onCpuApply(policy, selectedGovernor, min, max)
                                }
                            }
                        },
                        LayoutParams(MATCH_PARENT, dp(46)).apply { topMargin = dp(12) }
                    )
                } else {
                    addText(card, "Frequency controls hidden because current min/max are missing or do not match the live OPP table.",
                        MUTED, 11f, top = 8)
                }
            } else {
                addUnsupported(card, "Available governor list")
            }
            if (!data.root.available) addText(card, "Grant root access to apply changes.", WARNING, 11f, top = 8)
        }
    }

    private fun renderGpu(container: LinearLayout, data: DeviceSnapshot) {
        addPageHeading(container, "GPU telemetry", "Read-only Adreno / KGSL and devfreq readings.")
        addCard(container, "GPU STATUS", data.gpu.source ?: "No KGSL / GPU node detected") {
            addInfoRow(it, "Current frequency", DeviceInfo.frequencyHz(data.gpu.currentFrequencyHz))
            addInfoRow(it, "Utilization", data.gpu.utilizationPercent?.let { value -> "$value%" } ?: "Unsupported")
            addInfoRow(it, "Available OPP entries", data.gpu.availableFrequenciesHz.size.toString())
        }
        addCard(container, "GPU SAFETY POLICY") {
            addText(it, "This application does not expose GPU overclocking, maximum-clock caps, voltage overrides, regulator changes, or CPR/corner scaling. Existing kernel GPU operating points are left unchanged.",
                SECONDARY, 13f)
            addText(it, "Telemetry is shown only when the device exposes a readable KGSL or devfreq node.",
                MUTED, 11f, top = 9)
        }
    }

    private fun renderFabric(container: LinearLayout, data: DeviceSnapshot) {
        addPageHeading(container, "CCI & DDR fabric", "Current devfreq values from detected interconnect and memory-bus nodes.")
        if (data.devfreq.isEmpty()) {
            addCard(container, "Fabric telemetry unavailable") {
                addUnsupported(it, "No matching CCI, cpubw, bus, DDR, memory or LLCC devfreq node was detected.")
            }
        } else {
            data.devfreq.forEach { item ->
                addCard(container, item.name, item.path) {
                    addInfoRow(it, "Current frequency", DeviceInfo.frequencyHz(item.currentFrequencyHz))
                    addInfoRow(it, "Governor", item.governor ?: "Unsupported")
                    addInfoRow(it, "Available frequency entries", item.availableFrequenciesHz.size.toString())
                }
            }
        }
        addCard(container, "KERNEL-OWNED BOOST") {
            addText(it, "Kernel-side transient boosts, including Zygote-triggered DDR bandwidth behavior when implemented by the running kernel, are not recreated by this app. This page reports exposed devfreq state only; it does not poll or inject a replacement boost.",
                SECONDARY, 12f)
        }
    }

    private fun renderMemory(container: LinearLayout, data: DeviceSnapshot) {
        addPageHeading(container, "Memory", "RAM and ZRAM data read from system memory interfaces.")
        addCard(container, "RAM") {
            val used = DeviceInfo.memoryUsedPercent(data)
            addInfoRow(it, "Total", data.memory.totalKb?.let { value -> formatKb(value) } ?: "Unsupported")
            addInfoRow(it, "Available", data.memory.availableKb?.let { value -> formatKb(value) } ?: "Unsupported")
            addInfoRow(it, "Used", used?.let { "$it%" } ?: "Unsupported")
            if (used != null) addProgress(it, used)
        }
        addCard(container, "ZRAM") {
            addInfoRow(it, "Configured disk size", DeviceInfo.bytes(data.memory.zramDiskBytes))
            addInfoRow(it, "Original data", DeviceInfo.bytes(data.memory.zramOriginalBytes))
            addInfoRow(it, "Compressed data", DeviceInfo.bytes(data.memory.zramCompressedBytes))
            addInfoRow(it, "Memory consumed", DeviceInfo.bytes(data.memory.zramMemoryBytes))
            addText(it, "ZRAM configuration is shown read-only; no swap or compression policy is changed.", MUTED, 11f, top = 8)
        }
        addCard(container, "KERNEL OPTIMIZATION TELEMETRY") { card ->
            val extra = data.kernelExtras
            addInfoRow(card, "ZRAM compression algorithm", extra.zramCompressionAlgorithm ?: "Unsupported")
            addInfoRow(
                card,
                "Available ZRAM algorithms",
                extra.availableZramCompressionAlgorithms.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "Unsupported"
            )
            addInfoRow(card, "KSM status", when (extra.ksmEnabled) {
                true -> "Enabled"
                false -> "Disabled"
                null -> if (extra.ksmSupported) "Unknown" else "Unsupported"
            })
            addInfoRow(card, "KSM shared pages", extra.ksmPagesShared?.toString() ?: "Unsupported")
            addInfoRow(card, "KSM sharing pages", extra.ksmPagesSharing?.toString() ?: "Unsupported")
            addInfoRow(card, "Entropy available", extra.entropyAvailable?.toString() ?: "Unsupported")
            addInfoRow(card, "Entropy read threshold", extra.entropyReadWakeupThreshold?.toString() ?: "Unsupported")
            addInfoRow(card, "Entropy write threshold", extra.entropyWriteWakeupThreshold?.toString() ?: "Unsupported")
            addText(card, "Kernel optimization interfaces are shown as read-only telemetry.", MUTED, 11f, top = 8)
        }
        addCard(container, "VIRTUAL MEMORY TUNING", "Runtime VM parameters · current boot session") { card ->
            val swappiness = data.memory.swappiness
            val cachePressure = data.memory.vfsCachePressure
            addInfoRow(card, "Current swappiness", swappiness?.toString() ?: "Unsupported")
            addInfoRow(card, "Current VFS cache pressure", cachePressure?.toString() ?: "Unsupported")
            if (swappiness != null && cachePressure != null && swappiness in 0..100 && cachePressure in 10..200) {
                val swappinessBar = addSeekControl(card, "Swappiness", swappiness, 0, 100)
                val cacheBar = addSeekControl(card, "VFS cache pressure", cachePressure, 10, 200)
                card.addView(
                    actionButton("Apply VM settings", "Apply virtual memory settings") {
                        if (!data.root.available) {
                            toast("Root permission is required.")
                        } else {
                            val selectedSwappiness = (swappinessBar.tag as Int) + swappinessBar.progress
                            val selectedCache = (cacheBar.tag as Int) + cacheBar.progress
                            onVmApply(selectedSwappiness, selectedCache)
                        }
                    },
                    LayoutParams(MATCH_PARENT, dp(46)).apply { topMargin = dp(12) }
                )
            } else {
                addUnsupported(card, "VM tuning nodes or current values outside supported ranges")
            }
            if (!data.root.available) addText(card, "Root permission is required to apply changes.", WARNING, 11f, top = 8)
        }
    }

    private fun renderThermal(container: LinearLayout, data: DeviceSnapshot) {
        addPageHeading(container, "Thermal sensors", "All detected thermal zones. No thermal policy writes are exposed.")
        if (data.thermalSensors.isEmpty()) {
            addCard(container, "Thermal interface unavailable") {
                addUnsupported(it, "No readable thermal zone temperature node was found.")
            }
        } else {
            data.thermalSensors.forEach { sensor ->
                addCard(container, sensor.name) {
                    addText(it, String.format(Locale.US, "%.1f °C", sensor.temperatureCelsius),
                        if (sensor.temperatureCelsius >= 55.0) WARNING else ACCENT, 25f, bold = true)
                }
            }
        }
        addCard(container, "THERMAL SAFETY") {
            addText(it, "Thermal throttling, trip points, cooling devices and shutdown protection remain controlled by the kernel and Android thermal framework. This app does not disable or raise thermal limits.",
                SECONDARY, 12f)
        }
    }

    private fun renderIo(container: LinearLayout, data: DeviceSnapshot) {
        addPageHeading(container, "I/O scheduler", "Select only schedulers reported by each block device.")
        val readSectors = data.ioUsage.sectorsRead?.toString() ?: "Unsupported"
        val writtenSectors = data.ioUsage.sectorsWritten?.toString() ?: "Unsupported"
        addInfoCard(container, "Cumulative disk sectors", "Read: " + readSectors + " · Written: " + writtenSectors)
        if (!data.root.available) addRootRequired(container)

        if (data.ioSchedulers.isEmpty()) {
            addCard(container, "I/O scheduler interfaces unavailable") {
                addUnsupported(it, "No /sys/block/*/queue/scheduler node was readable.")
            }
        } else {
            data.ioSchedulers.forEach { scheduler ->
                addCard(container, scheduler.deviceName, scheduler.path) { card ->
                    addInfoRow(card, "Active scheduler", scheduler.activeScheduler ?: "Unknown")
                    addInfoRow(card, "Available", scheduler.availableSchedulers.joinToString(", "))
                    val schedulerOptions = if (scheduler.activeScheduler != null &&
                        scheduler.activeScheduler in scheduler.availableSchedulers) {
                        scheduler.availableSchedulers
                    } else {
                        listOf("Keep current") + scheduler.availableSchedulers
                    }
                    val picker = spinner(schedulerOptions, scheduler.activeScheduler ?: "Keep current")
                    card.addView(picker, LayoutParams(MATCH_PARENT, dp(46)))
                    card.addView(
                        actionButton("Apply scheduler", "Apply I/O scheduler") {
                            if (!data.root.available) toast("Root permission is required.")
                            else picker.selectedItem?.toString()
                                ?.takeIf { it != t("Keep current") }
                                ?.let { onIoApply(scheduler, it) }
                        },
                        LayoutParams(MATCH_PARENT, dp(46)).apply { topMargin = dp(10) }
                    )
                }
            }
        }
    }

    private fun renderSystem(container: LinearLayout, data: DeviceSnapshot) {
        addPageHeading(container, "System & diagnostics", "Battery, storage, network counters and kernel logs.")
        addCard(container, "BATTERY") {
            addInfoRow(it, "Level", data.battery.levelPercent?.let { value -> "$value%" } ?: "Unsupported")
            addInfoRow(it, "State", when (data.battery.charging) {
                true -> "Charging / full"
                false -> "Not charging"
                null -> "Unsupported"
            })
            addInfoRow(it, "Temperature", data.battery.temperatureCelsius?.let { value ->
                String.format(Locale.US, "%.1f °C", value)
            } ?: "Unsupported")
            addInfoRow(it, "Voltage", data.battery.voltageMillivolts?.let { value -> "$value mV" } ?: "Unsupported")
            addInfoRow(it, "Technology", data.battery.technology ?: "Unsupported")
        }
        addCard(container, "STORAGE") {
            addInfoRow(it, "Data partition total", DeviceInfo.bytes(data.storage.totalBytes))
            addInfoRow(it, "Available", DeviceInfo.bytes(data.storage.availableBytes))
            val totalBytes = data.storage.totalBytes
            val availableBytes = data.storage.availableBytes
            val percent = if (totalBytes != null && availableBytes != null && totalBytes > 0L) {
                ((availableBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)
            } else null
            addInfoRow(it, "Available space", percent?.let { value -> "$value%" } ?: "Unsupported")
        }
        addCard(container, "NETWORK") {
            addInfoRow(it, "Total RX", DeviceInfo.bytes(data.networkRxBytes))
            addInfoRow(it, "Total TX", DeviceInfo.bytes(data.networkTxBytes))
            addText(it, "Android TrafficStats counters; cumulative since the system counter reset.", MUTED, 11f, top = 7)
            addText(it, "TCP congestion control", SECONDARY, 12f, bold = true, top = 12)
            addInfoRow(it, "Current algorithm", data.tcpCongestionControl ?: "Unsupported")
            if (data.availableTcpCongestionControls.isNotEmpty()) {
                val tcpOptions = if (data.tcpCongestionControl != null &&
                    data.tcpCongestionControl in data.availableTcpCongestionControls) {
                    data.availableTcpCongestionControls
                } else {
                    listOf("Keep current") + data.availableTcpCongestionControls
                }
                val picker = spinner(tcpOptions, data.tcpCongestionControl ?: "Keep current")
                it.addView(picker, LayoutParams(MATCH_PARENT, dp(46)))
                cardButton(it, "Apply TCP algorithm") {
                    if (!data.root.available) toast("Root permission is required.")
                    else picker.selectedItem?.toString()
                        ?.takeIf { selected -> selected != t("Keep current") }
                        ?.let { selected -> onTcpApply(selected) }
                }
            } else {
                addUnsupported(it, "Available TCP congestion algorithms")
            }
        }
        addCard(container, "KERNEL LOG") {
            addText(it, "Read the latest kernel ring-buffer entries. Root access is required.", SECONDARY, 12f)
            cardButton(it, "View dmesg") { onShowLogs() }
        }
        addCard(container, "BUILD INFORMATION") {
            addInfoRow(it, "Kernel release", data.kernelVersion)
            addInfoRow(it, "Device", data.deviceCode)
            addInfoRow(it, "Platform", data.platform)
            addInfoRow(it, "Uptime", DeviceInfo.uptime(data.uptimeSeconds))
        }
    }

    private fun addPageHeading(container: LinearLayout, title: String, subtitle: String) {
        addText(container, title, TEXT, 23f, bold = true, bottom = 3)
        addText(container, subtitle, SECONDARY, 12f, bottom = 14)
    }

    private fun addInfoCard(container: LinearLayout, title: String, description: String) {
        addCard(container, title) { addText(it, description, SECONDARY, 12f) }
    }

    private fun addRootRequired(container: LinearLayout) {
        addCard(container, "Root access required") {
            addText(it, "Read-only values remain available where Android permits. Grant root through your installed root manager to apply supported controls.",
                WARNING, 12f)
        }
    }

    private fun addCard(
        container: LinearLayout,
        title: String,
        subtitle: String? = null,
        content: (LinearLayout) -> Unit
    ) {
        val card = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(dp(16), dp(15), dp(16), dp(16))
            background = rounded(SURFACE, BORDER, 18f)
        }
        addText(card, title, ACCENT, 10f, bold = true, bottom = 4).letterSpacing = 0.12f
        if (!subtitle.isNullOrBlank()) addText(card, subtitle, MUTED, 10f, bottom = 10)
        content(card)
        val params = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = dp(12) }
        container.addView(card, params)
    }

    private fun addText(
        parent: LinearLayout,
        value: String,
        color: Int,
        size: Float,
        bold: Boolean = false,
        top: Int = 0,
        bottom: Int = 0
    ): TextView {
        val view = TextView(context).apply {
            text = t(value)
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(0, dp(top), 0, dp(bottom))
            setLineSpacing(dp(2).toFloat(), 1f)
        }
        parent.addView(view, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        return view
    }

    private fun addInfoRow(parent: LinearLayout, label: String, value: String) {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, dp(6))
        }
        row.addView(
            TextView(context).apply {
                text = t(label)
                textSize = 12f
                setTextColor(SECONDARY)
            },
            LayoutParams(0, WRAP_CONTENT, 1f)
        )
        row.addView(
            TextView(context).apply {
                text = t(value)
                textSize = 12f
                setTextColor(TEXT)
                gravity = Gravity.END
                maxWidth = dp(210)
            },
            LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        )
        parent.addView(row, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
    }

    private fun addUnsupported(parent: LinearLayout, label: String) {
        addText(parent, t(label) + " · " + t("Unsupported"), MUTED, 12f, top = 5)
    }

    private fun addProgress(parent: LinearLayout, value: Int) {
        val holder = LinearLayout(context).apply {
            orientation = HORIZONTAL
            addView(View(context).apply { background = rounded(ACCENT, ACCENT, 8f) },
                LayoutParams(0, dp(6), value.coerceIn(1, 100).toFloat()))
            addView(View(context).apply { background = rounded(BORDER, BORDER, 8f) },
                LayoutParams(0, dp(6), (100 - value).coerceAtLeast(0).toFloat()))
        }
        parent.addView(holder, LayoutParams(MATCH_PARENT, dp(6)).apply { topMargin = dp(9) })
    }

    private fun spinner(options: List<String>, selected: String?): Spinner {
        val spinner = Spinner(context)
        val displayOptions = options.map { t(it) }
        val adapter = object : ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, displayOptions.toMutableList()) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = (convertView as? TextView) ?: TextView(context)
                view.text = getItem(position).orEmpty()
                view.textSize = 13f
                view.setTextColor(TEXT)
                view.gravity = Gravity.CENTER_VERTICAL
                view.setPadding(dp(10), 0, dp(10), 0)
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = (convertView as? TextView) ?: TextView(context)
                view.text = getItem(position).orEmpty()
                view.textSize = 13f
                view.setTextColor(TEXT)
                view.setBackgroundColor(SURFACE)
                view.setPadding(dp(14), dp(12), dp(14), dp(12))
                return view
            }
        }
        spinner.adapter = adapter
        val index = options.indexOf(selected)
        if (index >= 0) spinner.setSelection(index)
        spinner.background = rounded(SURFACE_ALT, BORDER, 10f)
        return spinner
    }

    private fun addSeekControl(
        parent: LinearLayout,
        label: String,
        current: Int,
        minimum: Int,
        maximum: Int
    ): SeekBar {
        addText(parent, label, SECONDARY, 12f, bold = true, top = 10)
        val valueLabel = addText(parent, current.toString(), TEXT, 13f, top = 3)
        val seek = SeekBar(context).apply {
            max = maximum - minimum
            progress = (current - minimum).coerceIn(0, max)
            tag = minimum
            progressTintList = ColorStateList.valueOf(ACCENT)
            thumbTintList = ColorStateList.valueOf(ACCENT)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    valueLabel.text = (minimum + progress).toString()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }
        parent.addView(seek, LayoutParams(MATCH_PARENT, dp(42)))
        return seek
    }

    private fun frequencySpinner(values: List<Long>, selected: Long): Spinner {
        val options = values.map { value ->
            value.toString() + " · " + KernelParsers.formatFrequency(value)
        }
        val current = values.indexOf(selected).takeIf { it >= 0 }?.let { options[it] }
        return spinner(options, current)
    }

    private fun selectedFrequency(spinner: Spinner): Long? =
        spinner.selectedItem?.toString()?.substringBefore(" · ")?.toLongOrNull()

    private fun cardButton(parent: LinearLayout, label: String, action: () -> Unit) {
        parent.addView(actionButton(label, label, action),
            LayoutParams(MATCH_PARENT, dp(44)).apply { topMargin = dp(10) })
    }

    private fun actionButton(label: String, description: String, action: () -> Unit): TextView {
        return TextView(context).apply {
            text = t(label)
            contentDescription = t(description)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
            setTextColor(BACKGROUND)
            background = rounded(ACCENT, ACCENT, 12f)
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }
    }

    private fun chip(label: String, color: Int): TextView =
        TextView(context).apply {
            text = t(label)
            textSize = 9f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(color)
            gravity = Gravity.CENTER
            setPadding(dp(10), 0, dp(10), 0)
            background = rounded(SURFACE_ALT, BORDER, 20f)
        }

    private fun rounded(fill: Int, stroke: Int, radius: Float): GradientDrawable =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dp(radius.toInt()).toFloat()
            setStroke(dp(1), stroke)
        }

    private fun formatKb(value: Long): String =
        String.format(Locale.US, "%.0f MB", value / 1024.0)

    private fun toast(message: String) {
        android.widget.Toast.makeText(context, t(message), android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT
        private const val WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT
        private const val BACKGROUND = 0xFF080B10.toInt()
        private const val SURFACE = 0xFF111821.toInt()
        private const val SURFACE_ALT = 0xFF182330.toInt()
        private const val BORDER = 0xFF263544.toInt()
        private const val TEXT = 0xFFF4F7FA.toInt()
        private const val SECONDARY = 0xFFB3C0CC.toInt()
        private const val MUTED = 0xFF778796.toInt()
        private const val ACCENT = 0xFFA5E56D.toInt()
        private const val WARNING = 0xFFFFC166.toInt()
    }
}
