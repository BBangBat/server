package com.bbangbat.live.repository

import com.bbangbat.auth.voter.VoterType
import com.bbangbat.live.domain.CongestionLevel
import com.bbangbat.live.domain.CongestionVote
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "congestion_votes",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_congestion_voter", columnNames = ["store_id", "voter_type", "voter_key"]),
    ],
    indexes = [
        Index(name = "idx_congestion_store_voted_at_level", columnList = "store_id, voted_at, level"),
    ],
)
class CongestionVoteJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long = 0L,
    @Column(name = "store_id", nullable = false)
    var storeId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 20)
    var level: CongestionLevel,
    @Enumerated(EnumType.STRING)
    @Column(name = "voter_type", nullable = false, length = 20)
    var voterType: VoterType,
    @Column(name = "voter_key", nullable = false, length = 100)
    var voterKey: String,
    @Column(name = "voted_at", nullable = false)
    var votedAt: LocalDateTime,
) : BaseEntity() {
    fun applyFrom(vote: CongestionVote) {
        storeId = vote.storeId
        level = vote.level
        voterType = vote.voterType
        voterKey = vote.voterKey
        votedAt = vote.votedAt
    }

    fun toDomain(): CongestionVote =
        CongestionVote(
            id = id,
            storeId = storeId,
            level = level,
            voterType = voterType,
            voterKey = voterKey,
            votedAt = votedAt,
        )

    companion object {
        fun from(vote: CongestionVote): CongestionVoteJpaEntity =
            CongestionVoteJpaEntity(
                storeId = vote.storeId,
                level = vote.level,
                voterType = vote.voterType,
                voterKey = vote.voterKey,
                votedAt = vote.votedAt,
            )
    }
}
