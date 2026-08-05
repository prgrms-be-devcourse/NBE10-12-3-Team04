package com.triptrace.global.error

import com.triptrace.global.app.Domain

/**
 * 도메인별 에러 코드가 공통으로 갖춰야 할 계약.
 *
 * 반환 타입을 nullable로 둔 이유는 이미 코틀린으로 전환된 구현체들
 * (TripErrorCode, MarkerErrorCode, PostErrorCode)이 `String?`로 선언돼 있기 때문이다.
 * 여기서 non-null로 좁히면 그 구현체들이 전부 컴파일되지 않는다.
 */
interface ErrorCode {
    fun getCode(): String?

    fun getMessage(): String?

    // 도메인을 따로 지정하지 않은 에러 코드는 공통으로 본다.
    fun getDomain(): Domain = Domain.COMMON
}
