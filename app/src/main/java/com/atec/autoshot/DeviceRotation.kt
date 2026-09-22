package com.atec.autoshot

import android.view.Surface

/**
 * 물리 방향(가속도계) 각도를 [Surface] 회전 상수로 변환한다 (F-11).
 *
 * Activity가 세로로 고정되고 화면이 없어 `display.rotation`이 항상 0이므로,
 * 사진의 회전은 기기 방향에서 직접 계산해 `ImageCapture.targetRotation`에 넣는다.
 */
object DeviceRotation {

    /**
     * @param orientationDegrees `OrientationEventListener`가 주는 0..359. 방향을 알 수 없으면 [Surface.ROTATION_0].
     */
    fun toSurfaceRotation(orientationDegrees: Int): Int {
        if (orientationDegrees < 0) return Surface.ROTATION_0 // ORIENTATION_UNKNOWN
        return when ((orientationDegrees + 45) / 90 % 4) {
            0 -> Surface.ROTATION_0
            1 -> Surface.ROTATION_270
            2 -> Surface.ROTATION_180
            else -> Surface.ROTATION_90
        }
    }
}
