package com.bbangbat.auth.oauth2

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OAuth2UserInfoTest {
    @Test
    fun `네이버 연령대 10대를 TEENS로 파싱한다`() {
        val userInfo = naverUserInfo(age = "10-19")

        assertThat(userInfo.ageGroup).isEqualTo("TEENS")
    }

    @Test
    fun `네이버 연령대 20대를 TWENTIES로 파싱한다`() {
        val userInfo = naverUserInfo(age = "20-29")

        assertThat(userInfo.ageGroup).isEqualTo("TWENTIES")
    }

    @Test
    fun `네이버 연령대 30대를 THIRTIES로 파싱한다`() {
        val userInfo = naverUserInfo(age = "30-39")

        assertThat(userInfo.ageGroup).isEqualTo("THIRTIES")
    }

    @Test
    fun `네이버 연령대를 구간별로 파싱한다`() {
        val forties = naverUserInfo(age = "40-49")
        val fifties = naverUserInfo(age = "50-59")
        val sixties = naverUserInfo(age = "60-")

        assertThat(forties.ageGroup).isEqualTo("FORTIES")
        assertThat(fifties.ageGroup).isEqualTo("FIFTIES")
        assertThat(sixties.ageGroup).isEqualTo("SIXTIES_PLUS")
    }

    @Test
    fun `네이버 연령대 정보가 없으면 null을 반환한다`() {
        val userInfo = naverUserInfo(age = null)

        assertThat(userInfo.ageGroup).isNull()
    }

    @Test
    fun `카카오 연령대 10대를 TEENS로 파싱한다`() {
        val early = kakaoUserInfo(ageRange = "10-14")
        val late = kakaoUserInfo(ageRange = "15-19")

        assertThat(early.ageGroup).isEqualTo("TEENS")
        assertThat(late.ageGroup).isEqualTo("TEENS")
    }

    @Test
    fun `카카오 연령대 20대를 TWENTIES로 파싱한다`() {
        val userInfo = kakaoUserInfo(ageRange = "20-29")

        assertThat(userInfo.ageGroup).isEqualTo("TWENTIES")
    }

    @Test
    fun `카카오 연령대 30대를 THIRTIES로 파싱한다`() {
        val userInfo = kakaoUserInfo(ageRange = "30-39")

        assertThat(userInfo.ageGroup).isEqualTo("THIRTIES")
    }

    @Test
    fun `카카오 연령대를 구간별로 파싱한다`() {
        val forties = kakaoUserInfo(ageRange = "40-49")
        val fifties = kakaoUserInfo(ageRange = "50-59")
        val sixties = kakaoUserInfo(ageRange = "60-69")
        val seventies = kakaoUserInfo(ageRange = "70-79")
        val eighties = kakaoUserInfo(ageRange = "80-89")
        val ninetyPlus = kakaoUserInfo(ageRange = "90-")

        assertThat(forties.ageGroup).isEqualTo("FORTIES")
        assertThat(fifties.ageGroup).isEqualTo("FIFTIES")
        assertThat(sixties.ageGroup).isEqualTo("SIXTIES_PLUS")
        assertThat(seventies.ageGroup).isEqualTo("SIXTIES_PLUS")
        assertThat(eighties.ageGroup).isEqualTo("SIXTIES_PLUS")
        assertThat(ninetyPlus.ageGroup).isEqualTo("SIXTIES_PLUS")
    }

    @Test
    fun `카카오 연령대 정보가 없으면 null을 반환한다`() {
        val userInfo = kakaoUserInfo(ageRange = null)

        assertThat(userInfo.ageGroup).isNull()
    }

    private fun naverUserInfo(age: String?): NaverOAuth2UserInfo {
        val response =
            mutableMapOf<String, Any>(
                "id" to "naver_123",
                "email" to "test@naver.com",
                "name" to "홍길동",
            )

        age?.let { response["age"] = it }

        return NaverOAuth2UserInfo(mapOf("response" to response))
    }

    private fun kakaoUserInfo(ageRange: String?): KakaoOAuth2UserInfo {
        val kakaoAccount =
            mutableMapOf<String, Any>(
                "email" to "test@kakao.com",
                "profile" to mapOf("nickname" to "홍길동"),
            )

        ageRange?.let { kakaoAccount["age_range"] = it }

        return KakaoOAuth2UserInfo(mapOf("id" to "kakao_123", "kakao_account" to kakaoAccount))
    }
}
