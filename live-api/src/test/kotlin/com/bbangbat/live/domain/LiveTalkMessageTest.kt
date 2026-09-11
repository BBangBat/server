package com.bbangbat.live.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class LiveTalkMessageTest {
    @Test
    fun `가게 ID가 0 이하면 예외를 던진다`() {
        assertThatThrownBy { message(storeId = 0L) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `내용이 비어 있으면 예외를 던진다`() {
        assertThatThrownBy { message(content = "   ") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `내용이 100자를 초과하면 예외를 던진다`() {
        assertThatThrownBy { message(content = "a".repeat(101)) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `내용이 100자이면 예외 없이 생성된다`() {
        message(content = "a".repeat(100))
    }

    @Test
    fun `삭제 시각이 없으면 살아 있는 메시지다`() {
        assertThat(message().isDeleted).isFalse()
    }

    @Test
    fun `delete는 삭제 시각을 채우고 원본은 그대로 둔다`() {
        val original = message()
        val deletedAt = LocalDateTime.now()

        val deleted = original.delete(deletedAt)

        assertThat(deleted.isDeleted).isTrue()
        assertThat(deleted.deletedAt).isEqualTo(deletedAt)
        assertThat(original.isDeleted).isFalse()
    }

    @Test
    fun `작성자 본인은 삭제할 수 있다`() {
        assertThat(message().canBeDeletedBy(AUTHOR_ID) { false }).isTrue()
    }

    @Test
    fun `작성자가 아니어도 운영자면 삭제할 수 있다`() {
        assertThat(message().canBeDeletedBy(999L) { true }).isTrue()
    }

    @Test
    fun `작성자도 운영자도 아니면 삭제할 수 없다`() {
        assertThat(message().canBeDeletedBy(999L) { false }).isFalse()
    }

    @Test
    fun `작성자 본인이면 운영자 여부를 조회하지 않는다`() {
        var adminChecked = false

        message().canBeDeletedBy(AUTHOR_ID) {
            adminChecked = true
            true
        }

        assertThat(adminChecked).isFalse()
    }

    private fun message(
        storeId: Long = 1L,
        content: String = "지금 사람 많아요!",
    ): LiveTalkMessage =
        LiveTalkMessage(
            storeId = storeId,
            authorId = AUTHOR_ID,
            authorNickname = "빵순이",
            content = content,
            createdAt = LocalDateTime.now(),
        )

    companion object {
        private const val AUTHOR_ID = 1L
    }
}
