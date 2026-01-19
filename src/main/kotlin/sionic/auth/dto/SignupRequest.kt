package sionic.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

@Schema(description = "회원가입 요청")
data class SignupRequest(
    @Schema(description = "이메일 주소", example = "user@example.com", required = true)
    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "올바른 이메일 형식이 아닙니다.")
    val email: String,

    @Schema(description = "비밀번호", example = "password123", required = true)
    @field:NotBlank(message = "비밀번호는 필수입니다.")
    val password: String,

    @Schema(description = "사용자 이름", example = "홍길동", required = true)
    @field:NotBlank(message = "이름은 필수입니다.")
    val name: String
)
