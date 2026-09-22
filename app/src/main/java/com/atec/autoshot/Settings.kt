package com.atec.autoshot

/** v1.0 고정 설정. UI가 없으므로 값은 코드로 관리한다. */
object Settings {
    /**
     * 앱이 셔터음을 재생할지 여부 (F-07).
     * 기본 false: 우리 앱은 소리를 내지 않는다. 그래도 소리가 나면 기기 펌웨어가 강제하는 것이다 (SPEC §8).
     */
    const val PLAY_SHUTTER_SOUND = false
}
