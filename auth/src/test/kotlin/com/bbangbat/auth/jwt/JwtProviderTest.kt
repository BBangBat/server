package com.bbangbat.auth.jwt

import com.bbangbat.auth.token.TempTokenProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class JwtProviderTest {
    private lateinit var jwtProvider: JwtProvider

    @BeforeEach
    fun setUp() {
        jwtProvider =
            JwtProvider(
                JwtProperties(
                    secret = SECRET,
                    accessTokenExpiry = 3600000L,
                    refreshTokenExpiry = 1209600000L,
                ),
            )
    }

    @Test
    fun `유효한 액세스 토큰을 생성한다`() {
        val memberId = 1L

        val token = jwtProvider.createAccessToken(memberId)

        assertThat(jwtProvider.validateToken(token)).isTrue()
        assertThat(jwtProvider.getMemberId(token)).isEqualTo(memberId)
    }

    @Test
    fun `유효한 리프레시 토큰을 생성한다`() {
        val memberId = 2L

        val token = jwtProvider.createRefreshToken(memberId)

        assertThat(jwtProvider.validateToken(token)).isTrue()
        assertThat(jwtProvider.getMemberId(token)).isEqualTo(memberId)
    }

    @Test
    fun `잘못된 형식의 토큰은 검증에 실패한다`() {
        val invalidToken = "invalid.token.string"

        assertThat(jwtProvider.validateToken(invalidToken)).isFalse()
    }

    @Test
    fun `만료된 토큰은 검증에 실패한다`() {
        val expiredJwtProvider =
            JwtProvider(
                JwtProperties(secret = SECRET, accessTokenExpiry = 1L, refreshTokenExpiry = 1L),
            )
        val token = expiredJwtProvider.createAccessToken(1L)
        Thread.sleep(10)

        assertThat(expiredJwtProvider.validateToken(token)).isFalse()
    }

    @Test
    fun `tempToken은 AT 검증에 실패한다`() {
        val tempTokenProvider =
            TempTokenProvider(
                JwtProperties(secret = SECRET, accessTokenExpiry = 3600000L, refreshTokenExpiry = 1209600000L),
            )
        val tempToken =
            tempTokenProvider.createTempToken(
                email = "test@test.com",
                name = "홍길동",
                provider = com.bbangbat.auth.oauth2.SocialProvider.NAVER,
                providerId = "naver_123",
                ageGroup = null,
                gender = null,
            )

        assertThat(jwtProvider.validateToken(tempToken)).isFalse()
    }

    @Test
    fun `다른 시크릿으로 서명된 토큰은 파싱 시 예외가 발생한다`() {
        val otherProvider =
            JwtProvider(
                JwtProperties(secret = OTHER_SECRET, accessTokenExpiry = 3600000L, refreshTokenExpiry = 1209600000L),
            )
        val token = otherProvider.createAccessToken(1L)

        assertThrows<Exception> { jwtProvider.getMemberId(token) }
    }

    companion object {
        private const val SECRET = "YmJhbmdiYXQtbG9jYWwtZGV2LXNlY3JldC1rZXktbWluaW11bS0zMmNoYXJz"
        private const val OTHER_SECRET = "b3RoZXJTZWNyZXRLZXlUaGF0SXNBbHNvTG9uZ0Vub3VnaEZvckpXVA=="
    }
}
