package id.kazera.nocturne.kernelmanager.core.kernel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KernelParsersTest {

    @Test
    fun parsesFrequencyTablesAndSortsUniqueValues() {
        assertEquals(
            listOf(300_000L, 576_000L, 1_200_000L),
            KernelParsers.frequencyList("1200000 300000 576000 300000")
        )
    }

    @Test
    fun parsesSparseCpuPresentLists() {
        assertEquals(listOf(0, 1, 2, 4, 7), KernelParsers.cpuList("0-2,4,7"))
        assertEquals(listOf(0, 2, 3, 4), KernelParsers.cpuList("0,2-4"))
        assertEquals(emptyList<Int>(), KernelParsers.cpuList("invalid,4-2,999-1000"))
    }

    @Test
    fun validatesSchedutilTunablesAgainstKnownBounds() {
        val frequencies = listOf(300_000L, 576_000L, 1_200_000L)
        assertEquals(true, KernelParsers.isValidSchedutilTunable("hispeed_load", 0, frequencies))
        assertEquals(true, KernelParsers.isValidSchedutilTunable("hispeed_load", 100, frequencies))
        assertEquals(false, KernelParsers.isValidSchedutilTunable("hispeed_load", 101, frequencies))
        assertEquals(true, KernelParsers.isValidSchedutilTunable("hispeed_freq", 576_000, frequencies))
        assertEquals(false, KernelParsers.isValidSchedutilTunable("hispeed_freq", 500_000, frequencies))
        assertEquals(true, KernelParsers.isValidSchedutilTunable("up_rate_limit_us", 1_000_000, frequencies))
        assertEquals(false, KernelParsers.isValidSchedutilTunable("down_rate_limit_us", 1_000_001, frequencies))
        assertEquals(true, KernelParsers.isValidSchedutilTunable("pl", 1, frequencies))
        assertEquals(false, KernelParsers.isValidSchedutilTunable("unknown", 0, frequencies))
    }

    @Test
    fun mapsKgslFrequenciesToDescendingPowerLevels() {
        val frequencies = listOf(133_300_000L, 216_000_000L, 400_000_000L, 650_000_000L, 824_000_000L)
        assertEquals(0, KernelParsers.gpuPowerLevelForFrequency(frequencies, 824_000_000L))
        assertEquals(3, KernelParsers.gpuPowerLevelForFrequency(frequencies, 216_000_000L))
        assertEquals(null, KernelParsers.gpuPowerLevelForFrequency(frequencies, 747_000_000L))
        assertEquals(650_000_000L, KernelParsers.gpuFrequencyForPowerLevel(frequencies, 1))
        assertEquals(null, KernelParsers.gpuFrequencyForPowerLevel(frequencies, 9))
    }

    @Test
    fun rejectsInvalidGpuUtilization() {
        assertNull(KernelParsers.gpuUtilization("not-a-counter"))
        assertNull(KernelParsers.gpuUtilization("100 0"))
        assertEquals(50, KernelParsers.gpuUtilization("50 100"))
        assertEquals(100, KernelParsers.gpuUtilization("150 100"))
    }

    @Test
    fun parsesThermalValuesInMillidegreesAndDegrees() {
        assertEquals(42.0, KernelParsers.thermalCelsius("42000") ?: 0.0, 0.01)
        assertEquals(42.0, KernelParsers.thermalCelsius("42") ?: 0.0, 0.01)
        assertNull(KernelParsers.thermalCelsius("not-a-temperature"))
    }

    @Test
    fun allowsOnlySecondaryCpuHotplugTargets() {
        (1..7).forEach { assertEquals(true, KernelParsers.isSupportedCpuHotplugCore(it)) }
        listOf(-1, 0, 8, 255).forEach {
            assertEquals(false, KernelParsers.isSupportedCpuHotplugCore(it))
        }
    }

    @Test
    fun acceptsOnlySupportedWholePhysicalBlockDevices() {
        listOf("mmcblk0", "mmcblk1", "sda", "sdb", "sdc", "nvme0n1", "nvme12n3")
            .forEach { assertEquals(true, KernelParsers.isSupportedIoDevice(it)) }

        listOf(
            "loop0", "ram0", "dm-0", "zram0", "sdd",
            "mmcblk0p1", "mmcblk0boot0", "sda1", "nvme0n1p1", "unknown"
        ).forEach { assertEquals(false, KernelParsers.isSupportedIoDevice(it)) }
    }

    @Test
    fun parsesActiveIoScheduler() {
        val result = KernelParsers.schedulerList("none [mq-deadline] bfq")
        assertEquals("mq-deadline", result.first)
        assertEquals(listOf("none", "mq-deadline", "bfq"), result.second)
    }
}
