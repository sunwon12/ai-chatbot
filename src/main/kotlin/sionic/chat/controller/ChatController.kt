package sionic.chat.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import sionic.chat.dto.ChatResponse
import sionic.chat.dto.CreateChatRequest
import sionic.chat.dto.ThreadWithChatsDto
import sionic.chat.service.ChatService
import sionic.common.exception.ErrorResponse
import sionic.common.exception.UserNotFoundException
import sionic.user.repository.UserRepository

@Tag(name = "대화", description = "AI 챗봇 대화 관련 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/chats")
class ChatController(
    private val chatService: ChatService,
    private val userRepository: UserRepository
) {
    @Operation(
        summary = "대화 생성",
        description = "AI에게 질문을 보내고 답변을 받습니다. 30분 내 질문은 같은 스레드로 묶입니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "대화 생성 성공",
                content = [Content(schema = Schema(implementation = ChatResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @PostMapping
    fun createChat(
        @Parameter(hidden = true) @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: CreateChatRequest
    ): ResponseEntity<ChatResponse> {
        val response = chatService.createChat(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @Operation(
        summary = "대화 목록 조회",
        description = "사용자의 대화 목록을 스레드별로 그룹화하여 조회합니다. 관리자는 모든 대화를 볼 수 있습니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공"
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @GetMapping
    fun getChats(
        @Parameter(hidden = true) @AuthenticationPrincipal userId: Long,
        @Parameter(description = "페이지 정보 (page, size, sort)")
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC)
        pageable: Pageable
    ): ResponseEntity<Page<ThreadWithChatsDto>> {
        val user = userRepository.findById(userId).orElseThrow { UserNotFoundException() }
        val response = chatService.getChats(userId, user, pageable)
        return ResponseEntity.ok(response)
    }
}
