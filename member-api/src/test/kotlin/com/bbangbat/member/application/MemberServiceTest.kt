package com.bbangbat.member.application

import com.bbangbat.auth.oauth2.SocialUnlinkClient
import com.bbangbat.auth.token.TokenService
import com.bbangbat.common.exception.BbangbatException
import com.bbangbat.common.exception.ErrorCode.CURRENT_SOCIAL_CANNOT_UNLINK
import com.bbangbat.common.exception.ErrorCode.EMAIL_ALREADY_REGISTERED
import com.bbangbat.common.exception.ErrorCode.MEMBER_NOT_FOUND
import com.bbangbat.common.exception.ErrorCode.SOCIAL_ALREADY_LINKED
import com.bbangbat.member.domain.AgeGroup
import com.bbangbat.member.domain.Gender
import com.bbangbat.member.domain.Member
import com.bbangbat.member.domain.Social
import com.bbangbat.member.domain.SocialType
import com.bbangbat.member.repository.FavoritePersistenceAdapter
import com.bbangbat.member.repository.MemberPersistenceAdapter
import com.bbangbat.member.repository.SocialPersistenceAdapter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class MemberServiceTest {
    @Mock
    private lateinit var memberPersistenceAdapter: MemberPersistenceAdapter

    @Mock
    private lateinit var socialPersistenceAdapter: SocialPersistenceAdapter

    @Mock
    private lateinit var favoritePersistenceAdapter: FavoritePersistenceAdapter

    @Mock
    private lateinit var livePort: LivePort

    @Mock
    private lateinit var profileImageStoragePort: ProfileImageStoragePort

    @Mock
    private lateinit var socialUnlinkClient: SocialUnlinkClient

    @Mock
    private lateinit var tokenService: TokenService

    private lateinit var memberService: MemberService

    @BeforeEach
    fun setUp() {
        memberService =
            MemberService(
                memberPersistenceAdapter,
                socialPersistenceAdapter,
                favoritePersistenceAdapter,
                livePort,
                profileImageStoragePort,
                socialUnlinkClient,
                tokenService,
            )
    }

    @Test
    fun `회원가입 시 회원과 소셜 계정을 저장한다`() {
        val savedMember =
            Member(
                id = 1L,
                email = "test@test.com",
                name = "홍길동",
                nickname = "빵괴물",
                profileImageKey = null,
                gender = Gender.MALE,
                ageGroup = AgeGroup.TWENTIES,
                termsAgreed = true,
                privacyAgreed = true,
                lastLoginAt = LocalDateTime.now(),
            )
        given(memberPersistenceAdapter.save(any())).willReturn(savedMember)

        val result =
            memberService.signup(
                email = "test@test.com",
                name = "홍길동",
                nickname = "빵괴물",
                profileImageKey = null,
                gender = Gender.MALE,
                ageGroup = AgeGroup.TWENTIES,
                termsAgreed = true,
                privacyAgreed = true,
                provider = SocialType.KAKAO,
                providerId = "kakao-123",
            )

        assertThat(result.id).isEqualTo(1L)
        assertThat(result.email).isEqualTo("test@test.com")
        then(socialPersistenceAdapter).should().save(
            Social(member = savedMember, provider = SocialType.KAKAO, providerId = "kakao-123"),
        )
    }

    @Test
    fun `회원가입 시 lastLoginAt이 설정된다`() {
        val beforeSignup = LocalDateTime.now()
        val savedMember =
            Member(
                id = 1L,
                email = "test@test.com",
                name = "홍길동",
                nickname = "빵괴물",
                profileImageKey = null,
                gender = Gender.MALE,
                ageGroup = AgeGroup.TWENTIES,
                termsAgreed = true,
                privacyAgreed = true,
                lastLoginAt = LocalDateTime.now(),
            )
        given(memberPersistenceAdapter.save(any())).willReturn(savedMember)
        val memberCaptor = argumentCaptor<Member>()

        memberService.signup(
            email = "test@test.com",
            name = "홍길동",
            nickname = "빵괴물",
            profileImageKey = null,
            gender = Gender.MALE,
            ageGroup = AgeGroup.TWENTIES,
            termsAgreed = true,
            privacyAgreed = true,
            provider = SocialType.NAVER,
            providerId = "naver-456",
        )

        verify(memberPersistenceAdapter).save(memberCaptor.capture())
        assertThat(memberCaptor.firstValue.lastLoginAt).isNotNull().isAfterOrEqualTo(beforeSignup)
    }

    @Test
    fun `사용 중인 닉네임이면 true를 반환한다`() {
        given(memberPersistenceAdapter.existsByNickname("빵괴물")).willReturn(true)

        val result = memberService.existsByNickname("빵괴물")

        assertThat(result).isTrue()
    }

    @Test
    fun `사용 가능한 닉네임이면 false를 반환한다`() {
        given(memberPersistenceAdapter.existsByNickname("새닉네임")).willReturn(false)

        val result = memberService.existsByNickname("새닉네임")

        assertThat(result).isFalse()
    }

    @Test
    fun `이미 가입된 이메일이면 예외를 던지고 저장하지 않는다`() {
        val existing =
            Member(
                id = 1L,
                email = "test@test.com",
                name = "기존회원",
                nickname = "기존닉네임",
                profileImageKey = null,
                gender = Gender.MALE,
                ageGroup = AgeGroup.TWENTIES,
                termsAgreed = true,
                privacyAgreed = true,
                lastLoginAt = LocalDateTime.now(),
            )
        given(memberPersistenceAdapter.findByEmailOrNull("test@test.com")).willReturn(existing)

        val exception =
            assertThrows<BbangbatException> {
                memberService.signup(
                    email = "test@test.com",
                    name = "홍길동",
                    nickname = "빵괴물",
                    profileImageKey = null,
                    gender = Gender.MALE,
                    ageGroup = AgeGroup.TWENTIES,
                    termsAgreed = true,
                    privacyAgreed = true,
                    provider = SocialType.NAVER,
                    providerId = "naver-999",
                )
            }

        assertThat(exception.errorCode).isEqualTo(EMAIL_ALREADY_REGISTERED)
        then(memberPersistenceAdapter).should(never()).save(any())
        then(socialPersistenceAdapter).should(never()).save(any())
    }

    @Test
    fun `연동 시 기존 회원에 소셜 계정을 추가한다`() {
        val existing =
            Member(
                id = 1L,
                email = "test@test.com",
                name = "기존회원",
                nickname = "기존닉네임",
                profileImageKey = null,
                gender = Gender.MALE,
                ageGroup = AgeGroup.TWENTIES,
                termsAgreed = true,
                privacyAgreed = true,
                lastLoginAt = LocalDateTime.now().minusDays(1),
            )
        given(memberPersistenceAdapter.findByEmailOrNull("test@test.com")).willReturn(existing)
        given(socialPersistenceAdapter.findByProviderAndProviderId(SocialType.NAVER, "naver-123")).willReturn(null)
        given(memberPersistenceAdapter.update(any())).willAnswer { it.getArgument<Member>(0) }

        val result = memberService.link("test@test.com", SocialType.NAVER, "naver-123")

        assertThat(result.id).isEqualTo(1L)
        then(socialPersistenceAdapter).should().save(
            Social(member = existing, provider = SocialType.NAVER, providerId = "naver-123"),
        )

        val captor = argumentCaptor<Member>()

        verify(memberPersistenceAdapter).update(captor.capture())
        assertThat(captor.firstValue.id).isEqualTo(1L)
        assertThat(captor.firstValue.lastLoginAt).isAfter(existing.lastLoginAt)
    }

    @Test
    fun `연동할 회원이 없으면 예외를 던진다`() {
        given(memberPersistenceAdapter.findByEmailOrNull("none@test.com")).willReturn(null)

        val exception =
            assertThrows<BbangbatException> {
                memberService.link("none@test.com", SocialType.NAVER, "naver-123")
            }

        assertThat(exception.errorCode).isEqualTo(MEMBER_NOT_FOUND)
        then(socialPersistenceAdapter).should(never()).save(any())
    }

    @Test
    fun `이미 연동된 소셜 계정이면 예외를 던진다`() {
        val existing =
            Member(
                id = 1L,
                email = "test@test.com",
                name = "기존회원",
                nickname = "기존닉네임",
                profileImageKey = null,
                gender = Gender.MALE,
                ageGroup = AgeGroup.TWENTIES,
                termsAgreed = true,
                privacyAgreed = true,
                lastLoginAt = LocalDateTime.now(),
            )
        val alreadyLinked = Social(id = 5L, member = existing, provider = SocialType.NAVER, providerId = "naver-123")
        given(memberPersistenceAdapter.findByEmailOrNull("test@test.com")).willReturn(existing)
        given(socialPersistenceAdapter.findByProviderAndProviderId(SocialType.NAVER, "naver-123")).willReturn(alreadyLinked)

        val exception =
            assertThrows<BbangbatException> {
                memberService.link("test@test.com", SocialType.NAVER, "naver-123")
            }

        assertThat(exception.errorCode).isEqualTo(SOCIAL_ALREADY_LINKED)
        then(socialPersistenceAdapter).should(never()).save(any())
    }

    @Test
    fun `현재 로그인 중인 소셜은 연동 해제할 수 없다`() {
        val exception =
            assertThrows<BbangbatException> {
                memberService.unlinkSocial(1L, SocialType.KAKAO, "KAKAO")
            }

        assertThat(exception.errorCode).isEqualTo(CURRENT_SOCIAL_CANNOT_UNLINK)
        then(socialPersistenceAdapter).should(never()).findAllByMemberId(any())
    }

    @Test
    fun `프로필 수정은 도메인이 병합한 결과를 어댑터에 넘긴다`() {
        val current =
            Member(
                id = 1L,
                email = "test@test.com",
                name = "기존회원",
                nickname = "기존닉네임",
                profileImageKey = null,
                gender = Gender.MALE,
                ageGroup = AgeGroup.TWENTIES,
                termsAgreed = true,
                privacyAgreed = true,
            )
        given(memberPersistenceAdapter.findById(1L)).willReturn(current)
        given(memberPersistenceAdapter.existsByNickname("새닉네임")).willReturn(false)
        given(memberPersistenceAdapter.update(any())).willAnswer { it.getArgument<Member>(0) }

        val result =
            memberService.updateProfile(
                memberId = 1L,
                name = null,
                nickname = "새닉네임",
                profileImageKey = null,
                gender = null,
                ageGroup = AgeGroup.THIRTIES,
            )

        assertThat(result.name).isEqualTo("기존회원")
        assertThat(result.nickname).isEqualTo("새닉네임")
        assertThat(result.gender).isEqualTo(Gender.MALE)
        assertThat(result.ageGroup).isEqualTo(AgeGroup.THIRTIES)
    }

    @Test
    fun `프로필 수정 시에도 도메인 규칙 위반은 걸러진다`() {
        val current =
            Member(
                id = 1L,
                email = "test@test.com",
                name = "기존회원",
                nickname = "기존닉네임",
                profileImageKey = null,
                gender = Gender.MALE,
                ageGroup = AgeGroup.TWENTIES,
                termsAgreed = true,
                privacyAgreed = true,
            )
        given(memberPersistenceAdapter.findById(1L)).willReturn(current)

        assertThrows<IllegalArgumentException> {
            memberService.updateProfile(
                memberId = 1L,
                name = null,
                nickname = null,
                profileImageKey = "reviews/abc",
                gender = null,
                ageGroup = null,
            )
        }

        then(memberPersistenceAdapter).should(never()).update(any())
    }
}
