package com.bbangbat.auth.token

import com.bbangbat.auth.oauth2.SocialProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class SocialTokenService(
    private val redisTemplate: StringRedisTemplate,
    @param:Value("\${app.redis.key-prefix:}") private val keyPrefix: String,
) {
    private fun key(
        provider: SocialProvider,
        providerId: String,
    ): String = "${keyPrefix}SAT:${provider.name}:$providerId"

    fun save(
        provider: SocialProvider,
        providerId: String,
        accessToken: String,
    ) {
        redisTemplate.opsForValue().set(key(provider, providerId), accessToken, TOKEN_TTL)
    }

    fun find(
        provider: SocialProvider,
        providerId: String,
    ): String? = redisTemplate.opsForValue().get(key(provider, providerId))

    fun delete(
        provider: SocialProvider,
        providerId: String,
    ) {
        redisTemplate.delete(key(provider, providerId))
    }

    companion object {
        private val TOKEN_TTL = Duration.ofMinutes(5)
    }
}
