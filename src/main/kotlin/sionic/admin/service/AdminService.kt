package sionic.admin.service

import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sionic.admin.domain.ActivityStats
import sionic.admin.domain.DateRange
import sionic.admin.domain.StatsResponse
import sionic.admin.repository.LoginLogRepository
import sionic.chat.repository.ChatRepository
import sionic.user.repository.UserRepository
import java.time.Instant

@Service
@Transactional(readOnly = true)
class AdminService(
    private val userRepository: UserRepository,
    private val loginLogRepository: LoginLogRepository,
    private val chatRepository: ChatRepository,
    private val csvReportGenerator: CsvReportGenerator
) {
    fun getStats(): StatsResponse {
        val range = DateRange.lastDay(Instant.now())

        val stats = ActivityStats(
            signupCount = userRepository.countByCreatedAtBetween(range.from, range.to),
            loginCount = loginLogRepository.countByCreatedAtBetween(range.from, range.to),
            chatCount = chatRepository.countByCreatedAtBetween(range.from, range.to)
        )

        return stats.toResponse()
    }

    fun generateReport(response: HttpServletResponse) {
        val range = DateRange.lastDay(Instant.now())
        val chatsStream = chatRepository.streamAllByCreatedAtBetween(range.from, range.to)

        response.contentType = "text/csv"
        response.setHeader("Content-Disposition", "attachment; filename=\"report.csv\"")

        csvReportGenerator.generate(chatsStream, response.outputStream)
    }
}
