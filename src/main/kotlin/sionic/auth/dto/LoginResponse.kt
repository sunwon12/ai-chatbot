package sionic.auth.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "로그인 응답")
data class LoginResponse(
    @Schema(description = "JWT 액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    val accessToken: String,

    @Schema(description = "토큰 타입", example = "Bearer", defaultValue = "Bearer")
    val tokenType: String = "Bearer"
)
