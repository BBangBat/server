package com.bbangbat.store.domain

import kotlin.math.cos

data class MapBounds(
    val south: Double,
    val north: Double,
    val west: Double,
    val east: Double,
) {
    init {
        require(south < north) { "north는 south보다 커야 합니다." }
        require(west < east) { "east는 west보다 커야 합니다." }
    }

    fun clampToServiceArea(): MapBounds? {
        val clampedSouth = south.coerceIn(LAT_MIN, LAT_MAX)
        val clampedNorth = north.coerceIn(LAT_MIN, LAT_MAX)
        val clampedWest = west.coerceIn(LNG_MIN, LNG_MAX)
        val clampedEast = east.coerceIn(LNG_MIN, LNG_MAX)

        if (clampedSouth >= clampedNorth || clampedWest >= clampedEast) {
            return null
        }

        return MapBounds(clampedSouth, clampedNorth, clampedWest, clampedEast)
    }

    companion object {
        const val MAX_RESULTS = 300

        private const val METERS_PER_LATITUDE_DEGREE = 111_320.0

        fun around(
            latitude: Double,
            longitude: Double,
            radiusMeters: Double,
        ): MapBounds {
            val latitudeDelta = radiusMeters / METERS_PER_LATITUDE_DEGREE
            val longitudeDelta = radiusMeters / (METERS_PER_LATITUDE_DEGREE * cos(Math.toRadians(latitude)))

            return MapBounds(
                south = latitude - latitudeDelta,
                north = latitude + latitudeDelta,
                west = longitude - longitudeDelta,
                east = longitude + longitudeDelta,
            )
        }

        private const val LAT_MIN = 36.19
        private const val LAT_MAX = 36.49
        private const val LNG_MIN = 127.22
        private const val LNG_MAX = 127.60
    }
}
