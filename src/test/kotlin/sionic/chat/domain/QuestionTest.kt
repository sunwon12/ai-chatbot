package sionic.chat.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import sionic.common.exception.InvalidQuestionException

class QuestionTest {

    @Nested
    @DisplayName("Question 생성 시")
    inner class Create {

        @Test
        @DisplayName("값이 있는 Question을 생성할 수 있다")
        fun createWithValue() {
            val question = Question("테스트 질문")

            assertThat(question.value).isEqualTo("테스트 질문")
        }

        @Test
        @DisplayName("빈 문자열은 생성에 실패한다")
        fun failWithEmptyString() {
            assertThatThrownBy { Question("") }
                .isInstanceOf(InvalidQuestionException::class.java)
        }

        @Test
        @DisplayName("공백만 있는 문자열은 생성에 실패한다")
        fun failWithBlankString() {
            assertThatThrownBy { Question("   ") }
                .isInstanceOf(InvalidQuestionException::class.java)
        }
    }
}
