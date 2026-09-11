package com.bbangbat.live.application

import com.bbangbat.common.exception.BbangbatException
import com.bbangbat.common.exception.ErrorCode.TALK_FORBIDDEN
import com.bbangbat.common.exception.ErrorCode.TALK_NOT_FOUND
import com.bbangbat.live.client.TalkSummaryClient
import com.bbangbat.live.domain.LiveTalkMessage
import com.bbangbat.live.domain.StoreTalkSummary
import com.bbangbat.live.repository.TalkPersistenceAdapter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class TalkService(
    private val talkPersistenceAdapter: TalkPersistenceAdapter,
    private val talkSummaryClient: TalkSummaryClient,
    private val memberPort: MemberPort,
    private val storePort: StorePort,
) {
    @Transactional
    fun sendMessage(
        storeId: Long,
        authorId: Long,
        content: String,
    ): LiveTalkMessage {
        val nickname = memberPort.getNickname(authorId)
        val message =
            LiveTalkMessage(
                storeId = storeId,
                authorId = authorId,
                authorNickname = nickname,
                content = content,
                createdAt = LocalDateTime.now(),
            )

        return talkPersistenceAdapter.saveMessage(message)
    }

    @Transactional(readOnly = true)
    fun getMessages(
        storeId: Long,
        afterId: Long?,
    ): List<LiveTalkMessage> {
        val from = LiveTalkMessage.windowStart(LocalDateTime.now())

        return talkPersistenceAdapter.findRecentMessages(storeId, from, afterId)
    }

    @Transactional(readOnly = true)
    fun getMyMessages(authorId: Long): List<MyTalkMessage> {
        val messages = talkPersistenceAdapter.findByAuthorId(authorId)
        val storeNames = storePort.findNames(messages.map { it.storeId }.distinct())

        return messages.map { MyTalkMessage.of(it, storeNames[it.storeId]) }
    }

    @Transactional
    fun deleteMessage(
        messageId: Long,
        memberId: Long,
    ) {
        val message = talkPersistenceAdapter.findMessageById(messageId) ?: throw BbangbatException(TALK_NOT_FOUND)

        if (message.isDeleted) {
            throw BbangbatException(TALK_NOT_FOUND)
        }

        if (!message.canBeDeletedBy(memberId) { memberPort.isAdmin(memberId) }) {
            throw BbangbatException(TALK_FORBIDDEN)
        }

        talkPersistenceAdapter.updateMessage(message.delete(LocalDateTime.now()))
    }

    fun summarizeActiveStores() {
        val from = StoreTalkSummary.windowStart(LocalDateTime.now())
        val activeStores = talkPersistenceAdapter.findActiveStores(from, StoreTalkSummary.MIN_MESSAGES)

        activeStores.forEach { active ->
            val existing = talkPersistenceAdapter.findSummaryByStoreId(active.storeId)

            if (existing != null && existing.isUpToDate(active.latestMessageId)) {
                return@forEach
            }

            val messages =
                talkPersistenceAdapter
                    .findRecentMessages(active.storeId, from, null)
                    .takeLast(StoreTalkSummary.MAX_MESSAGES)
                    .map { it.content }

            talkSummaryClient.requestSummary(active.storeId, active.latestMessageId, messages)
        }
    }

    @Transactional
    fun saveSummary(
        storeId: Long,
        summary: String,
        lastMessageId: Long,
    ): StoreTalkSummary {
        val existing = talkPersistenceAdapter.findSummaryByStoreId(storeId)

        if (existing != null) {
            return talkPersistenceAdapter.updateSummary(existing.update(summary, lastMessageId))
        }

        return talkPersistenceAdapter.saveSummary(
            StoreTalkSummary(storeId = storeId, summary = summary, lastMessageId = lastMessageId),
        )
    }

    @Transactional(readOnly = true)
    fun getSummaries(storeIds: List<Long>): List<StoreTalkSummary> {
        if (storeIds.isEmpty()) {
            return emptyList()
        }

        return talkPersistenceAdapter.findSummariesByStoreIds(storeIds)
    }
}
