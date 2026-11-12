package id.kazera.nocturne.kernelmanager.ui.home

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.content.res.ColorStateList
import android.widget.ArrayAdapter
import android.widget.HorizontalScrollView
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
import java.util.Locale

class HomeScreen(
    context: Context,
    private val onRefresh: () -> Unit,
    private val onCpuApply: (CpuPolicyInfo, String?, Long, Long) -> Unit,
    private val onIoApply: (IoSchedulerInfo, String) -> Unit,
    private val onVmApply: (Int, Int) -> Unit,
    private val onTcpApply: (String) -> Unit,
    private val onShowLogs: () -> Unit
) : LinearLayout(context) {

    private enum class Page(val label: String) {
        OVERVIEW("Overview"),
        CPU("CPU"),
        GPU("GPU"),
        FABRIC("CCI / DDR"),
        MEMORY("Memory"),
        THERMAL("Thermal"),
        IO("I/O"),
        SYSTEM("System")
    }

    private var selectedPage = Page.OVERVIEW
    private var snapshot: DeviceSnapshot? = null
    private var currentScroll: ScrollView? = null

    init {
        orientation = VERTICAL
        setBackgroundColor(BACKGROUND)
        render()
    }

    fun update(value: DeviceSnapshot, forceRender: Boolean = false) {
        snapshot = value
        if (forceRender || (selectedPage != Page.CPU && selectedPage != Page.IO &&
                selectedPage != Page.MEMORY && selectedPage != Page.SYSTEM)) {
            render(restoreScroll = true)
        }
    }

    private fun render(restoreScroll: Boolean = false) {
        val previousScroll = if (restoreScroll) currentScroll?.scrollY ?: 0 else 0
        removeAllViews()

        addView(buildHeader(), LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        addView(buildNavigation(), LayoutParams(MATCH_PARENT, dp(54)))

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

        val data = snapshot
        if (data == null) {
            addCard(content, "Initializing device interface") {
                addText(it, "Reading device information and probing available kernel nodes…", SECONDARY, 14f)
                addText(it, "No sample or placeholder telemetry is displayed.", MUTED, 12f, top = 8)
            }
        } else {
            when (selectedPage) {
                Page.OVERVIEW -> renderOverview(content, data)
                Page.CPU -> renderCpu(content, data)
                Page.GPU -> renderGpu(content, data)
                Page.FABRIC -> renderFabric(content, data)
                Page.MEMORY -> renderMemory(content, data)
                Page.THERMAL -> renderThermal(content, data)
                Page.IO -> renderIo(content, data)
                Page.SYSTEM -> renderSystem(content, data)
            }
        }

        if (previousScroll > 0) scroll.post { scroll.scrollTo(0, previousScroll) }
    }

    private fun buildHeader(): View {
        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(15), dp(16), dp(12))
            setBackgroundColor(BACKGROUND)
        }

        val titleStack = LinearLayout(context).apply { orientation = VERTICAL }
        val title = addText(titleStack, "NOCTURNE", TEXT, 20f, bold = true)
        title.letterSpacing = 0.12f
        addText(titleStack, "KERNEL MANAGER  ·  M11 / A11", SECONDARY, 10f, top = 3)
        header.addView(titleStack, LayoutParams(0, WRAP_CONTENT, 1f))

        val root = snapshot?.root
        val rootText = if (root?.available == true) "ROOT READY" else "ROOT OFF"
        val rootColor = if (root?.available == true) ACCENT else WARNING
        header.addView(chip(rootText, rootColor), LayoutParams(WRAP_CONTENT, dp(30)))
        val refresh = actionButton("↻", "Refresh") { onRefresh() }.apply {
            textSize = 20f
            setPadding(dp(8), 0, dp(8), 0)
        }
        val refreshParams = LayoutParams(dp(42), dp(38)).apply { leftMargin = dp(8) }
        header.addView(refresh, refreshParams)
        return header
    }

    private fun buildNavigation(): View {
        val horizontal = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            clipToPadding = false
            setPadding(dp(12), dp(4), dp(12), dp(4))
        }
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        Page.values().forEach { page ->
            val selected = page == selectedPage
            val button = TextView(context).apply {
                text = page.label
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                setTextColor(if (selected) BACKGROUND else SECONDARY)
                gravity = Gravity.CENTER
                setPadding(dp(13), 0, dp(13), 0)
                background = rounded(if (selected) ACCENT else SURFACE, if (selected) ACCENT else BORDER, 22f)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    selectedPage = page
                    render()
                }
            }
            val params = LayoutParams(WRAP_CONTENT, dp(38)).apply { rightMargin = dp(7) }
            row.addView(button, params)
        }
        horizontal.addView(row)
        return horizontal
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

        addCard(container, "ROOT ACCESS", data.root.provider) {
            addText(it, if (data.root.available) "Privileged shell verified" else "Root permission not available",
                if (data.root.available) ACCENT else WARNING, 15f, bold = true)
            addText(it, data.root.detail, SECONDARY, 12f, top = 5)
            addText(it, "Uses the standard su command interface. Manager-specific APIs are not required for basic commands.",
                MUTED, 11f, top = 9)
        }

        addCard(container, "PROCESSOR", "Detected CPU frequency policies") {
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

    private fun renderCpu(container: LinearLayout, data: DeviceSnapshot) {
        addPageHeading(container, "CPU controls", "Only values exposed by each cpufreq policy are selectable.")
        if (!data.root.available) addRootRequired(container)
        if (data.cpuPolicies.isEmpty()) {
            addCard(container, "CPU interface unavailable") { addUnsupported(it, "No readable cpufreq policy was found.") }
            return
        }
        data.cpuPolicies.forEach { policy -> addCpuPolicyCard(container, data, policy) }
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

            if (policy.availableGovernors.isNotEmpty()) {
                addText(card, "Governor", SECONDARY, 12f, bold = true, top = 12)
                val governorSpinner = spinner(policy.availableGovernors, policy.governor)
                card.addView(governorSpinner, LayoutParams(MATCH_PARENT, dp(46)))
                val frequencies = policy.availableFrequenciesKHz
                if (frequencies.isNotEmpty()) {
                    addText(card, "Minimum frequency", SECONDARY, 12f, bold = true, top = 8)
                    val minSpinner = frequencySpinner(frequencies, policy.minFrequencyKHz ?: frequencies.first())
                    card.addView(minSpinner, LayoutParams(MATCH_PARENT, dp(46)))
                    addText(card, "Maximum frequency", SECONDARY, 12f, bold = true, top = 8)
                    val maxSpinner = frequencySpinner(frequencies, policy.maxFrequencyKHz ?: frequencies.last())
                    card.addView(maxSpinner, LayoutParams(MATCH_PARENT, dp(46)))
                    card.addView(
                        actionButton("Apply CPU settings", "Apply governor and limits") {
                            if (!data.root.available) {
                                toast("Root permission is required.")
                            } else {
                                val min = selectedFrequency(minSpinner)
                                val max = selectedFrequency(maxSpinner)
                                if (min == null || max == null) toast("Kernel frequency values are unavailable.")
                                else onCpuApply(policy, governorSpinner.selectedItem?.toString(), min, max)
                            }
                        },
                        LayoutParams(MATCH_PARENT, dp(46)).apply { topMargin = dp(12) }
                    )
                } else {
                    addText(card, "Frequency table unsupported; frequency controls are hidden until valid OPP values are exposed.",
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
        addCard(container, "VIRTUAL MEMORY TUNING", "Runtime VM parameters · current boot session") { card ->
            val swappiness = data.memory.swappiness
            val cachePressure = data.memory.vfsCachePressure
            addInfoRow(card, "Current swappiness", swappiness?.toString() ?: "Unsupported")
            addInfoRow(card, "Current VFS cache pressure", cachePressure?.toString() ?: "Unsupported")
            if (swappiness in 0..100 && cachePressure in 10..200) {
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
                    val picker = spinner(scheduler.availableSchedulers, scheduler.activeScheduler)
                    card.addView(picker, LayoutParams(MATCH_PARENT, dp(46)))
                    card.addView(
                        actionButton("Apply scheduler", "Apply I/O scheduler") {
                            if (!data.root.available) toast("Root permission is required.")
                            else picker.selectedItem?.toString()?.let { onIoApply(scheduler, it) }
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
            val percent = if (data.storage.totalBytes > 0L) {
                ((data.storage.availableBytes * 100L) / data.storage.totalBytes).toInt().coerceIn(0, 100)
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
                val picker = spinner(data.availableTcpCongestionControls, data.tcpCongestionControl)
                it.addView(picker, LayoutParams(MATCH_PARENT, dp(46)))
                cardButton(it, "Apply TCP algorithm") {
                    if (!data.root.available) toast("Root permission is required.")
                    else picker.selectedItem?.toString()?.let { selected -> onTcpApply(selected) }
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
            text = value
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
                text = label
                textSize = 12f
                setTextColor(SECONDARY)
            },
            LayoutParams(0, WRAP_CONTENT, 1f)
        )
        row.addView(
            TextView(context).apply {
                text = value
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
        addText(parent, label + " · Unsupported", MUTED, 12f, top = 5)
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
        val adapter = object : ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, options.toMutableList()) {
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
            text = label
            contentDescription = description
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
            text = label
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
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
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
