package com.bbangbat.member.api.dto

import com.bbangbat.member.domain.AgeGroup
import com.bbangbat.member.domain.Gender
import com.bbangbat.member.domain.Member
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "회원 응답")
data class MemberResponse(
    @field:Schema(description = "회원 ID", example = "1234567890") val id: Long,
    @field:Schema(description = "이메일", example = "user@example.com") val email: String,
    @field:Schema(description = "이름", example = "홍길동") val name: String,
    @field:Schema(description = "닉네임", example = "빵괴물") val nickname: String,
    @field:Schema(description = "프로필 이미지 URL") val profileImageUrl: String?,
    @field:Schema(description = "성별") val gender: Gender,
    @field:Schema(description = "연령대") val ageGroup: AgeGroup,
    @field:Schema(description = "마지막 로그인 일시") val lastLoginAt: LocalDateTime?,
    @field:Schema(description = "가입 일시") val createdAt: LocalDateTime?,
) {
    companion object {
        fun from(
            member: Member,
            profileImageUrl: String?,
        ): MemberResponse =
            MemberResponse(
                id = member.id,
                email = member.email,
                name = member.name,
                nickname = member.nickname,
                profileImageUrl = profileImageUrl,
                gender = member.gender,
                ageGroup = member.ageGroup,
                lastLoginAt = member.lastLoginAt,
                createdAt = member.createdAt,
            )
    }
}
