package sionic.chat.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sionic.chat.domain.Chat
import sionic.chat.domain.Question
import sionic.chat.dto.ChatResponse
import sionic.chat.dto.CreateChatRequest
import sionic.chat.dto.ThreadWithChatsDto
import sionic.chat.repository.ChatRepository
import sionic.chat.repository.ThreadRepository
import sionic.common.exception.UserNotFoundException
import sionic.infrastructure.ai.AiClient
import sionic.user.domain.Role
import sionic.user.domain.User
import sionic.user.repository.UserRepository
import java.time.Instant

@Service
@Transactional(readOnly = true)
class ChatService(
    private val chatRepository: ChatRepository,
    private val threadRepository: ThreadRepository,
    private val userRepository: UserRepository,
    private val threadManager: ThreadManager,
    private val aiClient: AiClient
) {
    @Transactional
    fun createChat(userId: Long, request: CreateChatRequest): ChatResponse {
        val user = userRepository.findById(userId).orElseThrow { UserNotFoundException() }

        // 1. 스레드 결정 (Domain Service에 위임)
        val thread = threadManager.getOrCreateThread(user, Instant.now())

        // 2. 채팅 생성 (빈 답변으로 시작)
        val question = Question(request.question)
        val chat = Chat.create(thread, question)
        chatRepository.save(chat)

        // 3. AI 호출 (동기)
        val context = thread.chats.dropLast(1).map { it.question.value to it.answer.value }
        val answer = aiClient.completion(context, request.question, request.model)

        // 4. 최종 답변 저장
        chat.updateAnswer(answer)
        chatRepository.save(chat)

        return ChatResponse.from(chat)
    }

    fun getChats(userId: Long, user: User, pageable: Pageable): Page<ThreadWithChatsDto> {
        val threads = if (user.role == Role.ADMIN) {
            threadRepository.findAll(pageable)
        } else {
            PageImpl(threadRepository.findAllByUserOrderByCreatedAtDesc(user), pageable, 0)
        }

        return threads.map { ThreadWithChatsDto.from(it) }
    }
}
