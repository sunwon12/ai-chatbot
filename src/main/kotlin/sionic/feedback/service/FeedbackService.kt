package sionic.feedback.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sionic.chat.repository.ChatRepository
import sionic.common.exception.ChatNotFoundException
import sionic.common.exception.FeedbackNotFoundException
import sionic.common.exception.UnauthorizedAccessException
import sionic.common.exception.UserNotFoundException
import sionic.feedback.domain.Feedback
import sionic.feedback.domain.FeedbackStatus
import sionic.feedback.domain.FeedbackType
import sionic.feedback.dto.CreateFeedbackRequest
import sionic.feedback.dto.FeedbackResponse
import sionic.feedback.repository.FeedbackRepository
import sionic.user.domain.User
import sionic.user.repository.UserRepository

@Service
@Transactional(readOnly = true)
class FeedbackService(
    private val feedbackRepository: FeedbackRepository,
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository
) {
    @Transactional
    fun createOrUpdateFeedback(userId: Long, request: CreateFeedbackRequest): FeedbackResponse {
        val user = userRepository.findById(userId).orElseThrow { UserNotFoundException() }
        val chat = chatRepository.findById(request.chatId).orElseThrow { ChatNotFoundException() }

        // 권한 체크: 본인 채팅 또는 관리자
        if (!user.canAccess(chat.belongsToUserId())) {
            throw UnauthorizedAccessException()
        }

        // Upsert 로직
        val feedback = feedbackRepository.findByChatIdAndUserId(chat.id, user.id)
            ?.apply { updateType(FeedbackType.from(request.isPositive)) }
            ?: Feedback.create(chat, user, FeedbackType.from(request.isPositive))

        return FeedbackResponse.from(feedbackRepository.save(feedback))
    }

    fun listFeedbacks(
        userId: Long,
        user: User,
        isPositive: Boolean?,
        pageable: Pageable
    ): Page<FeedbackResponse> {
        val type = isPositive?.let { FeedbackType.from(it) }

        val feedbacks = if (user.role.isAdmin()) {
            feedbackRepository.findAllWithFilter(type, pageable)
        } else {
            feedbackRepository.findAllByUserIdWithFilter(userId, type, pageable)
        }

        return feedbacks.map { FeedbackResponse.from(it) }
    }

    @Transactional
    fun updateStatus(feedbackId: Long, user: User, newStatus: FeedbackStatus): FeedbackResponse {
        val feedback = feedbackRepository.findById(feedbackId)
            .orElseThrow { FeedbackNotFoundException() }

        // Domain에게 권한 검증 위임
        feedback.validateAdminAccess(user)

        // Domain에게 상태 변경 위임
        if (newStatus == FeedbackStatus.RESOLVED) {
            feedback.resolve()
        }

        return FeedbackResponse.from(feedbackRepository.save(feedback))
    }
}
