package sionic.admin.repository

import org.springframework.data.jpa.repository.JpaRepository
import sionic.admin.domain.LoginLog
import java.time.Instant

interface LoginLogRepository : JpaRepository<LoginLog, Long> {
    fun countByCreatedAtBetween(from: Instant, to: Instant): Long
}
