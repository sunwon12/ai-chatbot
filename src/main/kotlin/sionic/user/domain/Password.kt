package sionic.user.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import org.springframework.security.crypto.password.PasswordEncoder
import sionic.common.exception.InvalidPasswordException

@Embeddable
data class Password private constructor(
    @Column(name = "password", nullable = false)
    val value: String
) {
    fun matches(rawPassword: String, encoder: PasswordEncoder): Boolean {
        return encoder.matches(rawPassword, value)
    }

    fun validateAndThrow(rawPassword: String, encoder: PasswordEncoder) {
        if (!matches(rawPassword, encoder)) {
            throw InvalidPasswordException()
        }
    }

    companion object {
        fun encode(rawPassword: String, encoder: PasswordEncoder): Password {
            require(rawPassword.isNotBlank()) { "비밀번호는 비어있을 수 없습니다." }
            return Password(encoder.encode(rawPassword))
        }

        fun fromEncoded(encodedPassword: String): Password {
            return Password(encodedPassword)
        }
    }
}
