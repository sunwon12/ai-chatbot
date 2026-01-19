package sionic.admin.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import sionic.admin.domain.StatsResponse
import sionic.admin.service.AdminService
import sionic.common.exception.ErrorResponse

@Tag(name = "관리자", description = "관리자 전용 통계 및 보고서 API (ADMIN 권한 필요)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
class AdminController(
    private val adminService: AdminService
) {
    @Operation(
        summary = "활동 통계 조회",
        description = "최근 24시간 동안의 회원가입 수, 로그인 수, 대화 생성 수를 조회합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "통계 조회 성공",
                content = [Content(schema = Schema(implementation = StatsResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "관리자 권한 필요",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @GetMapping("/stats")
    fun getStats(): ResponseEntity<StatsResponse> {
        val response = adminService.getStats()
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "CSV 보고서 다운로드",
        description = "최근 24시간 동안의 모든 대화 목록을 CSV 파일로 다운로드합니다. 사용자 정보도 포함됩니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "CSV 파일 다운로드",
                content = [Content(mediaType = "text/csv")]
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "관리자 권한 필요",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @GetMapping("/reports/csv")
    fun downloadCsv(response: HttpServletResponse) {
        adminService.generateReport(response)
    }
}
