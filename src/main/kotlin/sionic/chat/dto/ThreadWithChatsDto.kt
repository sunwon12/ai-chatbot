package sionic.chat.dto

import sionic.chat.domain.Thread
import java.time.Instant

data class ThreadWithChatsDto(
    val threadId: Long,
    val userId: Long,
    val userName: String,
    val createdAt: Instant,
    val lastMessageAt: Instant,
    val chats: List<ChatResponse>
) {
    companion object {
        fun from(thread: Thread): ThreadWithChatsDto {
            return ThreadWithChatsDto(
                threadId = thread.id,
                userId = thread.user.id,
                userName = thread.user.name,
                createdAt = thread.createdAt,
                lastMessageAt = thread.lastMessageAt,
                chats = thread.chats.map { ChatResponse.from(it) }
            )
        }
    }
}
