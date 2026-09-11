package com.bbangbat

import com.bbangbat.live.application.CongestionService
import com.bbangbat.live.application.TalkStatsService
import com.bbangbat.member.application.LivePort
import org.springframework.stereotype.Component

@Component
class LivePortAdapter(
    private val talkStatsService: TalkStatsService,
    private val congestionService: CongestionService,
) : LivePort {
    override fun countByMemberId(memberId: Long): Long = talkStatsService.countByAuthorId(memberId)

    override fun deleteCongestionVotesByMemberId(memberId: Long) = congestionService.deleteVotesByMember(memberId)
}
