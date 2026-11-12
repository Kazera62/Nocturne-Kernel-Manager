package id.kazera.nocturne.kernelmanager.core.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectScopeTest {

    @Test
    fun declaresM11AndA11Target() {
        assertEquals("SM-M115 / SM-A115", ProjectScope.TARGET_DEVICE_FAMILIES)
        assertEquals("Qualcomm Snapdragon 450 (SDM450)", ProjectScope.TARGET_PLATFORM)
    }

    @Test
    fun enablesOnlyTheImplementedControlSurfaces() {
        assertTrue(ProjectScope.ROOT_BRIDGE_ENABLED)
        assertTrue(ProjectScope.DEVICE_MONITORING_ENABLED)
        assertTrue(ProjectScope.CPU_CONTROLS_ENABLED)
        assertTrue(ProjectScope.IO_SCHEDULER_CONTROLS_ENABLED)
        assertFalse(ProjectScope.GPU_WRITE_CONTROLS_ENABLED)
        assertFalse(ProjectScope.VOLTAGE_CONTROLS_ENABLED)
        assertFalse(ProjectScope.THERMAL_LIMIT_CONTROLS_ENABLED)
        assertFalse(ProjectScope.DDR_OVERCLOCK_CONTROLS_ENABLED)
    }
}
