package com.bbangbat.store.domain

import com.bbangbat.common.geo.GeoDistance
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test

class MapBoundsTest {
    @Test
    fun `around는 중심 좌표를 포함하는 영역을 만든다`() {
        val bounds = MapBounds.around(DAEJEON_LAT, DAEJEON_LNG, RADIUS_METERS)

        assertThat(bounds.south).isLessThan(DAEJEON_LAT)
        assertThat(bounds.north).isGreaterThan(DAEJEON_LAT)
        assertThat(bounds.west).isLessThan(DAEJEON_LNG)
        assertThat(bounds.east).isGreaterThan(DAEJEON_LNG)
    }

    @Test
    fun `around의 경계는 중심에서 반경만큼 떨어져 있다`() {
        val bounds = MapBounds.around(DAEJEON_LAT, DAEJEON_LNG, RADIUS_METERS)

        val toNorth = GeoDistance.meters(DAEJEON_LAT, DAEJEON_LNG, bounds.north, DAEJEON_LNG)
        val toEast = GeoDistance.meters(DAEJEON_LAT, DAEJEON_LNG, DAEJEON_LAT, bounds.east)

        assertThat(toNorth).isCloseTo(RADIUS_METERS, within(TOLERANCE_METERS))
        assertThat(toEast).isCloseTo(RADIUS_METERS, within(TOLERANCE_METERS))
    }

    @Test
    fun `around는 위도가 높을수록 경도 폭을 넓게 잡는다`() {
        val nearEquator = MapBounds.around(0.0, DAEJEON_LNG, RADIUS_METERS)
        val daejeon = MapBounds.around(DAEJEON_LAT, DAEJEON_LNG, RADIUS_METERS)

        assertThat(daejeon.east - daejeon.west).isGreaterThan(nearEquator.east - nearEquator.west)
    }

    @Test
    fun `around가 만든 영역은 원을 완전히 감싼다`() {
        val bounds = MapBounds.around(DAEJEON_LAT, DAEJEON_LNG, RADIUS_METERS)
        val latitudeDelta = RADIUS_METERS / 111_320.0

        assertThat(DAEJEON_LAT + latitudeDelta).isBetween(bounds.south, bounds.north)
        assertThat(DAEJEON_LAT - latitudeDelta).isBetween(bounds.south, bounds.north)
    }

    companion object {
        private const val DAEJEON_LAT = 36.3504
        private const val DAEJEON_LNG = 127.3845
        private const val RADIUS_METERS = 3000.0
        private const val TOLERANCE_METERS = 20.0
    }
}
