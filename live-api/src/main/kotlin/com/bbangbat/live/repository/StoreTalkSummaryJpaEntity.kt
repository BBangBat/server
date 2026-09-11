package com.bbangbat.live.repository

import com.bbangbat.live.domain.StoreTalkSummary
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "store_talk_summaries")
class StoreTalkSummaryJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long = 0L,
    @Column(name = "store_id", nullable = false, unique = true)
    var storeId: Long,
    @Column(name = "summary", nullable = false, length = 500)
    var summary: String,
    @Column(name = "last_message_id", nullable = false)
    var lastMessageId: Long,
) : BaseEntity() {
    fun applyFrom(summary: StoreTalkSummary) {
        storeId = summary.storeId
        this.summary = summary.summary
        lastMessageId = summary.lastMessageId
    }

    fun toDomain(): StoreTalkSummary =
        StoreTalkSummary(
            id = id,
            storeId = storeId,
            summary = summary,
            lastMessageId = lastMessageId,
            updatedAt = updatedAt,
        )

    companion object {
        fun from(summary: StoreTalkSummary): StoreTalkSummaryJpaEntity =
            StoreTalkSummaryJpaEntity(
                storeId = summary.storeId,
                summary = summary.summary,
                lastMessageId = summary.lastMessageId,
            )
    }
}
