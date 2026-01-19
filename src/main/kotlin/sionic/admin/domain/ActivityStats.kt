package sionic.admin.domain

import io.swagger.v3.oas.annotations.media.Schema

data class ActivityStats(
    val signupCount: Long,
    val loginCount: Long,
    val chatCount: Long
) {
    fun toResponse(): StatsResponse {
        return StatsResponse(
            signupCount = signupCount,
            loginCount = loginCount,
            chatCount = chatCount
        )
    }
}

@Schema(description = "활동 통계 응답")
data class StatsResponse(
    @Schema(description = "최근 24시간 회원가입 수", example = "15")
    val signupCount: Long,

    @Schema(description = "최근 24시간 로그인 수", example = "42")
    val loginCount: Long,

    @Schema(description = "최근 24시간 대화 생성 수", example = "128")
    val chatCount: Long
)
