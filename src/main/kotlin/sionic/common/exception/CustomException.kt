package sionic.common.exception

import org.springframework.http.HttpStatus

open class CustomException(
    val status: HttpStatus,
    override val message: String
) : RuntimeException(message)

class DuplicateEmailException : CustomException(HttpStatus.CONFLICT, "이미 존재하는 이메일입니다.")

class UserNotFoundException : CustomException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.")

class InvalidPasswordException : CustomException(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다.")

class InvalidEmailFormatException : CustomException(HttpStatus.BAD_REQUEST, "잘못된 이메일 형식입니다.")

class ThreadNotFoundException : CustomException(HttpStatus.NOT_FOUND, "스레드를 찾을 수 없습니다.")

class ChatNotFoundException : CustomException(HttpStatus.NOT_FOUND, "대화를 찾을 수 없습니다.")

class FeedbackNotFoundException : CustomException(HttpStatus.NOT_FOUND, "피드백을 찾을 수 없습니다.")

class UnauthorizedAccessException : CustomException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.")

class InvalidQuestionException : CustomException(HttpStatus.BAD_REQUEST, "질문은 비어있을 수 없습니다.")

class InvalidTokenException : CustomException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.")
