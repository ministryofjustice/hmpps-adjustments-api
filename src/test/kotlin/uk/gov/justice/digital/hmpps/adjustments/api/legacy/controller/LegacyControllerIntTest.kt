package uk.gov.justice.digital.hmpps.adjustments.api.legacy.controller

import jakarta.transaction.Transactional
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.Rollback
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentSource
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.entity.ChangeType
import uk.gov.justice.digital.hmpps.adjustments.api.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyAdjustment
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyAdjustmentCreatedResponse
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyAdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyData
import uk.gov.justice.digital.hmpps.adjustments.api.respository.AdjustmentRepository
import uk.gov.justice.digital.hmpps.adjustments.api.service.EventType
import java.time.LocalDate
import java.util.UUID

@Transactional
@Rollback
class LegacyControllerIntTest : SqsIntegrationTestBase() {

  @Autowired
  private lateinit var adjustmentRepository: AdjustmentRepository

  private lateinit var CREATED_ID: UUID

  @BeforeEach
  fun setup() {
    val result = webTestClient
      .post()
      .uri("/legacy/adjustments")
      .headers(
        setAuthorisation(),
      )
      .header("Content-Type", LegacyController.LEGACY_CONTENT_TYPE)
      .bodyValue(CREATED_ADJUSTMENT)
      .exchange()
      .expectStatus().isCreated
      .returnResult(LegacyAdjustmentCreatedResponse::class.java)
      .responseBody.blockFirst()!!
    CREATED_ID = result.adjustmentId
  }

  @Test
  fun create() {
    // Created in @BeforeEach.
    val adjustment = adjustmentRepository.findById(CREATED_ID).get()

    assertThat(adjustment.adjustmentType).isEqualTo(AdjustmentType.REMAND)
    assertThat(adjustment.adjustmentHistory).singleElement()
    assertThat(adjustment.adjustmentHistory[0].changeType).isEqualTo(ChangeType.CREATE)
    assertThat(adjustment.adjustmentHistory[0].changeByUsername).isEqualTo("NOMIS")
    assertThat(adjustment.adjustmentHistory[0].changeSource).isEqualTo(AdjustmentSource.NOMIS)
    assertThat(adjustment.source).isEqualTo(AdjustmentSource.NOMIS)

    assertThat(adjustment.fromDate).isEqualTo(LocalDate.now().minusDays(5))
    assertThat(adjustment.toDate).isEqualTo(LocalDate.now().minusDays(3))
    assertThat(adjustment.days).isEqualTo(3)
    assertThat(adjustment.daysCalculated).isEqualTo(3)

    val legacyData = objectMapper.convertValue(adjustment.legacyData, LegacyData::class.java)
    assertThat(legacyData).isEqualTo(LegacyData(bookingId = 1, sentenceSequence = 1, postedDate = LocalDate.now(), comment = "Created", type = LegacyAdjustmentType.UR, active = true, migration = false))

    awaitAtMost30Secs untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 1 }
    val latestMessage: String = getLatestMessage()!!.messages()[0].body()
    assertThat(latestMessage).contains(adjustment.id.toString())
    assertThat(latestMessage).contains(EventType.ADJUSTMENT_CREATED.value)
    assertThat(latestMessage).contains(AdjustmentSource.NOMIS.name)
  }

  @Test
  fun migration() {
    cleanQueue()
    val result = webTestClient
      .post()
      .uri("/legacy/adjustments/migration")
      .headers(
        setAuthorisation(),
      )
      .header("Content-Type", LegacyController.LEGACY_CONTENT_TYPE)
      .bodyValue(CREATED_ADJUSTMENT)
      .exchange()
      .expectStatus().isCreated
      .returnResult(LegacyAdjustmentCreatedResponse::class.java)
      .responseBody.blockFirst()!!

    val adjustment = adjustmentRepository.findById(result.adjustmentId).get()
    assertThat(adjustment).isNotNull

    val legacyData = objectMapper.convertValue(adjustment.legacyData, LegacyData::class.java)
    assertThat(legacyData.migration).isTrue

    awaitAtMost30Secs untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }
  }

  @Test
  fun get() {
    cleanQueue()
    val result = webTestClient
      .get()
      .uri("/legacy/adjustments/$CREATED_ID")
      .headers(
        setAuthorisation(),
      )
      .header("Content-Type", LegacyController.LEGACY_CONTENT_TYPE)
      .exchange()
      .expectStatus().isOk
      .returnResult(LegacyAdjustment::class.java)
      .responseBody.blockFirst()!!

    assertThat(result).isEqualTo(CREATED_ADJUSTMENT)
    awaitAtMost30Secs untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }
  }

  @Test
  fun update() {
    cleanQueue()
    webTestClient
      .put()
      .uri("/legacy/adjustments/$CREATED_ID")
      .headers(
        setAuthorisation(),
      )
      .header("Content-Type", LegacyController.LEGACY_CONTENT_TYPE)
      .bodyValue(
        CREATED_ADJUSTMENT.copy(
          adjustmentFromDate = CREATED_ADJUSTMENT.adjustmentFromDate!!.minusYears(1),
          adjustmentDays = 5,
          adjustmentType = LegacyAdjustmentType.RX,
          comment = "Updated",
        ),
      )
      .exchange()
      .expectStatus().isOk

    val adjustment = adjustmentRepository.findById(CREATED_ID).get()

    assertThat(adjustment.adjustmentType).isEqualTo(AdjustmentType.REMAND)
    assertThat(adjustment.adjustmentHistory.size).isEqualTo(2)
    assertThat(adjustment.adjustmentHistory[1].changeType).isEqualTo(ChangeType.UPDATE)
    assertThat(adjustment.adjustmentHistory[1].changeByUsername).isEqualTo("NOMIS")
    assertThat(adjustment.adjustmentHistory[1].changeSource).isEqualTo(AdjustmentSource.NOMIS)
    assertThat(adjustment.adjustmentHistory[1].change.toString()).contains("Created")
    assertThat(adjustment.source).isEqualTo(AdjustmentSource.NOMIS)

    assertThat(adjustment.fromDate).isEqualTo(LocalDate.now().minusDays(5).minusYears(1))
    assertThat(adjustment.toDate).isEqualTo(LocalDate.now().minusDays(1).minusYears(1))
    assertThat(adjustment.adjustmentType).isEqualTo(AdjustmentType.REMAND)
    assertThat(adjustment.days).isEqualTo(5)
    assertThat(adjustment.daysCalculated).isEqualTo(5)

    val legacyData = objectMapper.convertValue(adjustment.legacyData, LegacyData::class.java)
    assertThat(legacyData).isEqualTo(LegacyData(bookingId = 1, sentenceSequence = 1, postedDate = LocalDate.now(), comment = "Updated", type = LegacyAdjustmentType.RX, active = true, migration = false))

    awaitAtMost30Secs untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 1 }
    val latestMessage: String = getLatestMessage()!!.messages()[0].body()
    assertThat(latestMessage).contains(adjustment.id.toString())
    assertThat(latestMessage).contains(EventType.ADJUSTMENT_UPDATED.value)
    assertThat(latestMessage).contains(AdjustmentSource.NOMIS.name)
  }

  @Test
  fun delete() {
    cleanQueue()
    webTestClient
      .delete()
      .uri("/legacy/adjustments/$CREATED_ID")
      .headers(
        setAuthorisation(),
      )
      .header("Content-Type", LegacyController.LEGACY_CONTENT_TYPE)
      .exchange()
      .expectStatus().isOk

    val adjustment = adjustmentRepository.findById(CREATED_ID).get()

    assertThat(adjustment.deleted).isEqualTo(true)
    assertThat(adjustment.adjustmentHistory.size).isEqualTo(2)
    assertThat(adjustment.adjustmentHistory[1].changeType).isEqualTo(ChangeType.DELETE)

    awaitAtMost30Secs untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 1 }
    val latestMessage: String = getLatestMessage()!!.messages()[0].body()
    assertThat(latestMessage).contains(adjustment.id.toString())
    assertThat(latestMessage).contains(EventType.ADJUSTMENT_DELETED.value)
    assertThat(latestMessage).contains(AdjustmentSource.NOMIS.name)
    webTestClient
      .get()
      .uri("/legacy/adjustments/$CREATED_ID")
      .headers(
        setAuthorisation(),
      )
      .header("Content-Type", LegacyController.LEGACY_CONTENT_TYPE)
      .exchange()
      .expectStatus().isNotFound
  }

  companion object {
    private val CREATED_ADJUSTMENT = LegacyAdjustment(
      bookingId = 1,
      sentenceSequence = 1,
      offenderNo = "ABC123",
      adjustmentType = LegacyAdjustmentType.UR,
      adjustmentDate = LocalDate.now(),
      adjustmentFromDate = LocalDate.now().minusDays(5),
      adjustmentDays = 3,
      comment = "Created",
      active = true,
    )
  }
}
