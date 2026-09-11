package com.bbangbat.store.api.dto

import com.bbangbat.store.domain.Store
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "가게 응답")
data class StoreResponse(
    @field:Schema(description = "가게 ID", example = "1") val id: Long,
    @field:Schema(description = "가게명", example = "홍길동 베이커리") val name: String,
    @field:Schema(description = "위도", example = "37.5665") val latitude: Double,
    @field:Schema(description = "경도", example = "126.9780") val longitude: Double,
    @field:Schema(description = "주소", example = "서울시 강남구 테헤란로 1") val address: String,
    @field:Schema(description = "전화번호", example = "02-1234-5678") val phoneNumber: String?,
    @field:Schema(description = "가게 이미지 URL", example = "https://.../stores/1.jpg") val imageUrl: String,
    @field:Schema(description = "빵명록 수", example = "12") val reviewCount: Long,
) {
    companion object {
        fun from(
            store: Store,
            reviewCount: Long,
        ): StoreResponse =
            StoreResponse(
                id = store.id,
                name = store.name,
                latitude = store.latitude,
                longitude = store.longitude,
                address = store.address,
                phoneNumber = store.phoneNumber,
                imageUrl = requireNotNull(store.imageUrl) { "가게 이미지 URL은 서비스에서 채워져야 합니다" },
                reviewCount = reviewCount,
            )

        fun listOf(
            stores: List<Store>,
            reviewCounts: Map<Long, Long>,
        ): List<StoreResponse> = stores.map { from(it, reviewCounts[it.id] ?: 0L) }
    }
}
