package com.bbangbat.live.repository

import com.bbangbat.live.domain.LiveTalkMessage
import com.bbangbat.live.domain.StoreTalkSummary
import com.bbangbat.live.support.AbstractContainerBaseTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import java.time.LocalDateTime

@Import(TalkPersistenceAdapter::class)
class TalkPersistenceAdapterTest
    @Autowired
    constructor(
        private val talkPersistenceAdapter: TalkPersistenceAdapter,
        private val em: TestEntityManager,
    ) : AbstractContainerBaseTest() {
        @Test
        fun `메시지를 저장하면 id와 createdAt이 채워진다`() {
            val saved = talkPersistenceAdapter.saveMessage(message(storeId = 1L))

            assertThat(saved.id).isGreaterThan(0L)
            assertThat(saved.createdAt).isNotNull
        }

        @Test
        fun `메시지 ID는 DB 채번이 아니라 시간순 TSID로 발급된다`() {
            val first = talkPersistenceAdapter.saveMessage(message(storeId = 1L))
            val second = talkPersistenceAdapter.saveMessage(message(storeId = 1L))
            em.flush()
            em.clear()

            assertThat(first.id).isGreaterThan(MIN_TSID)
            assertThat(second.id).isGreaterThan(first.id)
        }

        @Test
        fun `afterId 폴링은 TSID에서도 이후 메시지만 돌려준다`() {
            val first = talkPersistenceAdapter.saveMessage(message(storeId = 1L, content = "먼저 온 메시지"))
            val second = talkPersistenceAdapter.saveMessage(message(storeId = 1L, content = "나중에 온 메시지"))
            em.flush()
            em.clear()

            val after = talkPersistenceAdapter.findRecentMessages(1L, LocalDateTime.now().minusMinutes(60), first.id)

            assertThat(after).hasSize(1)
            assertThat(after[0].id).isEqualTo(second.id)
            assertThat(after[0].content).isEqualTo("나중에 온 메시지")
        }

        @Test
        fun `findRecentMessages는 다른 가게의 메시지를 제외한다`() {
            talkPersistenceAdapter.saveMessage(message(storeId = 1L, content = "가게1 메시지"))
            talkPersistenceAdapter.saveMessage(message(storeId = 2L, content = "가게2 메시지"))
            em.flush()
            em.clear()

            val recent = talkPersistenceAdapter.findRecentMessages(1L, LocalDateTime.now().minusMinutes(60), null)

            assertThat(recent).hasSize(1)
            assertThat(recent[0].content).isEqualTo("가게1 메시지")
        }

        @Test
        fun `findActiveStores는 톡이 임계치 이상인 가게만 최신 메시지 ID와 함께 반환한다`() {
            repeat(5) { talkPersistenceAdapter.saveMessage(message(storeId = 1L)) }
            repeat(2) { talkPersistenceAdapter.saveMessage(message(storeId = 2L)) }
            em.flush()
            em.clear()

            val active = talkPersistenceAdapter.findActiveStores(LocalDateTime.now().minusMinutes(60), 5)

            assertThat(active).hasSize(1)
            assertThat(active[0].storeId).isEqualTo(1L)
            assertThat(active[0].latestMessageId).isGreaterThan(0L)
        }

        @Test
        fun `saveSummary는 신규 요약을 저장한다`() {
            talkPersistenceAdapter.saveSummary(summary(storeId = 1L, text = "요약", lastMessageId = 10L))
            em.flush()
            em.clear()

            val found = talkPersistenceAdapter.findSummaryByStoreId(1L)
            assertThat(found).isNotNull
            assertThat(found!!.summary).isEqualTo("요약")
            assertThat(found.lastMessageId).isEqualTo(10L)
        }

        @Test
        fun `updateSummary는 도메인이 만든 새 요약을 더티체킹으로 반영한다`() {
            val saved = talkPersistenceAdapter.saveSummary(summary(storeId = 1L, text = "이전 요약", lastMessageId = 10L))
            em.flush()
            em.clear()

            talkPersistenceAdapter.updateSummary(saved.update("최신 요약", 20L))
            em.flush()
            em.clear()

            val found = talkPersistenceAdapter.findSummaryByStoreId(1L)!!

            assertThat(found.summary).isEqualTo("최신 요약")
            assertThat(found.lastMessageId).isEqualTo(20L)
        }

        @Test
        fun `updateMessage는 도메인이 만든 소프트 삭제 상태를 더티체킹으로 반영한다`() {
            val saved = talkPersistenceAdapter.saveMessage(message(storeId = 1L))
            em.flush()
            em.clear()
            val deletedAt = LocalDateTime.now()

            talkPersistenceAdapter.updateMessage(saved.delete(deletedAt))
            em.flush()
            em.clear()

            val found = talkPersistenceAdapter.findMessageById(saved.id)!!

            assertThat(found.isDeleted).isTrue()
            assertThat(talkPersistenceAdapter.findRecentMessages(1L, deletedAt.minusHours(1), null)).isEmpty()
        }

        @Test
        fun `findSummariesByStoreIds는 요약이 있는 가게만 벌크로 반환한다`() {
            talkPersistenceAdapter.saveSummary(summary(storeId = 1L, text = "요약1", lastMessageId = 1L))
            talkPersistenceAdapter.saveSummary(summary(storeId = 2L, text = "요약2", lastMessageId = 2L))
            em.flush()
            em.clear()

            val result = talkPersistenceAdapter.findSummariesByStoreIds(listOf(1L, 2L, 3L))

            assertThat(result).hasSize(2)
            assertThat(result.map { it.storeId }).containsExactlyInAnyOrder(1L, 2L)
        }

        companion object {
            private const val MIN_TSID = 1_000_000_000_000L
        }

        private fun message(
            storeId: Long,
            content: String = "지금 사람 많아요!",
        ): LiveTalkMessage =
            LiveTalkMessage(
                storeId = storeId,
                authorId = 1L,
                authorNickname = "빵순이",
                content = content,
                createdAt = LocalDateTime.now(),
            )

        private fun summary(
            storeId: Long,
            text: String,
            lastMessageId: Long,
        ): StoreTalkSummary =
            StoreTalkSummary(
                storeId = storeId,
                summary = text,
                lastMessageId = lastMessageId,
            )
    }
