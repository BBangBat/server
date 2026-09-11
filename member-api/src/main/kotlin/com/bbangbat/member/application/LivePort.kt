package com.bbangbat.member.application

interface LivePort {
    fun countByMemberId(memberId: Long): Long

    fun deleteCongestionVotesByMemberId(memberId: Long)
}
