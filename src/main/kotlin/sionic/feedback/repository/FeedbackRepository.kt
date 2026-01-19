package sionic.feedback.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import sionic.feedback.domain.Feedback
import sionic.feedback.domain.FeedbackType

interface FeedbackRepository : JpaRepository<Feedback, Long> {
    fun findByChatIdAndUserId(chatId: Long, userId: Long): Feedback?

    fun findAllByUserId(userId: Long, pageable: Pageable): Page<Feedback>

    @Query("SELECT f FROM Feedback f WHERE (:type IS NULL OR f.type = :type)")
    fun findAllWithFilter(type: FeedbackType?, pageable: Pageable): Page<Feedback>

    @Query("SELECT f FROM Feedback f WHERE f.user.id = :userId AND (:type IS NULL OR f.type = :type)")
    fun findAllByUserIdWithFilter(userId: Long, type: FeedbackType?, pageable: Pageable): Page<Feedback>
}
