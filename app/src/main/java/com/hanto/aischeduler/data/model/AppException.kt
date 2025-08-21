package com.hanto.aischeduler.data.model

sealed class AppException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    // 네트워크 관련 예외
    class NetworkException(message: String, cause: Throwable? = null) : AppException(message, cause)
    class ApiException(val code: Int, message: String) : AppException("API Error ($code): $message")
    class TimeoutException(message: String = "요청 시간이 초과되었습니다") : AppException(message)

    // 데이터 관련 예외
    class ParseException(message: String = "응답 데이터 파싱에 실패했습니다") : AppException(message)
    class ValidationException(message: String) : AppException(message)

    // 비즈니스 로직 예외
    class ScheduleConflictException(message: String) : AppException(message)
    class TimeRangeException(message: String) : AppException(message)

    // 사용자 친화적 메시지 반환
    fun getUserMessage(): String? = when (this) {
        is NetworkException -> "인터넷 연결을 확인해주세요"
        is ApiException -> when (code) {
            401 -> "인증에 실패했습니다"
            429 -> "요청이 너무 많습니다. 잠시 후 다시 시도해주세요"
            500 -> "서버에 문제가 발생했습니다"
            else -> "서비스에 일시적인 문제가 있습니다"
        }

        is TimeoutException -> "응답 시간이 초과되었습니다. 다시 시도해주세요"
        is ParseException -> "데이터 처리 중 오류가 발생했습니다"
        is ValidationException -> message
        is ScheduleConflictException -> message
        is TimeRangeException -> message
    }
}