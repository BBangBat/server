package com.bbangbat.review.repository

import com.bbangbat.common.exception.BbangbatException
import com.bbangbat.common.exception.ErrorCode.REVIEW_NOT_FOUND
import com.bbangbat.review.domain.Review
import org.springframework.stereotype.Repository

@Repository
class ReviewPersistenceAdapter(
    private val reviewRepository: ReviewRepository,
    private val reviewImageRepository: ReviewImageRepository,
    private val reviewMenuRepository: ReviewMenuRepository,
) {
    fun findByIdOrNull(id: Long): Review? {
        val entity = reviewRepository.findById(id).orElse(null) ?: return null

        return toDomains(listOf(entity)).firstOrNull()
    }

    fun findAllByStoreId(storeId: Long): List<Review> {
        val entities = reviewRepository.findAllByStoreIdOrderByIdDesc(storeId)

        return toDomains(entities)
    }

    fun findAllByMemberId(memberId: Long): List<Review> {
        val entities = reviewRepository.findAllByMemberIdOrderByIdDesc(memberId)

        return toDomains(entities)
    }

    fun countByMemberId(memberId: Long): Long = reviewRepository.countByMemberId(memberId)

    fun countByStoreIds(storeIds: Collection<Long>): List<StoreReviewCount> {
        val results: List<StoreReviewCount?> =
            reviewRepository.findAll {
                selectNew<StoreReviewCount>(
                    path(ReviewJpaEntity::storeId),
                    count(path(ReviewJpaEntity::id)),
                ).from(
                    entity(ReviewJpaEntity::class),
                ).where(
                    path(ReviewJpaEntity::storeId).`in`(storeIds.toList()),
                ).groupBy(
                    path(ReviewJpaEntity::storeId),
                )
            }

        return results.filterNotNull()
    }

    private fun toDomains(entities: List<ReviewJpaEntity>): List<Review> {
        if (entities.isEmpty()) return emptyList()

        val images =
            reviewImageRepository
                .findAllByReviewInOrderByDisplayOrder(entities)
                .groupBy { it.review.id }
        val menus =
            reviewMenuRepository
                .findAllByReviewIn(entities)
                .groupBy { it.review.id }

        return entities.map { entity ->
            entity.toDomain(
                menus = menus[entity.id]?.map { it.menuName } ?: emptyList(),
                imageUrls = images[entity.id]?.map { it.imageUrl } ?: emptyList(),
            )
        }
    }

    fun save(review: Review): Review {
        val entity =
            reviewRepository.save(
                ReviewJpaEntity(
                    memberId = review.memberId,
                    storeId = review.storeId,
                    rating = review.rating,
                    content = review.content,
                ),
            )
        review.imageUrls.forEachIndexed { index, url ->
            reviewImageRepository.save(ReviewImageJpaEntity(review = entity, imageUrl = url, displayOrder = index))
        }
        review.menus.forEach { menu ->
            reviewMenuRepository.save(ReviewMenuJpaEntity(review = entity, menuName = menu))
        }

        return entity.toDomain(menus = review.menus, imageUrls = review.imageUrls)
    }

    fun delete(review: Review) {
        val entity = reviewRepository.findById(review.id).orElseThrow { BbangbatException(REVIEW_NOT_FOUND) }

        reviewMenuRepository.deleteAllByReview(entity)
        reviewImageRepository.deleteAllByReview(entity)
        reviewRepository.delete(entity)
    }
}
