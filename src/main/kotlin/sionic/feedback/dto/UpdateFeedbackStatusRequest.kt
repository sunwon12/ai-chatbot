package sionic.feedback.dto

import io.swagger.v3.oas.annotations.media.Schema
import sionic.feedback.domain.FeedbackStatus

@Schema(description = "피드백 상태 변경 요청")
data class UpdateFeedbackStatusRequest(
    @Schema(description = "변경할 상태", example = "RESOLVED", allowableValues = ["PENDING", "RESOLVED"])
    val status: FeedbackStatus
)
