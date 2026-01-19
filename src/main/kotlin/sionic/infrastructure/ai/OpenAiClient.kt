package sionic.infrastructure.ai

import com.openai.client.OpenAIClient
import com.openai.models.ChatModel
import com.openai.models.chat.completions.ChatCompletionCreateParams
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import sionic.chat.domain.Answer

@Component
@Primary
class OpenAiClient(
    private val openAIClient: OpenAIClient
) : AiClient {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun completion(
        context: List<Pair<String, String>>,
        question: String,
        model: String?
    ): Answer {
        val selectedModel = resolveModel(model)
        
        log.debug("OpenAI API 요청: model={}", selectedModel)

        val params = ChatCompletionCreateParams.builder()
            .model(selectedModel)
            .apply {
                // 시스템 메시지
                addSystemMessage("당신은 친절하고 도움이 되는 AI 어시스턴트입니다. 사용자의 질문에 명확하고 정확하게 답변해주세요.")
                
                // 이전 대화 문맥
                context.forEach { (q, a) ->
                    addUserMessage(q)
                    addAssistantMessage(a)
                }
                
                // 현재 질문
                addUserMessage(question)
            }
            .build()

        return try {
            val chatCompletion = openAIClient.chat().completions().create(params)
            val content = chatCompletion.choices().firstOrNull()?.message()?.content()?.orElse("") ?: ""
            
            log.debug("OpenAI API 응답: {}", content.take(100))
            Answer(content)
        } catch (e: Exception) {
            log.error("OpenAI API 호출 실패: ${e.message}", e)
            Answer("죄송합니다. AI 응답을 생성하는 중 오류가 발생했습니다: ${e.message}")
        }
    }

    private fun resolveModel(model: String?): ChatModel {
        return when (model?.lowercase()) {
            "gpt-4" -> ChatModel.GPT_4
            "gpt-4o" -> ChatModel.GPT_4O
            "gpt-4o-mini" -> ChatModel.GPT_4O_MINI
            "gpt-4-turbo" -> ChatModel.GPT_4_TURBO
            "gpt-3.5-turbo" -> ChatModel.GPT_3_5_TURBO
            else -> ChatModel.GPT_4O_MINI // 기본값: 무료 모델
        }
    }
}
