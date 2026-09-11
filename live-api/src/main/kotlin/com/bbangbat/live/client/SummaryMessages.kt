package com.bbangbat.live.client

data class SummaryRequestMessage(
    val storeId: Long,
    val lastMessageId: Long,
    val messages: List<String>,
)

data class SummaryResultMessage(
    val storeId: Long,
    val lastMessageId: Long,
    val summary: String,
)
