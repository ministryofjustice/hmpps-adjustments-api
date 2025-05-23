package uk.gov.justice.digital.hmpps.adjustments.api.legacy.controller

import jakarta.transaction.Transactional
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.Rollback
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentSource
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.entity.ChangeType
import uk.gov.justice.digital.hmpps.adjustments.api.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyAdjustment
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyAdjustmentCreatedResponse
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyAdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyData
import uk.gov.justice.digital.hmpps.adjustments.api.respository.AdjustmentRepository
import uk.gov.justice.digital.hmpps.adjustments.api.service.EventType
import uk.gov.justice.digital.hmpps.adjustments.api.wiremock.PrisonApiExtension
import java.time.LocalDate
import java.util.UUID

@Transactional
@Rollback
class LegacyControllerIntTest : SqsIntegrationTestBase() {

  @Autowired
  private lateinit var adjustmentRepository: AdjustmentRepository

  @Test
  fun create() {
    val createdId = createAdjustment(CREATED_ADJUSTMENT).adjustmentId
    val adjustment = adjustmentRepository.findById(createdId).get()

    assertThat(adjustment.adjustmentType).isEqualTo(AdjustmentType.UNUSED_DEDUCTIONS)
    assertThat(adjustment.adjustmentHistory).singleElement()
    assertThat(adjustment.adjustmentHistory[0].changeType).isEqualTo(ChangeType.CREATE)
    assertThat(adjustment.adjustmentHistory[0].changeByUsername).isEqualTo("NOMIS")
    assertThat(adjustment.adjustmentHistory[0].changeSource).isEqualTo(AdjustmentSource.NOMIS)
    assertThat(adjustment.adjustmentHistory[0].prisonId).isEqualTo("LDS")
    assertThat(adjustment.source).isEqualTo(AdjustmentSource.NOMIS)
    assertThat(adjustment.status).isEqualTo(AdjustmentStatus.INACTIVE)

    assertThat(adjustment.fromDate).isEqualTo(LocalDate.now().minusDays(5))
    assertThat(adjustment.toDate).isEqualTo(LocalDate.now().minusDays(3))
    assertThat(adjustment.days).isEqualTo(null)
    assertThat(adjustment.effectiveDays).isEqualTo(3)

    val legacyData = objectMapper.convertValue(adjustment.legacyData, LegacyData::class.java)
    assertThat(legacyData).isEqualTo(LegacyData(bookingId = 1, sentenceSequence = 1, postedDate = LocalDate.now(), comment = "Created", type = LegacyAdjustmentType.UR, migration = false, adjustmentActive = false))

    awaitAtMost30Secs untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 1 }
    val latestMessage: String = getLatestMessage()!!.messages()[0].body()
    assertThat(latestMessage).contains(adjustment.id.toString())
    assertThat(latestMessage).contains(EventType.ADJUSTMENT_CREATED.value)
    assertThat(latestMessage).contains(AdjustmentSource.NOMIS.name)
  }

  @Test
  fun migration() {
    val result = webTestClient
      .post()
      .uri("/legacy/adjustments/migration")
      .headers(
        setLegacySynchronisationAuth(),
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
    val createdId = createDefaultAdjustmentAndCleanQueue()
    val result = webTestClient
      .get()
      .uri("/legacy/adjustments/$createdId")
      .headers(
        setAdjustmentsROAuth(),
      )
      .header("Content-Type", LegacyController.LEGACY_CONTENT_TYPE)
      .exchange()
      .expectStatus().isOk
      .returnResult(LegacyAdjustment::class.java)
      .responseBody.blockFirst()!!

    assertThat(result).isEqualTo(CREATED_ADJUSTMENT.copy(agencyId = null))
    awaitAtMost30Secs untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }
  }

  @Test
  fun update() {
    val createdId = createDefaultAdjustmentAndCleanQueue()
    val newFromDate = CREATED_ADJUSTMENT.adjustmentFromDate!!.minusYears(1)
    webTestClient
      .put()
      .uri("/legacy/adjustments/$createdId")
      .headers(
        setLegacySynchronisationAuth(),
      )
      .header("Content-Type", LegacyController.LEGACY_CONTENT_TYPE)
      .bodyValue(
        CREATED_ADJUSTMENT.copy(
          adjustmentFromDate = newFromDate,
          adjustmentDays = 5,
          adjustmentType = LegacyAdjustmentType.RX,
          active = true,
          comment = "Updated",
        ),
      )
      .exchange()
      .expectStatus().isOk

    val adjustment = adjustmentRepository.findById(createdId).get()

    assertThat(adjustment.adjustmentType).isEqualTo(AdjustmentType.REMAND)
    assertThat(adjustment.adjustmentHistory.size).isEqualTo(2)
    assertThat(adjustment.adjustmentHistory[1].changeType).isEqualTo(ChangeType.UPDATE)
    assertThat(adjustment.adjustmentHistory[1].changeByUsername).isEqualTo("NOMIS")
    assertThat(adjustment.adjustmentHistory[1].changeSource).isEqualTo(AdjustmentSource.NOMIS)
    assertThat(adjustment.adjustmentHistory[1].change.toString()).contains("Created")
    assertThat(adjustment.adjustmentHistory[1].prisonId).isEqualTo("LDS")
    assertThat(adjustment.status).isEqualTo(AdjustmentStatus.ACTIVE)
    assertThat(adjustment.source).isEqualTo(AdjustmentSource.NOMIS)

    assertThat(adjustment.fromDate).isEqualTo(newFromDate)
    assertThat(adjustment.toDate).isEqualTo(newFromDate.plusDays(4))
    assertThat(adjustment.effectiveDays).isEqualTo(5)
    assertThat(adjustment.days).isEqualTo(null)

    val legacyData = objectMapper.convertValue(adjustment.legacyData, LegacyData::class.java)
    assertThat(legacyData).isEqualTo(LegacyData(bookingId = 1, sentenceSequence = 1, postedDate = LocalDate.now(), comment = "Updated", type = LegacyAdjustmentType.RX, migration = false))

    awaitAtMost30Secs untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 1 }
    val latestMessage: String = getLatestMessage()!!.messages()[0].body()
    assertThat(latestMessage).contains(adjustment.id.toString())
    assertThat(latestMessage).contains(EventType.ADJUSTMENT_UPDATED.value)
    assertThat(latestMessage).contains(AdjustmentSource.NOMIS.name)
  }

  @Test
  fun delete() {
    val createdId = createDefaultAdjustmentAndCleanQueue()
    webTestClient
      .delete()
      .uri("/legacy/adjustments/$createdId")
      .headers(
        setLegacySynchronisationAuth(),
      )
      .header("Content-Type", LegacyController.LEGACY_CONTENT_TYPE)
      .exchange()
      .expectStatus().isOk

    val adjustment = adjustmentRepository.findById(createdId).get()

    assertThat(adjustment.status).isEqualTo(AdjustmentStatus.INACTIVE_WHEN_DELETED)
    assertThat(adjustment.adjustmentHistory.size).isEqualTo(2)
    assertThat(adjustment.adjustmentHistory[1].changeType).isEqualTo(ChangeType.DELETE)

    awaitAtMost30Secs untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 1 }
    val latestMessage: String = getLatestMessage()!!.messages()[0].body()
    assertThat(latestMessage).contains(adjustment.id.toString())
    assertThat(latestMessage).contains(EventType.ADJUSTMENT_DELETED.value)
    assertThat(latestMessage).contains(AdjustmentSource.NOMIS.name)
    webTestClient
      .get()
      .uri("/legacy/adjustments/$createdId")
      .headers(
        setLegacySynchronisationAuth(),
      )
      .header("Content-Type", LegacyController.LEGACY_CONTENT_TYPE)
      .exchange()
      .expectStatus().isNotFound
  }

  @Test
  fun create_bookingReleased() {
    val result = webTestClient
      .post()
      .uri("/legacy/adjustments")
      .headers(
        setLegacySynchronisationAuth(),
      )
      .header("Content-Type", LegacyController.LEGACY_CONTENT_TYPE)
      .bodyValue(CREATED_ADJUSTMENT.copy(active = true, currentTerm = false))
      .exchange()
      .expectStatus().isCreated
      .returnResult(LegacyAdjustmentCreatedResponse::class.java)
      .responseBody.blockFirst()!!
    val createdId = result.adjustmentId
    // Created in @BeforeEach.
    val adjustment = adjustmentRepository.findById(createdId).get()

    assertThat(adjustment.status).isEqualTo(AdjustmentStatus.ACTIVE)
    assertThat(adjustment.currentPeriodOfCustody).isEqualTo(false)

    val legacyData = objectMapper.convertValue(adjustment.legacyData, LegacyData::class.java)
    assertThat(legacyData).isEqualTo(LegacyData(bookingId = 1, sentenceSequence = 1, postedDate = LocalDate.now(), comment = "Created", type = LegacyAdjustmentType.UR, migration = false, adjustmentActive = true))
  }

  private fun createAdjustment(legacyAdjustment: LegacyAdjustment): LegacyAdjustmentCreatedResponse = webTestClient
    .post()
    .uri("/legacy/adjustments")
    .headers(
      setLegacySynchronisationAuth(),
    )
    .header("Content-Type", LegacyController.LEGACY_CONTENT_TYPE)
    .bodyValue(legacyAdjustment)
    .exchange()
    .expectStatus().isCreated
    .returnResult(LegacyAdjustmentCreatedResponse::class.java)
    .responseBody.blockFirst()!!

  private fun createDefaultAdjustmentAndCleanQueue(): UUID {
    val createdId = createAdjustment(CREATED_ADJUSTMENT).adjustmentId
    awaitAtMost30Secs untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 1 }
    cleanQueue()
    return createdId
  }

  companion object {
    private val CREATED_ADJUSTMENT = LegacyAdjustment(
      bookingId = 1,
      sentenceSequence = 1,
      offenderNo = PrisonApiExtension.PRISONER_ID,
      adjustmentType = LegacyAdjustmentType.UR,
      adjustmentDate = LocalDate.now(),
      adjustmentFromDate = LocalDate.now().minusDays(5),
      adjustmentDays = 3,
      comment = "Created",
      active = false,
      bookingReleased = false,
      currentTerm = true,
      agencyId = "LDS",
    )
  }
}
