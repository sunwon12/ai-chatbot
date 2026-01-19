package sionic.chat.domain

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "chats")
class Chat private constructor(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    val thread: Thread,

    @Embedded
    val question: Question,

    @Embedded
    var answer: Answer = Answer.empty(),

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
) {
    fun updateAnswer(newAnswer: Answer) {
        this.answer = newAnswer
    }

    fun belongsTo(userId: Long): Boolean {
        return thread.isOwnedBy(userId)
    }

    fun belongsToUserId(): Long {
        return thread.user.id
    }

    companion object {
        fun create(thread: Thread, question: Question): Chat {
            val chat = Chat(thread = thread, question = question)
            thread.chats.add(chat)
            thread.updateLastMessageTime(Instant.now())
            return chat
        }
    }
}
