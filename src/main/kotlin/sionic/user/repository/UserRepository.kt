package sionic.user.repository

import org.springframework.data.jpa.repository.JpaRepository
import sionic.user.domain.Email
import sionic.user.domain.User
import java.time.Instant

interface UserRepository : JpaRepository<User, Long> {
    fun existsByEmail(email: Email): Boolean
    fun findByEmail(email: Email): User?
    fun countByCreatedAtBetween(from: Instant, to: Instant): Long
}
