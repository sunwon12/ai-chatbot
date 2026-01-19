package sionic.chat.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class Answer(
    @Column(name = "answer", columnDefinition = "TEXT")
    val value: String = ""
) {
    fun appendChunk(chunk: String): Answer {
        return Answer(value + chunk)
    }

    fun isEmpty(): Boolean = value.isBlank()

    companion object {
        fun empty(): Answer = Answer("")
    }
}
