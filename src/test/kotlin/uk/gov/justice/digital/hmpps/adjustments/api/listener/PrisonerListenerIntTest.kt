package uk.gov.justice.digital.hmpps.adjustments.api.listener

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus
import uk.gov.justice.digital.hmpps.adjustments.api.entity.ChangeType
import uk.gov.justice.digital.hmpps.adjustments.api.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.controller.LegacyController
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyAdjustment
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyAdjustmentCreatedResponse
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyAdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.service.LegacyService
import uk.gov.justice.digital.hmpps.adjustments.api.respository.AdjustmentRepository
import uk.gov.justice.digital.hmpps.adjustments.api.wiremock.PrisonApiExtension
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import java.time.LocalDate
import java.util.UUID

class PrisonerListenerIntTest : SqsIntegrationTestBase() {

  @Autowired
  lateinit var adjustmentRepository: AdjustmentRepository

  @Autowired
  lateinit var legacyService: LegacyService

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
      assertThat(prisonerListenerQueue.sqsClient.countAllMessagesOnQueue(prisonerListenerQueueUrl).get()).isEqualTo(0)
    }

    await untilAsserted {
      val adjustment = adjustmentRepository.findById(id).get()
      assertThat(adjustment.status).isEqualTo(AdjustmentStatus.DELETED)
    }
  }

  @Test
  @Transactional
  fun handleAdmission_sameBooking() {
    val id = createAnAdjustment(
      createdAdjustment.copy(),
    )
    await untilAsserted {
      val adjustment = adjustmentRepository.findById(id).get()
      assertThat(adjustment.status).isEqualTo(AdjustmentStatus.ACTIVE)
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
      assertThat(prisonerListenerQueue.sqsClient.countAllMessagesOnQueue(prisonerListenerQueueUrl).get()).isEqualTo(0)
    }

    await untilAsserted {
      val adjustment = adjustmentRepository.findById(id).get()
      assertThat(adjustment.status).isEqualTo(AdjustmentStatus.ACTIVE)
    }

    val adjustment = adjustmentRepository.findById(id).get()
    assertThat(adjustment.currentPeriodOfCustody).isEqualTo(true)
    assertThat(adjustment.adjustmentHistory.last().changeType).isEqualTo(ChangeType.CREATE)
  }

  @Test
  @Transactional
  fun handleAdmission_newBooking() {
    val id = createAnAdjustment(
      createdAdjustment.copy(
        bookingId = 321,
      ),
    )
    await untilAsserted {
      val adjustment = adjustmentRepository.findById(id).get()
      assertThat(adjustment.status).isEqualTo(AdjustmentStatus.ACTIVE)
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
      assertThat(prisonerListenerQueue.sqsClient.countAllMessagesOnQueue(prisonerListenerQueueUrl).get()).isEqualTo(0)
    }

    await untilAsserted {
      val adjustment = adjustmentRepository.findById(id).get()
      assertThat(adjustment.status).isEqualTo(AdjustmentStatus.ACTIVE)
    }

    val adjustment = adjustmentRepository.findById(id).get()
    assertThat(adjustment.currentPeriodOfCustody).isEqualTo(false)
    // assertThat(adjustment.adjustmentHistory.last().changeType).isEqualTo(ChangeType.ADMISSION)
  }

  @Test
  fun handlePrisonerBookingMoved() {
    val oldAdjustment = createAnAdjustment(
      createdAdjustment.copy(
        bookingId = PrisonApiExtension.BOOKING_MOVE_OLD_BOOKING_ID,
        offenderNo = PrisonApiExtension.BOOKING_MOVE_OLD_PRISONER_ID,
        currentTerm = false,
      ),
    )

    val updatedOldAdjustment = createAnAdjustment(
      createdAdjustment.copy(
        bookingId = PrisonApiExtension.BOOKING_MOVE_UPDATED_OLD_BOOKING_ID,
        offenderNo = PrisonApiExtension.BOOKING_MOVE_OLD_PRISONER_ID,
        currentTerm = false,
      ),
    )

    val newAdjustment = createAnAdjustment(
      createdAdjustment.copy(
        bookingId = PrisonApiExtension.BOOKING_MOVE_NEW_BOOKING_ID,
        offenderNo = PrisonApiExtension.BOOKING_MOVE_NEW_PRISONER_ID,
        currentTerm = false,
      ),
    )
    val eventType = "prison-offender-events.prisoner.booking.moved"
    val newPersonId = PrisonApiExtension.BOOKING_MOVE_NEW_PRISONER_ID
    val oldPersonId = PrisonApiExtension.BOOKING_MOVE_OLD_PRISONER_ID
    val bookingId = PrisonApiExtension.BOOKING_MOVE_OLD_BOOKING_ID

    val payload = prisonerBookingMovedPayload(eventType, bookingId.toString(), oldPersonId, newPersonId)

    domainEventsTopicSnsClient.publish(
      PublishRequest.builder().topicArn(domainEventsTopicArn)
        .message(payload)
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String")
              .stringValue(eventType).build(),
          ),
        ).build(),
    ).get()

    await untilAsserted {
      assertThat(prisonerListenerQueue.sqsClient.countAllMessagesOnQueue(prisonerListenerQueueUrl).get()).isEqualTo(0)
    }

    await untilAsserted {
      val adjustment = adjustmentRepository.findById(oldAdjustment).get()
      assertThat(adjustment.person).isEqualTo(newPersonId)
      val adjBookingId = legacyService.getBookingIdFromLegacyData(adjustment.legacyData)
      assertThat(adjBookingId).isEqualTo("456")
      assertThat(adjustment.currentPeriodOfCustody).isEqualTo(true)

      val adjustmentOld = adjustmentRepository.findById(updatedOldAdjustment).get()
      val adjOldBookingId = legacyService.getBookingIdFromLegacyData(adjustmentOld.legacyData)
      assertThat(adjOldBookingId).isEqualTo("789")
      assertThat(adjustmentOld.currentPeriodOfCustody).isEqualTo(true)

      val newPersonAdjustments = adjustmentRepository.findByPerson(newPersonId)
      assertThat(newPersonAdjustments.find { it.id == oldAdjustment }).isNotNull
    }
  }

  @Test
  @Transactional
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
      assertThat(prisonerListenerQueue.sqsClient.countAllMessagesOnQueue(prisonerListenerQueueUrl).get()).isEqualTo(0)
    }

    await untilAsserted {
      val adjustment = adjustmentRepository.findById(id).get()
      assertThat(adjustment.person).isEqualTo(newPersonId)
      assertThat(adjustmentRepository.findByPerson(PrisonApiExtension.PRISONER_ID)).isEmpty()
      assertThat(adjustmentRepository.findByPerson(newPersonId).find { it.id == id }).isNotNull
    }

    val adjustment = adjustmentRepository.findById(id).get()
    assertThat(adjustment.adjustmentHistory.last().changeType == ChangeType.MERGE)
  }

  @Test
  @Transactional
  fun handlePrisonerMergedWithDeletedOldPrisoner() {
    val id = createAnAdjustment(
      createdAdjustment.copy(offenderNo = "DELETED"),
    )
    val eventType = "prison-offender-events.prisoner.merged"
    val newPersonId = "NEWPERSON"
    val deletedId = "DELETED"
    domainEventsTopicSnsClient.publish(
      PublishRequest.builder().topicArn(domainEventsTopicArn)
        .message(prisonerMergedPayload(newPersonId, deletedId))
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String")
              .stringValue(eventType).build(),
          ),
        ).build(),
    ).get()

    await untilAsserted {
      assertThat(prisonerListenerQueue.sqsClient.countAllMessagesOnQueue(prisonerListenerQueueUrl).get()).isEqualTo(0)
    }

    await untilAsserted {
      val adjustment = adjustmentRepository.findById(id).get()
      assertThat(adjustment.person).isEqualTo(newPersonId)
      assertThat(adjustmentRepository.findByPerson(deletedId)).isEmpty()
      assertThat(adjustmentRepository.findByPerson(newPersonId).find { it.id == id }).isNotNull
    }

    val adjustment = adjustmentRepository.findById(id).get()
    assertThat(adjustment.adjustmentHistory.last().changeType == ChangeType.MERGE)
  }

  private fun prisonerAdmissionPayload(nomsNumber: String, eventType: String) = """{"eventType":"$eventType", "additionalInformation": {"nomsNumber":"$nomsNumber", "reason": "NEW_ADMISSION"}}"""

  private fun prisonerReleasedPayload(nomsNumber: String, eventType: String) = """{"eventType":"$eventType", "additionalInformation": {"nomsNumber":"$nomsNumber", "reason": "RELEASED"}}"""

  private fun prisonerMergedPayload(nomsNumber: String, removedNomsNumber: String) = """{"eventType":"prison-offender-events.prisoner.merged","description":"A prisoner has been merged from $removedNomsNumber to $nomsNumber","additionalInformation":{"nomsNumber":"$nomsNumber","removedNomsNumber":"$removedNomsNumber","reason":"MERGE"}}"""

  private fun prisonerBookingMovedPayload(eventType: String, bookingId: String, oldPersonId: String, newPersonId: String) =
    """
        {
            "eventType": "$eventType",
            "additionalInformation": {
                "bookingId": "$bookingId",
                "movedFromNomsNumber": "$oldPersonId",
                "movedToNomsNumber": "$newPersonId",
                "bookingStartDateTime": "2023-10-01T12:00:00Z"
            },
            "occurredAt": "2023-10-01T12:00:00Z",
            "personReference": {
                "identifiers": [
                    {
                        "type": "NOMS",
                        "value": "$newPersonId"
                    }
                ]
            },
            "publishedAt": "2023-10-01T12:00:00Z",
            "version": 1
        }
    """

  private fun createAnAdjustment(adjustment: LegacyAdjustment): UUID = webTestClient
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
      setAdjustmentsROAuth(),
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
    currentTerm = true,
    agencyId = null,
  )
}
