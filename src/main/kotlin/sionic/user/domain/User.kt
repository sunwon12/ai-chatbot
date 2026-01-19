package sionic.user.domain

import jakarta.persistence.*
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant

@Entity
@Table(name = "users")
class User private constructor(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Embedded
    val email: Email,

    @Embedded
    var password: Password,

    @Column(nullable = false)
    val name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: Role = Role.MEMBER,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
) {
    fun authenticate(rawPassword: String, encoder: PasswordEncoder) {
        password.validateAndThrow(rawPassword, encoder)
    }

    fun isOwnerOf(resourceOwnerId: Long): Boolean {
        return this.id == resourceOwnerId
    }

    fun canAccess(resourceOwnerId: Long): Boolean {
        return isOwnerOf(resourceOwnerId) || role.isAdmin()
    }

    companion object {
        fun create(
            email: Email,
            rawPassword: String,
            name: String,
            encoder: PasswordEncoder,
            role: Role = Role.ADMIN // MVP: 모든 사용자를 ADMIN으로 가입
        ): User {
            return User(
                email = email,
                password = Password.encode(rawPassword, encoder),
                name = name,
                role = role
            )
        }
    }
}
