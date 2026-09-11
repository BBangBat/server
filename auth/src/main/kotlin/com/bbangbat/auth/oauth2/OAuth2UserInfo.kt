package com.bbangbat.auth.oauth2

import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error

const val MISSING_USER_ATTRIBUTE_ERROR = "missing_user_attribute"

private fun missingAttribute(key: String): OAuth2AuthenticationException =
    OAuth2AuthenticationException(
        OAuth2Error(MISSING_USER_ATTRIBUTE_ERROR, "소셜 계정에서 필수 정보를 가져오지 못했습니다: $key", null),
        "소셜 계정에서 필수 정보를 가져오지 못했습니다: $key",
    )

@Suppress("UNCHECKED_CAST")
private fun Map<String, Any>.requiredMap(key: String): Map<String, Any> = this[key] as? Map<String, Any> ?: throw missingAttribute(key)

private fun Map<String, Any>.requiredString(key: String): String =
    (this[key] as? String)?.takeIf { it.isNotBlank() } ?: throw missingAttribute(key)

private fun Map<String, Any>.requiredId(key: String): String =
    this[key]?.toString()?.takeIf { it.isNotBlank() } ?: throw missingAttribute(key)

abstract class OAuth2UserInfo(
    val attributes: Map<String, Any>,
) {
    abstract val email: String
    abstract val name: String
    abstract val provider: SocialProvider
    abstract val providerId: String
    abstract val ageGroup: String?

    abstract val gender: String?
}

class NaverOAuth2UserInfo(
    attributes: Map<String, Any>,
) : OAuth2UserInfo(attributes) {
    private val response = attributes.requiredMap("response")
    override val email: String get() = response.requiredString("email")
    override val name: String get() = response.requiredString("name")
    override val provider = SocialProvider.NAVER
    override val providerId: String get() = response.requiredId("id")
    override val ageGroup: String?
        get() =
            when (response["age"] as? String) {
                "10-19" -> "TEENS"
                "20-29" -> "TWENTIES"
                "30-39" -> "THIRTIES"
                "40-49" -> "FORTIES"
                "50-59" -> "FIFTIES"
                "60-" -> "SIXTIES_PLUS"
                else -> null
            }
    override val gender: String?
        get() =
            when (response["gender"] as? String) {
                "M" -> "MALE"
                "F" -> "FEMALE"
                else -> null
            }
}

class KakaoOAuth2UserInfo(
    attributes: Map<String, Any>,
) : OAuth2UserInfo(attributes) {
    private val kakaoAccount = attributes.requiredMap("kakao_account")
    private val profile = kakaoAccount.requiredMap("profile")

    override val email: String get() = kakaoAccount.requiredString("email")
    override val name: String get() = profile.requiredString("nickname")
    override val provider = SocialProvider.KAKAO
    override val providerId: String get() = attributes.requiredId("id")
    override val ageGroup: String?
        get() =
            when (kakaoAccount["age_range"] as? String) {
                "10-14", "15-19" -> "TEENS"
                "20-29" -> "TWENTIES"
                "30-39" -> "THIRTIES"
                "40-49" -> "FORTIES"
                "50-59" -> "FIFTIES"
                "60-69", "70-79", "80-89", "90-" -> "SIXTIES_PLUS"
                else -> null
            }
    override val gender: String?
        get() =
            when (kakaoAccount["gender"] as? String) {
                "male" -> "MALE"
                "female" -> "FEMALE"
                else -> null
            }
}
