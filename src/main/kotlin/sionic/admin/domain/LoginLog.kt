package sionic.admin.domain

import jakarta.persistence.*
import sionic.user.domain.User
import java.time.Instant

@Entity
@Table(name = "login_logs")
class LoginLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
) {
    companion object {
        fun create(user: User): LoginLog {
            return LoginLog(user = user)
        }
    }
}
