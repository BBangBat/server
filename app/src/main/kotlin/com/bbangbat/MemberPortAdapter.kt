package com.bbangbat

import com.bbangbat.auth.oauth2.SocialProvider
import com.bbangbat.member.application.MemberService
import com.bbangbat.member.domain.MemberRole
import com.bbangbat.member.domain.SocialType
import com.bbangbat.review.application.ReviewAuthor
import org.springframework.stereotype.Component
import com.bbangbat.auth.oauth2.MemberPort as AuthMemberPort
import com.bbangbat.live.application.MemberPort as LiveMemberPort
import com.bbangbat.review.application.MemberPort as ReviewMemberPort

@Component
class MemberPortAdapter(
    private val memberService: MemberService,
) : AuthMemberPort,
    LiveMemberPort,
    ReviewMemberPort {
    override fun getNickname(memberId: Long): String = memberService.findById(memberId).nickname

    override fun isAdmin(memberId: Long): Boolean = memberService.findById(memberId).role == MemberRole.ADMIN

    override fun findByProviderAndProviderId(
        provider: SocialProvider,
        providerId: String,
    ): Long? = memberService.findMemberIdBySocial(SocialType.valueOf(provider.name), providerId)

    override fun existsByEmail(email: String): Boolean = memberService.findByEmailOrNull(email) != null

    override fun updateLastLoginAt(memberId: Long) = memberService.updateLastLoginAt(memberId)

    override fun findAuthors(memberIds: Collection<Long>): Map<Long, ReviewAuthor> =
        memberService.findByIds(memberIds).associate { member ->
            member.id to
                ReviewAuthor(
                    nickname = member.nickname,
                    profileImageUrl = memberService.profileImageUrlOf(member),
                )
        }
}
