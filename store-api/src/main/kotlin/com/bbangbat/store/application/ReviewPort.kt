package com.bbangbat.store.application

interface ReviewPort {
    fun countByStoreIds(storeIds: Collection<Long>): Map<Long, Long>
}
