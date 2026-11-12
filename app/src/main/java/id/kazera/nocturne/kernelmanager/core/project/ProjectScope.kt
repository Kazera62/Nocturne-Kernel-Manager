package id.kazera.nocturne.kernelmanager.core.project

/**
 * Runtime capabilities implemented by this application release.
 * Hardware support is still determined independently from live node discovery.
 */
object ProjectScope {
    const val TARGET_DEVICE_FAMILIES = "SM-M115 / SM-A115"
    const val TARGET_PLATFORM = "Qualcomm Snapdragon 450 (SDM450)"

    const val ROOT_BRIDGE_ENABLED = true
    const val DEVICE_MONITORING_ENABLED = true
    const val CPU_CONTROLS_ENABLED = true
    const val IO_SCHEDULER_CONTROLS_ENABLED = true

    const val GPU_WRITE_CONTROLS_ENABLED = false
    const val VOLTAGE_CONTROLS_ENABLED = false
    const val THERMAL_LIMIT_CONTROLS_ENABLED = false
    const val DDR_OVERCLOCK_CONTROLS_ENABLED = false
}
