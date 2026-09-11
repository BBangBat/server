package com.bbangbat

import com.bbangbat.review.application.ReviewService
import org.springframework.stereotype.Component
import com.bbangbat.member.application.ReviewPort as MemberReviewPort
import com.bbangbat.store.application.ReviewPort as StoreReviewPort

@Component
class ReviewPortAdapter(
    private val reviewService: ReviewService,
) : MemberReviewPort,
    StoreReviewPort {
    override fun countByMemberId(memberId: Long): Long = reviewService.countByMemberId(memberId)

    override fun countByStoreIds(storeIds: Collection<Long>): Map<Long, Long> = reviewService.countByStoreIds(storeIds)
}
