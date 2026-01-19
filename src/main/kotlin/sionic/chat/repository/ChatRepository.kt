package sionic.chat.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import sionic.chat.domain.Chat
import java.time.Instant
import java.util.stream.Stream

interface ChatRepository : JpaRepository<Chat, Long> {
    fun findAllByThreadUserId(userId: Long, pageable: Pageable): Page<Chat>

    @Query("SELECT c FROM Chat c JOIN FETCH c.thread t JOIN FETCH t.user")
    fun findAllWithThreadAndUser(pageable: Pageable): Page<Chat>

    fun countByCreatedAtBetween(from: Instant, to: Instant): Long

    @Query("SELECT c FROM Chat c JOIN FETCH c.thread t JOIN FETCH t.user WHERE c.createdAt BETWEEN :from AND :to")
    fun streamAllByCreatedAtBetween(from: Instant, to: Instant): Stream<Chat>
}
