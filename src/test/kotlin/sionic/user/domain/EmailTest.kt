package sionic.user.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import sionic.common.exception.InvalidEmailFormatException

class EmailTest {

    @Nested
    @DisplayName("Email 생성 시")
    inner class Create {

        @Test
        @DisplayName("올바른 형식의 이메일은 생성에 성공한다")
        fun success() {
            val email = Email("test@example.com")

            assertThat(email.value).isEqualTo("test@example.com")
        }

        @Test
        @DisplayName("잘못된 형식의 이메일은 생성에 실패한다")
        fun failWithInvalidFormat() {
            assertThatThrownBy { Email("invalid-email") }
                .isInstanceOf(InvalidEmailFormatException::class.java)
        }

        @Test
        @DisplayName("@ 기호가 없는 이메일은 생성에 실패한다")
        fun failWithoutAtSymbol() {
            assertThatThrownBy { Email("testexample.com") }
                .isInstanceOf(InvalidEmailFormatException::class.java)
        }
    }
}
