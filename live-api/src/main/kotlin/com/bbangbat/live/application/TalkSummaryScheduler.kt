package com.bbangbat.live.application

import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Profile("!local")
@Component
class TalkSummaryScheduler(
    private val talkService: TalkService,
) {
    @Scheduled(fixedRate = SUMMARY_INTERVAL_MS)
    fun refreshSummaries() {
        talkService.summarizeActiveStores()
    }

    companion object {
        private const val SUMMARY_INTERVAL_MS = 5 * 60 * 1000L
    }
}
