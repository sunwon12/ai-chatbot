package sionic.user.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import sionic.common.exception.InvalidPasswordException

class PasswordTest {

    private val encoder = BCryptPasswordEncoder()

    @Nested
    @DisplayName("Password 생성 시")
    inner class Create {

        @Test
        @DisplayName("평문 비밀번호를 암호화하여 생성한다")
        fun encodeRawPassword() {
            val password = Password.encode("password123", encoder)

            assertThat(password.value).isNotEqualTo("password123")
            assertThat(password.value).startsWith("\$2a\$")
        }

        @Test
        @DisplayName("빈 비밀번호는 생성에 실패한다")
        fun failWithEmptyPassword() {
            assertThatThrownBy { Password.encode("", encoder) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    @DisplayName("비밀번호 검증 시")
    inner class Validate {

        @Test
        @DisplayName("올바른 비밀번호는 검증에 성공한다")
        fun matchesSuccess() {
            val password = Password.encode("password123", encoder)

            assertThat(password.matches("password123", encoder)).isTrue()
        }

        @Test
        @DisplayName("잘못된 비밀번호는 검증에 실패한다")
        fun matchesFail() {
            val password = Password.encode("password123", encoder)

            assertThat(password.matches("wrongpassword", encoder)).isFalse()
        }

        @Test
        @DisplayName("validateAndThrow는 잘못된 비밀번호일 때 예외를 던진다")
        fun validateAndThrowFail() {
            val password = Password.encode("password123", encoder)

            assertThatThrownBy { password.validateAndThrow("wrongpassword", encoder) }
                .isInstanceOf(InvalidPasswordException::class.java)
        }
    }
}
