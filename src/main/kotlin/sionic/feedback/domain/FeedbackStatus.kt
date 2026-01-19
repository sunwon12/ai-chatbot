package sionic.feedback.domain

enum class FeedbackStatus {
    PENDING,
    RESOLVED;

    fun isResolved(): Boolean = this == RESOLVED

    fun canTransitionTo(next: FeedbackStatus): Boolean {
        return when (this) {
            PENDING -> next == RESOLVED
            RESOLVED -> false
        }
    }
}
