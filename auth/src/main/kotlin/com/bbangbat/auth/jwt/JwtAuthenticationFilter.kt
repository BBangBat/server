package com.bbangbat.auth.jwt

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class JwtAuthenticationFilter(
    private val jwtProvider: JwtProvider,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        resolveToken(request)
            ?.takeIf { jwtProvider.validateToken(it) }
            ?.let { token ->
                val memberId = jwtProvider.getMemberId(token)
                val auth =
                    UsernamePasswordAuthenticationToken(
                        memberId,
                        null,
                        listOf(SimpleGrantedAuthority("ROLE_MEMBER")),
                    )

                auth.details = jwtProvider.getProvider(token)
                SecurityContextHolder.getContext().authentication = auth
            }
        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val bearer = request.getHeader("Authorization") ?: return null

        return if (bearer.startsWith("Bearer ")) bearer.substring(7) else null
    }
}
