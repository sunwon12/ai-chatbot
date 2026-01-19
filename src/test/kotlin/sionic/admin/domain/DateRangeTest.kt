package sionic.admin.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant

class DateRangeTest {

    @Nested
    @DisplayName("DateRange 생성 시")
    inner class Create {

        @Test
        @DisplayName("lastDay는 현재 시점 기준 24시간 범위를 생성한다")
        fun lastDayCreatesCorrectRange() {
            val now = Instant.parse("2024-01-15T12:00:00Z")
            val range = DateRange.lastDay(now)

            assertThat(range.from).isEqualTo(Instant.parse("2024-01-14T12:00:00Z"))
            assertThat(range.to).isEqualTo(now)
        }
    }

    @Nested
    @DisplayName("DateRange contains 확인 시")
    inner class Contains {

        @Test
        @DisplayName("범위 내 시점은 true를 반환한다")
        fun containsReturnsTrueForInstantInRange() {
            val now = Instant.parse("2024-01-15T12:00:00Z")
            val range = DateRange.lastDay(now)
            val instantInRange = Instant.parse("2024-01-15T00:00:00Z")

            assertThat(range.contains(instantInRange)).isTrue()
        }

        @Test
        @DisplayName("범위 외 시점은 false를 반환한다")
        fun containsReturnsFalseForInstantOutOfRange() {
            val now = Instant.parse("2024-01-15T12:00:00Z")
            val range = DateRange.lastDay(now)
            val instantOutOfRange = Instant.parse("2024-01-13T00:00:00Z")

            assertThat(range.contains(instantOutOfRange)).isFalse()
        }

        @Test
        @DisplayName("경계값은 범위에 포함된다")
        fun containsIncludesBoundary() {
            val now = Instant.parse("2024-01-15T12:00:00Z")
            val range = DateRange.lastDay(now)

            assertThat(range.contains(range.from)).isTrue()
            assertThat(range.contains(range.to)).isTrue()
        }
    }
}
