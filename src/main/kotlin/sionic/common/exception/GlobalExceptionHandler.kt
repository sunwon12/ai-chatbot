package sionic.common.exception

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(CustomException::class)
    fun handleCustomException(e: CustomException): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            status = e.status.value(),
            error = e.status.reasonPhrase,
            message = e.message
        )
        return ResponseEntity.status(e.status).body(response)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message = e.bindingResult.fieldErrors
            .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        val response = ErrorResponse(
            status = 400,
            error = "Bad Request",
            message = message
        )
        return ResponseEntity.badRequest().body(response)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            status = 500,
            error = "Internal Server Error",
            message = e.message ?: "알 수 없는 오류가 발생했습니다."
        )
        return ResponseEntity.internalServerError().body(response)
    }
}
