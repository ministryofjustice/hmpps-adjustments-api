package uk.gov.justice.digital.hmpps.adjustments.api.service

import com.amazonaws.services.sns.model.MessageAttributeValue
import com.amazonaws.services.sns.model.PublishRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class SnsService(hmppsQueueService: HmppsQueueService, private val objectMapper: ObjectMapper) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  private val domaineventsTopic by lazy { hmppsQueueService.findByTopicId("domainevents") ?: throw RuntimeException("Topic with name domainevents doesn't exist") }
  private val domaineventsTopicClient by lazy { domaineventsTopic.snsClient }

  fun publishDomainEvent(eventType: EventType, description: String, occurredAt: LocalDateTime, additionalInformation: AdditionalInformation) {
    publishToDomainEventsTopic(
      HMPPSDomainEvent(
        eventType.value,
        additionalInformation,
        occurredAt.atZone(ZoneId.systemDefault()).toInstant(),
        description,
      )
    )
  }

  private fun publishToDomainEventsTopic(payload: HMPPSDomainEvent) {
    log.debug("Event {} for id {}", payload.eventType, payload.additionalInformation.id)
    domaineventsTopicClient.publish(
      PublishRequest(domaineventsTopic.arn, objectMapper.writeValueAsString(payload))
        .withMessageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue().withDataType("String").withStringValue(payload.eventType)
          )
        )
        .also { log.info("Published event $payload to outbound topic") }
    )
  }
}

data class AdditionalInformation(
  val id: UUID? = null,
  val nomsNumber: String? = null,
  val source: String? = null
)

data class HMPPSDomainEvent(
  val eventType: String? = null,
  val additionalInformation: AdditionalInformation,
  val version: String,
  val occurredAt: String,
  val description: String
) {
  constructor(
    eventType: String,
    additionalInformation: AdditionalInformation,
    occurredAt: Instant,
    description: String
  ) : this(
    eventType,
    additionalInformation,
    "1.0",
    occurredAt.toOffsetDateFormat(),
    description
  )
}

enum class EventType(val value: String) {
  ADJUSTMENT_CREATED("adjustments.adjustment.inserted"),
  ADJUSTMENT_UPDATED("adjustments.adjustment.updated"),
  ADJUSTMENT_DELETED("adjustments.adjustment.deleted")
}

fun Instant.toOffsetDateFormat(): String =
  atZone(ZoneId.of("Europe/London")).toOffsetDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
