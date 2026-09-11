package com.bbangbat.common.exception

class BbangbatException(
    val errorCode: ErrorCode,
    val retryAfterSeconds: Long? = null,
) : RuntimeException(errorCode.message)
