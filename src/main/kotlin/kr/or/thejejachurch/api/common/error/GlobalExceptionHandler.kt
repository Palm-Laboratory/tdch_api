package kr.or.thejejachurch.api.common.error

import kr.or.thejejachurch.api.common.response.ApiErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(ex: UnauthorizedException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            ApiErrorResponse(
                code = "UNAUTHORIZED",
                message = ex.message ?: "인증 정보가 올바르지 않습니다.",
            )
        )

    @ExceptionHandler(ForbiddenException::class)
    fun handleForbidden(ex: ForbiddenException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ApiErrorResponse(
                code = "FORBIDDEN",
                message = ex.message ?: "접근 권한이 없습니다.",
            )
        )

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(ex: NotFoundException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiErrorResponse(
                code = "NOT_FOUND",
                message = ex.message ?: "요청한 리소스를 찾을 수 없습니다.",
            )
        )

    @ExceptionHandler(MethodArgumentNotValidException::class, IllegalArgumentException::class)
    fun handleBadRequest(ex: Exception): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiErrorResponse(
                code = "INVALID_REQUEST",
                message = ex.message ?: "잘못된 요청입니다.",
            )
        )

    @ExceptionHandler(Exception::class)
    fun handleInternal(ex: Exception): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ApiErrorResponse(
                code = "INTERNAL_SERVER_ERROR",
                message = ex.message ?: "서버 오류가 발생했습니다.",
            )
        )
}
