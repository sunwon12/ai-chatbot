package sionic.chat.domain

import jakarta.persistence.*
import sionic.common.exception.UnauthorizedAccessException
import sionic.user.domain.User
import java.time.Duration
import java.time.Instant

@Entity
@Table(name = "threads")
class Thread private constructor(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false)
    var lastMessageAt: Instant = Instant.now(),

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
) {
    @OneToMany(mappedBy = "thread", cascade = [CascadeType.ALL], orphanRemoval = true)
    val chats: MutableList<Chat> = mutableListOf()

    fun isExpired(now: Instant): Boolean {
        return Duration.between(lastMessageAt, now).toMinutes() >= TIMEOUT_MINUTES
    }

    fun updateLastMessageTime(now: Instant) {
        this.lastMessageAt = now
    }

    fun isOwnedBy(userId: Long): Boolean {
        return this.user.id == userId
    }

    fun validateOwnership(userId: Long) {
        if (!isOwnedBy(userId)) {
            throw UnauthorizedAccessException()
        }
    }

    companion object {
        const val TIMEOUT_MINUTES = 30L

        fun create(user: User): Thread {
            return Thread(user = user)
        }
    }
}
