package sionic.feedback.domain

import jakarta.persistence.*
import sionic.chat.domain.Chat
import sionic.common.exception.UnauthorizedAccessException
import sionic.user.domain.User
import java.time.Instant

@Entity
@Table(
    name = "feedbacks",
    uniqueConstraints = [UniqueConstraint(columnNames = ["chat_id", "user_id"])]
)
class Feedback private constructor(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    val chat: Chat,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: FeedbackType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: FeedbackStatus = FeedbackStatus.PENDING,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
) {
    fun updateType(newType: FeedbackType) {
        this.type = newType
    }

    fun resolve() {
        this.status = FeedbackStatus.RESOLVED
    }

    fun isOwnedBy(userId: Long): Boolean {
        return this.user.id == userId
    }

    fun validateAdminAccess(user: User) {
        if (!user.role.isAdmin()) {
            throw UnauthorizedAccessException()
        }
    }

    companion object {
        fun create(chat: Chat, user: User, type: FeedbackType): Feedback {
            return Feedback(
                chat = chat,
                user = user,
                type = type
            )
        }
    }
}
