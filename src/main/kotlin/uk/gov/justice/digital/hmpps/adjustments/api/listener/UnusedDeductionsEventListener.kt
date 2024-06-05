package uk.gov.justice.digital.hmpps.adjustments.api.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.adjustments.api.service.UnusedDeductionsEventService

@Service
class UnusedDeductionsEventListener(
  private val objectMapper: ObjectMapper,
  private val eventService: UnusedDeductionsEventService,
) {

  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener("unuseddeductions", factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(
    rawMessage: String,
  ) {
    log.debug("Received message {}", rawMessage)
    val sqsMessage: SQSMessage = objectMapper.readValue(rawMessage)
    return when (sqsMessage.Type) {
      "Notification" -> {
        val (eventType) = objectMapper.readValue<HMPPSDomainEvent>(sqsMessage.Message)
        processMessage(eventType, sqsMessage.Message)
      } else -> {}
    }
  }

  private fun processMessage(eventType: String, message: String) {
    when (eventType) {
      "release-date-adjustments.adjustment.inserted",
      "release-date-adjustments.adjustment.updated",
      "release-date-adjustments.adjustment.deleted",
      ->
        eventService.handleAdjustmentMessage(objectMapper.readValue(message))
      "prisoner-offender-search.prisoner.updated",
      ->
        eventService.handlePrisonerSearchEvent(objectMapper.readValue(message))

      else -> log.info("Received a message I wasn't expecting: {}", eventType)
    }
  }
}
