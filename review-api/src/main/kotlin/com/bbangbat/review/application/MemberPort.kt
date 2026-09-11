package com.bbangbat.review.application

interface MemberPort {
    fun findAuthors(memberIds: Collection<Long>): Map<Long, ReviewAuthor>
}

data class ReviewAuthor(
    val nickname: String,
    val profileImageUrl: String?,
)
