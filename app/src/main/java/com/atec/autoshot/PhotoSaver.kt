package com.atec.autoshot

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** MediaStore 저장 위치와 파일명 규칙 (F-05, F-06). */
object PhotoSaver {

    /** `DCIM/AutoShot` — 갤러리 앱에서 "AutoShot" 앨범으로 보인다. */
    val RELATIVE_PATH = "${Environment.DIRECTORY_DCIM}/AutoShot"

    /** `AUTOSHOT_yyyyMMdd_HHmmss_SSS.jpg` */
    fun fileName(epochMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
        val format = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).apply { this.timeZone = timeZone }
        return "AUTOSHOT_${format.format(Date(epochMillis))}.jpg"
    }

    fun outputOptions(context: Context, epochMillis: Long): ImageCapture.OutputFileOptions {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName(epochMillis))
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_PATH)
        }
        return ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values,
        ).build()
    }
}
