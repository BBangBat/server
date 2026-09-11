package com.bbangbat.member.domain

import java.time.LocalDateTime

data class Member(
    val id: Long = 0L,
    val email: String,
    val name: String,
    val nickname: String,
    val profileImageKey: String? = null,
    val gender: Gender,
    val ageGroup: AgeGroup,
    val role: MemberRole = MemberRole.USER,
    val termsAgreed: Boolean,
    val privacyAgreed: Boolean,
    val lastLoginAt: LocalDateTime? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
) {
    init {
        require(email.isNotBlank()) { "이메일은 비어 있을 수 없습니다." }
        require(email.length <= 100) { "이메일은 100자를 초과할 수 없습니다." }
        require(name.isNotBlank()) { NamePolicy.BLANK_MESSAGE }
        require(name.length in NamePolicy.MIN_LENGTH..NamePolicy.MAX_LENGTH) { NamePolicy.LENGTH_MESSAGE }
        require(nickname.isNotBlank()) { "닉네임은 비어 있을 수 없습니다." }
        require(nickname.length in NicknamePolicy.MIN_LENGTH..NicknamePolicy.MAX_LENGTH) { NicknamePolicy.LENGTH_MESSAGE }
        require(NicknamePolicy.isValidFormat(nickname)) { NicknamePolicy.FORMAT_MESSAGE }
        profileImageKey?.let {
            require(it.isNotBlank()) { "프로필 이미지 키는 비어 있을 수 없습니다." }
            require(it.length <= 500) { "프로필 이미지 키는 500자를 초과할 수 없습니다." }
            require(!it.startsWith("/") && !it.contains("..")) { "프로필 이미지 키 형식이 올바르지 않습니다." }
            require(it.startsWith(PROFILE_IMAGE_KEY_PREFIX)) { "프로필 이미지 키는 $PROFILE_IMAGE_KEY_PREFIX 로 시작해야 합니다." }
        }
        require(termsAgreed) { "서비스 이용약관에 동의해야 합니다." }
        require(privacyAgreed) { "개인정보처리방침에 동의해야 합니다." }
    }

    fun updateProfile(
        name: String? = null,
        nickname: String? = null,
        profileImageKey: String? = null,
        gender: Gender? = null,
        ageGroup: AgeGroup? = null,
    ): Member =
        copy(
            name = name ?: this.name,
            nickname = nickname ?: this.nickname,
            profileImageKey = profileImageKey ?: this.profileImageKey,
            gender = gender ?: this.gender,
            ageGroup = ageGroup ?: this.ageGroup,
        )

    fun login(at: LocalDateTime): Member = copy(lastLoginAt = at)

    companion object {
        const val PROFILE_IMAGE_KEY_PREFIX = "members/"
    }
}
