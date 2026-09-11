package com.bbangbat.auth.oauth2

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration

class OAuth2RedirectUriCookieFilter(
    allowedOrigins: String,
) : OncePerRequestFilter() {
    private val allowed =
        allowedOrigins
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (request.requestURI.startsWith("/oauth2/authorization/")) {
            val redirectUri = request.getParameter("redirect_uri")

            if (redirectUri != null && redirectUri in allowed) {
                response.addHeader(HttpHeaders.SET_COOKIE, shortLivedCookie(REDIRECT_URI_COOKIE, redirectUri))
            }

            val purpose = request.getParameter("purpose")

            if (purpose in KNOWN_PURPOSES) {
                response.addHeader(HttpHeaders.SET_COOKIE, shortLivedCookie(PURPOSE_COOKIE, purpose!!))
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun shortLivedCookie(
        name: String,
        value: String,
    ): String =
        ResponseCookie
            .from(name, value)
            .path("/")
            .httpOnly(true)
            .secure(true)
            .sameSite("Lax")
            .maxAge(Duration.ofSeconds(COOKIE_MAX_AGE))
            .build()
            .toString()

    companion object {
        const val REDIRECT_URI_COOKIE = "oauth2_redirect_uri"
        const val PURPOSE_COOKIE = "oauth2_purpose"

        const val PURPOSE_UNLINK = "unlink"

        const val PURPOSE_LINK = "link"

        private val KNOWN_PURPOSES = setOf(PURPOSE_UNLINK, PURPOSE_LINK)
        private const val COOKIE_MAX_AGE = 180L
    }
}
