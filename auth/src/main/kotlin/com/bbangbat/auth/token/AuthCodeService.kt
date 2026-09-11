package com.bbangbat.auth.token

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.UUID

enum class AuthCodeType { LOGIN, SIGNUP, LINK, UNLINK }

data class AuthCodePayload(
    val type: AuthCodeType,
    val memberId: Long? = null,
    val provider: String? = null,
    val tempToken: String? = null,
    val existingAccount: Boolean = false,
)

@Service
class AuthCodeService(
    private val redisTemplate: StringRedisTemplate,
    @param:Value("\${app.redis.key-prefix:}") private val keyPrefix: String,
) {
    private fun key(code: String): String = "${keyPrefix}AUTHCODE:$code"

    fun issue(payload: AuthCodePayload): String {
        val code = UUID.randomUUID().toString()

        redisTemplate.opsForValue().set(key(code), serialize(payload), CODE_TTL)

        return code
    }

    fun consume(code: String): AuthCodePayload? = redisTemplate.opsForValue().getAndDelete(key(code))?.let { deserialize(it) }

    private fun serialize(payload: AuthCodePayload): String =
        listOf(
            payload.type.name,
            payload.memberId?.toString().orEmpty(),
            payload.provider.orEmpty(),
            payload.tempToken.orEmpty(),
            payload.existingAccount.toString(),
        ).joinToString(DELIMITER)

    private fun deserialize(value: String): AuthCodePayload {
        val parts = value.split(DELIMITER)

        return AuthCodePayload(
            type = AuthCodeType.valueOf(parts[0]),
            memberId = parts[1].takeIf { it.isNotEmpty() }?.toLong(),
            provider = parts[2].takeIf { it.isNotEmpty() },
            tempToken = parts[3].takeIf { it.isNotEmpty() },
            existingAccount = parts[4].toBoolean(),
        )
    }

    companion object {
        private val CODE_TTL = Duration.ofSeconds(60)

        private const val DELIMITER = "|"
    }
}
