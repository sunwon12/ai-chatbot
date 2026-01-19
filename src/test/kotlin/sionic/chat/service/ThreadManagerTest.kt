package sionic.chat.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import sionic.chat.domain.Thread
import sionic.chat.repository.ThreadRepository
import sionic.user.domain.Email
import sionic.user.domain.User
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class ThreadManagerTest {

    @Mock
    lateinit var threadRepository: ThreadRepository

    @InjectMocks
    lateinit var threadManager: ThreadManager

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
    @DisplayName("getOrCreateThread 호출 시")
    inner class GetOrCreateThread {

        @Test
        @DisplayName("기존 스레드가 없으면 새 스레드를 생성한다")
        fun createNewThreadWhenNoExisting() {
            whenever(threadRepository.findTopByUserOrderByLastMessageAtDesc(user)).thenReturn(null)
            whenever(threadRepository.save(any<Thread>())).thenAnswer { it.arguments[0] }

            val thread = threadManager.getOrCreateThread(user, Instant.now())

            assertThat(thread).isNotNull
            assertThat(thread.user).isEqualTo(user)
        }

        @Test
        @DisplayName("기존 스레드가 만료되면 새 스레드를 생성한다")
        fun createNewThreadWhenExpired() {
            val expiredThread = Thread.create(user)
            setLastMessageAt(expiredThread, Instant.now().minusSeconds(31 * 60))

            whenever(threadRepository.findTopByUserOrderByLastMessageAtDesc(user)).thenReturn(expiredThread)
            whenever(threadRepository.save(any<Thread>())).thenAnswer { it.arguments[0] }

            val thread = threadManager.getOrCreateThread(user, Instant.now())

            assertThat(thread).isNotNull
            assertThat(thread).isNotEqualTo(expiredThread)
        }

        @Test
        @DisplayName("기존 스레드가 만료되지 않으면 기존 스레드를 반환한다")
        fun returnExistingThreadWhenNotExpired() {
            val existingThread = Thread.create(user)

            whenever(threadRepository.findTopByUserOrderByLastMessageAtDesc(user)).thenReturn(existingThread)

            val thread = threadManager.getOrCreateThread(user, Instant.now())

            assertThat(thread).isEqualTo(existingThread)
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
