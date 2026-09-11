package com.bbangbat.live.application

interface MemberPort {
    fun getNickname(memberId: Long): String

    fun isAdmin(memberId: Long): Boolean
}
