package com.atec.autoshot

/** 권한 요청 결과를 화면 처리 방식으로 분류한다 (F-02). */
enum class PermissionOutcome {
    GRANTED,

    /** 한 번 거부됨. 다음 실행 때 다시 요청할 수 있다. */
    DENIED,

    /** "다시 묻지 않음" 상태. 시스템 다이얼로그가 더 이상 뜨지 않으므로 앱 설정으로 안내해야 한다. */
    PERMANENTLY_DENIED;

    companion object {
        /**
         * @param granted 권한 요청 결과
         * @param shouldShowRationale 요청 직후의 `shouldShowRequestPermissionRationale()` 값.
         *   거부 직후 false면 시스템이 더 이상 다이얼로그를 띄우지 않는 상태다.
         */
        fun of(granted: Boolean, shouldShowRationale: Boolean): PermissionOutcome = when {
            granted -> GRANTED
            shouldShowRationale -> DENIED
            else -> PERMANENTLY_DENIED
        }
    }
}
