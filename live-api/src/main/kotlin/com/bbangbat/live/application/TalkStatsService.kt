package com.bbangbat.live.application

import com.bbangbat.live.repository.TalkPersistenceAdapter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TalkStatsService(
    private val talkPersistenceAdapter: TalkPersistenceAdapter,
) {
    @Transactional(readOnly = true)
    fun countByAuthorId(authorId: Long): Long = talkPersistenceAdapter.countByAuthorId(authorId)
}
