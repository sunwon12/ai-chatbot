package sionic.infrastructure.ai

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Component
import sionic.chat.domain.Answer

@Component
@ConditionalOnMissingBean(name = ["openAiClient"])
class MockAiClient : AiClient {

    override fun completion(
        context: List<Pair<String, String>>,
        question: String,
        model: String?
    ): Answer {
        return Answer(generateMockResponse(question))
    }

    private fun generateMockResponse(question: String): String {
        return "이것은 \"$question\"에 대한 Mock AI 응답입니다. " +
                "실제 환경에서는 OpenAI API를 통해 생성된 응답이 반환됩니다."
    }
}
