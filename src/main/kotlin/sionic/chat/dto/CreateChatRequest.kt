package sionic.chat.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "대화 생성 요청")
data class CreateChatRequest(
    @Schema(description = "AI에게 보낼 질문", example = "LG 제품 중 가성비 스타일러 추천해줘", required = true)
    @field:NotBlank(message = "질문은 필수입니다.")
    val question: String,

    @Schema(description = "사용할 AI 모델 (기본값: gpt-4o-mini)", example = "gpt-4o-mini", nullable = true)
    val model: String? = null
)
