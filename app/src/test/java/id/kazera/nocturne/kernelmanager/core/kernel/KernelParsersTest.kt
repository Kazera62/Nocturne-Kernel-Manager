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
    fun parsesActiveIoScheduler() {
        val result = KernelParsers.schedulerList("none [mq-deadline] bfq")
        assertEquals("mq-deadline", result.first)
        assertEquals(listOf("none", "mq-deadline", "bfq"), result.second)
    }
}
