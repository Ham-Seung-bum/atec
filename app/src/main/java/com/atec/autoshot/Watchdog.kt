package com.atec.autoshot

import android.os.Handler
import android.os.Looper

/** [TIMEOUT_MS] 안에 [cancel]되지 않으면 메인 스레드에서 콜백을 한 번 실행한다. */
class Watchdog {
    private val handler = Handler(Looper.getMainLooper())

    fun start(onTimeout: () -> Unit) {
        cancel()
        handler.postDelayed(onTimeout, TIMEOUT_MS)
    }

    fun cancel() {
        handler.removeCallbacksAndMessages(null)
    }

    companion object {
        const val TIMEOUT_MS = 5_000L
    }
}
