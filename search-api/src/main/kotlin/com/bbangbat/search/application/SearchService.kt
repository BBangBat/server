package com.bbangbat.search.application

import com.bbangbat.store.domain.Store
import com.bbangbat.store.repository.StorePersistenceAdapter
import org.springframework.stereotype.Service

@Service
class SearchService(
    private val storePersistenceAdapter: StorePersistenceAdapter,
) {
    fun searchStores(keyword: String): List<Store> = storePersistenceAdapter.findAllByNameStartingWith(keyword)
}
