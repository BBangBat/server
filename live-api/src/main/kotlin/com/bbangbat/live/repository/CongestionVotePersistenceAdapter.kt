package com.bbangbat.live.repository

import com.bbangbat.auth.voter.VoterType
import com.bbangbat.common.exception.BbangbatException
import com.bbangbat.common.exception.ErrorCode.NOT_FOUND
import com.bbangbat.live.domain.CongestionVote
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class CongestionVotePersistenceAdapter(
    private val congestionVoteRepository: CongestionVoteRepository,
) {
    fun findByVoter(
        storeId: Long,
        voterType: VoterType,
        voterKey: String,
    ): CongestionVote? =
        congestionVoteRepository
            .findByStoreIdAndVoterTypeAndVoterKey(storeId, voterType, voterKey)
            .orElse(null)
            ?.toDomain()

    fun deleteAllByVoter(
        voterType: VoterType,
        voterKey: String,
    ) = congestionVoteRepository.deleteAllByVoterTypeAndVoterKey(voterType, voterKey)

    fun save(vote: CongestionVote): CongestionVote = congestionVoteRepository.save(CongestionVoteJpaEntity.from(vote)).toDomain()

    fun update(vote: CongestionVote): CongestionVote =
        congestionVoteRepository
            .findById(vote.id)
            .orElseThrow { BbangbatException(NOT_FOUND) }
            .also { it.applyFrom(vote) }
            .toDomain()

    fun findRecentVotes(
        storeId: Long,
        from: LocalDateTime,
    ): List<CongestionVote> =
        congestionVoteRepository
            .findAllByStoreIdAndVotedAtGreaterThanEqual(storeId, from)
            .map { it.toDomain() }

    fun countRecentVotesByStores(
        storeIds: List<Long>,
        from: LocalDateTime,
    ): List<StoreCongestionCount> {
        val counts: List<StoreCongestionCount?> =
            congestionVoteRepository.findAll {
                selectNew<StoreCongestionCount>(
                    path(CongestionVoteJpaEntity::storeId),
                    path(CongestionVoteJpaEntity::level),
                    count(path(CongestionVoteJpaEntity::id)),
                ).from(
                    entity(CongestionVoteJpaEntity::class),
                ).where(
                    and(
                        path(CongestionVoteJpaEntity::storeId).`in`(storeIds),
                        path(CongestionVoteJpaEntity::votedAt).ge(value(from)),
                    ),
                ).groupBy(
                    path(CongestionVoteJpaEntity::storeId),
                    path(CongestionVoteJpaEntity::level),
                )
            }

        return counts.filterNotNull()
    }
}
