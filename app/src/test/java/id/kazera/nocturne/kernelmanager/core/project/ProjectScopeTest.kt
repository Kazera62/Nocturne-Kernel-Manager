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
        assertTrue(ProjectScope.CPU_TOPOLOGY_MONITORING_ENABLED)
        assertTrue(ProjectScope.KERNEL_EXTRAS_MONITORING_ENABLED)
        assertTrue(ProjectScope.BILINGUAL_UI_ENABLED)
        assertTrue(ProjectScope.CPU_CONTROLS_ENABLED)
        assertTrue(ProjectScope.IO_SCHEDULER_CONTROLS_ENABLED)
        assertTrue(ProjectScope.VM_CONTROLS_ENABLED)
        assertTrue(ProjectScope.TCP_CONGESTION_CONTROL_ENABLED)
        assertTrue(ProjectScope.ADVANCED_KERNEL_MENU_ENABLED)
        assertTrue(ProjectScope.CPU_HOTPLUG_CONTROLS_ENABLED)
        assertTrue(ProjectScope.DEVFREQ_LIMIT_CONTROLS_ENABLED)
        assertTrue(ProjectScope.GPU_WRITE_CONTROLS_ENABLED)
        assertTrue(ProjectScope.GPU_MAX_POWER_LEVEL_CONTROL_ENABLED)
        assertFalse(ProjectScope.GPU_OPP_TABLE_WRITES_ENABLED)
        assertFalse(ProjectScope.VOLTAGE_CONTROLS_ENABLED)
        assertFalse(ProjectScope.THERMAL_LIMIT_CONTROLS_ENABLED)
        assertFalse(ProjectScope.DDR_OVERCLOCK_CONTROLS_ENABLED)
    }
}
