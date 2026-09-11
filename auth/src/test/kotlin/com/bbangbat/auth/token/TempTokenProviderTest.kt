package com.bbangbat.auth.token

import com.bbangbat.auth.jwt.JwtProperties
import com.bbangbat.auth.jwt.JwtProvider
import com.bbangbat.auth.oauth2.SocialProvider
import com.bbangbat.common.exception.BbangbatException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TempTokenProviderTest {
    private lateinit var tempTokenProvider: TempTokenProvider
    private lateinit var jwtProvider: JwtProvider

    @BeforeEach
    fun setUp() {
        val properties =
            JwtProperties(
                secret = SECRET,
                accessTokenExpiry = 3600000L,
                refreshTokenExpiry = 1209600000L,
            )
        tempTokenProvider = TempTokenProvider(properties)
        jwtProvider = JwtProvider(properties)
    }

    @Test
    fun `유효한 tempToken을 파싱한다`() {
        val token =
            tempTokenProvider.createTempToken(
                email = "test@naver.com",
                name = "홍길동",
                provider = SocialProvider.NAVER,
                providerId = "naver_123",
                ageGroup = "TWENTIES",
                gender = "MALE",
            )

        val claims = tempTokenProvider.parse(token)

        assertThat(claims.email).isEqualTo("test@naver.com")
        assertThat(claims.name).isEqualTo("홍길동")
        assertThat(claims.provider).isEqualTo(SocialProvider.NAVER)
        assertThat(claims.providerId).isEqualTo("naver_123")
        assertThat(claims.ageGroup).isEqualTo("TWENTIES")
    }

    @Test
    fun `AT를 tempToken으로 파싱하면 예외를 던진다`() {
        val accessToken = jwtProvider.createAccessToken(1L)

        assertThrows<BbangbatException> { tempTokenProvider.parse(accessToken) }
    }

    companion object {
        private const val SECRET = "YmJhbmdiYXQtbG9jYWwtZGV2LXNlY3JldC1rZXktbWluaW11bS0zMmNoYXJz"
    }
}
