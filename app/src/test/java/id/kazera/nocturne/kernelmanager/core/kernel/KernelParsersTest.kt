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
    fun acceptsOnlySupportedSchedutilTunablesPaths() {
        listOf(
            "/sys/devices/system/cpu/cpufreq/policy0/schedutil",
            "/sys/devices/system/cpu/cpufreq/policy12/schedutil",
            "/sys/devices/system/cpu/cpu0/cpufreq/schedutil",
            "/sys/devices/system/cpu/cpu7/cpufreq/schedutil",
            "/sys/devices/system/cpu/cpufreq/schedutil"
        ).forEach { assertEquals(true, KernelParsers.isSupportedSchedutilTunablesPath(it)) }

        listOf(
            "/sys/devices/system/cpu/cpufreq/policy0",
            "/sys/devices/system/cpu/cpufreq/policy99/schedutil/../scaling_min_freq",
            "/sys/devices/system/cpu/cpufreq/policyx/schedutil",
            "/sys/devices/system/cpu/cpu8/cpufreq/schedutil",
            "/sys/devices/system/cpu/cpufreq/interactive",
            "/proc/sys/kernel/dyn_fsync/Dyn_fsync_active"
        ).forEach { assertEquals(false, KernelParsers.isSupportedSchedutilTunablesPath(it)) }
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
    @Test
    fun allowsOnlyKernelReadRootsAndRejectsTraversal() {
        assertEquals(true, KernelParsers.isAllowedKernelReadPath("/sys/devices/system/cpu/online"))
        assertEquals(true, KernelParsers.isAllowedKernelReadPath("/proc/meminfo"))
        assertEquals(true, KernelParsers.isAllowedKernelReadPath("/vendor/etc/fstab"))
        assertEquals(false, KernelParsers.isAllowedKernelReadPath("/data/local/tmp/file"))
        assertEquals(false, KernelParsers.isAllowedKernelReadPath("/sys/../data/local/tmp"))
        assertEquals(false, KernelParsers.isAllowedKernelReadPath("/proc/version\u0000suffix"))
    }

    @Test
    fun acceptsOnlyDirectDevfreqClassNodes() {
        assertEquals(true, KernelParsers.isSupportedDevfreqPath("/sys/class/devfreq/soc:qcom,cpubw-cpufreq"))
        assertEquals(true, KernelParsers.isSupportedDevfreqPath("/sys/class/devfreq/cci"))
        assertEquals(false, KernelParsers.isSupportedDevfreqPath("/sys/class/devfreq/../cpubw"))
        assertEquals(false, KernelParsers.isSupportedDevfreqPath("/sys/class/devfreq/cci/min_freq"))
        assertEquals(false, KernelParsers.isSupportedDevfreqPath("/proc/sys/devfreq/cci"))
    }

    @Test
    fun acceptsOnlyKnownFabricDevfreqNames() {
        assertEquals(true, KernelParsers.isSupportedDevfreqName("qcom,cci"))
        assertEquals(true, KernelParsers.isSupportedDevfreqName("cpubw-cpufreq"))
        assertEquals(true, KernelParsers.isSupportedDevfreqName("soc-ddr"))
        assertEquals(false, KernelParsers.isSupportedDevfreqName("kgsl-3d0"))
        assertEquals(false, KernelParsers.isSupportedDevfreqName("unknown-device"))
    }

    @Test
    fun keepsAtLeastTwoCoresOnlineWhenDisablingSecondaryCores() {
        assertEquals(true, KernelParsers.canChangeCpuOnline(1, false, 3))
        assertEquals(false, KernelParsers.canChangeCpuOnline(1, false, 2))
        assertEquals(true, KernelParsers.canChangeCpuOnline(1, true, 1))
        assertEquals(false, KernelParsers.canChangeCpuOnline(0, false, 4))
        assertEquals(false, KernelParsers.canChangeCpuOnline(8, true, 4))
    }

}
