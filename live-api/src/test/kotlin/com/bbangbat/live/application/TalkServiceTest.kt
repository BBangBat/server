package com.bbangbat.live.application

import com.bbangbat.common.exception.BbangbatException
import com.bbangbat.common.exception.ErrorCode.TALK_FORBIDDEN
import com.bbangbat.common.exception.ErrorCode.TALK_NOT_FOUND
import com.bbangbat.live.client.TalkSummaryClient
import com.bbangbat.live.domain.LiveTalkMessage
import com.bbangbat.live.domain.StoreTalkSummary
import com.bbangbat.live.repository.ActiveStore
import com.bbangbat.live.repository.TalkPersistenceAdapter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.never
import org.mockito.kotlin.then
import org.mockito.kotlin.verifyNoInteractions
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class TalkServiceTest {
    @Mock
    private lateinit var talkPersistenceAdapter: TalkPersistenceAdapter

    @Mock
    private lateinit var talkSummaryClient: TalkSummaryClient

    @Mock
    private lateinit var memberPort: MemberPort

    @Mock
    private lateinit var storePort: StorePort

    private lateinit var talkService: TalkService

    @BeforeEach
    fun setUp() {
        talkService = TalkService(talkPersistenceAdapter, talkSummaryClient, memberPort, storePort)
    }

    @Test
    fun `메시지 전송 시 작성자 닉네임을 스냅샷으로 저장한다`() {
        val storeId = 1L
        val authorId = 10L
        given(memberPort.getNickname(authorId)).willReturn("빵순이")
        given(talkPersistenceAdapter.saveMessage(any())).willAnswer { it.arguments[0] }

        val result = talkService.sendMessage(storeId, authorId, "지금 사람 많아요!")

        val captor = argumentCaptor<LiveTalkMessage>()
        then(talkPersistenceAdapter).should().saveMessage(captor.capture())
        assertThat(captor.firstValue.authorNickname).isEqualTo("빵순이")
        assertThat(captor.firstValue.storeId).isEqualTo(storeId)
        assertThat(captor.firstValue.authorId).isEqualTo(authorId)
        assertThat(result.authorNickname).isEqualTo("빵순이")
    }

    @Test
    fun `afterId가 없으면 최근 24시간 윈도우 내 메시지를 조회한다`() {
        val storeId = 1L
        given(talkPersistenceAdapter.findRecentMessages(eq(storeId), any(), eq(null))).willReturn(emptyList())

        talkService.getMessages(storeId, null)

        then(talkPersistenceAdapter).should().findRecentMessages(eq(storeId), any(), eq(null))
    }

    @Test
    fun `afterId가 있으면 해당 ID 이후 메시지만 조회한다`() {
        val storeId = 1L
        val afterId = 5L
        given(talkPersistenceAdapter.findRecentMessages(eq(storeId), any(), eq(afterId))).willReturn(
            listOf(message(storeId, id = 6L)),
        )

        val result = talkService.getMessages(storeId, afterId)

        then(talkPersistenceAdapter).should().findRecentMessages(eq(storeId), any(), eq(afterId))
        assertThat(result).hasSize(1)
        assertThat(result[0].id).isEqualTo(6L)
    }

    @Test
    fun `활성 가게 중 마지막 요약 이후 새 톡이 있는 가게만 요약을 요청한다`() {
        val newTalkStore = ActiveStore(storeId = 1L, latestMessageId = 100L)
        val unchangedStore = ActiveStore(storeId = 2L, latestMessageId = 50L)
        given(talkPersistenceAdapter.findActiveStores(any(), eq(5L))).willReturn(listOf(newTalkStore, unchangedStore))
        given(talkPersistenceAdapter.findSummaryByStoreId(1L)).willReturn(null)
        given(talkPersistenceAdapter.findSummaryByStoreId(2L)).willReturn(
            StoreTalkSummary(storeId = 2L, summary = "기존 요약", lastMessageId = 50L),
        )
        given(talkPersistenceAdapter.findRecentMessages(eq(1L), any(), eq(null))).willReturn(
            listOf(message(1L, id = 100L)),
        )

        talkService.summarizeActiveStores()

        then(talkSummaryClient).should().requestSummary(eq(1L), eq(100L), any())
        then(talkSummaryClient).should(never()).requestSummary(eq(2L), any(), any())
    }

    @Test
    fun `빈 storeIds면 요약을 조회하지 않고 빈 리스트를 반환한다`() {
        val result = talkService.getSummaries(emptyList())

        assertThat(result).isEmpty()
        verifyNoInteractions(talkPersistenceAdapter)
    }

    @Test
    fun `storeIds로 요약을 벌크 조회한다`() {
        val storeIds = listOf(1L, 2L)
        given(talkPersistenceAdapter.findSummariesByStoreIds(storeIds)).willReturn(
            listOf(
                StoreTalkSummary(storeId = 1L, summary = "요약1", lastMessageId = 10L),
                StoreTalkSummary(storeId = 2L, summary = "요약2", lastMessageId = 20L),
            ),
        )

        val result = talkService.getSummaries(storeIds)

        assertThat(result).hasSize(2)
        assertThat(result.map { it.storeId }).containsExactly(1L, 2L)
    }

    @Test
    fun `작성자 본인은 톡을 소프트 삭제하고 운영자 조회는 하지 않는다`() {
        given(talkPersistenceAdapter.findMessageById(1L)).willReturn(message(storeId = 1L))

        talkService.deleteMessage(messageId = 1L, memberId = AUTHOR_ID)

        val captor = argumentCaptor<LiveTalkMessage>()

        then(talkPersistenceAdapter).should().updateMessage(captor.capture())
        assertThat(captor.firstValue.isDeleted).isTrue()
        then(memberPort).should(never()).isAdmin(any())
    }

    @Test
    fun `운영자는 남의 톡도 소프트 삭제할 수 있다`() {
        given(talkPersistenceAdapter.findMessageById(1L)).willReturn(message(storeId = 1L))
        given(memberPort.isAdmin(999L)).willReturn(true)

        talkService.deleteMessage(messageId = 1L, memberId = 999L)

        then(talkPersistenceAdapter).should().updateMessage(any())
    }

    @Test
    fun `작성자도 운영자도 아니면 삭제할 수 없다`() {
        given(talkPersistenceAdapter.findMessageById(1L)).willReturn(message(storeId = 1L))
        given(memberPort.isAdmin(999L)).willReturn(false)

        val exception =
            assertThrows<BbangbatException> {
                talkService.deleteMessage(messageId = 1L, memberId = 999L)
            }

        assertThat(exception.errorCode).isEqualTo(TALK_FORBIDDEN)
        then(talkPersistenceAdapter).should(never()).updateMessage(any())
    }

    @Test
    fun `이미 삭제된 톡은 없는 것으로 취급한다`() {
        given(talkPersistenceAdapter.findMessageById(1L)).willReturn(message(storeId = 1L).delete(LocalDateTime.now()))

        val exception =
            assertThrows<BbangbatException> {
                talkService.deleteMessage(messageId = 1L, memberId = AUTHOR_ID)
            }

        assertThat(exception.errorCode).isEqualTo(TALK_NOT_FOUND)
        then(talkPersistenceAdapter).should(never()).updateMessage(any())
    }

    @Test
    fun `기존 요약이 있으면 새로 저장하지 않고 갱신한다`() {
        val existing = StoreTalkSummary(id = 7L, storeId = 1L, summary = "이전 요약", lastMessageId = 10L)
        given(talkPersistenceAdapter.findSummaryByStoreId(1L)).willReturn(existing)

        talkService.saveSummary(storeId = 1L, summary = "최신 요약", lastMessageId = 20L)

        val captor = argumentCaptor<StoreTalkSummary>()

        then(talkPersistenceAdapter).should().updateSummary(captor.capture())
        assertThat(captor.firstValue.id).isEqualTo(7L)
        assertThat(captor.firstValue.summary).isEqualTo("최신 요약")
        assertThat(captor.firstValue.lastMessageId).isEqualTo(20L)
        then(talkPersistenceAdapter).should(never()).saveSummary(any())
    }

    @Test
    fun `기존 요약이 없으면 새로 저장한다`() {
        given(talkPersistenceAdapter.findSummaryByStoreId(1L)).willReturn(null)

        talkService.saveSummary(storeId = 1L, summary = "요약", lastMessageId = 20L)

        then(talkPersistenceAdapter).should().saveSummary(any())
        then(talkPersistenceAdapter).should(never()).updateSummary(any())
    }

    private fun message(
        storeId: Long,
        id: Long = 1L,
    ): LiveTalkMessage =
        LiveTalkMessage(
            id = id,
            storeId = storeId,
            authorId = AUTHOR_ID,
            authorNickname = "빵순이",
            content = "지금 사람 많아요!",
            createdAt = LocalDateTime.now(),
        )

    companion object {
        private const val AUTHOR_ID = 1L
    }
}
