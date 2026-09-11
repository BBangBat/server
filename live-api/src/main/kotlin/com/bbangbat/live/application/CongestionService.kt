package com.bbangbat.live.application

import com.bbangbat.auth.voter.Voter
import com.bbangbat.auth.voter.VoterType
import com.bbangbat.common.exception.BbangbatException
import com.bbangbat.common.exception.ErrorCode.CONGESTION_VOTE_COOLDOWN
import com.bbangbat.common.exception.ErrorCode.STORE_NOT_FOUND
import com.bbangbat.live.domain.Congestion
import com.bbangbat.live.domain.CongestionLevel
import com.bbangbat.live.domain.CongestionVote
import com.bbangbat.live.repository.CongestionVotePersistenceAdapter
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime

@Service
class CongestionService(
    private val congestionVotePersistenceAdapter: CongestionVotePersistenceAdapter,
    private val storePort: StorePort,
    @param:Value("\${app.congestion.vote-cooldown-minutes}") private val cooldownMinutes: Long,
) {
    @Transactional
    fun vote(
        storeId: Long,
        level: CongestionLevel,
        voter: Voter,
    ): Congestion {
        if (!storePort.exists(storeId)) {
            throw BbangbatException(STORE_NOT_FOUND)
        }

        val now = LocalDateTime.now()

        val existing = congestionVotePersistenceAdapter.findByVoter(storeId, voter.type, voter.key)

        if (existing != null) {
            verifyCooldown(existing.votedAt, now)
            congestionVotePersistenceAdapter.update(existing.revote(level, now))
        } else {
            saveFirstVote(storeId, level, voter, now)
        }

        return getCongestion(storeId)
    }

    @Transactional(readOnly = true)
    fun getCongestion(storeId: Long): Congestion {
        val from = Congestion.windowStart(LocalDateTime.now())
        val votes = congestionVotePersistenceAdapter.findRecentVotes(storeId, from)

        return Congestion.summarizeVotes(storeId, votes)
    }

    @Transactional(readOnly = true)
    fun getCongestions(storeIds: List<Long>): Map<Long, Congestion> {
        if (storeIds.isEmpty()) {
            return emptyMap()
        }

        val from = Congestion.windowStart(LocalDateTime.now())
        val countsByStore =
            congestionVotePersistenceAdapter
                .countRecentVotesByStores(storeIds, from)
                .groupBy { it.storeId }

        return storeIds.associateWith { storeId ->
            val counts = countsByStore[storeId].orEmpty().associate { it.level to it.count.toInt() }

            Congestion.summarizeCounts(storeId, counts)
        }
    }

    @Transactional
    fun deleteVotesByMember(memberId: Long) {
        congestionVotePersistenceAdapter.deleteAllByVoter(VoterType.MEMBER, memberId.toString())
    }

    private fun verifyCooldown(
        lastVotedAt: LocalDateTime,
        now: LocalDateTime,
    ) {
        val availableAt = lastVotedAt.plusMinutes(cooldownMinutes)

        if (now.isBefore(availableAt)) {
            val retryAfterSeconds = Duration.between(now, availableAt).seconds.coerceAtLeast(1)

            throw BbangbatException(CONGESTION_VOTE_COOLDOWN, retryAfterSeconds)
        }
    }

    private fun saveFirstVote(
        storeId: Long,
        level: CongestionLevel,
        voter: Voter,
        now: LocalDateTime,
    ) {
        try {
            congestionVotePersistenceAdapter.save(
                CongestionVote(
                    storeId = storeId,
                    level = level,
                    voterType = voter.type,
                    voterKey = voter.key,
                    votedAt = now,
                ),
            )
        } catch (e: DataIntegrityViolationException) {
            throw BbangbatException(CONGESTION_VOTE_COOLDOWN, Duration.ofMinutes(cooldownMinutes).seconds)
        }
    }
}
