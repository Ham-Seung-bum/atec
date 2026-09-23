package com.atec.autoshot

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.TimeZone

class PhotoSaverTest {

    private val seoul = TimeZone.getTimeZone("Asia/Seoul")

    @Test
    fun fileNameFollowsSpecFormat() {
        // 2026-09-22T22:30:45.123Z = 2026-09-23 07:30:45.123 KST
        assertEquals("AUTOSHOT_20260923_073045_123.jpg", PhotoSaver.fileName(1_790_116_245_123L, seoul))
    }

    @Test
    fun millisecondsAreZeroPadded() {
        assertEquals("AUTOSHOT_20260923_073045_007.jpg", PhotoSaver.fileName(1_790_116_245_007L, seoul))
    }
}
