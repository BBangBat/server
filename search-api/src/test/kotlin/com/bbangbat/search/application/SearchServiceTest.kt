package com.bbangbat.search.application

import com.bbangbat.store.domain.Store
import com.bbangbat.store.repository.StorePersistenceAdapter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class SearchServiceTest {
    @Mock
    private lateinit var storePersistenceAdapter: StorePersistenceAdapter

    private lateinit var searchService: SearchService

    @BeforeEach
    fun setUp() {
        searchService = SearchService(storePersistenceAdapter)
    }

    @Test
    fun `검색어가 가게명에 포함된 가게를 반환한다`() {
        val keyword = "베이커리"
        val stores =
            listOf(
                Store(id = 1L, name = "홍길동 베이커리", latitude = 37.5665, longitude = 126.9780, address = "서울시 중구"),
            )
        given(storePersistenceAdapter.findAllByNameStartingWith(keyword)).willReturn(stores)

        val result = searchService.searchStores(keyword)

        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("홍길동 베이커리")
    }

    @Test
    fun `검색 결과가 없으면 빈 리스트를 반환한다`() {
        val keyword = "존재하지않는가게"
        given(storePersistenceAdapter.findAllByNameStartingWith(keyword)).willReturn(emptyList())

        val result = searchService.searchStores(keyword)

        assertThat(result).isEmpty()
    }
}
