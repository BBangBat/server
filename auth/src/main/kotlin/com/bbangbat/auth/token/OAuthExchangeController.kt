package com.bbangbat.auth.token

import com.bbangbat.auth.jwt.JwtProvider
import com.bbangbat.auth.oauth2.MemberPort
import com.bbangbat.common.exception.BbangbatException
import com.bbangbat.common.exception.ErrorCode.INVALID_TOKEN
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus.OK
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "인증", description = "JWT 토큰 관리 API")
@RestController
@RequestMapping("/auth/oauth")
class OAuthExchangeController(
    private val authCodeService: AuthCodeService,
    private val jwtProvider: JwtProvider,
    private val tokenService: TokenService,
    private val refreshTokenCookieProvider: RefreshTokenCookieProvider,
    private val memberPort: MemberPort,
    private val tempTokenProvider: TempTokenProvider,
) {
    @Operation(
        summary = "OAuth 결과 교환",
        description =
            "소셜 로그인 후 리다이렉트로 받은 1회용 code를 토큰으로 교환합니다. " +
                "code는 60초 내 한 번만 사용할 수 있습니다. " +
                "type이 LOGIN이면 accessToken과 refresh_token 쿠키가, SIGNUP/LINK면 tempToken이 반환됩니다. " +
                "SIGNUP은 소셜에서 받은 gender/ageGroup도 함께 반환합니다. (미제공이면 null) " +
                "type이 UNLINK면 재인증한 provider만 반환되고 기존 로그인 세션은 그대로 유지됩니다.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "교환 성공"),
        ApiResponse(responseCode = "401", description = "이미 사용했거나 만료된 code"),
    )
    @PostMapping("/exchange")
    @ResponseStatus(OK)
    fun exchange(
        @RequestBody @Valid request: OAuthExchangeRequest,
        response: HttpServletResponse,
    ): OAuthExchangeResponse {
        val payload = authCodeService.consume(request.code) ?: throw BbangbatException(INVALID_TOKEN)

        return when (payload.type) {
            AuthCodeType.LOGIN -> completeLogin(payload, response)
            AuthCodeType.SIGNUP -> signupResponse(payload)
            AuthCodeType.LINK -> OAuthExchangeResponse(type = payload.type, tempToken = payload.tempToken)

            AuthCodeType.UNLINK -> OAuthExchangeResponse(type = payload.type, provider = payload.provider)
        }
    }

    private fun signupResponse(payload: AuthCodePayload): OAuthExchangeResponse {
        val claims = payload.tempToken?.let { tempTokenProvider.parse(it) }

        return OAuthExchangeResponse(
            type = payload.type,
            tempToken = payload.tempToken,
            existingAccount = payload.existingAccount,
            gender = claims?.gender,
            ageGroup = claims?.ageGroup,
        )
    }

    private fun completeLogin(
        payload: AuthCodePayload,
        response: HttpServletResponse,
    ): OAuthExchangeResponse {
        val memberId = payload.memberId ?: throw BbangbatException(INVALID_TOKEN)
        val accessToken = jwtProvider.createAccessToken(memberId, payload.provider)
        val refreshToken = jwtProvider.createRefreshToken(memberId, payload.provider)

        tokenService.saveRefreshToken(memberId, refreshToken)
        refreshTokenCookieProvider.addCookie(response, refreshToken)
        memberPort.updateLastLoginAt(memberId)

        return OAuthExchangeResponse(type = payload.type, accessToken = accessToken)
    }
}
