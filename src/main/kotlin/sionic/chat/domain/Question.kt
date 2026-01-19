package sionic.chat.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import sionic.common.exception.InvalidQuestionException

@Embeddable
data class Question(
    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    val value: String
) {
    init {
        validate()
    }

    private fun validate() {
        if (value.isBlank()) {
            throw InvalidQuestionException()
        }
    }
}
