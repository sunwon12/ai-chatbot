package sionic.chat.repository

import org.springframework.data.jpa.repository.JpaRepository
import sionic.chat.domain.Thread
import sionic.user.domain.User

interface ThreadRepository : JpaRepository<Thread, Long> {
    fun findTopByUserOrderByLastMessageAtDesc(user: User): Thread?
    fun findAllByUserOrderByCreatedAtDesc(user: User): List<Thread>
}
