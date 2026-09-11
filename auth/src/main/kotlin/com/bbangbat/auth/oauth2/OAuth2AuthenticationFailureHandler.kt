package com.bbangbat.auth.oauth2

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.stereotype.Component

@Component
class OAuth2AuthenticationFailureHandler(
    private val redirectUriResolver: OAuth2RedirectUriResolver,
) : AuthenticationFailureHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException,
    ) {
        val errorCode = (exception as? OAuth2AuthenticationException)?.error?.errorCode ?: UNKNOWN_ERROR_CODE
        val base = redirectUriResolver.resolveAndClear(request, response)

        log.warn("소셜 로그인 실패: errorCode={}", errorCode, exception)

        response.sendRedirect("$base/oauth2/callback?error=oauth_failed")
    }

    companion object {
        private const val UNKNOWN_ERROR_CODE = "unknown"
    }
}
