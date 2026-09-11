package com.bbangbat.store.api

import com.bbangbat.store.api.dto.StoreResponse
import com.bbangbat.store.application.StoreService
import com.bbangbat.store.application.StoreStatsService
import com.bbangbat.store.domain.MapBounds
import com.bbangbat.store.domain.Store
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "가게", description = "가게 API")
@Validated
@RestController
@RequestMapping("/api/stores")
class StoreController(
    private val storeService: StoreService,
    private val storeStatsService: StoreStatsService,
) {
    @Operation(summary = "반경 내 가게 리스트 조회", description = "지도 중심 좌표 기준 반경 3km 내 가게를 거리순으로 조회합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "400", description = "잘못된 좌표 요청"),
    )
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    fun getStores(
        @RequestParam lat: Double,
        @RequestParam lng: Double,
    ): List<StoreResponse> = withReviewCounts(storeService.findStores(lat, lng))

    @Operation(
        summary = "지도 영역 내 가게 조회",
        description =
            "지도 사각 영역(남/북/서/동) 안의 가게를 조회합니다. " +
                "요청 범위는 서비스 지역(대전) 경계로 잘라내며, 최대 300개까지 반환합니다.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "400", description = "잘못된 영역 (north<=south 또는 east<=west)"),
    )
    @GetMapping("/bounds")
    @ResponseStatus(HttpStatus.OK)
    fun getStoresInBounds(
        @Parameter(description = "남쪽 위도", example = "36.30") @RequestParam south: Double,
        @Parameter(description = "북쪽 위도", example = "36.40") @RequestParam north: Double,
        @Parameter(description = "서쪽 경도", example = "127.30") @RequestParam west: Double,
        @Parameter(description = "동쪽 경도", example = "127.45") @RequestParam east: Double,
    ): List<StoreResponse> = withReviewCounts(storeService.findInBounds(MapBounds(south = south, north = north, west = west, east = east)))

    @Operation(
        summary = "가게 일괄 조회",
        description = "가게 ID 목록으로 여러 가게를 한 번에 조회합니다. 최대 100개까지 요청할 수 있습니다.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "400", description = "요청 개수 초과(100개 초과)"),
    )
    @GetMapping("/bulk")
    @ResponseStatus(HttpStatus.OK)
    fun getStoresByIds(
        @Parameter(description = "조회할 가게 ID 목록 (쉼표 구분)", example = "1,2,3")
        @RequestParam
        @Size(max = 100, message = "가게는 한 번에 100개까지 조회할 수 있습니다.")
        storeIds: List<Long>,
    ): List<StoreResponse> = withReviewCounts(storeService.findByIds(storeIds))

    @Operation(summary = "가게 단건 조회", description = "가게 ID로 가게 상세 정보를 조회합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "404", description = "가게 없음"),
    )
    @GetMapping("/{storeId}")
    @ResponseStatus(HttpStatus.OK)
    fun getStore(
        @PathVariable storeId: Long,
    ): StoreResponse {
        val store = storeService.findById(storeId)

        return StoreResponse.from(store, storeStatsService.reviewCounts(listOf(storeId)).getValue(storeId))
    }

    private fun withReviewCounts(stores: List<Store>): List<StoreResponse> =
        StoreResponse.listOf(stores, storeStatsService.reviewCounts(stores.map { it.id }))
}
