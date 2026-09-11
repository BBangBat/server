package com.bbangbat.live.application

import com.bbangbat.live.domain.LiveTalkMessage
import java.time.LocalDateTime

data class MyTalkMessage(
    val id: Long,
    val storeId: Long,
    val storeName: String,
    val content: String,
    val createdAt: LocalDateTime,
) {
    companion object {
        const val UNKNOWN_STORE_NAME = "알 수 없는 가게"

        fun of(
            message: LiveTalkMessage,
            storeName: String?,
        ): MyTalkMessage =
            MyTalkMessage(
                id = message.id,
                storeId = message.storeId,
                storeName = storeName ?: UNKNOWN_STORE_NAME,
                content = message.content,
                createdAt = message.createdAt,
            )
    }
}
