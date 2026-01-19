package sionic.user.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import sionic.common.exception.InvalidPasswordException

class UserTest {

    private val encoder = BCryptPasswordEncoder()

    @Nested
    @DisplayName("User 생성 시")
    inner class Create {

        @Test
        @DisplayName("정적 팩토리 메서드로 ADMIN를 생성한다")
        fun createUser() {
            val user = User.create(
                email = Email("test@example.com"),
                rawPassword = "password123",
                name = "테스트",
                encoder = encoder
            )

            assertThat(user.email.value).isEqualTo("test@example.com")
            assertThat(user.name).isEqualTo("테스트")
            assertThat(user.role).isEqualTo(Role.ADMIN)
        }

        @Test
        @DisplayName("관리자 권한으로 User를 생성할 수 있다")
        fun createAdminUser() {
            val user = User.create(
                email = Email("admin@example.com"),
                rawPassword = "password123",
                name = "관리자",
                encoder = encoder,
                role = Role.ADMIN
            )

            assertThat(user.role).isEqualTo(Role.ADMIN)
            assertThat(user.role.isAdmin()).isTrue()
        }
    }

    @Nested
    @DisplayName("User 인증 시")
    inner class Authenticate {

        @Test
        @DisplayName("올바른 비밀번호로 인증에 성공한다")
        fun authenticateSuccess() {
            val user = User.create(
                email = Email("test@example.com"),
                rawPassword = "password123",
                name = "테스트",
                encoder = encoder
            )

            // 예외가 발생하지 않으면 성공
            user.authenticate("password123", encoder)
        }

        @Test
        @DisplayName("잘못된 비밀번호로 인증에 실패한다")
        fun authenticateFail() {
            val user = User.create(
                email = Email("test@example.com"),
                rawPassword = "password123",
                name = "테스트",
                encoder = encoder
            )

            assertThatThrownBy { user.authenticate("wrongpassword", encoder) }
                .isInstanceOf(InvalidPasswordException::class.java)
        }
    }

    @Nested
    @DisplayName("User 권한 확인 시")
    inner class AccessControl {

        @Test
        @DisplayName("본인 리소스에 접근 가능하다")
        fun canAccessOwnResource() {
            val user = createUserWithId(1L)

            assertThat(user.canAccess(1L)).isTrue()
        }

        @Test
        @DisplayName("관리자는 모든 리소스에 접근 가능하다")
        fun adminCanAccessAllResources() {
            val admin = createAdminWithId(1L)

            assertThat(admin.canAccess(999L)).isTrue()
        }

        @Test
        @DisplayName("일반 사용자는 타인 리소스에 접근 불가하다")
        fun memberCannotAccessOthersResource() {
            val user = createUserWithId(1L)

            assertThat(user.canAccess(999L)).isFalse()
        }

        private fun createUserWithId(id: Long): User {
            // 테스트용 리플렉션으로 ID 설정 - MEMBER 역할로 명시적 생성
            val user = User.create(
                email = Email("test$id@example.com"),
                rawPassword = "password123",
                name = "테스트$id",
                encoder = encoder,
                role = Role.MEMBER // MVP에서는 기본이 ADMIN이므로 테스트에서 명시적으로 MEMBER 지정
            )
            val idField = User::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(user, id)
            return user
        }

        private fun createAdminWithId(id: Long): User {
            val user = User.create(
                email = Email("admin$id@example.com"),
                rawPassword = "password123",
                name = "관리자$id",
                encoder = encoder,
                role = Role.ADMIN
            )
            val idField = User::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(user, id)
            return user
        }
    }
}
