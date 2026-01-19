package sionic.infrastructure.ai

import sionic.chat.domain.Answer

interface AiClient {
    fun completion(
        context: List<Pair<String, String>>,
        question: String,
        model: String?
    ): Answer
}
