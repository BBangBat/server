package com.bbangbat.live.repository

import com.bbangbat.live.domain.CongestionLevel

data class StoreCongestionCount(
    val storeId: Long,
    val level: CongestionLevel,
    val count: Long,
)
