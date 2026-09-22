package com.atec.autoshot

import android.view.Surface
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceRotationTest {

    @Test
    fun unknownOrientationDefaultsToNaturalRotation() {
        assertEquals(Surface.ROTATION_0, DeviceRotation.toSurfaceRotation(-1))
    }

    @Test
    fun portraitUp() {
        assertEquals(Surface.ROTATION_0, DeviceRotation.toSurfaceRotation(0))
        assertEquals(Surface.ROTATION_0, DeviceRotation.toSurfaceRotation(359))
        assertEquals(Surface.ROTATION_0, DeviceRotation.toSurfaceRotation(20))
    }

    @Test
    fun landscape() {
        // 기기를 왼쪽으로 눕히면(윗변이 왼쪽) 90도 → 화면은 ROTATION_270
        assertEquals(Surface.ROTATION_270, DeviceRotation.toSurfaceRotation(90))
        // 오른쪽으로 눕히면 270도 → ROTATION_90
        assertEquals(Surface.ROTATION_90, DeviceRotation.toSurfaceRotation(270))
    }

    @Test
    fun upsideDown() {
        assertEquals(Surface.ROTATION_180, DeviceRotation.toSurfaceRotation(180))
    }
}
