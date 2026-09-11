package com.bbangbat.store.repository

import com.bbangbat.store.domain.MapBounds
import com.bbangbat.store.domain.Store
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository

@Repository
class StorePersistenceAdapter(
    private val storeRepository: StoreRepository,
) {
    fun findAllWithinBounds(bounds: MapBounds): List<Store> =
        storeRepository
            .findAllByLatitudeBetweenAndLongitudeBetween(
                bounds.south,
                bounds.north,
                bounds.west,
                bounds.east,
            ).map { it.toDomain() }

    fun findWithinBounds(
        bounds: MapBounds,
        limit: Int,
    ): List<Store> =
        storeRepository
            .findAllByLatitudeBetweenAndLongitudeBetween(
                bounds.south,
                bounds.north,
                bounds.west,
                bounds.east,
                PageRequest.of(0, limit),
            ).map { it.toDomain() }

    fun save(store: Store): Store = storeRepository.save(StoreJpaEntity.from(store)).toDomain()

    fun findByIdOrNull(id: Long): Store? = storeRepository.findById(id).orElse(null)?.toDomain()

    fun findAllByIds(ids: Collection<Long>): List<Store> = storeRepository.findAllById(ids).map { it.toDomain() }

    fun findAllByNameStartingWith(keyword: String): List<Store> = storeRepository.findAllByNameStartingWith(keyword).map { it.toDomain() }
}
