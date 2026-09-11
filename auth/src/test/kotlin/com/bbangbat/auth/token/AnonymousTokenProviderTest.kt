package com.bbangbat.auth.token

import com.bbangbat.auth.jwt.JwtProperties
import com.bbangbat.auth.jwt.JwtProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AnonymousTokenProviderTest {
    private lateinit var anonymousTokenProvider: AnonymousTokenProvider
    private lateinit var jwtProvider: JwtProvider

    @BeforeEach
    fun setUp() {
        val properties =
            JwtProperties(
                secret = SECRET,
                accessTokenExpiry = 3600000L,
                refreshTokenExpiry = 1209600000L,
            )
        anonymousTokenProvider = AnonymousTokenProvider(properties)
        jwtProvider = JwtProvider(properties)
    }

    @Test
    fun `발급한 익명 토큰에서 anonymousId를 추출한다`() {
        val anonymousId = "anon-123"
        val token = anonymousTokenProvider.createAnonymousToken(anonymousId)

        assertThat(anonymousTokenProvider.getAnonymousId(token)).isEqualTo(anonymousId)
    }

    @Test
    fun `AT는 익명 토큰으로 파싱하면 null을 반환한다`() {
        val accessToken = jwtProvider.createAccessToken(1L)

        assertThat(anonymousTokenProvider.getAnonymousId(accessToken)).isNull()
    }

    @Test
    fun `잘못된 형식의 토큰은 null을 반환한다`() {
        assertThat(anonymousTokenProvider.getAnonymousId("invalid.token.string")).isNull()
    }

    companion object {
        private const val SECRET = "YmJhbmdiYXQtbG9jYWwtZGV2LXNlY3JldC1rZXktbWluaW11bS0zMmNoYXJz"
    }
}
