package com.bbangbat.review.application

import com.bbangbat.common.exception.BbangbatException
import com.bbangbat.common.exception.ErrorCode.REVIEW_FORBIDDEN
import com.bbangbat.common.exception.ErrorCode.REVIEW_NOT_FOUND
import com.bbangbat.review.domain.Review
import com.bbangbat.review.repository.ReviewPersistenceAdapter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReviewService(
    private val reviewPersistenceAdapter: ReviewPersistenceAdapter,
    private val s3Service: S3Service,
    private val storePort: StorePort,
    private val memberPort: MemberPort,
) {
    fun getReviews(storeId: Long): List<AuthoredReview> = withAuthors(reviewPersistenceAdapter.findAllByStoreId(storeId))

    private fun withAuthors(reviews: List<Review>): List<AuthoredReview> {
        val authors = memberPort.findAuthors(reviews.map { it.memberId }.toSet())

        return reviews.map { review -> AuthoredReview.of(review, authors[review.memberId]) }
    }

    fun getMyReviews(memberId: Long): List<MyReview> {
        val reviews = reviewPersistenceAdapter.findAllByMemberId(memberId)

        val storesById = storePort.findByIds(reviews.map { it.storeId }.toSet())

        return reviews.map { review ->
            val store =
                checkNotNull(storesById[review.storeId]) {
                    "리뷰에 연결된 가게를 찾을 수 없습니다. storeId=${review.storeId}"
                }
            MyReview(review = review, store = store)
        }
    }

    @Transactional(readOnly = true)
    fun countByMemberId(memberId: Long): Long = reviewPersistenceAdapter.countByMemberId(memberId)

    @Transactional(readOnly = true)
    fun countByStoreIds(storeIds: Collection<Long>): Map<Long, Long> =
        reviewPersistenceAdapter.countByStoreIds(storeIds).associate { it.storeId to it.count }

    @Transactional
    fun create(
        memberId: Long,
        storeId: Long,
        rating: Int,
        content: String,
        menus: List<String>,
        imageKeys: List<String>,
    ): AuthoredReview {
        val review =
            Review(
                memberId = memberId,
                storeId = storeId,
                rating = rating,
                content = content,
                menus = menus,
                imageUrls = imageKeys.map { s3Service.buildUrl(it) },
            )

        val saved = reviewPersistenceAdapter.save(review)

        return AuthoredReview.of(saved, memberPort.findAuthors(setOf(memberId))[memberId])
    }

    @Transactional
    fun delete(
        memberId: Long,
        reviewId: Long,
    ) {
        val review =
            reviewPersistenceAdapter.findByIdOrNull(reviewId)
                ?: throw BbangbatException(REVIEW_NOT_FOUND)

        if (!review.canBeDeletedBy(memberId)) throw BbangbatException(REVIEW_FORBIDDEN)

        reviewPersistenceAdapter.delete(review)

        review.imageUrls.forEach { url ->
            val objectKey = url.substringAfter(".amazonaws.com/")
            s3Service.delete(objectKey)
        }
    }

    fun generatePresignedUrls(contentTypes: List<String>): List<PresignedUpload> = contentTypes.map { s3Service.generatePresignedUrl(it) }
}
