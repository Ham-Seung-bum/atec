package com.atec.autoshot

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner

/**
 * 미리보기 없이 후면 카메라를 열고 AE/AF 수렴을 기다린다 (SPEC §7.1, F-03, F-04, F-04a).
 *
 * `Preview`를 바인딩하지 않고 `ImageCapture`만 바인딩한다. 모든 콜백은 메인 스레드에서 호출된다.
 */
class CameraController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
) {

    /** 카메라가 촬영 가능한 상태가 되었을 때의 측정값. */
    data class Ready(
        val imageCapture: ImageCapture,
        val bindMs: Long,
        val meteringMs: Long,
        /** 초점 성공 여부. 타임아웃이거나 메터링이 실패하면 null. */
        val focusSuccessful: Boolean?,
        val resolution: Size?,
    )

    private val mainHandler = Handler(Looper.getMainLooper())

    fun open(onReady: (Ready) -> Unit, onError: (Throwable) -> Unit) {
        val start = SystemClock.uptimeMillis()
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                .build()
            val camera = try {
                val provider = providerFuture.get()
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, imageCapture)
            } catch (e: Exception) {
                onError(e)
                return@addListener
            }
            val bindMs = SystemClock.uptimeMillis() - start
            awaitMetering(camera) { focusSuccessful, meteringMs ->
                onReady(
                    Ready(
                        imageCapture = imageCapture,
                        bindMs = bindMs,
                        meteringMs = meteringMs,
                        focusSuccessful = focusSuccessful,
                        resolution = imageCapture.resolutionInfo?.resolution,
                    )
                )
            }
        }, context.mainExecutor)
    }

    /**
     * 화면 중앙 기준 AF+AE를 실행하고, 완료와 [MAX_WAIT_MS] 타임아웃 중 먼저 오는 쪽에서 [onDone]을 한 번 호출한다.
     * 완료가 빨라도 [MIN_WAIT_MS]까지는 기다린다.
     */
    private fun awaitMetering(camera: Camera, onDone: (focusSuccessful: Boolean?, elapsedMs: Long) -> Unit) {
        val start = SystemClock.uptimeMillis()
        var finished = false
        fun finish(focusSuccessful: Boolean?) {
            if (finished) return
            finished = true
            mainHandler.removeCallbacksAndMessages(null)
            val delay = CaptureTiming.remainingMinWait(SystemClock.uptimeMillis() - start)
            mainHandler.postDelayed({ onDone(focusSuccessful, SystemClock.uptimeMillis() - start) }, delay)
        }

        mainHandler.postDelayed({
            Log.w(TAG, "metering timeout after ${CaptureTiming.MAX_WAIT_MS}ms")
            finish(null)
        }, CaptureTiming.MAX_WAIT_MS)

        // 미리보기가 없으므로 뷰 좌표 대신 정규화 좌표(0..1)의 중앙점을 쓴다.
        val center = SurfaceOrientedMeteringPointFactory(1f, 1f).createPoint(0.5f, 0.5f)
        val action = FocusMeteringAction.Builder(center, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .build()
        val future = camera.cameraControl.startFocusAndMetering(action)
        future.addListener({
            val result = try {
                future.get().isFocusSuccessful
            } catch (e: Exception) {
                Log.w(TAG, "metering failed", e)
                null
            }
            finish(result)
        }, context.mainExecutor)
    }

    private companion object {
        const val TAG = "AutoShot"
    }
}
