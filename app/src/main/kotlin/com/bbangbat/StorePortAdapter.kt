package com.bbangbat

import com.bbangbat.review.application.ReviewStore
import com.bbangbat.store.application.StoreService
import org.springframework.stereotype.Component
import com.bbangbat.live.application.StorePort as LiveStorePort
import com.bbangbat.review.application.StorePort as ReviewStorePort

@Component
class StorePortAdapter(
    private val storeService: StoreService,
) : ReviewStorePort,
    LiveStorePort {
    override fun findByIds(storeIds: Collection<Long>): Map<Long, ReviewStore> =
        storeService
            .findByIds(storeIds)
            .associate { store ->
                store.id to
                    ReviewStore(
                        id = store.id,
                        name = store.name,
                        imageUrl = requireNotNull(store.imageUrl),
                    )
            }

    override fun exists(storeId: Long): Boolean = storeService.exists(storeId)

    override fun findNames(storeIds: Collection<Long>): Map<Long, String> = storeService.findByIds(storeIds).associate { it.id to it.name }
}
