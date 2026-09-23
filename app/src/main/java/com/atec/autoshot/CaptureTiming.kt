package com.atec.autoshot

/** AE/AF 대기 시간 규칙 (F-04a). */
object CaptureTiming {
    /** 메터링이 일찍 끝나도 최소 이만큼은 기다린다. */
    const val MIN_WAIT_MS = 500L

    /** 메터링이 끝나지 않아도 이 시간이 지나면 촬영한다. */
    const val MAX_WAIT_MS = 1_000L

    /** 메터링 시작 후 [elapsedMs]가 지났을 때 추가로 기다릴 시간. */
    fun remainingMinWait(elapsedMs: Long): Long = (MIN_WAIT_MS - elapsedMs).coerceIn(0L, MIN_WAIT_MS)
}
