package uk.gov.justice.digital.hmpps.adjustments.api.legacy.controller

import jakarta.transaction.Transactional
import org.assertj.core.api.Assertions.assertThat
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
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentDto
import uk.gov.justice.digital.hmpps.adjustments.api.respository.AdjustmentRepository
import java.time.LocalDate
import java.time.LocalDateTime

/*
This is testing the sync between NOMIS and our service.
 */
@Transactional
@Rollback
class LegacyAndAdjustmentsControllerIntTest : SqsIntegrationTestBase() {

  @Autowired
  private lateinit var adjustmentRepository: AdjustmentRepository

  @Test
  fun `Create an adjustments from NOMIS and update it from DPS`() {
    val result = webTestClient
      .post()
      .uri("/legacy/adjustments")
      .headers(
        setAuthorisation(),
      )
      .header("Content-Type", LegacyController.LEGACY_CONTENT_TYPE)
      .bodyValue(LEGACY_ADJUSTMENT)
      .exchange()
      .expectStatus().isCreated
      .returnResult(LegacyAdjustmentCreatedResponse::class.java)
      .responseBody.blockFirst()!!

    val id = result.adjustmentId

    webTestClient
      .put()
      .uri("/adjustments/$id")
      .headers(
        setAuthorisation(),
      )
      .bodyValue(ADJUSTMENT)
      .exchange()
      .expectStatus().isOk

    val adjustment = adjustmentRepository.findById(id).get()

    assertThat(adjustment.adjustmentType).isEqualTo(AdjustmentType.REMAND)
    assertThat(adjustment.adjustmentHistory.size).isEqualTo(2)
    assertThat(adjustment.adjustmentHistory[0].changeType).isEqualTo(ChangeType.CREATE)
    assertThat(adjustment.adjustmentHistory[0].changeByUsername).isEqualTo("NOMIS")
    assertThat(adjustment.adjustmentHistory[0].changeSource).isEqualTo(AdjustmentSource.NOMIS)
    assertThat(adjustment.adjustmentHistory[1].changeType).isEqualTo(ChangeType.UPDATE)
    assertThat(adjustment.adjustmentHistory[1].changeByUsername).isEqualTo("Test User")
    assertThat(adjustment.adjustmentHistory[1].changeSource).isEqualTo(AdjustmentSource.DPS)
    assertThat(adjustment.source).isEqualTo(AdjustmentSource.DPS)

    assertThat(adjustment.fromDate).isEqualTo(LocalDate.now().minusDays(5))
    assertThat(adjustment.toDate).isEqualTo(LocalDate.now().plusDays(2))
    assertThat(adjustment.days).isEqualTo(null)
    assertThat(adjustment.daysCalculated).isEqualTo(8)

    val legacyData = objectMapper.convertValue(adjustment.legacyData, LegacyData::class.java)
    assertThat(legacyData).isEqualTo(LegacyData(bookingId = 1, sentenceSequence = 1, postedDate = LocalDate.now(), comment = null, type = LegacyAdjustmentType.UR, active = true, migration = false))
  }

  @Test
  fun `Create an adjustments from NOMIS with minimal data and update it from DPS`() {
    val createResult = webTestClient
      .post()
      .uri("/legacy/adjustments")
      .headers(
        setAuthorisation(),
      )
      .header("Content-Type", LegacyController.LEGACY_CONTENT_TYPE)
      .bodyValue(LEGACY_ADJUSTMENT.copy(adjustmentFromDate = null))
      .exchange()
      .expectStatus().isCreated
      .returnResult(LegacyAdjustmentCreatedResponse::class.java)
      .responseBody.blockFirst()!!

    val id = createResult.adjustmentId

    webTestClient
      .put()
      .uri("/adjustments/$id")
      .headers(
        setAuthorisation(),
      )
      .bodyValue(ADJUSTMENT)
      .exchange()
      .expectStatus().isOk

    val getResult = webTestClient
      .get()
      .uri("/legacy/adjustments/$id")
      .headers(
        setAuthorisation(),
      )
      .header("Content-Type", LegacyController.LEGACY_CONTENT_TYPE)
      .exchange()
      .expectStatus().isOk
      .returnResult(LegacyAdjustment::class.java)
      .responseBody.blockFirst()!!

    assertThat(getResult.adjustmentDays).isEqualTo(8)
  }

  @Test
  fun `Create an adjustments from NOMIS with minimal data and delete it from DPS`() {
    val createResult = webTestClient
      .post()
      .uri("/legacy/adjustments")
      .headers(
        setAuthorisation(),
      )
      .header("Content-Type", LegacyController.LEGACY_CONTENT_TYPE)
      .bodyValue(LEGACY_ADJUSTMENT.copy(adjustmentFromDate = null))
      .exchange()
      .expectStatus().isCreated
      .returnResult(LegacyAdjustmentCreatedResponse::class.java)
      .responseBody.blockFirst()!!

    val id = createResult.adjustmentId

    webTestClient
      .delete()
      .uri("/adjustments/$id")
      .headers(
        setAuthorisation(),
      )
      .exchange()
      .expectStatus().isOk
  }

  companion object {
    private val LEGACY_ADJUSTMENT = LegacyAdjustment(
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

    private val ADJUSTMENT = AdjustmentDto(
      id = null,
      bookingId = 1,
      sentenceSequence = 1,
      person = "ABC123",
      adjustmentType = AdjustmentType.REMAND,
      fromDate = LocalDate.now().minusDays(5),
      toDate = LocalDate.now().plusDays(2),
      days = null,
      additionalDaysAwarded = null,
      unlawfullyAtLarge = null,
      remand = null,
      lastUpdatedDate = LocalDateTime.now(),
    )
  }
}
