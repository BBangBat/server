package com.bbangbat.store.repository

import com.bbangbat.store.domain.Store
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "stores",
    indexes = [
        Index(name = "idx_stores_latitude_longitude", columnList = "latitude, longitude"),
        Index(name = "idx_stores_name", columnList = "name"),
    ],
)
class StoreJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long = 0L,
    @Column(name = "name", nullable = false, length = 100)
    var name: String,
    @Column(name = "latitude", nullable = false)
    var latitude: Double,
    @Column(name = "longitude", nullable = false)
    var longitude: Double,
    @Column(name = "address", nullable = false, length = 255)
    var address: String,
    @Column(name = "phone_number", length = 20)
    var phoneNumber: String? = null,
    @Column(name = "image_url", length = 500)
    var imageUrl: String? = null,
) : BaseEntity() {
    fun toDomain(): Store =
        Store(
            id = id,
            name = name,
            latitude = latitude,
            longitude = longitude,
            address = address,
            phoneNumber = phoneNumber,
            imageUrl = imageUrl,
        )

    companion object {
        fun from(store: Store): StoreJpaEntity =
            StoreJpaEntity(
                name = store.name,
                latitude = store.latitude,
                longitude = store.longitude,
                address = store.address,
                phoneNumber = store.phoneNumber,
                imageUrl = store.imageUrl,
            )
    }
}
