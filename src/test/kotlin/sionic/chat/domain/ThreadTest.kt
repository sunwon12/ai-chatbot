package sionic.chat.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import sionic.common.exception.UnauthorizedAccessException
import sionic.user.domain.Email
import sionic.user.domain.User
import java.time.Instant

class ThreadTest {

    private val encoder = BCryptPasswordEncoder()
    private lateinit var user: User

    @BeforeEach
    fun setUp() {
        user = User.create(
            email = Email("test@example.com"),
            rawPassword = "password123",
            name = "테스트",
            encoder = encoder
        )
        setUserId(user, 1L)
    }

    @Nested
    @DisplayName("Thread 만료 확인 시")
    inner class Expiration {

        @Test
        @DisplayName("30분 이상 경과하면 만료된다")
        fun isExpiredAfter30Minutes() {
            val thread = Thread.create(user)
            setLastMessageAt(thread, Instant.now().minusSeconds(31 * 60))

            assertThat(thread.isExpired(Instant.now())).isTrue()
        }

        @Test
        @DisplayName("30분 미만이면 만료되지 않는다")
        fun isNotExpiredWithin30Minutes() {
            val thread = Thread.create(user)
            setLastMessageAt(thread, Instant.now().minusSeconds(29 * 60))

            assertThat(thread.isExpired(Instant.now())).isFalse()
        }

        @Test
        @DisplayName("정확히 30분이면 만료된다")
        fun isExpiredAtExactly30Minutes() {
            val thread = Thread.create(user)
            setLastMessageAt(thread, Instant.now().minusSeconds(30 * 60))

            assertThat(thread.isExpired(Instant.now())).isTrue()
        }
    }

    @Nested
    @DisplayName("Thread 소유권 확인 시")
    inner class Ownership {

        @Test
        @DisplayName("소유자 ID가 일치하면 true를 반환한다")
        fun isOwnedByTrueForOwner() {
            val thread = Thread.create(user)

            assertThat(thread.isOwnedBy(1L)).isTrue()
        }

        @Test
        @DisplayName("소유자 ID가 일치하지 않으면 false를 반환한다")
        fun isOwnedByFalseForNonOwner() {
            val thread = Thread.create(user)

            assertThat(thread.isOwnedBy(999L)).isFalse()
        }

        @Test
        @DisplayName("validateOwnership은 소유자가 아니면 예외를 던진다")
        fun validateOwnershipThrowsException() {
            val thread = Thread.create(user)

            assertThatThrownBy { thread.validateOwnership(999L) }
                .isInstanceOf(UnauthorizedAccessException::class.java)
        }
    }

    private fun setUserId(user: User, id: Long) {
        val idField = User::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(user, id)
    }

    private fun setLastMessageAt(thread: Thread, instant: Instant) {
        thread.updateLastMessageTime(instant)
    }
}
