package com.bbangbat.live.api

import com.bbangbat.auth.voter.VoterResolver
import com.bbangbat.common.exception.ErrorResponse
import com.bbangbat.live.api.dto.CongestionResponse
import com.bbangbat.live.api.dto.CongestionVoteRequest
import com.bbangbat.live.application.CongestionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "혼잡도", description = "실시간 혼잡도 API")
@RestController
@RequestMapping("/api/congestion")
class CongestionController(
    private val congestionService: CongestionService,
    private val voterResolver: VoterResolver,
) {
    @Operation(
        summary = "혼잡도 투표",
        description = "가게의 현재 혼잡도를 투표합니다. 회원/비회원 모두 가능하며, 같은 사용자의 재투표는 덮어씁니다.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "투표 완료, 갱신된 혼잡도 반환"),
        ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 (필수값 누락 등)",
            content = [
                Content(
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(value = "{\"code\": \"INVALID_INPUT\", \"message\": \"혼잡도는 필수입니다.\"}")],
                ),
            ],
        ),
        ApiResponse(
            responseCode = "401",
            description = "식별 토큰 없음 (비회원은 익명 토큰 발급 필요)",
            content = [
                Content(
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(value = "{\"code\": \"UNAUTHORIZED\", \"message\": \"인증이 필요합니다.\"}")],
                ),
            ],
        ),
    )
    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    fun vote(
        @RequestBody @Valid request: CongestionVoteRequest,
        httpRequest: HttpServletRequest,
    ): CongestionResponse {
        val voter = voterResolver.resolve(httpRequest)
        val congestion =
            congestionService.vote(
                storeId = request.storeId!!,
                level = request.level!!,
                voter = voter,
            )

        return CongestionResponse.from(congestion)
    }

    @Operation(summary = "혼잡도 단건 조회", description = "가게의 최근 15분 내 투표를 집계한 현재 혼잡도와 혼잡도별 투표 수를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{storeId}")
    @ResponseStatus(HttpStatus.OK)
    fun getCongestion(
        @Parameter(description = "조회할 가게 ID", example = "1")
        @PathVariable storeId: Long,
    ): CongestionResponse = CongestionResponse.from(congestionService.getCongestion(storeId))

    @Operation(
        summary = "혼잡도 벌크 조회",
        description =
            "여러 가게의 현재 혼잡도를 한 번에 조회합니다. 빵집 리스트 화면에서 가게별 혼잡도 표시 용도입니다. " +
                "요청한 모든 storeId가 응답에 포함됩니다 (투표 없는 가게는 UNCROWDED).",
    )
    @ApiResponse(responseCode = "200", description = "조회 성공 (가게별 혼잡도 배열)")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    fun getCongestions(
        @Parameter(description = "조회할 가게 ID 목록 (쉼표 구분)", example = "1,2,3")
        @RequestParam storeIds: List<Long>,
    ): List<CongestionResponse> =
        congestionService
            .getCongestions(storeIds)
            .values
            .map { CongestionResponse.from(it) }
}
