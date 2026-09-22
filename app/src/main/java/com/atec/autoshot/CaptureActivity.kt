package com.atec.autoshot

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity

/**
 * 앱 진입점. 화면을 그리지 않는 투명 Activity (SPEC §7.3).
 *
 * M0: 실행 → 토스트 → 즉시 종료만 수행해 실기기 설치/실행과 투명 테마(N-07)를 확인한다.
 * 권한·카메라·저장은 M1~M2에서 추가한다.
 */
class CaptureActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "launched uptime=${SystemClock.uptimeMillis()}ms")

        Toast.makeText(this, R.string.launch_ok, Toast.LENGTH_SHORT).show()
        finishWithoutAnimation()
    }

    private fun finishWithoutAnimation() {
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
