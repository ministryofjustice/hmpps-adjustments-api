package uk.gov.justice.digital.hmpps.adjustments.api.listener

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus
import uk.gov.justice.digital.hmpps.adjustments.api.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.controller.LegacyController
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyAdjustment
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyAdjustmentCreatedResponse
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyAdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.respository.AdjustmentRepository
import uk.gov.justice.digital.hmpps.adjustments.api.wiremock.PrisonApiExtension
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import java.time.LocalDate
import java.util.UUID

class PrisonerListenerIntTest : SqsIntegrationTestBase() {

  @Autowired
  lateinit var adjustmentRepository: AdjustmentRepository

  @Test
  fun handleReleased() {
    val id = createAnAdjustment(createdAdjustment.copy())
    await untilAsserted {
      val adjustment = adjustmentRepository.findById(id).get()
      assertThat(adjustment.status).isEqualTo(AdjustmentStatus.ACTIVE)
    }
    val eventType = "prisoner-offender-search.prisoner.released"
    domainEventsTopicSnsClient.publish(
      PublishRequest.builder().topicArn(domainEventsTopicArn)
        .message(prisonerReleasedPayload(PrisonApiExtension.PRISONER_ID, eventType))
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String")
              .stringValue(eventType).build(),
          ),
        ).build(),
    ).get()

    await untilAsserted {
      assertThat(prisonerListenerQueue.sqsClient.countAllMessagesOnQueue(prisonerListenerQueueUrl).get()).isEqualTo(1)
    }
    await untilAsserted {
      assertThat(prisonerListenerQueue.sqsClient.countAllMessagesOnQueue(prisonerListenerQueueUrl).get()).isEqualTo(0)
    }

    await untilAsserted {
      val adjustment = adjustmentRepository.findById(id).get()
      assertThat(adjustment.status).isEqualTo(AdjustmentStatus.INACTIVE)
    }

    val legacyAdjustment = getLegacyAdjustment(id)

    assertThat(legacyAdjustment.bookingReleased).isEqualTo(true)
    assertThat(legacyAdjustment.active).isEqualTo(true)
  }

  @Test
  fun `handleReleased for deleted adjustment`() {
    val id = createAnAdjustment(createdAdjustment.copy())
    deleteAdjustment(id)
    await untilAsserted {
      val adjustment = adjustmentRepository.findById(id).get()
      assertThat(adjustment.status).isEqualTo(AdjustmentStatus.DELETED)
    }
    val eventType = "prisoner-offender-search.prisoner.released"
    domainEventsTopicSnsClient.publish(
      PublishRequest.builder().topicArn(domainEventsTopicArn)
        .message(prisonerReleasedPayload(PrisonApiExtension.PRISONER_ID, eventType))
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String")
              .stringValue(eventType).build(),
          ),
        ).build(),
    ).get()

    await untilAsserted {
      assertThat(prisonerListenerQueue.sqsClient.countAllMessagesOnQueue(prisonerListenerQueueUrl).get()).isEqualTo(1)
    }
    await untilAsserted {
      assertThat(prisonerListenerQueue.sqsClient.countAllMessagesOnQueue(prisonerListenerQueueUrl).get()).isEqualTo(0)
    }

    await untilAsserted {
      val adjustment = adjustmentRepository.findById(id).get()
      assertThat(adjustment.status).isEqualTo(AdjustmentStatus.DELETED)
    }
  }

  @Test
  fun handleAdmission() {
    val id = createAnAdjustment(
      createdAdjustment.copy(
        bookingReleased = true,
      ),
    )
    await untilAsserted {
      val adjustment = adjustmentRepository.findById(id).get()
      assertThat(adjustment.status).isEqualTo(AdjustmentStatus.INACTIVE)
    }
    val eventType = "prisoner-offender-search.prisoner.received"
    domainEventsTopicSnsClient.publish(
      PublishRequest.builder().topicArn(domainEventsTopicArn)
        .message(prisonerAdmissionPayload(PrisonApiExtension.PRISONER_ID, eventType))
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String")
              .stringValue(eventType).build(),
          ),
        ).build(),
    ).get()

    await untilAsserted {
      assertThat(prisonerListenerQueue.sqsClient.countAllMessagesOnQueue(prisonerListenerQueueUrl).get()).isEqualTo(1)
    }
    await untilAsserted {
      assertThat(prisonerListenerQueue.sqsClient.countAllMessagesOnQueue(prisonerListenerQueueUrl).get()).isEqualTo(0)
    }

    await untilAsserted {
      val adjustment = adjustmentRepository.findById(id).get()
      assertThat(adjustment.status).isEqualTo(AdjustmentStatus.ACTIVE)
    }

    val legacyAdjustment = getLegacyAdjustment(id)

    assertThat(legacyAdjustment.bookingReleased).isEqualTo(false)
    assertThat(legacyAdjustment.active).isEqualTo(true)
  }

  @Test
  fun `handleAdmission for deleted adjustment`() {
    val id = createAnAdjustment(
      createdAdjustment.copy(),
    )
    deleteAdjustment(id)
    await untilAsserted {
      val adjustment = adjustmentRepository.findById(id).get()
      assertThat(adjustment.status).isEqualTo(AdjustmentStatus.DELETED)
    }
    val eventType = "prisoner-offender-search.prisoner.received"
    domainEventsTopicSnsClient.publish(
      PublishRequest.builder().topicArn(domainEventsTopicArn)
        .message(prisonerAdmissionPayload(PrisonApiExtension.PRISONER_ID, eventType))
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String")
              .stringValue(eventType).build(),
          ),
        ).build(),
    ).get()

    await untilAsserted {
      assertThat(prisonerListenerQueue.sqsClient.countAllMessagesOnQueue(prisonerListenerQueueUrl).get()).isEqualTo(1)
    }
    await untilAsserted {
      assertThat(prisonerListenerQueue.sqsClient.countAllMessagesOnQueue(prisonerListenerQueueUrl).get()).isEqualTo(0)
    }

    await untilAsserted {
      val adjustment = adjustmentRepository.findById(id).get()
      assertThat(adjustment.status).isEqualTo(AdjustmentStatus.DELETED)
    }
  }

  @Test
  fun handlePrisonerMerged() {
    val id = createAnAdjustment(
      createdAdjustment.copy(),
    )
    val eventType = "prison-offender-events.prisoner.merged"
    val newPersonId = "NEWPERSON"
    domainEventsTopicSnsClient.publish(
      PublishRequest.builder().topicArn(domainEventsTopicArn)
        .message(prisonerMergedPayload(newPersonId, PrisonApiExtension.PRISONER_ID))
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String")
              .stringValue(eventType).build(),
          ),
        ).build(),
    ).get()

    await untilAsserted {
      assertThat(prisonerListenerQueue.sqsClient.countAllMessagesOnQueue(prisonerListenerQueueUrl).get()).isEqualTo(1)
    }
    await untilAsserted {
      assertThat(prisonerListenerQueue.sqsClient.countAllMessagesOnQueue(prisonerListenerQueueUrl).get()).isEqualTo(0)
    }

    await untilAsserted {
      val adjustment = adjustmentRepository.findById(id).get()
      assertThat(adjustment.person).isEqualTo(newPersonId)
      assertThat(adjustmentRepository.findByPerson(PrisonApiExtension.PRISONER_ID)).isEmpty()
      assertThat(adjustmentRepository.findByPerson(newPersonId).find { it.id == id }).isNotNull
    }
  }

  private fun prisonerAdmissionPayload(nomsNumber: String, eventType: String) =
    """{"eventType":"$eventType", "additionalInformation": {"nomsNumber":"$nomsNumber", "reason": "NEW_ADMISSION"}}"""

  private fun prisonerReleasedPayload(nomsNumber: String, eventType: String) =
    """{"eventType":"$eventType", "additionalInformation": {"nomsNumber":"$nomsNumber", "reason": "RELEASED"}}"""

  private fun prisonerMergedPayload(nomsNumber: String, removedNomsNumber: String) =
    """{"eventType":"prison-offender-events.prisoner.merged","description":"A prisoner has been merged from $removedNomsNumber to $nomsNumber","additionalInformation":{"nomsNumber":"$nomsNumber","removedNomsNumber":"$removedNomsNumber","reason":"MERGE"}}"""

  private fun createAnAdjustment(adjustment: LegacyAdjustment): UUID {
    return webTestClient
      .post()
      .uri("/legacy/adjustments")
      .headers(
        setLegacySynchronisationAuth(),
      )
      .header("Content-Type", LegacyController.LEGACY_CONTENT_TYPE)
      .bodyValue(adjustment)
      .exchange()
      .expectStatus().isCreated
      .returnResult(LegacyAdjustmentCreatedResponse::class.java)
      .responseBody.blockFirst()!!.adjustmentId
  }

  private fun deleteAdjustment(adjustmentId: UUID) {
    webTestClient
      .delete()
      .uri("/legacy/adjustments/$adjustmentId")
      .headers(
        setLegacySynchronisationAuth(),
      )
      .header("Content-Type", LegacyController.LEGACY_CONTENT_TYPE)
      .exchange()
      .expectStatus().isOk
  }

  private fun getLegacyAdjustment(id: UUID) = webTestClient
    .get()
    .uri("/legacy/adjustments/$id")
    .headers(
      setViewAdjustmentsAuth(),
    )
    .header("Content-Type", LegacyController.LEGACY_CONTENT_TYPE)
    .exchange()
    .expectStatus().isOk
    .returnResult(LegacyAdjustment::class.java)
    .responseBody.blockFirst()!!

  private val createdAdjustment = LegacyAdjustment(
    bookingId = PrisonApiExtension.BOOKING_ID,
    sentenceSequence = 1,
    offenderNo = PrisonApiExtension.PRISONER_ID,
    adjustmentType = LegacyAdjustmentType.UR,
    adjustmentDate = LocalDate.now(),
    adjustmentFromDate = LocalDate.now().minusDays(5),
    adjustmentDays = 3,
    comment = "Created",
    active = true,
    bookingReleased = false,
  )
}
