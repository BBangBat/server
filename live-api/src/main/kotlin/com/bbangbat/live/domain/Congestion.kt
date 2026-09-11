package com.bbangbat.live.domain

import java.time.LocalDateTime

data class Congestion(
    val storeId: Long,
    val counts: Map<CongestionLevel, Int>,
) {
    val totalVotes: Int = counts.values.sum()

    val current: CongestionLevel =
        counts
            .filterValues { it > 0 }
            .maxWithOrNull(compareBy({ it.value }, { it.key.ordinal }))
            ?.key
            ?: CongestionLevel.UNCROWDED

    companion object {
        const val WINDOW_MINUTES = 15L

        fun windowStart(now: LocalDateTime): LocalDateTime = now.minusMinutes(WINDOW_MINUTES)

        fun summarizeVotes(
            storeId: Long,
            votes: List<CongestionVote>,
        ): Congestion = summarizeCounts(storeId, votes.groupingBy { it.level }.eachCount())

        fun summarizeCounts(
            storeId: Long,
            counts: Map<CongestionLevel, Int>,
        ): Congestion {
            val normalized = CongestionLevel.entries.associateWith { counts[it] ?: 0 }

            return Congestion(storeId = storeId, counts = normalized)
        }
    }
}
