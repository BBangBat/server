package com.bbangbat.member.repository

import com.bbangbat.common.id.Tsid
import com.bbangbat.member.domain.AgeGroup
import com.bbangbat.member.domain.Gender
import com.bbangbat.member.domain.Member
import com.bbangbat.member.domain.MemberRole
import com.bbangbat.member.domain.NamePolicy
import com.bbangbat.member.domain.NicknamePolicy
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "members",
    uniqueConstraints = [UniqueConstraint(name = "uk_members_nickname", columnNames = ["nickname"])],
)
class MemberJpaEntity(
    @Id
    @Tsid
    @Column(name = "id", nullable = false, unique = true)
    var id: Long = 0L,
    @Column(name = "email", nullable = false, unique = true, length = 100)
    var email: String,
    @Column(name = "name", nullable = false, length = NamePolicy.MAX_LENGTH)
    var name: String,
    @Column(name = "nickname", nullable = false, length = NicknamePolicy.MAX_LENGTH)
    var nickname: String,
    @Column(name = "profile_image_key", nullable = true, length = 500)
    var profileImageKey: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 10)
    var gender: Gender,
    @Enumerated(EnumType.STRING)
    @Column(name = "age_group", nullable = false, length = 20)
    var ageGroup: AgeGroup,
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    var role: MemberRole = MemberRole.USER,
    @Column(name = "terms_agreed", nullable = false)
    var termsAgreed: Boolean,
    @Column(name = "privacy_agreed", nullable = false)
    var privacyAgreed: Boolean,
    @Column(name = "last_login_at", nullable = true)
    var lastLoginAt: LocalDateTime? = null,
) : BaseEntity() {
    fun applyFrom(member: Member) {
        email = member.email
        name = member.name
        nickname = member.nickname
        profileImageKey = member.profileImageKey
        gender = member.gender
        ageGroup = member.ageGroup
        role = member.role
        termsAgreed = member.termsAgreed
        privacyAgreed = member.privacyAgreed
        lastLoginAt = member.lastLoginAt
    }

    fun toDomain(): Member =
        Member(
            id = id,
            email = email,
            name = name,
            nickname = nickname,
            profileImageKey = profileImageKey,
            gender = gender,
            ageGroup = ageGroup,
            role = role,
            termsAgreed = termsAgreed,
            privacyAgreed = privacyAgreed,
            lastLoginAt = lastLoginAt,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    companion object {
        fun from(member: Member): MemberJpaEntity =
            MemberJpaEntity(
                id = member.id,
                email = member.email,
                name = member.name,
                nickname = member.nickname,
                profileImageKey = member.profileImageKey,
                gender = member.gender,
                ageGroup = member.ageGroup,
                role = member.role,
                termsAgreed = member.termsAgreed,
                privacyAgreed = member.privacyAgreed,
                lastLoginAt = member.lastLoginAt,
            )
    }
}
