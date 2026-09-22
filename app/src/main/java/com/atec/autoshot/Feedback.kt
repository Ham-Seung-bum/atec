package com.atec.autoshot

import android.content.Context
import android.media.MediaActionSound
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** 촬영 피드백: 셔터음 + 짧은 진동 (F-07). 셔터음은 무음 옵션 없이 항상 재생한다 (SPEC §8). */
class Feedback(context: Context) {

    private val sound = MediaActionSound().apply { load(MediaActionSound.SHUTTER_CLICK) }

    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    fun shutter() {
        sound.play(MediaActionSound.SHUTTER_CLICK)
        vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
    }

    fun release() {
        sound.release()
    }
}
