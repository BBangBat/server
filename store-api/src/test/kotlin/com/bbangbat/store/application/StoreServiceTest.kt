package com.bbangbat.store.application

import com.bbangbat.common.exception.BbangbatException
import com.bbangbat.common.exception.ErrorCode.STORE_NOT_FOUND
import com.bbangbat.store.domain.MapBounds
import com.bbangbat.store.domain.Store
import com.bbangbat.store.repository.StorePersistenceAdapter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class StoreServiceTest {
    @Mock
    private lateinit var storePersistenceAdapter: StorePersistenceAdapter

    private lateinit var storeService: StoreService

    @BeforeEach
    fun setUp() {
        storeService = StoreService(storePersistenceAdapter, DEFAULT_IMAGE_URL)
    }

    @Test
    fun `반경 내 가게 목록을 거리순으로 반환한다`() {
        val lat = 37.5665
        val lng = 126.9780
        val stores =
            listOf(
                Store(id = 1L, name = "가까운 베이커리", latitude = 37.5670, longitude = 126.9780, address = "서울시 중구"),
                Store(id = 2L, name = "먼 베이커리", latitude = 37.5700, longitude = 126.9780, address = "서울시 중구"),
            )
        given(storePersistenceAdapter.findAllWithinBounds(MapBounds.around(lat, lng, 3000.0))).willReturn(stores)

        val result = storeService.findStores(lat, lng)

        assertThat(result).hasSize(2)
        assertThat(result[0].name).isEqualTo("가까운 베이커리")
        assertThat(result[1].name).isEqualTo("먼 베이커리")
    }

    @Test
    fun `사각 영역 안이어도 반경 밖이면 제외한다`() {
        val lat = 37.5665
        val lng = 126.9780
        val stores =
            listOf(
                Store(id = 1L, name = "반경 안", latitude = 37.5670, longitude = 126.9780, address = "서울시 중구"),
                Store(id = 2L, name = "박스 모서리", latitude = 37.5900, longitude = 127.0100, address = "서울시 중구"),
            )
        given(storePersistenceAdapter.findAllWithinBounds(MapBounds.around(lat, lng, 3000.0))).willReturn(stores)

        val result = storeService.findStores(lat, lng)

        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("반경 안")
    }

    @Test
    fun `반경 내 가게가 없으면 빈 리스트를 반환한다`() {
        val lat = 37.5665
        val lng = 126.9780
        given(storePersistenceAdapter.findAllWithinBounds(MapBounds.around(lat, lng, 3000.0))).willReturn(emptyList())

        val result = storeService.findStores(lat, lng)

        assertThat(result).isEmpty()
    }

    @Test
    fun `가게 ID로 단건 조회한다`() {
        val store =
            Store(
                id = 1L,
                name = "빵빵 베이커리",
                latitude = 37.5670,
                longitude = 126.9780,
                address = "서울시 중구",
                imageUrl = null,
            )
        given(storePersistenceAdapter.findByIdOrNull(1L)).willReturn(store)

        val result = storeService.findById(1L)

        assertThat(result.name).isEqualTo("빵빵 베이커리")
        assertThat(result.imageUrl).isEqualTo(DEFAULT_IMAGE_URL)
    }

    @Test
    fun `존재하지 않는 가게를 단건 조회하면 예외를 던진다`() {
        given(storePersistenceAdapter.findByIdOrNull(99L)).willReturn(null)

        val exception = assertThrows<BbangbatException> { storeService.findById(99L) }

        assertThat(exception.errorCode).isEqualTo(STORE_NOT_FOUND)
    }

    @Test
    fun `이미지가 없는 가게는 기본 이미지 URL로 채운다`() {
        val lat = 37.5665
        val lng = 126.9780
        val stores =
            listOf(
                Store(
                    id = 1L,
                    name = "이미지 없는 빵집",
                    latitude = 37.5670,
                    longitude = 126.9780,
                    address = "서울시 중구",
                    imageUrl = null,
                ),
            )
        given(storePersistenceAdapter.findAllWithinBounds(MapBounds.around(lat, lng, 3000.0))).willReturn(stores)

        val result = storeService.findStores(lat, lng)

        assertThat(result[0].imageUrl).isEqualTo(DEFAULT_IMAGE_URL)
    }

    @Test
    fun `이미지가 있는 가게는 원래 URL을 유지한다`() {
        val lat = 37.5665
        val lng = 126.9780
        val customUrl = "store-specific-image-url"
        val stores =
            listOf(
                Store(
                    id = 1L,
                    name = "이미지 있는 빵집",
                    latitude = 37.5670,
                    longitude = 126.9780,
                    address = "서울시 중구",
                    imageUrl = customUrl,
                ),
            )
        given(storePersistenceAdapter.findAllWithinBounds(MapBounds.around(lat, lng, 3000.0))).willReturn(stores)

        val result = storeService.findStores(lat, lng)

        assertThat(result[0].imageUrl).isEqualTo(customUrl)
    }

    @Test
    fun `ID로 조회한 가게에도 기본 이미지를 적용한다`() {
        val storeIds = listOf(1L, 2L)
        val stores =
            listOf(
                Store(
                    id = 1L,
                    name = "이미지 없는 빵집",
                    latitude = 37.5670,
                    longitude = 126.9780,
                    address = "서울시 중구",
                    imageUrl = null,
                ),
                Store(
                    id = 2L,
                    name = "이미지 있는 빵집",
                    latitude = 37.5670,
                    longitude = 126.9780,
                    address = "서울시 중구",
                    imageUrl = "custom-image-url",
                ),
            )
        given(storePersistenceAdapter.findAllByIds(storeIds)).willReturn(stores)

        val result = storeService.findByIds(storeIds)

        assertThat(result[0].imageUrl).isEqualTo(DEFAULT_IMAGE_URL)
        assertThat(result[1].imageUrl).isEqualTo("custom-image-url")
    }

    companion object {
        private const val DEFAULT_IMAGE_URL = "default-image-url"
    }
}
