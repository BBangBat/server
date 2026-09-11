package com.bbangbat.store.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class StoreTest {
    @Test
    fun `유효한 가게를 생성할 수 있다`() {
        val store = Store(name = "가 베이커리", latitude = 37.5665, longitude = 126.9780, address = "서울시 중구")

        assertThat(store.name).isEqualTo("가 베이커리")
        assertThat(store.phoneNumber).isNull()
    }

    @Test
    fun `전화번호가 있는 가게를 생성할 수 있다`() {
        val store =
            Store(
                name = "가 베이커리",
                latitude = 37.5665,
                longitude = 126.9780,
                address = "서울시 중구",
                phoneNumber = "02-1234-5678",
            )

        assertThat(store.phoneNumber).isEqualTo("02-1234-5678")
    }

    @Test
    fun `가게명이 공백이면 예외가 발생한다`() {
        assertThrows<IllegalArgumentException> {
            Store(name = " ", latitude = 37.5665, longitude = 126.9780, address = "서울시 중구")
        }
    }

    @Test
    fun `주소가 공백이면 예외가 발생한다`() {
        assertThrows<IllegalArgumentException> {
            Store(name = "가 베이커리", latitude = 37.5665, longitude = 126.9780, address = " ")
        }
    }

    @Test
    fun `위도가 90을 초과하면 예외가 발생한다`() {
        assertThrows<IllegalArgumentException> {
            Store(name = "가 베이커리", latitude = 91.0, longitude = 126.9780, address = "서울시 중구")
        }
    }

    @Test
    fun `위도가 -90 미만이면 예외가 발생한다`() {
        assertThrows<IllegalArgumentException> {
            Store(name = "가 베이커리", latitude = -91.0, longitude = 126.9780, address = "서울시 중구")
        }
    }

    @Test
    fun `경도가 180을 초과하면 예외가 발생한다`() {
        assertThrows<IllegalArgumentException> {
            Store(name = "가 베이커리", latitude = 37.5665, longitude = 181.0, address = "서울시 중구")
        }
    }

    @Test
    fun `경도가 -180 미만이면 예외가 발생한다`() {
        assertThrows<IllegalArgumentException> {
            Store(name = "가 베이커리", latitude = 37.5665, longitude = -181.0, address = "서울시 중구")
        }
    }
}
