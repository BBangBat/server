package com.bbangbat.live.domain

import java.time.LocalDateTime

data class LiveTalkMessage(
    val id: Long = 0L,
    val storeId: Long,
    val authorId: Long,
    val authorNickname: String,
    val content: String,
    val createdAt: LocalDateTime,
    val deletedAt: LocalDateTime? = null,
) {
    init {
        require(storeId > 0) { "가게 ID가 올바르지 않습니다." }
        require(content.isNotBlank()) { "메시지는 비어 있을 수 없습니다." }
        require(content.length <= MAX_CONTENT_LENGTH) { "메시지는 ${MAX_CONTENT_LENGTH}자를 초과할 수 없습니다." }
    }

    val isDeleted: Boolean
        get() = deletedAt != null

    fun canBeDeletedBy(
        memberId: Long,
        isAdmin: () -> Boolean,
    ): Boolean = authorId == memberId || isAdmin()

    fun delete(at: LocalDateTime): LiveTalkMessage = copy(deletedAt = at)

    companion object {
        const val MAX_CONTENT_LENGTH = 100

        const val MAX_AUTHOR_NICKNAME_LENGTH = 10

        const val WINDOW_HOURS = 24L

        fun windowStart(now: LocalDateTime): LocalDateTime = now.minusHours(WINDOW_HOURS)
    }
}
