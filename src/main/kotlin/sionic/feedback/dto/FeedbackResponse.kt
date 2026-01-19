package sionic.feedback.dto

import io.swagger.v3.oas.annotations.media.Schema
import sionic.feedback.domain.Feedback
import java.time.Instant

@Schema(description = "피드백 응답")
data class FeedbackResponse(
    @Schema(description = "피드백 ID", example = "1")
    val id: Long,

    @Schema(description = "대화 ID", example = "1")
    val chatId: Long,

    @Schema(description = "사용자 ID", example = "1")
    val userId: Long,

    @Schema(description = "긍정 피드백 여부", example = "true")
    val isPositive: Boolean,

    @Schema(description = "처리 상태", example = "PENDING", allowableValues = ["PENDING", "RESOLVED"])
    val status: String,

    @Schema(description = "생성 일시")
    val createdAt: Instant
) {
    companion object {
        fun from(feedback: Feedback): FeedbackResponse {
            return FeedbackResponse(
                id = feedback.id,
                chatId = feedback.chat.id,
                userId = feedback.user.id,
                isPositive = feedback.type.isPositive(),
                status = feedback.status.name,
                createdAt = feedback.createdAt
            )
        }
    }
}
