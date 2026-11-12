package id.kazera.nocturne.kernelmanager.core.project

/**
 * Documents the target and boundaries of the initial application foundation.
 *
 * These flags deliberately remain disabled until the related implementation
 * is designed, reviewed, and added as a separate project phase.
 */
object ProjectScope {
    const val TARGET_DEVICE_FAMILIES = "SM-M115 / SM-A115"
    const val TARGET_PLATFORM = "Qualcomm Snapdragon 450 (SDM450)"

    const val FOUNDATION_ONLY = true
    const val ROOT_COMMANDS_ENABLED = false
    const val KERNEL_CONTROLS_ENABLED = false
    const val DEVICE_MONITORING_ENABLED = false
}
