package com.atec.autoshot

import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureTimingTest {

    @Test
    fun waitsUntilMinimumWhenMeteringIsFast() {
        assertEquals(500L, CaptureTiming.remainingMinWait(0))
        assertEquals(300L, CaptureTiming.remainingMinWait(200))
    }

    @Test
    fun noExtraWaitAfterMinimum() {
        assertEquals(0L, CaptureTiming.remainingMinWait(500))
        assertEquals(0L, CaptureTiming.remainingMinWait(1_000))
    }

    @Test
    fun negativeElapsedIsClamped() {
        assertEquals(500L, CaptureTiming.remainingMinWait(-10))
    }
}
