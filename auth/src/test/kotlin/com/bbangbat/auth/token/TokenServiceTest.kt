package com.bbangbat.auth.token

import com.bbangbat.auth.jwt.JwtProperties
import com.bbangbat.auth.jwt.JwtProvider
import com.bbangbat.common.exception.BbangbatException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension::class)
class TokenServiceTest {
    @Mock
    private lateinit var redisTemplate: StringRedisTemplate

    @Mock
    private lateinit var valueOperations: ValueOperations<String, String>

    private lateinit var jwtProvider: JwtProvider
    private lateinit var tokenService: TokenService
    private lateinit var properties: JwtProperties

    @BeforeEach
    fun setUp() {
        properties =
            JwtProperties(
                secret = SECRET,
                accessTokenExpiry = 3600000L,
                refreshTokenExpiry = 1209600000L,
            )
        jwtProvider = JwtProvider(properties)
        given(redisTemplate.opsForValue()).willReturn(valueOperations)
        tokenService = TokenService(redisTemplate, jwtProvider, properties, keyPrefix = "")
    }

    @Test
    fun `Redis에 RT를 저장한다`() {
        val memberId = 1L
        val refreshToken = "sample-refresh-token"

        tokenService.saveRefreshToken(memberId, refreshToken)

        then(valueOperations).should().set(
            eq("RT:$memberId"),
            eq(refreshToken),
            any(Duration::class.java),
        )
    }

    @Test
    fun `유효한 RT로 새 AT와 RT를 반환한다`() {
        val memberId = 1L
        val refreshToken = jwtProvider.createRefreshToken(memberId)
        given(valueOperations.get("RT:$memberId")).willReturn(refreshToken)

        val (newAt, newRt) = tokenService.rotateToken(refreshToken)

        assertThat(jwtProvider.getMemberId(newAt)).isEqualTo(memberId)
        assertThat(jwtProvider.getMemberId(newRt)).isEqualTo(memberId)
        assertThat(newAt).isNotEqualTo(newRt)
    }

    @Test
    fun `유효하지 않은 RT면 예외를 던진다`() {
        val invalidToken = "invalid.token.string"

        assertThrows<BbangbatException> { tokenService.rotateToken(invalidToken) }
    }

    @Test
    fun `Redis에 저장된 RT와 다르면 예외를 던진다`() {
        val memberId = 1L
        val refreshToken = jwtProvider.createRefreshToken(memberId)
        given(valueOperations.get("RT:$memberId")).willReturn("different-token")

        assertThrows<BbangbatException> { tokenService.rotateToken(refreshToken) }
    }

    @Test
    fun `Redis에서 RT를 삭제한다`() {
        val memberId = 1L

        tokenService.deleteRefreshToken(memberId)

        then(redisTemplate).should().delete("RT:$memberId")
    }

    companion object {
        private const val SECRET = "YmJhbmdiYXQtbG9jYWwtZGV2LXNlY3JldC1rZXktbWluaW11bS0zMmNoYXJz"
    }
}
