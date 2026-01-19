package sionic.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import sionic.user.domain.User
import java.time.Instant

@Schema(description = "회원가입 응답")
data class SignupResponse(
    @Schema(description = "사용자 ID", example = "1")
    val id: Long,

    @Schema(description = "이메일 주소", example = "user@example.com")
    val email: String,

    @Schema(description = "사용자 이름", example = "홍길동")
    val name: String,

    @Schema(description = "권한", example = "MEMBER", allowableValues = ["MEMBER", "ADMIN"])
    val role: String,

    @Schema(description = "가입 일시")
    val createdAt: Instant
) {
    companion object {
        fun from(user: User): SignupResponse {
            return SignupResponse(
                id = user.id,
                email = user.email.value,
                name = user.name,
                role = user.role.name,
                createdAt = user.createdAt
            )
        }
    }
}
