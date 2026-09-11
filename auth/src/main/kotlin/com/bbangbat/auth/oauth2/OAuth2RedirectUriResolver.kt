package com.bbangbat.auth.oauth2

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component

@Component
class OAuth2RedirectUriResolver(
    @param:Value("\${app.frontend-url}") private val frontendUrl: String,
    @param:Value("\${app.frontend-allowed-origins:}") allowedOrigins: String,
) {
    private val allowed =
        allowedOrigins
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()

    fun resolveAndClear(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): String {
        val cookie = request.cookies?.firstOrNull { it.name == OAuth2RedirectUriCookieFilter.REDIRECT_URI_COOKIE }

        if (cookie != null) {
            clearCookie(response)
        }

        val requested = cookie?.value

        return if (requested != null && requested in allowed) requested else frontendUrl
    }

    private fun clearCookie(response: HttpServletResponse) {
        response.addHeader(
            HttpHeaders.SET_COOKIE,
            ResponseCookie
                .from(OAuth2RedirectUriCookieFilter.REDIRECT_URI_COOKIE, "")
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .maxAge(0)
                .build()
                .toString(),
        )
    }
}
