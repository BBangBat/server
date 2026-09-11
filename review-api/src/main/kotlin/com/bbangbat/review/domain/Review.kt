package com.bbangbat.review.domain

import java.time.LocalDateTime

data class Review(
    val id: Long = 0L,
    val memberId: Long,
    val storeId: Long,
    val rating: Int,
    val content: String,
    val menus: List<String>,
    val imageUrls: List<String>,
    val createdAt: LocalDateTime? = null,
) {
    init {
        require(rating in 1..5) { "별점은 1~5점 사이여야 합니다." }
        require(content.length in 10..500) { "후기는 10자 이상 500자 이하여야 합니다." }
        require(menus.isNotEmpty()) { "구매한 메뉴를 입력해야 합니다." }
        require(imageUrls.size <= 5) { "사진은 최대 5장까지 업로드할 수 있습니다." }
    }

    fun canBeDeletedBy(memberId: Long): Boolean = this.memberId == memberId
}
