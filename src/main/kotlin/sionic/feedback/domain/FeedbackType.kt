package sionic.feedback.domain

enum class FeedbackType {
    POSITIVE,
    NEGATIVE;

    fun isPositive(): Boolean = this == POSITIVE

    companion object {
        fun from(isPositive: Boolean): FeedbackType {
            return if (isPositive) POSITIVE else NEGATIVE
        }
    }
}
