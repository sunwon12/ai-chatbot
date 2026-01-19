package sionic.chat.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AnswerTest {

    @Nested
    @DisplayName("Answer 생성 시")
    inner class Create {

        @Test
        @DisplayName("빈 Answer를 생성할 수 있다")
        fun createEmpty() {
            val answer = Answer.empty()

            assertThat(answer.isEmpty()).isTrue()
            assertThat(answer.value).isEmpty()
        }

        @Test
        @DisplayName("값이 있는 Answer를 생성할 수 있다")
        fun createWithValue() {
            val answer = Answer("테스트 답변")

            assertThat(answer.isEmpty()).isFalse()
            assertThat(answer.value).isEqualTo("테스트 답변")
        }
    }

    @Nested
    @DisplayName("Answer 스트리밍 시")
    inner class Streaming {

        @Test
        @DisplayName("청크를 추가하면 새로운 Answer를 반환한다")
        fun appendChunk() {
            val answer = Answer.empty()
            val newAnswer = answer.appendChunk("첫 번째 ")

            assertThat(answer.value).isEmpty() // 원본은 불변
            assertThat(newAnswer.value).isEqualTo("첫 번째 ")
        }

        @Test
        @DisplayName("여러 청크를 순차적으로 추가할 수 있다")
        fun appendMultipleChunks() {
            val answer = Answer.empty()
                .appendChunk("첫 번째 ")
                .appendChunk("두 번째 ")
                .appendChunk("세 번째")

            assertThat(answer.value).isEqualTo("첫 번째 두 번째 세 번째")
        }
    }
}
