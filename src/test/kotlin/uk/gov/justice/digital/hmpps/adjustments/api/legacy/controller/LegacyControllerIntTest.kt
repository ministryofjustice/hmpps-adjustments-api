package uk.gov.justice.digital.hmpps.adjustments.api.legacy.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.Rollback
import uk.gov.justice.digital.hmpps.adjustments.api.config.ErrorResponse
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentSource
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.entity.ChangeType
import uk.gov.justice.digital.hmpps.adjustments.api.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyAdjustment
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyAdjustmentCreatedResponse
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyAdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyData
import uk.gov.justice.digital.hmpps.adjustments.api.respository.AdjustmentRepository
import java.time.LocalDate
import java.util.UUID
import javax.transaction.Transactional

@Transactional
@Rollback
class LegacyControllerIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var adjustmentRepository: AdjustmentRepository

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  private lateinit var CREATED_ID: UUID

  @BeforeEach
  fun setup() {
    if (!this::CREATED_ID.isInitialized) {
      val result = webTestClient
        .post()
        .uri("/legacy/adjustments")
        .headers(
          setAuthorisation()
        )
        .header("Content-Type", LegacyController.LEGACY_CONTENT_TYPE)
        .bodyValue(CREATED_ADJUSTMENT)
        .exchange()
        .expectStatus().isCreated
        .returnResult(LegacyAdjustmentCreatedResponse::class.java)
        .responseBody.blockFirst()!!
      CREATED_ID = result.adjustmentId
    }
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
    assertThat(adjustment.toDate).isEqualTo(LocalDate.now().minusDays(2))
    assertThat(adjustment.days).isEqualTo(3)
    assertThat(adjustment.daysCalculated).isEqualTo(3)

    val legacyData = objectMapper.convertValue(adjustment.legacyData, LegacyData::class.java)
    assertThat(legacyData).isEqualTo(LegacyData(bookingId = 1, sentenceSequence = 1, comment = "Created", type = LegacyAdjustmentType.UR, active = true))
  }

  @Test
  fun get() {
    val result = webTestClient
      .get()
      .uri("/legacy/adjustments/$CREATED_ID")
      .headers(
        setAuthorisation()
      )
      .header("Content-Type", LegacyController.LEGACY_CONTENT_TYPE)
      .exchange()
      .expectStatus().isOk
      .returnResult(LegacyAdjustment::class.java)
      .responseBody.blockFirst()!!

    assertThat(result).isEqualTo(CREATED_ADJUSTMENT)
  }

  @Test
  fun update() {
    webTestClient
      .put()
      .uri("/legacy/adjustments/$CREATED_ID")
      .headers(
        setAuthorisation()
      )
      .header("Content-Type", LegacyController.LEGACY_CONTENT_TYPE)
      .bodyValue(
        CREATED_ADJUSTMENT.copy(
          adjustmentFromDate = CREATED_ADJUSTMENT.adjustmentFromDate!!.minusYears(1),
          adjustmentDate = CREATED_ADJUSTMENT.adjustmentDate!!.minusYears(1),
          comment = "Updated"
        )
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
    assertThat(adjustment.toDate).isEqualTo(LocalDate.now().minusDays(2).minusYears(1))
    assertThat(adjustment.days).isEqualTo(3)
    assertThat(adjustment.daysCalculated).isEqualTo(3)

    val legacyData = objectMapper.convertValue(adjustment.legacyData, LegacyData::class.java)
    assertThat(legacyData).isEqualTo(LegacyData(bookingId = 1, sentenceSequence = 1, comment = "Updated", type = LegacyAdjustmentType.UR, active = true))
  }

  @Test
  fun `update with different adjustment type`() {

    val result = webTestClient
      .put()
      .uri("/legacy/adjustments/$CREATED_ID")
      .headers(
        setAuthorisation()
      )
      .header("Content-Type", LegacyController.LEGACY_CONTENT_TYPE)
      .bodyValue(
        CREATED_ADJUSTMENT.copy(
          adjustmentType = LegacyAdjustmentType.UAL
        )
      )
      .exchange()
      .expectStatus().isBadRequest
      .returnResult(ErrorResponse::class.java)
      .responseBody.blockFirst()!!
    assertThat(result.userMessage).isEqualTo("The provided adjustment type UAL doesn't match the persisted type UR")
  }

  @Test
  fun delete() {
    webTestClient
      .delete()
      .uri("/legacy/adjustments/$CREATED_ID")
      .headers(
        setAuthorisation()
      )
      .header("Content-Type", LegacyController.LEGACY_CONTENT_TYPE)
      .exchange()
      .expectStatus().isOk

    val adjustment = adjustmentRepository.findById(CREATED_ID).get()

    assertThat(adjustment.deleted).isEqualTo(true)
    assertThat(adjustment.adjustmentHistory.size).isEqualTo(2)
    assertThat(adjustment.adjustmentHistory[1].changeType).isEqualTo(ChangeType.DELETE)

    webTestClient
      .get()
      .uri("/legacy/adjustments/$CREATED_ID")
      .headers(
        setAuthorisation()
      )
      .header("Content-Type", LegacyController.LEGACY_CONTENT_TYPE)
      .exchange()
      .expectStatus().isNotFound
  }

  companion object {
    private val CREATED_ADJUSTMENT = LegacyAdjustment(
      bookingId = 1,
      sentenceSequence = 1,
      offenderId = "ABC123",
      adjustmentType = LegacyAdjustmentType.UR,
      adjustmentDate = LocalDate.now().minusDays(2),
      adjustmentFromDate = LocalDate.now().minusDays(5),
      adjustmentDays = 3,
      comment = "Created",
      active = true
    )
  }
}
