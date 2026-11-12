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
import android.widget.Switch
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
    private val onRefreshIntervalChanged: (Int) -> Unit,
    private val onGpuMaximumFrequencyApply: (Long) -> Unit,
    private val onCpuCoreToggle: (Int, Boolean) -> Unit,
    private val onDevfreqRangeApply: (String, Long, Long) -> Unit,
    private val onSchedutilTunableApply: (String, String, String) -> Unit,
    private val onDynamicFsyncApply: (Boolean) -> Unit
) : LinearLayout(context) {

    private enum class Page(val label: String, val icon: String) {
        OVERVIEW("Overview", "⌂"),
        PERFORMANCE("Performance", "⚡"),
        MONITOR("Monitor", "◉"),
        SYSTEM("System", "▦"),
        ADVANCED("Advanced", "⚙"),
        SETTINGS("Settings", "⚙")
    }

    private var selectedPage = Page.OVERVIEW
    private var snapshot: DeviceSnapshot? = null
    private var rootStatus: RootAccess? = null
    private var currentScroll: ScrollView? = null
    private val liveBindings = mutableListOf<(DeviceSnapshot) -> Unit>()
    private var renderedStructureKey: String? = null
    private var pendingUiSnapshot: DeviceSnapshot? = null
    private var frameUpdatePending = false

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
        if (forceRender) {
            render(restoreScroll = true)
            return
        }
        if (selectedPage == Page.SETTINGS) return

        if (renderedStructureKey != structureKey(value)) {
            render(restoreScroll = true)
            return
        }

        // Coalesce telemetry changes and apply them on the next display frame.
        pendingUiSnapshot = value
        if (!frameUpdatePending) {
            frameUpdatePending = true
            postOnAnimation {
                frameUpdatePending = false
                val latest = pendingUiSnapshot
                pendingUiSnapshot = null
                if (latest != null) liveBindings.forEach { it(latest) }
            }
        }
    }

    private fun structureKey(data: DeviceSnapshot): String = buildString {
        append(data.cpuCores.map { it.coreId })
        append('|')
        append(data.cpuPolicies.map {
            listOf(it.path, it.governor, it.minFrequencyKHz, it.maxFrequencyKHz,
                it.availableFrequenciesKHz, it.availableGovernors)
        })
        append('|')
        append(data.devfreq.map { listOf(it.path, it.minimumFrequencyHz, it.maximumFrequencyHz, it.availableFrequenciesHz) })
        append('|')
        append(data.ioSchedulers.map { listOf(it.path, it.activeScheduler, it.availableSchedulers) })
        append('|')
        append(data.tcpCongestionControl)
        append('|')
        append(data.availableTcpCongestionControls)
        append('|')
        append(data.memory.swappiness)
        append('|')
        append(data.memory.vfsCachePressure)
        append('|')
        append(data.thermalSensors.map { it.name })
        append('|')
        append(data.gpu.availableFrequenciesHz)
        append('|')
        append(data.gpu.source.orEmpty())
        append('|')
        append(data.gpu.maximumPowerLevel)
        append('|')
        append(data.gpu.minimumPowerLevel)
        append('|')
        append(data.gpu.numberOfPowerLevels)
        append('|')
        append(data.regulators.map { it.name })
        append('|')
        append(data.advancedKernel.schedutilTunables.map {
            listOf(
                it.policyPath, it.hispeedLoad, it.hispeedFrequencyKHz,
                it.upRateLimitUs, it.downRateLimitUs, it.powerLimitEnabled
            )
        })
        append('|')
        append(data.advancedKernel.dynamicFsyncPath)
        append('|')
        append(data.advancedKernel.dynamicFsyncEnabled)
        append('|')
        append(data.warnings.sorted())
    }

    private fun render(restoreScroll: Boolean = false) {
        val previousScroll = if (restoreScroll) currentScroll?.scrollY ?: 0 else 0
        liveBindings.clear()
        pendingUiSnapshot = null
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
                Page.ADVANCED -> data?.let { renderAdvanced(content, it) }
                Page.SETTINGS -> renderSettings(content, data)
            }
        }

        if (previousScroll > 0) scroll.post { scroll.scrollTo(0, previousScroll) }
        renderedStructureKey = snapshot?.let(::structureKey)
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

        Page.values().filter {
            it != Page.SETTINGS &&
                (it != Page.ADVANCED || UiPreferences.advancedMenuEnabled(context))
        }.forEach { page ->
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
        addPageHeading(container, "Device overview", "Hardware and kernel status.")

        addCard(container, "DEVICE", "Hardware identity") {
            addInfoRow(it, "Model", data.model)
            addInfoRow(it, "Device code", data.deviceCode)
            addInfoRow(it, "Platform", data.platform)
            addInfoRow(it, "Kernel", data.kernelVersion)
            addLiveInfoRow(it, "Uptime", DeviceInfo.uptime(data.uptimeSeconds)) { DeviceInfo.uptime(it.uptimeSeconds) }
        }

        val root = rootStatus ?: data.root
        addCard(container, "ROOT ACCESS", root.provider) {
            addText(it, if (root.available) "Privileged shell verified" else "Root permission not available",
                if (root.available) ACCENT else WARNING, 15f, bold = true)
            addText(it, root.detail, SECONDARY, 12f, top = 5)
            addText(it, "Standard su interface; no manager-specific API required.",
                MUTED, 11f, top = 9)
        }

        addCard(container, "PROCESSOR", "Frequency policies") {
            val onlineCores = data.cpuCores.count { it.online == true }
            addLiveInfoRow(it, "Cores online", onlineCores.toString() + "/" + data.cpuCores.size) { current ->
                current.cpuCores.count { core -> core.online == true }.toString() + "/" + current.cpuCores.size
            }
            if (data.cpuPolicies.isEmpty()) {
                addUnsupported(it, "CPU cpufreq policies")
            } else {
                data.cpuPolicies.forEach { policy ->
                    addLiveInfoRow(it, policy.name + " · current", DeviceInfo.frequency(policy.currentFrequencyKHz)) { current ->
                        DeviceInfo.frequency(current.cpuPolicies.firstOrNull { item -> item.path == policy.path }?.currentFrequencyKHz)
                    }
                    addLiveInfoRow(it, "Governor", policy.governor ?: "Unsupported") { current ->
                        current.cpuPolicies.firstOrNull { item -> item.path == policy.path }?.governor ?: "Unsupported"
                    }
                }
            }
            addText(it, "Set CPU governor and frequency on Performance.", MUTED, 11f, top = 8)
        }

        addCard(container, "GRAPHICS", "Adreno / KGSL telemetry") {
            addLiveInfoRow(it, "GPU clock", DeviceInfo.frequencyHz(data.gpu.currentFrequencyHz)) { DeviceInfo.frequencyHz(it.gpu.currentFrequencyHz) }
            addLiveInfoRow(it, "GPU utilization", data.gpu.utilizationPercent?.let { value -> "$value%" } ?: "Unsupported") { current ->
                current.gpu.utilizationPercent?.let { value -> "$value%" } ?: "Unsupported"
            }
            addText(it, "GPU tuning is read-only.", MUTED, 11f, top = 7)
        }

        addCard(container, "MEMORY", "RAM and compressed swap") {
            val used = DeviceInfo.memoryUsedPercent(data)
            addLiveInfoRow(it, "RAM usage", used?.let { value -> "$value%" } ?: "Unsupported") { current ->
                DeviceInfo.memoryUsedPercent(current)?.let { value -> "$value%" } ?: "Unsupported"
            }
            addLiveInfoRow(it, "RAM total", data.memory.totalKb?.let { value -> formatKb(value) } ?: "Unsupported") { current ->
                current.memory.totalKb?.let { value -> formatKb(value) } ?: "Unsupported"
            }
            addLiveInfoRow(it, "ZRAM size", DeviceInfo.bytes(data.memory.zramDiskBytes)) { DeviceInfo.bytes(it.memory.zramDiskBytes) }
        }

        addCard(container, "THERMAL", "Sensor summary") {
            val hottest = data.thermalSensors.maxByOrNull { it.temperatureCelsius }
            addLiveInfoRow(it, "Sensors detected", data.thermalSensors.size.toString()) { it.thermalSensors.size.toString() }
            addLiveInfoRow(it, "Highest temperature", hottest?.let { sensor ->
                String.format(Locale.US, "%.1f °C · %s", sensor.temperatureCelsius, sensor.name)
            } ?: "Unsupported") { current ->
                current.thermalSensors.maxByOrNull { sensor -> sensor.temperatureCelsius }?.let { sensor ->
                    String.format(Locale.US, "%.1f °C · %s", sensor.temperatureCelsius, sensor.name)
                } ?: "Unsupported"
            }
            addText(it, "Thermal limits are kernel-managed.", MUTED, 11f, top = 7)
        }

        if (data.warnings.isNotEmpty()) {
            addCard(container, "DETECTION NOTES") {
                data.warnings.forEach { warning -> addText(it, "•  $warning", SECONDARY, 12f, top = 5) }
            }
        }
    }

    private fun renderPerformance(container: LinearLayout, data: DeviceSnapshot) {
        addPageHeading(container, "Performance", "CPU, VM and I/O controls.")
        renderCpu(container, data)
        renderMemory(container, data)
        renderIo(container, data)
    }

    private fun renderMonitor(container: LinearLayout, data: DeviceSnapshot) {
        addPageHeading(container, "Monitor", "Live hardware telemetry.")
        renderGpu(container, data)
        renderFabric(container, data)
        renderThermal(container, data)
    }

    private fun renderSettings(container: LinearLayout, data: DeviceSnapshot?) {
        addPageHeading(container, "Settings", "App settings and root status.")

        addCard(container, "Application preferences") { card ->
            addText(card, "Language", SECONDARY, 13f, bold = true)
            addText(card, "App language.", MUTED, 11f, top = 4, bottom = 10)
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
            addText(card, "Telemetry refresh rate.", MUTED, 11f, bottom = 8)
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

        addCard(container, "Advanced kernel menu") { card ->
            val row = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val labels = LinearLayout(context).apply { orientation = VERTICAL }
            addText(labels, "Enable advanced controls", TEXT, 13f, bold = true)
            addText(labels, "GPU OPP limits and kernel telemetry.", MUTED, 11f, top = 3)
            row.addView(labels, LayoutParams(0, WRAP_CONTENT, 1f))
            val toggle = Switch(context).apply {
                isChecked = UiPreferences.advancedMenuEnabled(context)
                contentDescription = t("Enable advanced controls")
                setOnCheckedChangeListener { _, enabled ->
                    UiPreferences.setAdvancedMenuEnabled(context, enabled)
                    if (!enabled && selectedPage == Page.ADVANCED) {
                        selectedPage = Page.OVERVIEW
                    }
                    render(restoreScroll = true)
                }
            }
            row.addView(toggle, LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
            card.addView(row, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }

        addCard(container, "Root diagnostics") { card ->
            val root = rootStatus ?: data?.root
            addInfoRow(card, "Root access", when (root?.available) {
                true -> "Enabled"
                false -> "Disabled"
                null -> "Unknown"
            })
            addInfoRow(card, "Provider", root?.provider ?: "Unknown")
            addText(card, "Root is checked at startup; the SU session is reused.",
                MUTED, 11f, top = 8)
            cardButton(card, "Check root now") {
                onRootRefresh()
            }
        }

        addCard(container, "ABOUT") { card ->
            addText(card, "Nocturne Kernel Manager", TEXT, 15f, bold = true)
            addText(card, "Target: Samsung Galaxy M11 / A11 · SDM450", SECONDARY, 12f, top = 5)
            addText(card, "Version 0.4.0", MUTED, 11f, top = 5)
            addText(card, "Hardware controls require validated kernel interfaces.",
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
        addPageHeading(container, "CPU controls", "Select an available frequency.")
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
                    addLiveInfoRow(
                        card,
                        "Core " + core.coreId,
                        when (core.online) {
                            true -> "Online"
                            false -> "Offline"
                            null -> "Unknown"
                        }
                    ) { current ->
                        when (current.cpuCores.firstOrNull { it.coreId == core.coreId }?.online) {
                            true -> "Online"
                            false -> "Offline"
                            null -> "Unknown"
                        }
                    }
                }
            }
            addText(card, "Core hotplug control is not enabled in this build. This page only observes the kernel state.",
                MUTED, 11f, top = 8)
        }
        addCard(container, "Control boundary") {
            addText(it, "Live OPP validation. Failed writes restore limits; voltage nodes are untouched.",
                SECONDARY, 12f)
        }
    }

    private fun addCpuPolicyCard(container: LinearLayout, data: DeviceSnapshot, policy: CpuPolicyInfo) {
        addCard(container, policy.name.uppercase(Locale.US), policy.path) { card ->
            addLiveInfoRow(card, "Current frequency", DeviceInfo.frequency(policy.currentFrequencyKHz)) { current -> DeviceInfo.frequency(current.cpuPolicies.firstOrNull { it.path == policy.path }?.currentFrequencyKHz) }
            addLiveInfoRow(card, "Current governor", policy.governor ?: "Unsupported") { current -> current.cpuPolicies.firstOrNull { it.path == policy.path }?.governor ?: "Unsupported" }
            addLiveInfoRow(card, "Current minimum", DeviceInfo.frequency(policy.minFrequencyKHz)) { current -> DeviceInfo.frequency(current.cpuPolicies.firstOrNull { it.path == policy.path }?.minFrequencyKHz) }
            addLiveInfoRow(card, "Current maximum", DeviceInfo.frequency(policy.maxFrequencyKHz)) { current -> DeviceInfo.frequency(current.cpuPolicies.firstOrNull { it.path == policy.path }?.maxFrequencyKHz) }
            addLiveInfoRow(card, "Available OPP entries", policy.availableFrequenciesKHz.size.toString()) { current -> current.cpuPolicies.firstOrNull { it.path == policy.path }?.availableFrequenciesKHz?.size?.toString() ?: "Unsupported" }

            val writablePolicyPath =
                Regex("^/sys/devices/system/cpu/(cpufreq/policy[0-9]+|cpu[0-7]/cpufreq)$").matches(policy.path)
            if (!writablePolicyPath) {
                addText(card, "Non-standard policy path; read-only.",
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
                    addText(card, "Frequency controls unavailable: limits do not match live OPPs.",
                        MUTED, 11f, top = 8)
                }
            } else {
                addUnsupported(card, "Available governor list")
            }
            if (!data.root.available) addText(card, "Grant root access to apply changes.", WARNING, 11f, top = 8)
        }
    }

    private fun renderAdvanced(container: LinearLayout, data: DeviceSnapshot) {
        addPageHeading(container, "Advanced kernel controls", "Runtime controls from live kernel interfaces.")

        addCard(container, "STABILITY NOTICE") {
            addText(
                it,
                "Higher frequency limits increase power and heat. Instability may cause artifacts, freezes, reboots or data loss. Test gradually and keep thermal protection enabled.",
                WARNING,
                12f
            )
            addText(
                it,
                "Changes are runtime-only and may reset after reboot.",
                MUTED,
                11f,
                top = 7
            )
        }

        val frequencies = data.gpu.availableFrequenciesHz.sortedDescending()
        val maximumLevel = data.gpu.maximumPowerLevel
        val minimumLevel = data.gpu.minimumPowerLevel
        val levelCount = data.gpu.numberOfPowerLevels
        val currentMaximum = maximumLevel?.let {
            KernelParsers.gpuFrequencyForPowerLevel(frequencies, it)
        }

        addCard(container, "ADRENO 506 · GPU OPP LIMIT", data.gpu.source ?: "KGSL interface unavailable") { card ->
            addLiveInfoRow(card, "Current GPU clock", DeviceInfo.frequencyHz(data.gpu.currentFrequencyHz)) {
                DeviceInfo.frequencyHz(it.gpu.currentFrequencyHz)
            }
            addInfoRow(card, "Maximum allowed OPP", currentMaximum?.let { DeviceInfo.frequencyHz(it) } ?: "Unsupported")
            addInfoRow(card, "Minimum power level", minimumLevel?.toString() ?: "Unsupported")
            addInfoRow(card, "Power-level entries", levelCount?.toString() ?: "Unsupported")
            addInfoRow(card, "Live OPP table", frequencies.size.toString())

            val validTable = frequencies.isNotEmpty() &&
                levelCount == frequencies.size &&
                maximumLevel != null && maximumLevel in frequencies.indices &&
                minimumLevel != null && minimumLevel in frequencies.indices &&
                maximumLevel <= minimumLevel
            if (validTable) {
                val selectable = frequencies.take(minimumLevel!! + 1)
                val labels = selectable.map { DeviceInfo.frequencyHz(it) }
                val selectedLabel = currentMaximum?.let { DeviceInfo.frequencyHz(it) }
                val picker = spinner(labels, selectedLabel)
                card.addView(picker, LayoutParams(MATCH_PARENT, dp(46)).apply { topMargin = dp(8) })
                card.addView(
                    actionButton("Apply GPU OPP limit", "Apply GPU maximum operating point") {
                        val selectedFrequency = selectable.getOrNull(picker.selectedItemPosition)
                        if (!data.root.available) {
                            toast("Root permission is required.")
                        } else if (selectedFrequency != null) {
                            onGpuMaximumFrequencyApply(selectedFrequency)
                        } else {
                            toast("GPU frequency selection is unavailable.")
                        }
                    },
                    LayoutParams(MATCH_PARENT, dp(46)).apply { topMargin = dp(10) }
                )
            } else {
                addUnsupported(card, "KGSL power-level controls")
                addText(
                    card,
                    "The kernel must expose matching gpu_available_frequencies, num_pwrlevels, max_pwrlevel and min_pwrlevel nodes.",
                    MUTED,
                    11f,
                    top = 6
                )
            }
            addText(
                card,
                "Uses existing KGSL OPPs only. No new OPP, voltage or regulator value is written; thermal limits remain active.",
                SECONDARY,
                11f,
                top = 9
            )
        }

        addCard(container, "CPU FREQUENCY") {
            if (data.cpuPolicies.isEmpty()) {
                addUnsupported(it, "CPU cpufreq policies")
            } else {
                data.cpuPolicies.forEach { policy ->
                    addLiveInfoRow(it, policy.name + " · current", DeviceInfo.frequency(policy.currentFrequencyKHz)) { current ->
                        DeviceInfo.frequency(current.cpuPolicies.firstOrNull { item -> item.path == policy.path }?.currentFrequencyKHz)
                    }
                    addLiveInfoRow(it, "Governor", policy.governor ?: "Unsupported") { current ->
                        current.cpuPolicies.firstOrNull { item -> item.path == policy.path }?.governor ?: "Unsupported"
                    }
                    addLiveInfoRow(
                        it,
                        "Live frequency range",
                        DeviceInfo.frequency(policy.minFrequencyKHz) + " – " +
                            DeviceInfo.frequency(policy.maxFrequencyKHz)
                    ) { current ->
                        current.cpuPolicies.firstOrNull { item -> item.path == policy.path }?.let { item ->
                            DeviceInfo.frequency(item.minFrequencyKHz) + " – " + DeviceInfo.frequency(item.maxFrequencyKHz)
                        } ?: "Unsupported"
                    }
                }
            }
            addText(
                it,
                "Governor and frequency controls are available under Performance. Selection is limited to the live CPU OPP table.",
                MUTED,
                11f,
                top = 8
            )
        }

        addCard(container, "SCHEDUTIL TUNABLES", "Live CPU governor controls") { card ->
            val tunables = data.advancedKernel.schedutilTunables
            if (tunables.isEmpty()) {
                addUnsupported(card, "No schedutil tunables exposed.")
            } else {
                tunables.forEach { item ->
                    val policy = data.cpuPolicies.firstOrNull { it.path == item.policyPath }
                    addText(card, policy?.name ?: item.policyPath.substringAfterLast('/'),
                        SECONDARY, 12f, bold = true, top = 9)
                    item.hispeedLoad?.takeIf { it in 0..100 }?.let { current ->
                        val loadBar = addSeekControl(card, "Hispeed load (%)", current, 0, 100)
                        card.addView(
                            actionButton("Apply hispeed load", "Apply schedutil hispeed load") {
                                if (!data.root.available) toast("Root permission is required.")
                                else onSchedutilTunableApply(
                                    item.path,
                                    "hispeed_load",
                                    ((loadBar.tag as Int) + loadBar.progress).toString()
                                )
                            },
                            LayoutParams(MATCH_PARENT, dp(42)).apply { topMargin = dp(4) }
                        )
                    }
                    val frequencies = policy?.availableFrequenciesKHz.orEmpty()
                    item.hispeedFrequencyKHz?.takeIf { it in frequencies }?.let { current ->
                        addText(card, "Hispeed frequency", SECONDARY, 12f, bold = true, top = 9)
                        val picker = frequencySpinner(frequencies, current)
                        card.addView(picker, LayoutParams(MATCH_PARENT, dp(44)))
                        card.addView(
                            actionButton("Apply hispeed frequency", "Apply schedutil hispeed frequency") {
                                if (!data.root.available) toast("Root permission is required.")
                                else selectedFrequency(picker)?.let { selected ->
                                    onSchedutilTunableApply(item.path, "hispeed_freq", selected.toString())
                                }
                            },
                            LayoutParams(MATCH_PARENT, dp(42)).apply { topMargin = dp(4) }
                        )
                    }
                    item.upRateLimitUs?.takeIf { it in 0L..1_000_000L }?.let { current ->
                        addSchedutilRateControl(card, item.path, "up_rate_limit_us", "Up rate limit", current, data.root.available)
                    }
                    item.downRateLimitUs?.takeIf { it in 0L..1_000_000L }?.let { current ->
                        addSchedutilRateControl(card, item.policyPath, "down_rate_limit_us", "Down rate limit", current, data.root.available)
                    }
                    item.powerLimitEnabled?.let { enabled ->
                        addInfoRow(card, "PL flag", if (enabled) "Enabled" else "Disabled")
                    }
                }
            }
            addText(
                card,
                "Hispeed frequency must match the active CPU OPP table. Rate limits are bounded to 0–1,000,000 µs.",
                MUTED,
                11f,
                top = 8
            )
        }

        addCard(container, "CPU CORE HOTPLUG", "CPU0 protected") { card ->
            if (data.cpuCores.isEmpty()) {
                addUnsupported(card, "CPU core topology")
            } else {
                addInfoRow(card, "Core 0", "Protected")
                val controllable = data.cpuCores.filter {
                    KernelParsers.isSupportedCpuHotplugCore(it.coreId) && it.online != null
                }
                if (controllable.isEmpty()) {
                    addUnsupported(card, "CPU hotplug interface")
                } else {
                    controllable.forEach { core ->
                        addLiveSwitchRow(
                            card,
                            "Core " + core.coreId,
                            core.online == true,
                            data.root.available,
                            onChanged = { enabled -> onCpuCoreToggle(core.coreId, enabled) },
                            valueProvider = { current ->
                                current.cpuCores.firstOrNull { it.coreId == core.coreId }?.online
                            }
                        )
                    }
                }
            }
            addText(card, "At least two cores must remain online. Runtime-only control.",
                MUTED, 11f, top = 8)
        }

        addCard(container, "CCI / DDR DEVFREQ", "Live fabric frequency limits") { card ->
            if (data.devfreq.isEmpty()) {
                addUnsupported(card, "No supported CCI/DDR devfreq node found.")
            } else {
                data.devfreq.forEach { item ->
                    addText(card, item.name, SECONDARY, 12f, bold = true, top = 8)
                    addLiveInfoRow(card, "Current frequency", DeviceInfo.frequencyHz(item.currentFrequencyHz)) { current ->
                        DeviceInfo.frequencyHz(current.devfreq.firstOrNull { entry -> entry.path == item.path }?.currentFrequencyHz)
                    }
                    addInfoRow(card, "Minimum exposed", item.minimumFrequencyHz?.let { value -> DeviceInfo.frequencyHz(value) } ?: "Unsupported")
                    addInfoRow(card, "Maximum exposed", item.maximumFrequencyHz?.let { value -> DeviceInfo.frequencyHz(value) } ?: "Unsupported")
                    addInfoRow(card, "Available OPP entries", item.availableFrequenciesHz.size.toString())

                    val values = item.availableFrequenciesHz.distinct().sorted()
                    val currentMin = item.minimumFrequencyHz
                    val currentMax = item.maximumFrequencyHz
                    val writable = values.isNotEmpty() && currentMin != null && currentMax != null &&
                        currentMin in values && currentMax in values
                    if (writable) {
                        val options = values.map { "$it Hz" }
                        addText(card, "Minimum frequency", SECONDARY, 12f, bold = true, top = 7)
                        val minPicker = spinner(options, "$currentMin Hz")
                        card.addView(minPicker, LayoutParams(MATCH_PARENT, dp(44)))
                        addText(card, "Maximum frequency", SECONDARY, 12f, bold = true, top = 7)
                        val maxPicker = spinner(options, "$currentMax Hz")
                        card.addView(maxPicker, LayoutParams(MATCH_PARENT, dp(44)))
                        card.addView(
                            actionButton("Apply fabric limits", "Apply CCI and DDR frequency limits") {
                                val minimum = minPicker.selectedItem?.toString()?.substringBefore(" ")?.toLongOrNull()
                                val maximum = maxPicker.selectedItem?.toString()?.substringBefore(" ")?.toLongOrNull()
                                if (!data.root.available) toast("Root permission is required.")
                                else if (minimum == null || maximum == null || minimum > maximum) {
                                    toast("Invalid devfreq range.")
                                } else {
                                    onDevfreqRangeApply(item.path, minimum, maximum)
                                }
                            },
                            LayoutParams(MATCH_PARENT, dp(44)).apply { topMargin = dp(8) }
                        )
                    } else {
                        addText(card, "No writable min_freq/max_freq pair. Telemetry only.",
                            MUTED, 11f, top = 7)
                    }
                }
            }
            addText(card, "Only live frequencies are selectable. Kernel-managed bandwidth boosts remain untouched.",
                MUTED, 11f, top = 8)
        }

        addCard(container, "DYNAMIC FSYNC", "Optional kernel write policy") { card ->
            val fsyncState = data.advancedKernel.dynamicFsyncEnabled
            if (data.advancedKernel.dynamicFsyncPath == null || fsyncState == null) {
                addUnsupported(card, "Dynamic Fsync runtime node")
            } else {
                val row = LinearLayout(context).apply {
                    orientation = HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                val labels = LinearLayout(context).apply { orientation = VERTICAL }
                addText(labels, "Defer fsync while screen is on", TEXT, 13f, bold = true)
                addText(labels, if (fsyncState) "Enabled" else "Disabled", MUTED, 11f, top = 3)
                row.addView(labels, LayoutParams(0, WRAP_CONTENT, 1f))
                val toggle = Switch(context).apply {
                    isChecked = fsyncState
                    isEnabled = data.root.available
                    contentDescription = t("Dynamic Fsync")
                    setOnCheckedChangeListener { _, enabled -> onDynamicFsyncApply(enabled) }
                }
                row.addView(toggle, LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
                card.addView(row, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            }
            addText(
                card,
                "Risk: fsync/fdatasync may be deferred until screen-off or reboot. A crash or forced power loss while the screen is on can lose recent writes or corrupt data.",
                WARNING,
                11f,
                top = 9
            )
        }

        addCard(container, "VOLTAGE / CPR") {
            if (data.regulators.isEmpty()) {
                addUnsupported(it, "Regulator voltage telemetry")
            } else {
                data.regulators.forEach { regulator ->
                    addText(it, regulator.name, SECONDARY, 12f, bold = true, top = 7)
                    addLiveInfoRow(
                        it,
                        "Current voltage",
                        regulator.voltageMicrovolts?.let { value -> String.format(Locale.US, "%.3f V", value / 1_000_000.0) } ?: "Unsupported"
                    ) { current ->
                        current.regulators.firstOrNull { entry -> entry.name == regulator.name }?.voltageMicrovolts
                            ?.let { value -> String.format(Locale.US, "%.3f V", value / 1_000_000.0) } ?: "Unsupported"
                    }
                    addLiveInfoRow(
                        it,
                        "Voltage range",
                        listOfNotNull(
                            regulator.minimumMicrovolts?.let { value -> String.format(Locale.US, "%.3f", value / 1_000_000.0) },
                            regulator.maximumMicrovolts?.let { value -> String.format(Locale.US, "%.3f", value / 1_000_000.0) }
                        ).takeIf { values -> values.isNotEmpty() }?.joinToString(" – ")?.plus(" V") ?: "Unsupported"
                    ) { current ->
                        current.regulators.firstOrNull { entry -> entry.name == regulator.name }?.let { entry ->
                            listOfNotNull(
                                entry.minimumMicrovolts?.let { value -> String.format(Locale.US, "%.3f", value / 1_000_000.0) },
                                entry.maximumMicrovolts?.let { value -> String.format(Locale.US, "%.3f", value / 1_000_000.0) }
                            ).takeIf { values -> values.isNotEmpty() }?.joinToString(" – ")?.plus(" V") ?: "Unsupported"
                        } ?: "Unsupported"
                    }
                    addLiveInfoRow(it, "State", regulator.state ?: "Unknown") { current ->
                        current.regulators.firstOrNull { entry -> entry.name == regulator.name }?.state ?: "Unknown"
                    }
                }
            }
            addText(
                it,
                "CPU/GPU voltage is managed by Qualcomm CPR, regulator rails and DTB corner tables. No validated runtime undervolt interface is exposed by this kernel build; this section is read-only.",
                WARNING,
                11f,
                top = 9
            )
            addText(
                it,
                "KonaBess modifies boot-image DTB data and its published device list does not include SDM450. Do not flash its output on M11/A11.",
                MUTED,
                11f,
                top = 7
            )
        }
    }

    private fun renderGpu(container: LinearLayout, data: DeviceSnapshot) {
        addPageHeading(container, "GPU telemetry", "GPU and devfreq telemetry (read-only).")
        addCard(container, "GPU STATUS", data.gpu.source ?: "No KGSL / GPU node detected") {
            addLiveInfoRow(it, "Current frequency", DeviceInfo.frequencyHz(data.gpu.currentFrequencyHz)) { DeviceInfo.frequencyHz(it.gpu.currentFrequencyHz) }
            addLiveInfoRow(it, "Utilization", data.gpu.utilizationPercent?.let { value -> "$value%" } ?: "Unsupported") { current ->
                current.gpu.utilizationPercent?.let { value -> "$value%" } ?: "Unsupported"
            }
            addInfoRow(it, "Available OPP entries", data.gpu.availableFrequenciesHz.size.toString())
        }
        addCard(container, "GPU SAFETY POLICY") {
            addText(it, "GPU OPPs, voltage, regulator and corner-scaling controls are not modified.",
                SECONDARY, 13f)
            addText(it, "Requires a readable KGSL or devfreq node.",
                MUTED, 11f, top = 9)
        }
    }

    private fun renderFabric(container: LinearLayout, data: DeviceSnapshot) {
        addPageHeading(container, "CCI & DDR fabric", "CCI and DDR devfreq readings.")
        if (data.devfreq.isEmpty()) {
            addCard(container, "Fabric telemetry unavailable") {
                addUnsupported(it, "No supported CCI/DDR devfreq node found.")
            }
        } else {
            data.devfreq.forEach { item ->
                addCard(container, item.name, item.path) {
                    addLiveInfoRow(it, "Current frequency", DeviceInfo.frequencyHz(item.currentFrequencyHz)) { current ->
                        DeviceInfo.frequencyHz(current.devfreq.firstOrNull { entry -> entry.path == item.path }?.currentFrequencyHz)
                    }
                    addLiveInfoRow(it, "Governor", item.governor ?: "Unsupported") { current ->
                        current.devfreq.firstOrNull { entry -> entry.path == item.path }?.governor ?: "Unsupported"
                    }
                    addInfoRow(it, "Available frequency entries", item.availableFrequenciesHz.size.toString())
                }
            }
        }
        addCard(container, "KERNEL-OWNED BOOST") {
            addText(it, "Kernel-managed boosts remain untouched. This page only reads devfreq state.",
                SECONDARY, 12f)
        }
    }

    private fun renderMemory(container: LinearLayout, data: DeviceSnapshot) {
        addPageHeading(container, "Memory", "RAM and ZRAM telemetry.")
        addCard(container, "RAM") {
            val used = DeviceInfo.memoryUsedPercent(data)
            addLiveInfoRow(it, "Total", data.memory.totalKb?.let { value -> formatKb(value) } ?: "Unsupported") { current -> current.memory.totalKb?.let { value -> formatKb(value) } ?: "Unsupported" }
            addLiveInfoRow(it, "Available", data.memory.availableKb?.let { value -> formatKb(value) } ?: "Unsupported") { current -> current.memory.availableKb?.let { value -> formatKb(value) } ?: "Unsupported" }
            addLiveInfoRow(it, "Used", used?.let { "$it%" } ?: "Unsupported") { current -> DeviceInfo.memoryUsedPercent(current)?.let { value -> "$value%" } ?: "Unsupported" }
            if (used != null) addLiveProgress(it, used) { current -> DeviceInfo.memoryUsedPercent(current) }
        }
        addCard(container, "ZRAM") {
            addLiveInfoRow(it, "Configured disk size", DeviceInfo.bytes(data.memory.zramDiskBytes)) { DeviceInfo.bytes(it.memory.zramDiskBytes) }
            addLiveInfoRow(it, "Original data", DeviceInfo.bytes(data.memory.zramOriginalBytes)) { DeviceInfo.bytes(it.memory.zramOriginalBytes) }
            addLiveInfoRow(it, "Compressed data", DeviceInfo.bytes(data.memory.zramCompressedBytes)) { DeviceInfo.bytes(it.memory.zramCompressedBytes) }
            addLiveInfoRow(it, "Memory consumed", DeviceInfo.bytes(data.memory.zramMemoryBytes)) { DeviceInfo.bytes(it.memory.zramMemoryBytes) }
            addText(it, "ZRAM settings are read-only.", MUTED, 11f, top = 8)
        }
        addCard(container, "KERNEL OPTIMIZATION TELEMETRY") { card ->
            val extra = data.kernelExtras
            addLiveInfoRow(card, "ZRAM compression algorithm", extra.zramCompressionAlgorithm ?: "Unsupported") { it.kernelExtras.zramCompressionAlgorithm ?: "Unsupported" }
            addLiveInfoRow(
                card,
                "Available ZRAM algorithms",
                extra.availableZramCompressionAlgorithms.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "Unsupported"
            ) { current -> current.kernelExtras.availableZramCompressionAlgorithms.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "Unsupported" }
            addLiveInfoRow(card, "KSM status", when (extra.ksmEnabled) {
                true -> "Enabled"
                false -> "Disabled"
                null -> if (extra.ksmSupported) "Unknown" else "Unsupported"
            }) { current ->
                when (current.kernelExtras.ksmEnabled) {
                    true -> "Enabled"
                    false -> "Disabled"
                    null -> if (current.kernelExtras.ksmSupported) "Unknown" else "Unsupported"
                }
            }
            addLiveInfoRow(card, "KSM shared pages", extra.ksmPagesShared?.toString() ?: "Unsupported") { it.kernelExtras.ksmPagesShared?.toString() ?: "Unsupported" }
            addLiveInfoRow(card, "KSM sharing pages", extra.ksmPagesSharing?.toString() ?: "Unsupported") { it.kernelExtras.ksmPagesSharing?.toString() ?: "Unsupported" }
            addLiveInfoRow(card, "Entropy available", extra.entropyAvailable?.toString() ?: "Unsupported") { it.kernelExtras.entropyAvailable?.toString() ?: "Unsupported" }
            addLiveInfoRow(card, "Entropy read threshold", extra.entropyReadWakeupThreshold?.toString() ?: "Unsupported") { it.kernelExtras.entropyReadWakeupThreshold?.toString() ?: "Unsupported" }
            addLiveInfoRow(card, "Entropy write threshold", extra.entropyWriteWakeupThreshold?.toString() ?: "Unsupported") { it.kernelExtras.entropyWriteWakeupThreshold?.toString() ?: "Unsupported" }
            addText(card, "Kernel optimization interfaces are shown as read-only telemetry.", MUTED, 11f, top = 8)
        }
        addCard(container, "VIRTUAL MEMORY TUNING", "Runtime VM parameters · current boot session") { card ->
            val swappiness = data.memory.swappiness
            val cachePressure = data.memory.vfsCachePressure
            addLiveInfoRow(card, "Current swappiness", swappiness?.toString() ?: "Unsupported") { it.memory.swappiness?.toString() ?: "Unsupported" }
            addLiveInfoRow(card, "Current VFS cache pressure", cachePressure?.toString() ?: "Unsupported") { it.memory.vfsCachePressure?.toString() ?: "Unsupported" }
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
                addUnsupported(card, "VM controls unavailable.")
            }
            if (!data.root.available) addText(card, "Root permission is required to apply changes.", WARNING, 11f, top = 8)
        }
    }

    private fun renderThermal(container: LinearLayout, data: DeviceSnapshot) {
        addPageHeading(container, "Thermal sensors", "Detected zones; thermal policy is read-only.")
        if (data.thermalSensors.isEmpty()) {
            addCard(container, "Thermal interface unavailable") {
                addUnsupported(it, "No thermal sensor found.")
            }
        } else {
            data.thermalSensors.forEach { sensor ->
                addCard(container, sensor.name) {
                    addLiveText(
                        it,
                        String.format(Locale.US, "%.1f °C", sensor.temperatureCelsius),
                        if (sensor.temperatureCelsius >= 55.0) WARNING else ACCENT,
                        25f,
                        { current ->
                            current.thermalSensors.firstOrNull { entry -> entry.name == sensor.name }?.let { entry ->
                                String.format(Locale.US, "%.1f °C", entry.temperatureCelsius)
                            } ?: "Unsupported"
                        },
                        { current ->
                            current.thermalSensors.firstOrNull { entry -> entry.name == sensor.name }
                                ?.let { entry -> if (entry.temperatureCelsius >= 55.0) WARNING else ACCENT }
                        }
                    ).apply { typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) }
                }
            }
        }
        addCard(container, "THERMAL SAFETY") {
            addText(it, "Thermal throttling, trip points and shutdown protection remain kernel-managed.",
                SECONDARY, 12f)
        }
    }

    private fun renderIo(container: LinearLayout, data: DeviceSnapshot) {
        addPageHeading(container, "I/O scheduler", "Available schedulers per device.")
        addCard(container, "Cumulative disk sectors") { card ->
            addLiveInfoRow(card, "Read sectors", data.ioUsage.sectorsRead?.toString() ?: "Unsupported") {
                it.ioUsage.sectorsRead?.toString() ?: "Unsupported"
            }
            addLiveInfoRow(card, "Written sectors", data.ioUsage.sectorsWritten?.toString() ?: "Unsupported") {
                it.ioUsage.sectorsWritten?.toString() ?: "Unsupported"
            }
        }
        if (!data.root.available) addRootRequired(container)

        if (data.ioSchedulers.isEmpty()) {
            addCard(container, "I/O scheduler interfaces unavailable") {
                addUnsupported(it, "No supported scheduler found.")
            }
        } else {
            data.ioSchedulers.forEach { scheduler ->
                addCard(container, scheduler.deviceName, scheduler.path) { card ->
                    addLiveInfoRow(card, "Active scheduler", scheduler.activeScheduler ?: "Unknown") { current ->
                        current.ioSchedulers.firstOrNull { it.path == scheduler.path }?.activeScheduler ?: "Unknown"
                    }
                    addLiveInfoRow(card, "Available", scheduler.availableSchedulers.joinToString(", ")) { current ->
                        current.ioSchedulers.firstOrNull { it.path == scheduler.path }
                            ?.availableSchedulers?.joinToString(", ") ?: "Unsupported"
                    }
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
        addPageHeading(container, "System & diagnostics", "Battery, storage, network and dmesg.")
        addCard(container, "BATTERY") {
            addLiveInfoRow(it, "Level", data.battery.levelPercent?.let { value -> "$value%" } ?: "Unsupported") { current ->
                current.battery.levelPercent?.let { value -> "$value%" } ?: "Unsupported"
            }
            addLiveInfoRow(it, "State", when (data.battery.charging) {
                true -> "Charging / full"
                false -> "Not charging"
                null -> "Unsupported"
            }) { current ->
                when (current.battery.charging) {
                    true -> "Charging / full"
                    false -> "Not charging"
                    null -> "Unsupported"
                }
            }
            addLiveInfoRow(it, "Temperature", data.battery.temperatureCelsius?.let { value ->
                String.format(Locale.US, "%.1f °C", value)
            } ?: "Unsupported") { current ->
                current.battery.temperatureCelsius?.let { value ->
                    String.format(Locale.US, "%.1f °C", value)
                } ?: "Unsupported"
            }
            addLiveInfoRow(it, "Voltage", data.battery.voltageMillivolts?.let { value -> "$value mV" } ?: "Unsupported") { current ->
                current.battery.voltageMillivolts?.let { value -> "$value mV" } ?: "Unsupported"
            }
            addLiveInfoRow(it, "Technology", data.battery.technology ?: "Unsupported") {
                it.battery.technology ?: "Unsupported"
            }
        }
        addCard(container, "STORAGE") {
            addLiveInfoRow(it, "Data partition total", DeviceInfo.bytes(data.storage.totalBytes)) {
                DeviceInfo.bytes(it.storage.totalBytes)
            }
            addLiveInfoRow(it, "Available", DeviceInfo.bytes(data.storage.availableBytes)) {
                DeviceInfo.bytes(it.storage.availableBytes)
            }
            val totalBytes = data.storage.totalBytes
            val availableBytes = data.storage.availableBytes
            val percent = if (totalBytes != null && availableBytes != null && totalBytes > 0L) {
                ((availableBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)
            } else null
            addLiveInfoRow(it, "Available space", percent?.let { value -> "$value%" } ?: "Unsupported") { current ->
                val total = current.storage.totalBytes
                val free = current.storage.availableBytes
                if (total != null && free != null && total > 0L) {
                    (((free * 100L) / total).toInt().coerceIn(0, 100)).toString() + "%"
                } else "Unsupported"
            }
        }
        addCard(container, "NETWORK") {
            addLiveInfoRow(it, "Total RX", DeviceInfo.bytes(data.networkRxBytes)) {
                DeviceInfo.bytes(it.networkRxBytes)
            }
            addLiveInfoRow(it, "Total TX", DeviceInfo.bytes(data.networkTxBytes)) {
                DeviceInfo.bytes(it.networkTxBytes)
            }
            addText(it, "TrafficStats totals since counter reset.", MUTED, 11f, top = 7)
            addText(it, "TCP congestion control", SECONDARY, 12f, bold = true, top = 12)
            addLiveInfoRow(it, "Current algorithm", data.tcpCongestionControl ?: "Unsupported") {
                it.tcpCongestionControl ?: "Unsupported"
            }
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
            addText(it, "Read kernel ring buffer (root required).", SECONDARY, 12f)
            cardButton(it, "View dmesg") { onShowLogs() }
        }
        addCard(container, "BUILD INFORMATION") {
            addInfoRow(it, "Kernel release", data.kernelVersion)
            addInfoRow(it, "Device", data.deviceCode)
            addInfoRow(it, "Platform", data.platform)
            addLiveInfoRow(it, "Uptime", DeviceInfo.uptime(data.uptimeSeconds)) {
                DeviceInfo.uptime(it.uptimeSeconds)
            }
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
            addText(it, "Root is required for write controls. Telemetry remains read-only without root.",
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

    private fun addLiveSwitchRow(
        parent: LinearLayout,
        label: String,
        initialChecked: Boolean,
        enabled: Boolean,
        onChanged: (Boolean) -> Unit,
        valueProvider: (DeviceSnapshot) -> Boolean?
    ) {
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
        var bindingSnapshot = false
        val toggle = Switch(context).apply {
            isChecked = initialChecked
            isEnabled = enabled
            setOnCheckedChangeListener { _, checked ->
                if (!bindingSnapshot) onChanged(checked)
            }
        }
        row.addView(toggle, LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        parent.addView(row, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        liveBindings += { current ->
            valueProvider(current)?.let { checked ->
                if (toggle.isChecked != checked) {
                    bindingSnapshot = true
                    toggle.isChecked = checked
                    bindingSnapshot = false
                }
            }
        }
    }

    private fun addLiveInfoRow(
        parent: LinearLayout,
        label: String,
        initialValue: String,
        valueProvider: (DeviceSnapshot) -> String
    ) {
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
        val valueView = TextView(context).apply {
            text = t(initialValue)
            textSize = 12f
            setTextColor(TEXT)
            gravity = Gravity.END
            maxWidth = dp(210)
        }
        row.addView(valueView, LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        parent.addView(row, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        liveBindings += { current ->
            val next = t(valueProvider(current))
            if (valueView.text.toString() != next) valueView.text = next
        }
    }

    private fun addLiveText(
        parent: LinearLayout,
        initialValue: String,
        initialColor: Int,
        size: Float,
        valueProvider: (DeviceSnapshot) -> String,
        colorProvider: (DeviceSnapshot) -> Int?
    ): TextView {
        val valueView = addText(parent, initialValue, initialColor, size, bold = true)
        liveBindings += { current ->
            val next = t(valueProvider(current))
            if (valueView.text.toString() != next) valueView.text = next
            colorProvider(current)?.let(valueView::setTextColor)
        }
        return valueView
    }

    private fun addUnsupported(parent: LinearLayout, label: String) {
        addText(parent, t(label) + " · " + t("Unsupported"), MUTED, 12f, top = 5)
    }

    private fun addSchedutilRateControl(
        parent: LinearLayout,
        tunablesPath: String,
        tunable: String,
        label: String,
        current: Long,
        rootAvailable: Boolean
    ) {
        val presets = listOf(
            0L, 100L, 250L, 500L, 1_000L, 2_000L, 5_000L,
            10_000L, 20_000L, 50_000L, 100_000L, 250_000L,
            500_000L, 1_000_000L
        )
        val values = (presets + current).distinct().sorted()
        addText(parent, label, SECONDARY, 12f, bold = true, top = 9)
        val labels = values.map { "$it µs" }
        val picker = spinner(labels, "$current µs")
        parent.addView(picker, LayoutParams(MATCH_PARENT, dp(44)))
        val actionLabel = if (tunable == "up_rate_limit_us") {
            "Apply up rate limit"
        } else {
            "Apply down rate limit"
        }
        parent.addView(
            actionButton(actionLabel, "Apply schedutil rate limit") {
                if (!rootAvailable) {
                    toast("Root permission is required.")
                } else {
                    picker.selectedItem?.toString()
                        ?.substringBefore(" ")
                        ?.toLongOrNull()
                        ?.takeIf { it in 0L..1_000_000L }
                        ?.let { value ->
                            onSchedutilTunableApply(tunablesPath, tunable, value.toString())
                        }
                }
            },
            LayoutParams(MATCH_PARENT, dp(42)).apply { topMargin = dp(4) }
        )
    }

    private fun addLiveProgress(
        parent: LinearLayout,
        initialValue: Int,
        valueProvider: (DeviceSnapshot) -> Int?
    ) {
        val usedView = View(context).apply { background = rounded(ACCENT, ACCENT, 8f) }
        val freeView = View(context).apply { background = rounded(BORDER, BORDER, 8f) }
        val holder = LinearLayout(context).apply {
            orientation = HORIZONTAL
            addView(usedView, LayoutParams(0, dp(6), initialValue.coerceIn(0, 100).coerceAtLeast(1).toFloat()))
            addView(freeView, LayoutParams(0, dp(6), (100 - initialValue).coerceAtLeast(0).toFloat()))
        }
        parent.addView(holder, LayoutParams(MATCH_PARENT, dp(6)).apply { topMargin = dp(9) })
        liveBindings += { current ->
            val next = valueProvider(current)?.coerceIn(0, 100) ?: 0
            (usedView.layoutParams as? LayoutParams)?.let { params ->
                params.weight = next.coerceAtLeast(1).toFloat()
                usedView.layoutParams = params
            }
            (freeView.layoutParams as? LayoutParams)?.let { params ->
                params.weight = (100 - next).coerceAtLeast(0).toFloat()
                freeView.layoutParams = params
            }
        }
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
