package com.bbangbat.auth.token

import com.bbangbat.auth.jwt.JwtProperties
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

@Component
class AnonymousTokenProvider(
    private val jwtProperties: JwtProperties,
) {
    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret))
    }

    fun createAnonymousToken(anonymousId: String): String =
        Jwts
            .builder()
            .subject(anonymousId)
            .claim("tokenType", TOKEN_TYPE)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + EXPIRY))
            .signWith(key)
            .compact()

    fun getAnonymousId(token: String): String? =
        try {
            val payload =
                Jwts
                    .parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .payload

            if (payload["tokenType"] == TOKEN_TYPE) payload.subject else null
        } catch (e: JwtException) {
            null
        }

    companion object {
        private const val TOKEN_TYPE = "ANONYMOUS"
        const val EXPIRY = 31_536_000_000L
    }
}
