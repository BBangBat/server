package com.bbangbat.common.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val httpStatus: HttpStatus,
    val message: String,
) {
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "이미 가입된 이메일입니다. 기존에 사용한 소셜 계정으로 로그인해주세요."),
    SOCIAL_ALREADY_LINKED(HttpStatus.CONFLICT, "이미 연동된 소셜 계정입니다."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    SOCIAL_NOT_LINKED(HttpStatus.NOT_FOUND, "연동되지 않은 소셜 계정입니다."),
    LAST_SOCIAL_CANNOT_UNLINK(HttpStatus.CONFLICT, "마지막 소셜 계정은 해제할 수 없습니다."),
    CURRENT_SOCIAL_CANNOT_UNLINK(HttpStatus.CONFLICT, "현재 로그인 중인 소셜 계정은 해제할 수 없습니다. 다른 소셜 계정으로 로그인 후 시도해주세요."),
    SOCIAL_REAUTH_REQUIRED(HttpStatus.CONFLICT, "소셜 재인증이 필요합니다. 소셜 로그인 후 다시 시도해주세요."),
    FAVORITE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 즐겨찾기에 추가된 가게입니다."),
    FAVORITE_NOT_FOUND(HttpStatus.NOT_FOUND, "즐겨찾기에 없는 가게입니다."),

    STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "가게를 찾을 수 없습니다."),
    CONGESTION_UNAVAILABLE(HttpStatus.NOT_FOUND, "혼잡도 정보를 조회할 수 없습니다."),

    CONGESTION_VOTE_COOLDOWN(HttpStatus.TOO_MANY_REQUESTS, "잠시 후 다시 투표할 수 있습니다."),

    TALK_NOT_FOUND(HttpStatus.NOT_FOUND, "실시간 톡을 찾을 수 없습니다."),
    TALK_FORBIDDEN(HttpStatus.FORBIDDEN, "본인이 작성한 톡만 삭제할 수 있습니다."),

    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."),
    REVIEW_FORBIDDEN(HttpStatus.FORBIDDEN, "본인의 리뷰만 삭제할 수 있습니다."),
}
