package sionic.auth.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sionic.admin.domain.LoginLog
import sionic.admin.repository.LoginLogRepository
import sionic.user.domain.User

@Service
class LoginLogService(
    private val loginLogRepository: LoginLogRepository
) {
    @Transactional
    fun log(user: User) {
        loginLogRepository.save(LoginLog.create(user))
    }
}
