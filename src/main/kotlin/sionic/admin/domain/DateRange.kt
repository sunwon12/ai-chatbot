package sionic.admin.domain

import java.time.Duration
import java.time.Instant

data class DateRange(
    val from: Instant,
    val to: Instant
) {
    fun contains(instant: Instant): Boolean {
        return !instant.isBefore(from) && !instant.isAfter(to)
    }

    companion object {
        private val ONE_DAY = Duration.ofDays(1)

        fun lastDay(now: Instant = Instant.now()): DateRange {
            return DateRange(
                from = now.minus(ONE_DAY),
                to = now
            )
        }
    }
}
