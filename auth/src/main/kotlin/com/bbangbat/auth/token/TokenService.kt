package com.bbangbat.auth.token

import com.bbangbat.auth.jwt.JwtProperties
import com.bbangbat.auth.jwt.JwtProvider
import com.bbangbat.common.exception.BbangbatException
import com.bbangbat.common.exception.ErrorCode.INVALID_TOKEN
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class TokenService(
    private val redisTemplate: StringRedisTemplate,
    private val jwtProvider: JwtProvider,
    private val jwtProperties: JwtProperties,
    @param:Value("\${app.redis.key-prefix:}") private val keyPrefix: String,
) {
    private fun rtKey(memberId: Long): String = "${keyPrefix}RT:$memberId"

    fun saveRefreshToken(
        memberId: Long,
        refreshToken: String,
    ) {
        redisTemplate.opsForValue().set(
            rtKey(memberId),
            refreshToken,
            Duration.ofMillis(jwtProperties.refreshTokenExpiry),
        )
    }

    fun rotateToken(refreshToken: String): Pair<String, String> {
        if (!jwtProvider.validateToken(refreshToken)) throw BbangbatException(INVALID_TOKEN)

        val memberId = jwtProvider.getMemberId(refreshToken)
        val stored = redisTemplate.opsForValue().get(rtKey(memberId))

        if (stored != refreshToken) throw BbangbatException(INVALID_TOKEN)

        val provider = jwtProvider.getProvider(refreshToken)
        val newAccessToken = jwtProvider.createAccessToken(memberId, provider)
        val newRefreshToken = jwtProvider.createRefreshToken(memberId, provider)

        saveRefreshToken(memberId, newRefreshToken)

        return newAccessToken to newRefreshToken
    }

    fun deleteRefreshToken(memberId: Long) {
        redisTemplate.delete(rtKey(memberId))
    }
}
