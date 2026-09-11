package com.bbangbat.live.application

interface StorePort {
    fun exists(storeId: Long): Boolean

    fun findNames(storeIds: Collection<Long>): Map<Long, String>
}
