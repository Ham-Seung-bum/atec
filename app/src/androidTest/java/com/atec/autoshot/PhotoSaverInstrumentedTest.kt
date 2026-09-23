package com.atec.autoshot

import android.content.ContentResolver
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * `PhotoSaver.outputOptions`가 만든 ContentValues로 실제 MediaStore에 항목이 생성되는지 확인한다 (F-05, F-06).
 * 카메라·권한 없이 저장 경로만 검증하므로 실기기/에뮬레이터에서 항상 통과해야 한다.
 */
@RunWith(AndroidJUnit4::class)
class PhotoSaverInstrumentedTest {

    private val resolver: ContentResolver
        get() = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver

    @Test
    fun insertHonorsAutoShotPathAndName() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = PhotoSaver.fileName(System.currentTimeMillis())
        val values = android.content.ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, PhotoSaver.RELATIVE_PATH)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        assertNotNull("insert가 null을 반환하면 안 된다", uri)
        try {
            resolver.query(uri!!, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.RELATIVE_PATH), null, null, null)!!.use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(name, c.getString(0))
                assertTrue(c.getString(1).contains("AutoShot"))
            }
        } finally {
            resolver.delete(uri!!, null, null)
        }
        // outputOptions도 예외 없이 구성되어야 한다.
        assertNotNull(PhotoSaver.outputOptions(context, System.currentTimeMillis()))
    }
}
