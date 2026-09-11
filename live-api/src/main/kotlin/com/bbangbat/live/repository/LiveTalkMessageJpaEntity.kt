package com.bbangbat.live.repository

import com.bbangbat.common.id.Tsid
import com.bbangbat.live.domain.LiveTalkMessage
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "live_talk_messages",
    indexes = [
        Index(name = "idx_talk_store_created_at", columnList = "store_id, created_at"),
        Index(name = "idx_talk_author_id", columnList = "author_id"),
        Index(name = "idx_talk_deleted_created", columnList = "deleted_at, created_at"),
    ],
)
class LiveTalkMessageJpaEntity(
    @Id
    @Tsid
    @Column(name = "id", nullable = false)
    var id: Long = 0L,
    @Column(name = "store_id", nullable = false)
    var storeId: Long,
    @Column(name = "author_id", nullable = false)
    var authorId: Long,
    @Column(name = "author_nickname", nullable = false, length = LiveTalkMessage.MAX_AUTHOR_NICKNAME_LENGTH)
    var authorNickname: String,
    @Column(name = "content", nullable = false, length = LiveTalkMessage.MAX_CONTENT_LENGTH)
    var content: String,
    @Column(name = "deleted_at", nullable = true)
    var deletedAt: LocalDateTime? = null,
) : BaseEntity() {
    fun applyFrom(message: LiveTalkMessage) {
        storeId = message.storeId
        authorId = message.authorId
        authorNickname = message.authorNickname
        content = message.content
        deletedAt = message.deletedAt
    }

    fun toDomain(): LiveTalkMessage =
        LiveTalkMessage(
            id = id,
            storeId = storeId,
            authorId = authorId,
            authorNickname = authorNickname,
            content = content,
            createdAt = requireNotNull(createdAt) { "영속화되지 않은 엔티티입니다." },
            deletedAt = deletedAt,
        )

    companion object {
        fun from(message: LiveTalkMessage): LiveTalkMessageJpaEntity =
            LiveTalkMessageJpaEntity(
                storeId = message.storeId,
                authorId = message.authorId,
                authorNickname = message.authorNickname,
                content = message.content,
                deletedAt = message.deletedAt,
            )
    }
}
