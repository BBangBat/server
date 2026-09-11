package com.bbangbat.store.application

import org.springframework.stereotype.Service

@Service
class StoreStatsService(
    private val reviewPort: ReviewPort,
) {
    fun reviewCounts(storeIds: Collection<Long>): Map<Long, Long> {
        if (storeIds.isEmpty()) {
            return emptyMap()
        }

        val counts = reviewPort.countByStoreIds(storeIds)

        return storeIds.associateWith { counts[it] ?: 0L }
    }
}
