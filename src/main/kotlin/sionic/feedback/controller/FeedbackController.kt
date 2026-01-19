package sionic.feedback.controller

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
import sionic.common.exception.ErrorResponse
import sionic.common.exception.UserNotFoundException
import sionic.feedback.dto.CreateFeedbackRequest
import sionic.feedback.dto.FeedbackResponse
import sionic.feedback.dto.UpdateFeedbackStatusRequest
import sionic.feedback.service.FeedbackService
import sionic.user.repository.UserRepository

@Tag(name = "피드백", description = "대화에 대한 사용자 피드백 관리 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/feedbacks")
class FeedbackController(
    private val feedbackService: FeedbackService,
    private val userRepository: UserRepository
) {
    @Operation(
        summary = "피드백 생성/수정",
        description = "특정 대화에 대한 피드백(긍정/부정)을 생성하거나 수정합니다. 동일 대화에 대해 중복 생성 시 수정됩니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "피드백 생성/수정 성공",
                content = [Content(schema = Schema(implementation = FeedbackResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "접근 권한 없음",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "대화를 찾을 수 없음",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @PostMapping
    fun createFeedback(
        @Parameter(hidden = true) @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: CreateFeedbackRequest
    ): ResponseEntity<FeedbackResponse> {
        val response = feedbackService.createOrUpdateFeedback(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @Operation(
        summary = "피드백 목록 조회",
        description = "피드백 목록을 조회합니다. 일반 사용자는 본인 피드백만, 관리자는 모든 피드백을 볼 수 있습니다."
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
    fun listFeedbacks(
        @Parameter(hidden = true) @AuthenticationPrincipal userId: Long,
        @Parameter(description = "긍정/부정 필터 (true: 긍정, false: 부정, null: 전체)")
        @RequestParam(required = false) isPositive: Boolean?,
        @Parameter(description = "페이지 정보")
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC)
        pageable: Pageable
    ): ResponseEntity<Page<FeedbackResponse>> {
        val user = userRepository.findById(userId).orElseThrow { UserNotFoundException() }
        val response = feedbackService.listFeedbacks(userId, user, isPositive, pageable)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "피드백 상태 변경",
        description = "피드백의 상태를 PENDING에서 RESOLVED로 변경합니다. 관리자만 사용 가능합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "상태 변경 성공",
                content = [Content(schema = Schema(implementation = FeedbackResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "관리자 권한 필요",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "피드백을 찾을 수 없음",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @PutMapping("/{feedbackId}/status")
    fun updateStatus(
        @Parameter(hidden = true) @AuthenticationPrincipal userId: Long,
        @Parameter(description = "피드백 ID") @PathVariable feedbackId: Long,
        @RequestBody request: UpdateFeedbackStatusRequest
    ): ResponseEntity<FeedbackResponse> {
        val user = userRepository.findById(userId).orElseThrow { UserNotFoundException() }
        val response = feedbackService.updateStatus(feedbackId, user, request.status)
        return ResponseEntity.ok(response)
    }
}
