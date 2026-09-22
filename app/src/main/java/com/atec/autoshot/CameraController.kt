package com.atec.autoshot

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer

/**
 * 미리보기 없이 후면 카메라를 열고, AE/AF를 맞춘 뒤 촬영한다 (SPEC §7.1, F-03, F-04, F-04a, F-05).
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
        /** 바인딩 시작부터 카메라 OPEN까지. */
        val openMs: Long,
        /** 카메라 OPEN부터 메터링 완료(또는 타임아웃)까지. */
        val meteringMs: Long,
        val focus: FocusResult,
        val resolution: Size?,
    )

    class CameraOpenException(message: String) : Exception(message)

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
            // 바인딩 직후에는 카메라 세션이 아직 열리지 않아 메터링 요청이 실패한다(M1 실기기 결과).
            // OPEN 상태가 된 뒤에 메터링을 시작한다.
            awaitOpen(camera, onError) {
                val openMs = SystemClock.uptimeMillis() - start
                awaitMetering(camera) { focus, meteringMs ->
                    onReady(
                        Ready(
                            imageCapture = imageCapture,
                            openMs = openMs,
                            meteringMs = meteringMs,
                            focus = focus,
                            resolution = imageCapture.resolutionInfo?.resolution,
                        )
                    )
                }
            }
        }, context.mainExecutor)
    }

    /**
     * 사진 1장을 찍어 `DCIM/AutoShot/`에 저장한다 (F-05, F-06).
     *
     * @param onCaptureStarted 셔터가 실제로 동작하는 시점. 셔터음은 여기서 재생한다.
     */
    fun capture(
        imageCapture: ImageCapture,
        targetRotation: Int,
        onCaptureStarted: () -> Unit,
        onSaved: (Uri?) -> Unit,
        onError: (ImageCaptureException) -> Unit,
    ) {
        imageCapture.targetRotation = targetRotation
        val options = PhotoSaver.outputOptions(context, System.currentTimeMillis())
        val startedCallback = onCaptureStarted
        val savedCallback = onSaved
        val errorCallback = onError
        imageCapture.takePicture(options, context.mainExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onCaptureStarted() = startedCallback()

            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) =
                savedCallback(outputFileResults.savedUri)

            override fun onError(exception: ImageCaptureException) = errorCallback(exception)
        })
    }

    /** 카메라가 OPEN 상태가 되면 [onOpen]을, 오류가 보고되면 [onError]를 한 번 호출한다. */
    private fun awaitOpen(camera: Camera, onError: (Throwable) -> Unit, onOpen: () -> Unit) {
        val cameraState = camera.cameraInfo.cameraState
        val observer = object : Observer<CameraState> {
            override fun onChanged(value: CameraState) {
                Log.d(TAG, "camera state=${value.type} error=${value.error?.code}")
                val error = value.error
                when {
                    // 다른 앱이 카메라를 쓰는 중이면 CameraX는 PENDING_OPEN으로 무기한 재시도하므로 바로 실패 처리한다 (F-10).
                    error != null -> {
                        cameraState.removeObserver(this)
                        onError(CameraOpenException("camera error code=${error.code} type=${error.type}"))
                    }
                    value.type == CameraState.Type.OPEN -> {
                        cameraState.removeObserver(this)
                        onOpen()
                    }
                }
            }
        }
        cameraState.observe(lifecycleOwner, observer)
    }

    /**
     * 화면 중앙 기준 AF+AE를 실행하고, 완료와 [CaptureTiming.MAX_WAIT_MS] 타임아웃 중 먼저 오는 쪽에서 [onDone]을 한 번 호출한다.
     * 완료가 빨라도 [CaptureTiming.MIN_WAIT_MS]까지는 기다린다.
     */
    private fun awaitMetering(camera: Camera, onDone: (FocusResult, elapsedMs: Long) -> Unit) {
        val start = SystemClock.uptimeMillis()
        var finished = false
        fun finish(result: FocusResult) {
            if (finished) return
            finished = true
            mainHandler.removeCallbacksAndMessages(null)
            val delay = CaptureTiming.remainingMinWait(SystemClock.uptimeMillis() - start)
            mainHandler.postDelayed({ onDone(result, SystemClock.uptimeMillis() - start) }, delay)
        }

        mainHandler.postDelayed({
            Log.w(TAG, "metering timeout after ${CaptureTiming.MAX_WAIT_MS}ms")
            finish(FocusResult.TIMEOUT)
        }, CaptureTiming.MAX_WAIT_MS)

        // 미리보기가 없으므로 뷰 좌표 대신 정규화 좌표(0..1)의 중앙점을 쓴다.
        val center = SurfaceOrientedMeteringPointFactory(1f, 1f).createPoint(0.5f, 0.5f)
        val action = FocusMeteringAction.Builder(center, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .build()
        val future = camera.cameraControl.startFocusAndMetering(action)
        future.addListener({
            val result = try {
                if (future.get().isFocusSuccessful) FocusResult.FOCUSED else FocusResult.NOT_FOCUSED
            } catch (e: Exception) {
                Log.w(TAG, "metering failed", e)
                FocusResult.ERROR
            }
            finish(result)
        }, context.mainExecutor)
    }

    private companion object {
        const val TAG = "AutoShot"
    }
}
