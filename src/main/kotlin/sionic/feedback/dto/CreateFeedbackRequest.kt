package sionic.feedback.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "피드백 생성 요청")
data class CreateFeedbackRequest(
    @Schema(description = "피드백을 남길 대화 ID", example = "1", required = true)
    @field:NotNull(message = "대화 ID는 필수입니다.")
    val chatId: Long,

    @Schema(description = "긍정 피드백 여부 (true: 긍정, false: 부정)", example = "true", required = true)
    @field:NotNull(message = "피드백 유형은 필수입니다.")
    val isPositive: Boolean
)
