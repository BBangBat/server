package com.bbangbat.member.domain

object NicknamePolicy {
    const val MIN_LENGTH = 2
    const val MAX_LENGTH = 10
    const val REGEX = "^[가-힣a-zA-Z0-9]+$"

    const val LENGTH_MESSAGE = "닉네임은 ${MIN_LENGTH}자 이상 ${MAX_LENGTH}자 이하여야 합니다."
    const val FORMAT_MESSAGE = "닉네임은 한글, 영문, 숫자만 사용할 수 있습니다. (자음/모음 단독, 공백 불가)"

    private val PATTERN = Regex(REGEX)

    fun isValidFormat(nickname: String): Boolean = PATTERN.matches(nickname)
}
