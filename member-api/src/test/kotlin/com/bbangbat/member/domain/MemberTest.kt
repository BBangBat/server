package com.bbangbat.member.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

class MemberTest {
    private fun validMember(
        email: String = "test@test.com",
        name: String = "홍길동",
        nickname: String = "빵괴물",
        profileImageKey: String? = null,
        gender: Gender = Gender.MALE,
        ageGroup: AgeGroup = AgeGroup.TWENTIES,
        termsAgreed: Boolean = true,
        privacyAgreed: Boolean = true,
    ) = Member(
        email = email,
        name = name,
        nickname = nickname,
        profileImageKey = profileImageKey,
        gender = gender,
        ageGroup = ageGroup,
        termsAgreed = termsAgreed,
        privacyAgreed = privacyAgreed,
    )

    @Test
    fun `유효한 회원을 생성할 수 있다`() {
        val email = "test@test.com"
        val name = "홍길동"
        val nickname = "빵괴물"

        val member = validMember(email = email, name = name, nickname = nickname)

        assertThat(member.email).isEqualTo("test@test.com")
        assertThat(member.name).isEqualTo("홍길동")
        assertThat(member.nickname).isEqualTo("빵괴물")
        assertThat(member.profileImageKey).isNull()
    }

    @Test
    fun `프로필 이미지 키를 포함한 회원을 생성할 수 있다`() {
        val profileImageKey = "members/3f2a-image.jpg"

        val member = validMember(profileImageKey = profileImageKey)

        assertThat(member.profileImageKey).isEqualTo("members/3f2a-image.jpg")
    }

    @Test
    fun `이메일은 100자까지 허용된다`() {
        val email = "a".repeat(91) + "@test.com"

        val member = validMember(email = email)

        assertThat(member.email.length).isEqualTo(100)
    }

    @Test
    fun `이메일이 비어있으면 예외가 발생한다`() {
        val email = ""

        assertThrows<IllegalArgumentException> { validMember(email = email) }
    }

    @Test
    fun `이메일이 100자를 초과하면 예외가 발생한다`() {
        val email = "a".repeat(92) + "@test.com"

        assertThrows<IllegalArgumentException> { validMember(email = email) }
    }

    @Test
    fun `이름은 30자까지 허용된다`() {
        val name = "가".repeat(30)

        val member = validMember(name = name)

        assertThat(member.name.length).isEqualTo(30)
    }

    @Test
    fun `이름이 비어있으면 예외가 발생한다`() {
        val name = ""

        assertThrows<IllegalArgumentException> { validMember(name = name) }
    }

    @Test
    fun `이름이 30자를 초과하면 예외가 발생한다`() {
        val name = "가".repeat(31)

        assertThrows<IllegalArgumentException> { validMember(name = name) }
    }

    @Test
    fun `닉네임은 2자 이상 10자 이하여야 한다`() {
        val minNickname = "빵괴"
        val maxNickname = "가".repeat(10)

        val memberWithMin = validMember(nickname = minNickname)
        val memberWithMax = validMember(nickname = maxNickname)

        assertThat(memberWithMin.nickname.length).isEqualTo(2)
        assertThat(memberWithMax.nickname.length).isEqualTo(10)
    }

    @Test
    fun `닉네임에 자음이나 모음 단독이 들어가면 예외가 발생한다`() {
        val nicknames = listOf("ㅃㅏㅇ", "빵ㄱ", "ㅏㅏ")

        nicknames.forEach { nickname ->
            assertThrows<IllegalArgumentException> { validMember(nickname = nickname) }
        }
    }

    @Test
    fun `닉네임에 공백이 들어가면 예외가 발생한다`() {
        val nickname = "빵 괴물"

        assertThrows<IllegalArgumentException> { validMember(nickname = nickname) }
    }

    @Test
    fun `닉네임이 비어있으면 예외가 발생한다`() {
        val nickname = "   "

        assertThrows<IllegalArgumentException> { validMember(nickname = nickname) }
    }

    @Test
    fun `닉네임이 2자 미만이면 예외가 발생한다`() {
        val nickname = "빵"

        assertThrows<IllegalArgumentException> { validMember(nickname = nickname) }
    }

    @Test
    fun `닉네임이 10자를 초과하면 예외가 발생한다`() {
        val nickname = "가".repeat(11)

        assertThrows<IllegalArgumentException> { validMember(nickname = nickname) }
    }

    @Test
    fun `프로필 이미지 키가 허용된 접두사로 시작하지 않으면 예외가 발생한다`() {
        val profileImageKey = "reviews/image.jpg"

        assertThrows<IllegalArgumentException> { validMember(profileImageKey = profileImageKey) }
    }

    @Test
    fun `프로필 이미지 키가 500자를 초과하면 예외가 발생한다`() {
        val profileImageKey = "members/" + "a".repeat(493)

        assertThrows<IllegalArgumentException> { validMember(profileImageKey = profileImageKey) }
    }

    @Test
    fun `서비스 이용약관에 동의하지 않으면 예외가 발생한다`() {
        assertThrows<IllegalArgumentException> { validMember(termsAgreed = false) }
    }

    @Test
    fun `개인정보처리방침에 동의하지 않으면 예외가 발생한다`() {
        assertThrows<IllegalArgumentException> { validMember(privacyAgreed = false) }
    }

    @Test
    fun `updateProfile은 null인 필드를 기존 값으로 유지한다`() {
        val member = validMember(name = "홍길동", nickname = "빵괴물")

        val updated = member.updateProfile(nickname = "빵덕후")

        assertThat(updated.name).isEqualTo("홍길동")
        assertThat(updated.nickname).isEqualTo("빵덕후")
        assertThat(updated.gender).isEqualTo(member.gender)
        assertThat(updated.ageGroup).isEqualTo(member.ageGroup)
    }

    @Test
    fun `updateProfile은 원본을 변경하지 않는다`() {
        val member = validMember(nickname = "빵괴물")

        member.updateProfile(nickname = "빵덕후")

        assertThat(member.nickname).isEqualTo("빵괴물")
    }

    @Test
    fun `updateProfile은 수정 시점에도 닉네임 형식을 검증한다`() {
        val member = validMember()

        assertThrows<IllegalArgumentException> { member.updateProfile(nickname = "빵!!") }
    }

    @Test
    fun `updateProfile은 수정 시점에도 이름 길이를 검증한다`() {
        val member = validMember()

        assertThrows<IllegalArgumentException> { member.updateProfile(name = "가".repeat(31)) }
    }

    @Test
    fun `updateProfile은 수정 시점에도 프로필 이미지 키 형식을 검증한다`() {
        val member = validMember()

        assertThrows<IllegalArgumentException> { member.updateProfile(profileImageKey = "reviews/abc") }
    }

    @Test
    fun `login은 마지막 로그인 시각을 갱신한다`() {
        val member = validMember()
        val now = LocalDateTime.now()

        val loggedIn = member.login(now)

        assertThat(loggedIn.lastLoginAt).isEqualTo(now)
    }
}
