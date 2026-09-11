package com.bbangbat.store.domain

data class Store(
    val id: Long = 0L,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val phoneNumber: String? = null,
    val imageUrl: String? = null,
) {
    init {
        require(name.isNotBlank()) { "가게명은 비어 있을 수 없습니다." }
        require(latitude in -90.0..90.0) { "위도는 -90 ~ 90 사이여야 합니다." }
        require(longitude in -180.0..180.0) { "경도는 -180 ~ 180 사이여야 합니다." }
        require(address.isNotBlank()) { "주소는 비어 있을 수 없습니다." }
    }

    companion object {
        const val SEARCH_RADIUS_METERS = 3000.0
    }
}
