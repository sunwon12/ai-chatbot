package sionic.chat.service

import org.springframework.stereotype.Service
import sionic.chat.domain.Thread
import sionic.chat.repository.ThreadRepository
import sionic.user.domain.User
import java.time.Instant

@Service
class ThreadManager(
    private val threadRepository: ThreadRepository
) {
    fun getOrCreateThread(user: User, now: Instant): Thread {
        val latestThread = threadRepository.findTopByUserOrderByLastMessageAtDesc(user)

        return if (latestThread == null || latestThread.isExpired(now)) {
            threadRepository.save(Thread.create(user))
        } else {
            latestThread
        }
    }
}
