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
 * M1: 권한 확인 → 미리보기 없이 카메라 열기 → AE/AF 대기 → 측정값 토스트/로그 → 종료.
 * 촬영·저장은 M2에서 추가한다.
 */
class CaptureActivity : ComponentActivity() {

    private val launchUptime = SystemClock.uptimeMillis()
    private val watchdog = Watchdog()
    private var cameraStarted = false

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
        super.onDestroy()
    }

    private fun openCamera() {
        if (cameraStarted) return
        cameraStarted = true
        // 투명 Activity가 남아 있으면 홈 화면이 멈춘 것처럼 보이므로, 카메라가 응답하지 않아도 반드시 종료한다.
        watchdog.start {
            Log.e(TAG, "camera watchdog fired after ${Watchdog.TIMEOUT_MS}ms")
            Toast.makeText(this, R.string.camera_open_failed, Toast.LENGTH_LONG).show()
            finishWithoutAnimation()
        }
        CameraController(this, this).open(
            onReady = { ready ->
                watchdog.cancel()
                val sinceLaunch = SystemClock.uptimeMillis() - launchUptime
                Log.i(
                    TAG,
                    "camera ready sinceLaunch=${sinceLaunch}ms bind=${ready.bindMs}ms " +
                        "metering=${ready.meteringMs}ms focus=${ready.focusSuccessful} " +
                        "resolution=${ready.resolution}",
                )
                val focus = when (ready.focusSuccessful) {
                    true -> getString(R.string.focus_ok)
                    false -> getString(R.string.focus_failed)
                    null -> getString(R.string.focus_timeout)
                }
                val resolution = ready.resolution?.let { "${it.width}x${it.height}" } ?: "?"
                Toast.makeText(
                    this,
                    getString(R.string.camera_ready, focus, sinceLaunch, resolution),
                    Toast.LENGTH_LONG,
                ).show()
                finishWithoutAnimation()
            },
            onError = { error ->
                watchdog.cancel()
                Log.e(TAG, "camera open failed", error)
                Toast.makeText(this, R.string.camera_open_failed, Toast.LENGTH_LONG).show()
                finishWithoutAnimation()
            },
        )
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
