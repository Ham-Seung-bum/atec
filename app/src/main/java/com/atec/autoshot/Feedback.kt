package com.atec.autoshot

import android.content.Context
import android.media.MediaActionSound
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * 촬영 피드백: 셔터음(선택) + 짧은 진동 (F-07).
 *
 * 셔터음은 우리 앱이 [MediaActionSound]로 직접 재생하는 소리이며 [playSound]로 끌 수 있다.
 * 단, 국내판 일부 기기는 펌웨어가 촬영음을 강제할 수 있고 그 소리는 앱에서 끌 수 없다 (SPEC §8).
 */
class Feedback(context: Context, private val playSound: Boolean) {

    private val sound: MediaActionSound? =
        if (playSound) MediaActionSound().apply { load(MediaActionSound.SHUTTER_CLICK) } else null

    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    /** 셔터가 실제로 동작하는 순간 호출한다. */
    fun shutter() {
        sound?.play(MediaActionSound.SHUTTER_CLICK)
        vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
    }

    fun release() {
        sound?.release()
    }
}
