package com.bbangbat.member.domain

object NamePolicy {
    const val MIN_LENGTH = 1
    const val MAX_LENGTH = 30

    const val LENGTH_MESSAGE = "이름은 ${MIN_LENGTH}자 이상 ${MAX_LENGTH}자 이하여야 합니다."
    const val BLANK_MESSAGE = "이름은 비어 있을 수 없습니다."
}
