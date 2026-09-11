package com.bbangbat.live.repository

import com.bbangbat.auth.voter.VoterType
import com.bbangbat.live.domain.CongestionLevel
import com.bbangbat.live.domain.CongestionVote
import com.bbangbat.live.support.AbstractContainerBaseTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDateTime

@Import(CongestionVotePersistenceAdapter::class)
class CongestionVotePersistenceAdapterTest
    @Autowired
    constructor(
        private val congestionVotePersistenceAdapter: CongestionVotePersistenceAdapter,
        private val em: TestEntityManager,
    ) : AbstractContainerBaseTest() {
        @Test
        fun `투표자로 기존 투표를 조회한다`() {
            congestionVotePersistenceAdapter.save(vote(storeId = 1L, level = CongestionLevel.NORMAL, voterKey = "10"))
            em.flush()
            em.clear()

            val found = congestionVotePersistenceAdapter.findByVoter(1L, VoterType.MEMBER, "10")

            assertThat(found).isNotNull
            assertThat(found!!.level).isEqualTo(CongestionLevel.NORMAL)
        }

        @Test
        fun `update는 도메인이 만든 재투표 상태를 더티체킹으로 반영한다`() {
            val saved = congestionVotePersistenceAdapter.save(vote(storeId = 1L, level = CongestionLevel.NORMAL, voterKey = "10"))
            em.flush()
            em.clear()
            val newVotedAt = LocalDateTime.now()

            congestionVotePersistenceAdapter.update(saved.revote(CongestionLevel.CROWDED, newVotedAt))
            em.flush()
            em.clear()

            val updated = congestionVotePersistenceAdapter.findByVoter(1L, VoterType.MEMBER, "10")!!

            assertThat(updated.level).isEqualTo(CongestionLevel.CROWDED)
            assertThat(updated.votedAt).isCloseTo(newVotedAt, within(1, java.time.temporal.ChronoUnit.SECONDS))
        }

        @Test
        fun `findRecentVotes는 윈도우 밖의 오래된 투표를 제외한다`() {
            val now = LocalDateTime.now()
            congestionVotePersistenceAdapter.save(vote(storeId = 1L, level = CongestionLevel.CROWDED, voterKey = "recent", votedAt = now))
            congestionVotePersistenceAdapter.save(
                vote(storeId = 1L, level = CongestionLevel.UNCROWDED, voterKey = "old", votedAt = now.minusMinutes(30)),
            )
            em.flush()
            em.clear()

            val recent = congestionVotePersistenceAdapter.findRecentVotes(1L, now.minusMinutes(15))

            assertThat(recent).hasSize(1)
            assertThat(recent[0].level).isEqualTo(CongestionLevel.CROWDED)
        }

        @Test
        fun `countRecentVotesByStores는 여러 가게의 최근 투표를 가게별 혼잡도별로 집계한다`() {
            val now = LocalDateTime.now()
            congestionVotePersistenceAdapter.save(vote(storeId = 1L, level = CongestionLevel.CROWDED, voterKey = "a", votedAt = now))
            congestionVotePersistenceAdapter.save(vote(storeId = 1L, level = CongestionLevel.CROWDED, voterKey = "b", votedAt = now))
            congestionVotePersistenceAdapter.save(vote(storeId = 1L, level = CongestionLevel.NORMAL, voterKey = "c", votedAt = now))
            congestionVotePersistenceAdapter.save(vote(storeId = 2L, level = CongestionLevel.UNCROWDED, voterKey = "d", votedAt = now))

            congestionVotePersistenceAdapter.save(
                vote(storeId = 1L, level = CongestionLevel.UNCROWDED, voterKey = "old", votedAt = now.minusMinutes(30)),
            )
            em.flush()
            em.clear()

            val counts = congestionVotePersistenceAdapter.countRecentVotesByStores(listOf(1L, 2L), now.minusMinutes(15))

            val store1 = counts.filter { it.storeId == 1L }.associate { it.level to it.count }
            val store2 = counts.filter { it.storeId == 2L }.associate { it.level to it.count }

            assertThat(store1[CongestionLevel.CROWDED]).isEqualTo(2)
            assertThat(store1[CongestionLevel.NORMAL]).isEqualTo(1)
            assertThat(store1[CongestionLevel.UNCROWDED]).isNull()
            assertThat(store2[CongestionLevel.UNCROWDED]).isEqualTo(1)
        }

        @Test
        fun `같은 투표자의 투표를 중복 저장하면 unique 제약에 걸린다`() {
            congestionVotePersistenceAdapter.save(vote(storeId = 1L, level = CongestionLevel.NORMAL, voterKey = "10"))
            em.flush()

            assertThatThrownBy {
                congestionVotePersistenceAdapter.save(vote(storeId = 1L, level = CongestionLevel.CROWDED, voterKey = "10"))
                em.flush()
            }.isInstanceOf(DataIntegrityViolationException::class.java)
        }

        private fun vote(
            storeId: Long,
            level: CongestionLevel,
            voterKey: String,
            votedAt: LocalDateTime = LocalDateTime.now(),
        ): CongestionVote =
            CongestionVote(
                storeId = storeId,
                level = level,
                voterType = VoterType.MEMBER,
                voterKey = voterKey,
                votedAt = votedAt,
            )
    }
