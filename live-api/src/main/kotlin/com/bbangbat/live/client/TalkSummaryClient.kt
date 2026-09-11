package com.bbangbat.live.client

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import tools.jackson.databind.ObjectMapper

@Component
class TalkSummaryClient(
    private val sqsClient: SqsClient,
    private val objectMapper: ObjectMapper,
    @param:Value("\${cloud.aws.sqs.summary-request-queue-url:}") private val requestQueueUrl: String,
) {
    fun requestSummary(
        storeId: Long,
        lastMessageId: Long,
        messages: List<String>,
    ) {
        val body = objectMapper.writeValueAsString(SummaryRequestMessage(storeId, lastMessageId, messages))

        sqsClient.sendMessage(
            SendMessageRequest
                .builder()
                .queueUrl(requestQueueUrl)
                .messageBody(body)
                .build(),
        )
    }
}
