package id.kazera.nocturne.kernelmanager.core.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectScopeTest {

    @Test
    fun targetFamiliesAreSamsungM11AndA11() {
        assertEquals("SM-M115 / SM-A115", ProjectScope.TARGET_DEVICE_FAMILIES)
        assertEquals("Qualcomm Snapdragon 450 (SDM450)", ProjectScope.TARGET_PLATFORM)
    }

    @Test
    fun privilegedAndDeviceFeaturesRemainOutOfFoundation() {
        assertTrue(ProjectScope.FOUNDATION_ONLY)
        assertFalse(ProjectScope.ROOT_COMMANDS_ENABLED)
        assertFalse(ProjectScope.KERNEL_CONTROLS_ENABLED)
        assertFalse(ProjectScope.DEVICE_MONITORING_ENABLED)
    }
}
