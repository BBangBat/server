package com.bbangbat.auth.oauth2

import com.bbangbat.auth.token.SocialTokenService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class SocialUnlinkClient(
    private val socialTokenService: SocialTokenService,
    @param:Value("\${spring.security.oauth2.client.registration.naver.client-id:}") private val naverClientId: String,
    @param:Value("\${spring.security.oauth2.client.registration.naver.client-secret:}") private val naverClientSecret: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val restClient = RestClient.create()

    fun hasUsableToken(
        provider: SocialProvider,
        providerId: String,
    ): Boolean = socialTokenService.find(provider, providerId) != null

    fun unlink(
        provider: SocialProvider,
        providerId: String,
    ): Boolean {
        val accessToken = socialTokenService.find(provider, providerId)

        if (accessToken == null) {
            log.warn("연동 해제용 소셜 토큰이 없어 건너뜁니다. 재인증이 필요합니다. provider={}", provider)

            return false
        }

        val result =
            when (provider) {
                SocialProvider.KAKAO -> unlinkKakao(accessToken)
                SocialProvider.NAVER -> unlinkNaver(accessToken)
            }

        socialTokenService.delete(provider, providerId)

        return result
    }

    private fun unlinkKakao(accessToken: String): Boolean =
        runCatching {
            restClient
                .post()
                .uri(KAKAO_UNLINK_URL)
                .header("Authorization", "Bearer $accessToken")
                .retrieve()
                .toBodilessEntity()
        }.onFailure { e ->
            log.error("카카오 연동 해제 실패", e)
        }.isSuccess

    private fun unlinkNaver(accessToken: String): Boolean =
        runCatching {
            restClient
                .post()
                .uri(
                    "$NAVER_UNLINK_URL?grant_type=delete&client_id=$naverClientId" +
                        "&client_secret=$naverClientSecret&access_token=$accessToken&service_provider=NAVER",
                ).retrieve()
                .toBodilessEntity()
        }.onFailure { e ->
            log.error("네이버 연동 해제 실패", e)
        }.isSuccess

    companion object {
        private const val KAKAO_UNLINK_URL = "https://kapi.kakao.com/v1/user/unlink"
        private const val NAVER_UNLINK_URL = "https://nid.naver.com/oauth2.0/token"
    }
}
