package sionic.user.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import sionic.common.exception.InvalidEmailFormatException

@Embeddable
data class Email(
    @Column(name = "email", nullable = false, unique = true)
    val value: String
) {
    init {
        validate()
    }

    private fun validate() {
        if (!EMAIL_REGEX.matches(value)) {
            throw InvalidEmailFormatException()
        }
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
    }
}
