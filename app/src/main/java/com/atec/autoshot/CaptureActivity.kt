package com.atec.autoshot

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

/**
 * 앱 진입점. 화면을 그리지 않는 투명 Activity (SPEC §7.3).
 *
 * 권한 확인 → 미리보기 없이 카메라 열기 → AE/AF 대기 → 촬영·저장 → 종료 (SPEC §3).
 */
class CaptureActivity : ComponentActivity() {

    private val launchUptime = SystemClock.uptimeMillis()
    private val watchdog = Watchdog()
    private var cameraStarted = false
    private var feedback: Feedback? = null

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val outcome = PermissionOutcome.of(
                granted = granted,
                shouldShowRationale = shouldShowRequestPermissionRationale(Manifest.permission.CAMERA),
            )
            Log.i(TAG, "permission result=$outcome")
            when (outcome) {
                PermissionOutcome.GRANTED -> openCamera()
                PermissionOutcome.DENIED -> {
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show()
                    finishWithoutAnimation()
                }
                PermissionOutcome.PERMANENTLY_DENIED -> showOpenSettingsDialog()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "launched sinceProcessStart=${launchUptime - Process.getStartUptimeMillis()}ms")

        when {
            checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> openCamera()
            // 재생성된 경우 이전 인스턴스의 권한 요청 결과가 곧 전달되므로 다시 요청하지 않는다.
            savedInstanceState == null -> requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onDestroy() {
        watchdog.cancel()
        feedback?.release()
        super.onDestroy()
    }

    private fun openCamera() {
        if (cameraStarted) return
        cameraStarted = true
        // 투명 Activity가 남아 있으면 홈 화면이 멈춘 것처럼 보이므로, 카메라가 응답하지 않아도 반드시 종료한다.
        watchdog.start {
            Log.e(TAG, "watchdog fired after ${Watchdog.TIMEOUT_MS}ms")
            Toast.makeText(this, R.string.camera_open_failed, Toast.LENGTH_LONG).show()
            finishWithoutAnimation()
        }
        // 셔터음을 미리 로드해 촬영 순간 지연 없이 재생되게 한다.
        feedback = Feedback(this)
        val controller = CameraController(this, this)
        controller.open(
            onReady = { ready ->
                Log.i(
                    TAG,
                    "camera ready sinceLaunch=${SystemClock.uptimeMillis() - launchUptime}ms " +
                        "open=${ready.openMs}ms metering=${ready.meteringMs}ms focus=${ready.focus} " +
                        "resolution=${ready.resolution}",
                )
                capture(controller, ready)
            },
            onError = { error ->
                watchdog.cancel()
                Log.e(TAG, "camera open failed", error)
                Toast.makeText(this, R.string.camera_open_failed, Toast.LENGTH_LONG).show()
                finishWithoutAnimation()
            },
        )
    }

    private fun capture(controller: CameraController, ready: CameraController.Ready) {
        controller.capture(
            ready.imageCapture,
            onCaptureStarted = {
                Log.i(TAG, "shutter sinceLaunch=${SystemClock.uptimeMillis() - launchUptime}ms")
                feedback?.shutter()
            },
            onSaved = { uri ->
                watchdog.cancel()
                val sinceLaunch = SystemClock.uptimeMillis() - launchUptime
                Log.i(TAG, "saved sinceLaunch=${sinceLaunch}ms uri=$uri")
                Toast.makeText(
                    this,
                    getString(R.string.photo_saved, getString(ready.focus.labelRes), sinceLaunch),
                    Toast.LENGTH_SHORT,
                ).show()
                finishWithoutAnimation()
            },
            onError = { error ->
                watchdog.cancel()
                Log.e(TAG, "capture failed code=${error.imageCaptureError}", error)
                Toast.makeText(this, R.string.capture_failed, Toast.LENGTH_LONG).show()
                finishWithoutAnimation()
            },
        )
    }

    private val FocusResult.labelRes: Int
        get() = when (this) {
            FocusResult.FOCUSED -> R.string.focus_ok
            FocusResult.NOT_FOCUSED -> R.string.focus_failed
            FocusResult.TIMEOUT -> R.string.focus_timeout
            FocusResult.ERROR -> R.string.focus_error
        }

    private fun showOpenSettingsDialog() {
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(R.string.permission_required_title)
            .setMessage(R.string.permission_required_message)
            .setPositiveButton(R.string.open_settings) { _, _ ->
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            .setNegativeButton(R.string.close, null)
            .setOnDismissListener { finishWithoutAnimation() }
            .show()
    }

    private fun finishWithoutAnimation() {
        if (isFinishing) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
            finishAndRemoveTask()
        } else {
            finishAndRemoveTask()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    private companion object {
        const val TAG = "AutoShot"
    }
}
