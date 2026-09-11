package com.bbangbat.member.domain

import com.bbangbat.member.domain.SocialType.KAKAO
import com.bbangbat.member.domain.SocialType.NAVER
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SocialTest {
    private val member =
        Member(
            email = "test@test.com",
            name = "홍길동",
            nickname = "빵괴물",
            gender = Gender.MALE,
            ageGroup = AgeGroup.TWENTIES,
            termsAgreed = true,
            privacyAgreed = true,
        )

    @Test
    fun `유효한 소셜 계정을 생성할 수 있다`() {
        val provider = NAVER
        val providerId = "naver_12345"

        val social = Social(member = member, provider = provider, providerId = providerId)

        assertThat(social.provider).isEqualTo(NAVER)
        assertThat(social.providerId).isEqualTo("naver_12345")
    }

    @Test
    fun `소셜 제공자 ID가 비어있으면 예외가 발생한다`() {
        val providerId = ""

        assertThrows<IllegalArgumentException> {
            Social(member = member, provider = KAKAO, providerId = providerId)
        }
    }

    @Test
    fun `소셜 제공자 ID가 공백만 있으면 예외가 발생한다`() {
        val providerId = "   "

        assertThrows<IllegalArgumentException> {
            Social(member = member, provider = KAKAO, providerId = providerId)
        }
    }
}
