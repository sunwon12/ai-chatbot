package sionic.chat.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import sionic.chat.service.ThreadService
import sionic.common.exception.ErrorResponse

@Tag(name = "스레드", description = "대화 스레드 관리 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/threads")
class ThreadController(
    private val threadService: ThreadService
) {
    @Operation(
        summary = "스레드 삭제",
        description = "특정 스레드와 해당 스레드의 모든 대화를 삭제합니다. 본인의 스레드만 삭제할 수 있습니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "삭제 성공"
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "접근 권한 없음 (본인 스레드가 아님)",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "스레드를 찾을 수 없음",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @DeleteMapping("/{threadId}")
    fun deleteThread(
        @Parameter(hidden = true) @AuthenticationPrincipal userId: Long,
        @Parameter(description = "삭제할 스레드 ID") @PathVariable threadId: Long
    ): ResponseEntity<Void> {
        threadService.deleteThread(threadId, userId)
        return ResponseEntity.noContent().build()
    }
}
