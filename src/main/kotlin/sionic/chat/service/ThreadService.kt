package sionic.chat.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sionic.chat.repository.ThreadRepository
import sionic.common.exception.ThreadNotFoundException
import sionic.common.exception.UserNotFoundException
import sionic.user.repository.UserRepository

@Service
@Transactional(readOnly = true)
class ThreadService(
    private val threadRepository: ThreadRepository,
    private val userRepository: UserRepository
) {
    @Transactional
    fun deleteThread(threadId: Long, userId: Long) {
        val thread = threadRepository.findById(threadId)
            .orElseThrow { ThreadNotFoundException() }

        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException() }

        // 관리자가 아닌 경우 소유권 검증
        if (!user.role.isAdmin()) {
            thread.validateOwnership(userId)
        }

        threadRepository.delete(thread)
    }
}
