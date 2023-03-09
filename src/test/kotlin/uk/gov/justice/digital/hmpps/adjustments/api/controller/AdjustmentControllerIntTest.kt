package uk.gov.justice.digital.hmpps.adjustments.api.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.annotation.Rollback
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.adjustments.api.config.ErrorResponse
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentSource
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.entity.ChangeType
import uk.gov.justice.digital.hmpps.adjustments.api.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyData
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.CreateResponseDto
import uk.gov.justice.digital.hmpps.adjustments.api.respository.AdjustmentRepository
import java.time.LocalDate
import java.util.UUID
import javax.transaction.Transactional

@Transactional
@Rollback
class AdjustmentControllerIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var adjustmentRepository: AdjustmentRepository

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  private lateinit var CREATED_ID: UUID

  @BeforeEach
  fun setup() {
    val result = webTestClient
      .post()
      .uri("/adjustments")
      .headers(
        setAuthorisation()
      )
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(CREATED_ADJUSTMENT)
      .exchange()
      .expectStatus().isCreated
      .returnResult(CreateResponseDto::class.java)
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
    assertThat(adjustment.adjustmentHistory[0].changeByUsername).isEqualTo("Test User")
    assertThat(adjustment.adjustmentHistory[0].changeSource).isEqualTo(AdjustmentSource.DPS)
    assertThat(adjustment.source).isEqualTo(AdjustmentSource.DPS)

    assertThat(adjustment.fromDate).isEqualTo(LocalDate.now().minusDays(5))
    assertThat(adjustment.toDate).isEqualTo(LocalDate.now().minusDays(2))
    assertThat(adjustment.days).isEqualTo(null)
    assertThat(adjustment.daysCalculated).isEqualTo(4)

    val legacyData = objectMapper.convertValue(adjustment.legacyData, LegacyData::class.java)
    assertThat(legacyData).isEqualTo(LegacyData(bookingId = 1, sentenceSequence = 1, comment = null, type = null, active = true))
  }

  @Test
  fun get() {
    val result = webTestClient
      .get()
      .uri("/adjustments/$CREATED_ID")
      .headers(
        setAuthorisation()
      )
      .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
      .exchange()
      .expectStatus().isOk
      .returnResult(AdjustmentDto::class.java)
      .responseBody.blockFirst()!!

    assertThat(result).isEqualTo(CREATED_ADJUSTMENT)
  }

  @Test
  fun findByPerson() {
    val result = webTestClient
      .get()
      .uri("/adjustments?person=ABC123")
      .headers(
        setAuthorisation()
      )
      .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
      .exchange()
      .expectStatus().isOk
      .expectBodyList<AdjustmentDto>()
      .returnResult()
      .responseBody!!

    assertThat(result.size).isEqualTo(1)
    assertThat(result[0]).isEqualTo(CREATED_ADJUSTMENT)
  }

  @Test
  fun update() {
    webTestClient
      .put()
      .uri("/adjustments/$CREATED_ID")
      .headers(
        setAuthorisation()
      )
      .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
      .bodyValue(
        CREATED_ADJUSTMENT.copy(
          fromDate = CREATED_ADJUSTMENT.fromDate.minusYears(1),
          toDate = CREATED_ADJUSTMENT.toDate!!.minusYears(1),
        )
      )
      .exchange()
      .expectStatus().isOk

    val adjustment = adjustmentRepository.findById(CREATED_ID).get()

    assertThat(adjustment.adjustmentType).isEqualTo(AdjustmentType.REMAND)
    assertThat(adjustment.adjustmentHistory.size).isEqualTo(2)
    assertThat(adjustment.adjustmentHistory[1].changeType).isEqualTo(ChangeType.UPDATE)
    assertThat(adjustment.adjustmentHistory[1].changeByUsername).isEqualTo("Test User")
    assertThat(adjustment.adjustmentHistory[1].changeSource).isEqualTo(AdjustmentSource.DPS)
    assertThat(adjustment.source).isEqualTo(AdjustmentSource.DPS)

    assertThat(adjustment.fromDate).isEqualTo(LocalDate.now().minusDays(5).minusYears(1))
    assertThat(adjustment.toDate).isEqualTo(LocalDate.now().minusDays(2).minusYears(1))
    assertThat(adjustment.days).isEqualTo(null)
    assertThat(adjustment.daysCalculated).isEqualTo(4)

    val legacyData = objectMapper.convertValue(adjustment.legacyData, LegacyData::class.java)
    assertThat(legacyData).isEqualTo(LegacyData(bookingId = 1, sentenceSequence = 1, comment = null, type = null, active = true))
  }

  @Test
  fun `update with different adjustment type`() {

    val result = webTestClient
      .put()
      .uri("/adjustments/$CREATED_ID")
      .headers(
        setAuthorisation()
      )
      .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
      .bodyValue(
        CREATED_ADJUSTMENT.copy(
          adjustmentType = AdjustmentType.UNLAWFULLY_AT_LARGE
        )
      )
      .exchange()
      .expectStatus().isBadRequest
      .returnResult(ErrorResponse::class.java)
      .responseBody.blockFirst()!!
    assertThat(result.userMessage).isEqualTo("The provided adjustment type UNLAWFULLY_AT_LARGE doesn't match the persisted type REMAND")
  }

  @Test
  fun delete() {
    webTestClient
      .delete()
      .uri("/adjustments/$CREATED_ID")
      .headers(
        setAuthorisation()
      )
      .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
      .exchange()
      .expectStatus().isOk

    val adjustment = adjustmentRepository.findById(CREATED_ID).get()

    assertThat(adjustment.deleted).isEqualTo(true)
    assertThat(adjustment.adjustmentHistory.size).isEqualTo(2)
    assertThat(adjustment.adjustmentHistory[1].changeType).isEqualTo(ChangeType.DELETE)

    webTestClient
      .get()
      .uri("/adjustments/$CREATED_ID")
      .headers(
        setAuthorisation()
      )
      .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
      .exchange()
      .expectStatus().isNotFound
  }

  companion object {
    private val CREATED_ADJUSTMENT = AdjustmentDto(
      bookingId = 1,
      sentenceSequence = 1,
      offenderId = "ABC123",
      adjustmentType = AdjustmentType.REMAND,
      fromDate = LocalDate.now().minusDays(5),
      toDate = LocalDate.now().minusDays(2),
      days = null
    )
  }
}
