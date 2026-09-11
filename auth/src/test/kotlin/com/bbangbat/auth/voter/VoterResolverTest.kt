package com.bbangbat.auth.voter

import com.bbangbat.auth.token.AnonymousTokenProvider
import com.bbangbat.common.exception.BbangbatException
import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

@ExtendWith(MockitoExtension::class)
class VoterResolverTest {
    @Mock
    private lateinit var anonymousTokenProvider: AnonymousTokenProvider

    private lateinit var voterResolver: VoterResolver

    @BeforeEach
    fun setUp() {
        voterResolver = VoterResolver(anonymousTokenProvider)
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `인증된 회원은 MEMBER 투표자로 식별한다`() {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(42L, null, emptyList())
        val request = MockHttpServletRequest()

        val voter = voterResolver.resolve(request)

        assertThat(voter.type).isEqualTo(VoterType.MEMBER)
        assertThat(voter.key).isEqualTo("42")
    }

    @Test
    fun `익명 토큰 쿠키를 가진 비회원은 GUEST 투표자로 식별한다`() {
        val request = MockHttpServletRequest()
        request.setCookies(Cookie(VoterResolver.ANONYMOUS_COOKIE, "anon-token"))
        org.mockito.BDDMockito
            .given(anonymousTokenProvider.getAnonymousId("anon-token"))
            .willReturn("anon-123")

        val voter = voterResolver.resolve(request)

        assertThat(voter.type).isEqualTo(VoterType.GUEST)
        assertThat(voter.key).isEqualTo("anon-123")
    }

    @Test
    fun `회원도 익명 토큰도 없으면 예외를 던진다`() {
        val request = MockHttpServletRequest()

        assertThrows<BbangbatException> { voterResolver.resolve(request) }
    }
}
