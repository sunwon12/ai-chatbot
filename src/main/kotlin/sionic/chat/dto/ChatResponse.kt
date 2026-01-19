package sionic.chat.dto

import io.swagger.v3.oas.annotations.media.Schema
import sionic.chat.domain.Chat
import java.time.Instant

@Schema(description = "대화 응답")
data class ChatResponse(
    @Schema(description = "대화 ID", example = "1")
    val id: Long,

    @Schema(description = "스레드 ID", example = "1")
    val threadId: Long,

    @Schema(description = "질문 내용", example = "오늘 날씨는 어때?")
    val question: String,

    @Schema(description = "AI 답변", example = "오늘은 맑고 따뜻한 날씨입니다.")
    val answer: String,

    @Schema(description = "생성 일시")
    val createdAt: Instant
) {
    companion object {
        fun from(chat: Chat): ChatResponse {
            return ChatResponse(
                id = chat.id,
                threadId = chat.thread.id,
                question = chat.question.value,
                answer = chat.answer.value,
                createdAt = chat.createdAt
            )
        }
    }
}
