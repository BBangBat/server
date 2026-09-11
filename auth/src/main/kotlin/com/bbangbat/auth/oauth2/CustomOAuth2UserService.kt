package com.bbangbat.auth.oauth2

import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

@Service
class CustomOAuth2UserService(
    private val memberPort: MemberPort,
) : DefaultOAuth2UserService() {
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = super.loadUser(userRequest)
        val userInfo =
            when (userRequest.clientRegistration.registrationId.uppercase()) {
                "NAVER" -> NaverOAuth2UserInfo(oAuth2User.attributes)
                "KAKAO" -> KakaoOAuth2UserInfo(oAuth2User.attributes)
                else ->
                    throw OAuth2AuthenticationException(
                        OAuth2Error(UNSUPPORTED_PROVIDER_ERROR, "지원하지 않는 소셜 로그인입니다.", null),
                        "지원하지 않는 소셜 로그인입니다.",
                    )
            }
        val memberId = memberPort.findByProviderAndProviderId(userInfo.provider, userInfo.providerId)
        val attributes =
            mutableMapOf<String, Any>(
                "email" to userInfo.email,
                "name" to userInfo.name,
                "provider" to userInfo.provider,
                "providerId" to userInfo.providerId,
                SOCIAL_ACCESS_TOKEN_ATTRIBUTE to userRequest.accessToken.tokenValue,
            )

        userInfo.ageGroup?.let { attributes["ageGroup"] = it }
        userInfo.gender?.let { attributes["gender"] = it }

        if (memberId != null) {
            attributes["memberId"] = memberId
        } else {
            attributes["existingAccount"] = memberPort.existsByEmail(userInfo.email)
        }

        return DefaultOAuth2User(
            listOf(SimpleGrantedAuthority("ROLE_MEMBER")),
            attributes,
            "email",
        )
    }

    companion object {
        const val SOCIAL_ACCESS_TOKEN_ATTRIBUTE = "socialAccessToken"
        private const val UNSUPPORTED_PROVIDER_ERROR = "unsupported_provider"
    }
}
