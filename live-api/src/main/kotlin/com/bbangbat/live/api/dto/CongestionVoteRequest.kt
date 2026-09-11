package com.bbangbat.live.api.dto

import com.bbangbat.live.domain.CongestionLevel
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "혼잡도 투표 요청")
data class CongestionVoteRequest(
    @field:Schema(description = "가게 ID", example = "1")
    @field:NotNull(message = "가게 ID는 필수입니다.")
    val storeId: Long?,
    @field:Schema(description = "혼잡도", example = "CROWDED")
    @field:NotNull(message = "혼잡도는 필수입니다.")
    val level: CongestionLevel?,
)
