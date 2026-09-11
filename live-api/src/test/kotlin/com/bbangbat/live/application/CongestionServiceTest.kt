package com.bbangbat.live.application

import com.bbangbat.auth.voter.Voter
import com.bbangbat.auth.voter.VoterType
import com.bbangbat.common.exception.BbangbatException
import com.bbangbat.common.exception.ErrorCode
import com.bbangbat.live.domain.CongestionLevel
import com.bbangbat.live.domain.CongestionVote
import com.bbangbat.live.repository.CongestionVotePersistenceAdapter
import com.bbangbat.live.repository.StoreCongestionCount
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.never
import org.mockito.kotlin.then
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class CongestionServiceTest {
    @Mock
    private lateinit var congestionVotePersistenceAdapter: CongestionVotePersistenceAdapter

    @Mock
    private lateinit var storePort: StorePort

    private lateinit var congestionService: CongestionService

    @BeforeEach
    fun setUp() {
        congestionService =
            CongestionService(congestionVotePersistenceAdapter, storePort, COOLDOWN_MINUTES)
    }

    @Test
    fun `기존 투표가 없으면 새로 저장한다`() {
        val storeId = 1L
        val voter = Voter(VoterType.MEMBER, "1")
        given(storePort.exists(storeId)).willReturn(true)
        given(congestionVotePersistenceAdapter.findByVoter(storeId, VoterType.MEMBER, "1")).willReturn(null)
        given(congestionVotePersistenceAdapter.findRecentVotes(eq(storeId), any())).willReturn(
            listOf(recentVote(storeId, CongestionLevel.CROWDED, "1")),
        )

        val result = congestionService.vote(storeId, CongestionLevel.CROWDED, voter)

        then(congestionVotePersistenceAdapter).should().save(any())
        then(congestionVotePersistenceAdapter).should(never()).update(any())
        assertThat(result.current).isEqualTo(CongestionLevel.CROWDED)
    }

    @Test
    fun `쿨다운이 지난 기존 투표는 덮어쓴다`() {
        val storeId = 1L
        val voter = Voter(VoterType.MEMBER, "1")
        val existing =
            CongestionVote(
                id = 5L,
                storeId = storeId,
                level = CongestionLevel.NORMAL,
                voterType = VoterType.MEMBER,
                voterKey = "1",
                votedAt = LocalDateTime.now().minusMinutes(20),
            )
        given(storePort.exists(storeId)).willReturn(true)
        given(congestionVotePersistenceAdapter.findByVoter(storeId, VoterType.MEMBER, "1")).willReturn(existing)
        given(congestionVotePersistenceAdapter.findRecentVotes(eq(storeId), any())).willReturn(
            listOf(recentVote(storeId, CongestionLevel.CROWDED, "1")),
        )

        congestionService.vote(storeId, CongestionLevel.CROWDED, voter)

        then(congestionVotePersistenceAdapter).should().update(
            argThat { id == 5L && level == CongestionLevel.CROWDED && votedAt.isAfter(existing.votedAt) },
        )
        then(congestionVotePersistenceAdapter).should(never()).save(any())
    }

    @Test
    fun `혼잡도 조회는 최근 투표를 집계해 반환한다`() {
        val storeId = 1L
        given(congestionVotePersistenceAdapter.findRecentVotes(eq(storeId), any())).willReturn(
            listOf(
                recentVote(storeId, CongestionLevel.NORMAL, "a"),
                recentVote(storeId, CongestionLevel.NORMAL, "b"),
                recentVote(storeId, CongestionLevel.UNCROWDED, "c"),
            ),
        )

        val result = congestionService.getCongestion(storeId)

        assertThat(result.current).isEqualTo(CongestionLevel.NORMAL)
        assertThat(result.totalVotes).isEqualTo(3)
    }

    @Test
    fun `벌크 조회는 가게별 집계를 반환하고 투표 없는 가게도 빈 혼잡도로 포함한다`() {
        val storeIds = listOf(1L, 2L)
        given(congestionVotePersistenceAdapter.countRecentVotesByStores(eq(storeIds), any())).willReturn(
            listOf(
                StoreCongestionCount(storeId = 1L, level = CongestionLevel.CROWDED, count = 3),
                StoreCongestionCount(storeId = 1L, level = CongestionLevel.NORMAL, count = 1),
            ),
        )

        val result = congestionService.getCongestions(storeIds)

        assertThat(result.keys).containsExactlyInAnyOrder(1L, 2L)
        assertThat(result.getValue(1L).current).isEqualTo(CongestionLevel.CROWDED)
        assertThat(result.getValue(1L).totalVotes).isEqualTo(4)
        assertThat(result.getValue(2L).current).isEqualTo(CongestionLevel.UNCROWDED)
        assertThat(result.getValue(2L).totalVotes).isEqualTo(0)
    }

    @Test
    fun `벌크 조회에 빈 목록이 들어오면 빈 맵을 반환하고 조회하지 않는다`() {
        val result = congestionService.getCongestions(emptyList())

        assertThat(result).isEmpty()
        verifyNoInteractions(congestionVotePersistenceAdapter)
    }

    private fun recentVote(
        storeId: Long,
        level: CongestionLevel,
        voterKey: String,
    ): CongestionVote =
        CongestionVote(
            storeId = storeId,
            level = level,
            voterType = VoterType.GUEST,
            voterKey = voterKey,
            votedAt = LocalDateTime.now(),
        )

    @Test
    fun `쿨다운 내 재투표는 남은 시간과 함께 거절된다`() {
        val storeId = 1L
        val voter = Voter(VoterType.MEMBER, "1")
        val existing =
            CongestionVote(
                id = 5L,
                storeId = storeId,
                level = CongestionLevel.NORMAL,
                voterType = VoterType.MEMBER,
                voterKey = "1",
                votedAt = LocalDateTime.now().minusMinutes(5),
            )
        given(storePort.exists(storeId)).willReturn(true)
        given(congestionVotePersistenceAdapter.findByVoter(storeId, VoterType.MEMBER, "1")).willReturn(existing)

        val exception =
            assertThrows<BbangbatException> {
                congestionService.vote(storeId, CongestionLevel.CROWDED, voter)
            }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.CONGESTION_VOTE_COOLDOWN)
        assertThat(exception.retryAfterSeconds).isNotNull()
        then(congestionVotePersistenceAdapter).should(never()).update(any())
    }

    @Test
    fun `없는 가게에는 투표할 수 없고 저장하지 않는다`() {
        val voter = Voter(VoterType.GUEST, "guest-1")
        given(storePort.exists(99L)).willReturn(false)

        val exception =
            assertThrows<BbangbatException> {
                congestionService.vote(99L, CongestionLevel.NORMAL, voter)
            }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.STORE_NOT_FOUND)
        verifyNoInteractions(congestionVotePersistenceAdapter)
    }

    @Test
    fun `첫 투표가 동시에 들어와 unique 키에 걸리면 쿨다운으로 거절된다`() {
        val storeId = 1L
        val voter = Voter(VoterType.GUEST, "guest-1")
        given(storePort.exists(storeId)).willReturn(true)
        given(congestionVotePersistenceAdapter.findByVoter(storeId, VoterType.GUEST, "guest-1")).willReturn(null)
        given(congestionVotePersistenceAdapter.save(any())).willThrow(DataIntegrityViolationException("uk_congestion_voter"))

        val exception =
            assertThrows<BbangbatException> {
                congestionService.vote(storeId, CongestionLevel.NORMAL, voter)
            }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.CONGESTION_VOTE_COOLDOWN)
        assertThat(exception.retryAfterSeconds).isEqualTo(COOLDOWN_MINUTES * 60)
    }

    companion object {
        private const val COOLDOWN_MINUTES = 15L
    }
}
