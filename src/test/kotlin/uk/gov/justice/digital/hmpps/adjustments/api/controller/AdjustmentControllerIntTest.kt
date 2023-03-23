package uk.gov.justice.digital.hmpps.adjustments.api.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.adjustments.api.config.ErrorResponse
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentSource
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.entity.ChangeType
import uk.gov.justice.digital.hmpps.adjustments.api.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyData
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentDetailsDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.CreateResponseDto
import uk.gov.justice.digital.hmpps.adjustments.api.respository.AdjustmentRepository
import uk.gov.justice.digital.hmpps.adjustments.api.service.EventType
import java.time.LocalDate
import java.util.UUID
import javax.transaction.Transactional

@Transactional
class AdjustmentControllerIntTest : SqsIntegrationTestBase() {

  @Autowired
  private lateinit var adjustmentRepository: AdjustmentRepository

  @Test
  fun create() {
    val id = createAnAdjustment()
    val adjustment = adjustmentRepository.findById(id).get()

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
    assertThat(legacyData).isEqualTo(LegacyData(bookingId = 1, sentenceSequence = 1, postedDate = LocalDate.now(), comment = null, type = null, active = true))

    assertThat(getNumberOfMessagesCurrentlyOnQueue()).isEqualTo(1)
    val latestMessage = getLatestMessage()!!.messages[0].body
    assertThat(latestMessage).contains(adjustment.id.toString())
    assertThat(latestMessage).contains(EventType.ADJUSTMENT_CREATED.value)
    assertThat(latestMessage).contains(AdjustmentSource.DPS.name)
  }

  @Test
  fun get() {
    val id = createAnAdjustment().also {
      cleanQueue()
    }
    val result = webTestClient
      .get()
      .uri("/adjustments/$id")
      .headers(
        setAuthorisation()
      )
      .exchange()
      .expectStatus().isOk
      .returnResult(AdjustmentDetailsDto::class.java)
      .responseBody.blockFirst()!!

    assertThat(result).isEqualTo(CREATED_ADJUSTMENT.copy(days = 4))
    assertThat(getNumberOfMessagesCurrentlyOnQueue()).isEqualTo(0)
  }

  @Test
  fun findByPerson() {
    val person = "BCDEFG"
    val id = createAnAdjustment(person).also {
      cleanQueue()
    }
    val result = webTestClient
      .get()
      .uri("/adjustments?person=$person")
      .headers(
        setAuthorisation()
      )
      .exchange()
      .expectStatus().isOk
      .expectBodyList<AdjustmentDto>()
      .returnResult()
      .responseBody!!

    assertThat(result.size).isEqualTo(1)
    assertThat(result[0].id).isEqualTo(id)
    assertThat(result[0].adjustment).isEqualTo(CREATED_ADJUSTMENT.copy(person = person, days = 4))

    assertThat(getNumberOfMessagesCurrentlyOnQueue()).isEqualTo(0)
  }
  @Test
  fun findByPersonAndSource() {
    val person = "HIJKLM"
    val id = createAnAdjustment(person).also {
      cleanQueue()
    }
    val dpsResult = webTestClient
      .get()
      .uri("/adjustments?person=$person&source=${AdjustmentSource.DPS}")
      .headers(
        setAuthorisation()
      )
      .exchange()
      .expectStatus().isOk
      .expectBodyList<AdjustmentDto>()
      .returnResult()
      .responseBody!!

    assertThat(dpsResult.size).isEqualTo(1)
    assertThat(dpsResult[0].id).isEqualTo(id)

    val nomisResult = webTestClient
      .get()
      .uri("/adjustments?person=$person&source=${AdjustmentSource.NOMIS}")
      .headers(
        setAuthorisation()
      )
      .exchange()
      .expectStatus().isOk
      .expectBodyList<AdjustmentDto>()
      .returnResult()
      .responseBody!!

    assertThat(nomisResult.size).isEqualTo(0)

    assertThat(getNumberOfMessagesCurrentlyOnQueue()).isEqualTo(0)
  }

  @Test
  fun update() {
    val id = createAnAdjustment().also {
      cleanQueue()
    }
    webTestClient
      .put()
      .uri("/adjustments/$id")
      .headers(
        setAuthorisation()
      )
      .bodyValue(
        CREATED_ADJUSTMENT.copy(
          fromDate = CREATED_ADJUSTMENT.fromDate!!.minusYears(1),
          toDate = CREATED_ADJUSTMENT.toDate!!.minusYears(1),
        )
      )
      .exchange()
      .expectStatus().isOk

    val adjustment = adjustmentRepository.findById(id).get()

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
    assertThat(legacyData).isEqualTo(LegacyData(bookingId = 1, sentenceSequence = 1, postedDate = LocalDate.now(), comment = null, type = null, active = true))

    assertThat(getNumberOfMessagesCurrentlyOnQueue()).isEqualTo(1)
    val latestMessage = getLatestMessage()!!.messages[0].body
    assertThat(latestMessage).contains(adjustment.id.toString())
    assertThat(latestMessage).contains(EventType.ADJUSTMENT_UPDATED.value)
    assertThat(latestMessage).contains(AdjustmentSource.DPS.name)
  }

  @Test
  fun `update with different adjustment type`() {
    val id = createAnAdjustment().also {
      cleanQueue()
    }

    val result = webTestClient
      .put()
      .uri("/adjustments/$id")
      .headers(
        setAuthorisation()
      )
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
    assertThat(getNumberOfMessagesCurrentlyOnQueue()).isEqualTo(0)
  }

  @Test
  fun delete() {
    val id = createAnAdjustment().also {
      cleanQueue()
    }
    webTestClient
      .delete()
      .uri("/adjustments/$id")
      .headers(
        setAuthorisation()
      )
      .exchange()
      .expectStatus().isOk

    val adjustment = adjustmentRepository.findById(id).get()

    assertThat(adjustment.deleted).isEqualTo(true)
    assertThat(adjustment.adjustmentHistory.size).isEqualTo(2)
    assertThat(adjustment.adjustmentHistory[1].changeType).isEqualTo(ChangeType.DELETE)

    webTestClient
      .get()
      .uri("/adjustments/$id")
      .headers(
        setAuthorisation()
      )
      .exchange()
      .expectStatus().isNotFound

    assertThat(getNumberOfMessagesCurrentlyOnQueue()).isEqualTo(1)
    val latestMessage = getLatestMessage()!!.messages[0].body
    assertThat(latestMessage).contains(adjustment.id.toString())
    assertThat(latestMessage).contains(EventType.ADJUSTMENT_DELETED.value)
    assertThat(latestMessage).contains(AdjustmentSource.DPS.name)
  }
  private fun createAnAdjustment(person: String = "ABC123"): UUID {
    return webTestClient
      .post()
      .uri("/adjustments")
      .headers(
        setAuthorisation()
      )
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(CREATED_ADJUSTMENT.copy(person = person))
      .exchange()
      .expectStatus().isCreated
      .returnResult(CreateResponseDto::class.java)
      .responseBody.blockFirst()!!.adjustmentId
  }

  companion object {
    private val CREATED_ADJUSTMENT = AdjustmentDetailsDto(
      bookingId = 1,
      sentenceSequence = 1,
      person = "ABC123",
      adjustmentType = AdjustmentType.REMAND,
      fromDate = LocalDate.now().minusDays(5),
      toDate = LocalDate.now().minusDays(2),
      days = null
    )
  }
}
