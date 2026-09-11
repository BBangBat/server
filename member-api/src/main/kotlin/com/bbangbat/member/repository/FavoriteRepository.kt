package com.bbangbat.member.repository

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface FavoriteRepository : JpaRepository<FavoriteJpaEntity, Long> {
    fun existsByMemberIdAndStoreId(
        memberId: Long,
        storeId: Long,
    ): Boolean

    fun findByMemberIdAndStoreId(
        memberId: Long,
        storeId: Long,
    ): Optional<FavoriteJpaEntity>

    fun findAllByMemberIdOrderByIdDesc(memberId: Long): List<FavoriteJpaEntity>

    fun countByMemberId(memberId: Long): Long

    fun deleteAllByMemberId(memberId: Long)
}
