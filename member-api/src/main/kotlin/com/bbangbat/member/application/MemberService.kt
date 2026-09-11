package com.bbangbat.member.application

import com.bbangbat.auth.oauth2.SocialProvider
import com.bbangbat.auth.oauth2.SocialUnlinkClient
import com.bbangbat.auth.token.TokenService
import com.bbangbat.common.exception.BbangbatException
import com.bbangbat.common.exception.ErrorCode.CURRENT_SOCIAL_CANNOT_UNLINK
import com.bbangbat.common.exception.ErrorCode.EMAIL_ALREADY_REGISTERED
import com.bbangbat.common.exception.ErrorCode.LAST_SOCIAL_CANNOT_UNLINK
import com.bbangbat.common.exception.ErrorCode.MEMBER_NOT_FOUND
import com.bbangbat.common.exception.ErrorCode.NICKNAME_ALREADY_EXISTS
import com.bbangbat.common.exception.ErrorCode.SOCIAL_ALREADY_LINKED
import com.bbangbat.common.exception.ErrorCode.SOCIAL_NOT_LINKED
import com.bbangbat.common.exception.ErrorCode.SOCIAL_REAUTH_REQUIRED
import com.bbangbat.member.domain.AgeGroup
import com.bbangbat.member.domain.Gender
import com.bbangbat.member.domain.Member
import com.bbangbat.member.domain.Social
import com.bbangbat.member.domain.SocialType
import com.bbangbat.member.repository.FavoritePersistenceAdapter
import com.bbangbat.member.repository.MemberPersistenceAdapter
import com.bbangbat.member.repository.SocialPersistenceAdapter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class MemberService(
    private val memberPersistenceAdapter: MemberPersistenceAdapter,
    private val socialPersistenceAdapter: SocialPersistenceAdapter,
    private val favoritePersistenceAdapter: FavoritePersistenceAdapter,
    private val livePort: LivePort,
    private val profileImageStoragePort: ProfileImageStoragePort,
    private val socialUnlinkClient: SocialUnlinkClient,
    private val tokenService: TokenService,
) {
    @Transactional
    fun updateProfile(
        memberId: Long,
        name: String?,
        nickname: String?,
        profileImageKey: String?,
        gender: Gender?,
        ageGroup: AgeGroup?,
    ): Member {
        val current = memberPersistenceAdapter.findById(memberId)

        if (nickname != null && nickname != current.nickname && memberPersistenceAdapter.existsByNickname(nickname)) {
            throw BbangbatException(NICKNAME_ALREADY_EXISTS)
        }

        val updated = current.updateProfile(name, nickname, profileImageKey, gender, ageGroup)

        return memberPersistenceAdapter.update(updated)
    }

    @Transactional
    fun linkSocialToMember(
        memberId: Long,
        provider: SocialType,
        providerId: String,
    ): Member {
        val member = memberPersistenceAdapter.findById(memberId)
        val alreadyLinkedElsewhere = socialPersistenceAdapter.findByProviderAndProviderId(provider, providerId) != null
        val alreadyHasProvider = socialPersistenceAdapter.findAllByMemberId(memberId).any { it.provider == provider }

        if (alreadyLinkedElsewhere || alreadyHasProvider) {
            throw BbangbatException(SOCIAL_ALREADY_LINKED)
        }

        socialPersistenceAdapter.save(Social(member = member, provider = provider, providerId = providerId))

        return member
    }

    @Transactional(readOnly = true)
    fun findLinkedProviders(memberId: Long): List<SocialType> = socialPersistenceAdapter.findAllByMemberId(memberId).map { it.provider }

    @Transactional
    fun withdraw(memberId: Long) {
        val member = memberPersistenceAdapter.findById(memberId)
        val socials = socialPersistenceAdapter.findAllByMemberId(member.id)

        if (socials.isNotEmpty() && socials.none { socialUnlinkClient.hasUsableToken(providerOf(it.provider), it.providerId) }) {
            throw BbangbatException(SOCIAL_REAUTH_REQUIRED)
        }

        socials.forEach { social -> socialUnlinkClient.unlink(providerOf(social.provider), social.providerId) }

        livePort.deleteCongestionVotesByMemberId(member.id)
        favoritePersistenceAdapter.deleteAllByMemberId(member.id)
        socialPersistenceAdapter.deleteAllByMemberId(member.id)
        memberPersistenceAdapter.deleteById(member.id)
        tokenService.deleteRefreshToken(member.id)
    }

    @Transactional
    fun unlinkSocial(
        memberId: Long,
        provider: SocialType,
        currentProvider: String?,
    ) {
        if (provider.name == currentProvider) {
            throw BbangbatException(CURRENT_SOCIAL_CANNOT_UNLINK)
        }

        val socials = socialPersistenceAdapter.findAllByMemberId(memberId)
        val target = socials.firstOrNull { it.provider == provider } ?: throw BbangbatException(SOCIAL_NOT_LINKED)

        if (socials.size <= 1) {
            throw BbangbatException(LAST_SOCIAL_CANNOT_UNLINK)
        }

        val socialProvider = providerOf(provider)

        if (!socialUnlinkClient.hasUsableToken(socialProvider, target.providerId)) {
            throw BbangbatException(SOCIAL_REAUTH_REQUIRED)
        }

        if (!socialUnlinkClient.unlink(socialProvider, target.providerId)) {
            throw BbangbatException(SOCIAL_REAUTH_REQUIRED)
        }

        socialPersistenceAdapter.delete(target.id)
    }

    private fun providerOf(provider: SocialType): SocialProvider = SocialProvider.valueOf(provider.name)

    fun generateProfileImageUpload(contentType: String): ProfileImageUpload = profileImageStoragePort.generateUpload(contentType)

    fun profileImageUrlOf(member: Member): String? = member.profileImageKey?.let { profileImageStoragePort.buildUrl(it) }

    @Transactional
    fun signup(
        email: String,
        name: String,
        nickname: String,
        profileImageKey: String?,
        gender: Gender?,
        ageGroup: AgeGroup?,
        termsAgreed: Boolean,
        privacyAgreed: Boolean,
        provider: SocialType,
        providerId: String,
    ): Member {
        if (memberPersistenceAdapter.findByEmailOrNull(email) != null) {
            throw BbangbatException(EMAIL_ALREADY_REGISTERED)
        }

        val member =
            memberPersistenceAdapter.save(
                Member(
                    email = email,
                    name = name,
                    nickname = nickname,
                    profileImageKey = profileImageKey,
                    gender = gender ?: Gender.UNKNOWN,
                    ageGroup = ageGroup ?: AgeGroup.UNKNOWN,
                    termsAgreed = termsAgreed,
                    privacyAgreed = privacyAgreed,
                    lastLoginAt = LocalDateTime.now(),
                ),
            )

        socialPersistenceAdapter.save(Social(member = member, provider = provider, providerId = providerId))

        return member
    }

    @Transactional
    fun link(
        email: String,
        provider: SocialType,
        providerId: String,
    ): Member {
        val member =
            memberPersistenceAdapter.findByEmailOrNull(email)
                ?: throw BbangbatException(MEMBER_NOT_FOUND)

        if (socialPersistenceAdapter.findByProviderAndProviderId(provider, providerId) != null) {
            throw BbangbatException(SOCIAL_ALREADY_LINKED)
        }

        socialPersistenceAdapter.save(Social(member = member, provider = provider, providerId = providerId))

        return memberPersistenceAdapter.update(member.login(LocalDateTime.now()))
    }

    @Transactional
    fun updateLastLoginAt(id: Long) {
        val member = memberPersistenceAdapter.findById(id)

        memberPersistenceAdapter.update(member.login(LocalDateTime.now()))
    }

    fun findById(id: Long): Member = memberPersistenceAdapter.findById(id)

    fun existsByNickname(nickname: String): Boolean = memberPersistenceAdapter.existsByNickname(nickname)

    fun findByEmailOrNull(email: String): Member? = memberPersistenceAdapter.findByEmailOrNull(email)

    fun findByIds(ids: Collection<Long>): List<Member> = memberPersistenceAdapter.findAllByIds(ids)

    fun findMemberIdBySocial(
        provider: SocialType,
        providerId: String,
    ): Long? = socialPersistenceAdapter.findByProviderAndProviderId(provider, providerId)?.member?.id
}
