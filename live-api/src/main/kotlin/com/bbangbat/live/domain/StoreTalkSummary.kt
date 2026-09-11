package com.bbangbat.live.domain

import java.time.LocalDateTime

data class StoreTalkSummary(
    val id: Long = 0L,
    val storeId: Long,
    val summary: String,
    val lastMessageId: Long,
    val updatedAt: LocalDateTime? = null,
) {
    init {
        require(summary.isNotBlank()) { "요약은 비어 있을 수 없습니다." }
    }

    fun update(
        summary: String,
        lastMessageId: Long,
    ): StoreTalkSummary = copy(summary = summary, lastMessageId = lastMessageId)

    fun isUpToDate(latestMessageId: Long): Boolean = lastMessageId >= latestMessageId

    companion object {
        const val WINDOW_MINUTES = 60L

        const val MIN_MESSAGES = 5L

        const val MAX_MESSAGES = 100

        fun windowStart(now: LocalDateTime): LocalDateTime = now.minusMinutes(WINDOW_MINUTES)
    }
}
