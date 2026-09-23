package com.atec.autoshot

/** 촬영 전 AF 메터링 결과. 어느 경우든 촬영은 진행한다 (F-04a). */
enum class FocusResult {
    FOCUSED,

    /** AF가 끝났지만 초점을 잡지 못함 (너무 가깝거나 대비가 낮은 장면). */
    NOT_FOCUSED,

    /** [CaptureTiming.MAX_WAIT_MS] 안에 메터링이 끝나지 않음. */
    TIMEOUT,

    /** 메터링 요청 자체가 실패함. */
    ERROR,
}
