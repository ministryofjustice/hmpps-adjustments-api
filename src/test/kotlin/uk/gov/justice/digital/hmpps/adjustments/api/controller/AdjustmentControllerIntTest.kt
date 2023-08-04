package uk.gov.justice.digital.hmpps.adjustments.api.controller

import jakarta.transaction.Transactional
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.adjustments.api.config.ErrorResponse
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentSource
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType.UNLAWFULLY_AT_LARGE
import uk.gov.justice.digital.hmpps.adjustments.api.entity.ChangeType
import uk.gov.justice.digital.hmpps.adjustments.api.enums.UnlawfullyAtLargeType.ESCAPE
import uk.gov.justice.digital.hmpps.adjustments.api.enums.UnlawfullyAtLargeType.RECALL
import uk.gov.justice.digital.hmpps.adjustments.api.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyData
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdditionalDaysAwardedDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.CreateResponseDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.UnlawfullyAtLargeDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationCode
import uk.gov.justice.digital.hmpps.adjustments.api.model.ValidationMessage
import uk.gov.justice.digital.hmpps.adjustments.api.respository.AdjustmentRepository
import uk.gov.justice.digital.hmpps.adjustments.api.service.EventType
import uk.gov.justice.digital.hmpps.adjustments.api.wiremock.PrisonApiExtension
import java.time.LocalDate
import java.util.UUID

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

    awaitAtMost30Secs untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 1 }
    val latestMessage: String = getLatestMessage()!!.messages()[0].body()
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
        setAuthorisation(),
      )
      .exchange()
      .expectStatus().isOk
      .returnResult(AdjustmentDto::class.java)
      .responseBody.blockFirst()!!

    assertThat(result).isEqualTo(CREATED_ADJUSTMENT.copy(id = id, days = 4, lastUpdatedBy = "Test User", status = "Active"))
    awaitAtMost30Secs untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }
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
        setAuthorisation(),
      )
      .exchange()
      .expectStatus().isOk
      .expectBodyList<AdjustmentDto>()
      .returnResult()
      .responseBody!!

    assertThat(result.size).isEqualTo(1)
    assertThat(result[0]).isEqualTo(CREATED_ADJUSTMENT.copy(id = id, person = person, days = 4, lastUpdatedBy = "Test User", status = "Active"))

    awaitAtMost30Secs untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }
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
        setAuthorisation(),
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
        setAuthorisation(),
      )
      .exchange()
      .expectStatus().isOk
      .expectBodyList<AdjustmentDto>()
      .returnResult()
      .responseBody!!

    assertThat(nomisResult.size).isEqualTo(0)

    awaitAtMost30Secs untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }
  }

  @Test
  fun update() {
    val id = createAnAdjustment().also {
      cleanQueue()
    }
    putAdjustmentUpdate(
      id,
      CREATED_ADJUSTMENT.copy(
        fromDate = CREATED_ADJUSTMENT.fromDate!!.minusYears(1),
        toDate = CREATED_ADJUSTMENT.toDate!!.minusYears(1),
      ),
    )

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

    awaitAtMost30Secs untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 1 }
    val latestMessage: String = getLatestMessage()!!.messages()[0].body()
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
        setAuthorisation(),
      )
      .bodyValue(
        CREATED_ADJUSTMENT.copy(
          adjustmentType = UNLAWFULLY_AT_LARGE,
        ),
      )
      .exchange()
      .expectStatus().isBadRequest
      .returnResult(ErrorResponse::class.java)
      .responseBody.blockFirst()!!
    assertThat(result.userMessage).isEqualTo("The provided adjustment type UNLAWFULLY_AT_LARGE doesn't match the persisted type REMAND")
    awaitAtMost30Secs untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }
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
        setAuthorisation(),
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
        setAuthorisation(),
      )
      .exchange()
      .expectStatus().isNotFound

    awaitAtMost30Secs untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 1 }
    val latestMessage: String = getLatestMessage()!!.messages()[0].body()
    assertThat(latestMessage).contains(adjustment.id.toString())
    assertThat(latestMessage).contains(EventType.ADJUSTMENT_DELETED.value)
    assertThat(latestMessage).contains(AdjustmentSource.DPS.name)
  }

  @Test
  fun adaAdjustments() {
    val adjustmentId = webTestClient
      .post()
      .uri("/adjustments")
      .headers(
        setAuthorisation(),
      )
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(
        CREATED_ADJUSTMENT.copy(
          person = "ADA123",
          adjustmentType = AdjustmentType.ADDITIONAL_DAYS_AWARDED,
          additionalDaysAwarded = AdditionalDaysAwardedDto(
            adjudicationId = "987654321",
            consecutive = true,
          ),
        ),
      )
      .exchange()
      .expectStatus().isCreated
      .returnResult(CreateResponseDto::class.java)
      .responseBody.blockFirst()!!.adjustmentId

    val adjustment = adjustmentRepository.findById(adjustmentId).get()

    assertThat(adjustment.additionalDaysAwarded).isNotNull
    assertThat(adjustment.additionalDaysAwarded!!.adjudicationId).isEqualTo("987654321")
    assertThat(adjustment.additionalDaysAwarded!!.consecutive).isEqualTo(true)

    val updateDto = CREATED_ADJUSTMENT.copy(
      id = adjustmentId,
      person = "ADA123",
      adjustmentType = AdjustmentType.ADDITIONAL_DAYS_AWARDED,
      additionalDaysAwarded = AdditionalDaysAwardedDto(
        adjudicationId = "123456789",
        consecutive = false,
      ),
    )
    putAdjustmentUpdate(adjustmentId, updateDto)

    val result = webTestClient
      .get()
      .uri("/adjustments/$adjustmentId")
      .headers(
        setAuthorisation(),
      )
      .exchange()
      .expectStatus().isOk
      .returnResult(AdjustmentDto::class.java)
      .responseBody.blockFirst()!!

    assertThat(result).isEqualTo(updateDto.copy(id = adjustmentId, days = 4, status = "Active", lastUpdatedBy = "Test User"))
  }

  @Test
  fun `Create a UAL Adjustment, then update it`() {
    val adjustmentId = postCreateAdjustment(
      CREATED_ADJUSTMENT.copy(
        person = "UAL123",
        adjustmentType = UNLAWFULLY_AT_LARGE,
        unlawfullyAtLarge = UnlawfullyAtLargeDto(type = RECALL),
      ),
    )

    val adjustment = adjustmentRepository.findById(adjustmentId).get()

    assertThat(adjustment.unlawfullyAtLarge).isNotNull
    assertThat(adjustment.unlawfullyAtLarge!!.type).isEqualTo(RECALL)
    assertThat(adjustment.unlawfullyAtLarge!!.adjustment.id).isEqualTo(adjustmentId)

    val createdAdjustment = getAdjustmentById(adjustmentId)

    assertThat(createdAdjustment).isEqualTo(
      CREATED_ADJUSTMENT.copy(
        id = adjustmentId,
        person = "UAL123",
        adjustmentType = UNLAWFULLY_AT_LARGE,
        unlawfullyAtLarge = UnlawfullyAtLargeDto(type = RECALL),
        days = 4,
        lastUpdatedBy = "Test User",
        status = "Active",
      ),
    )

    val updateDto = createdAdjustment.copy(unlawfullyAtLarge = UnlawfullyAtLargeDto(type = ESCAPE))
    putAdjustmentUpdate(adjustmentId, updateDto)
    val updatedAdjustment = getAdjustmentById(adjustmentId)
    assertThat(updatedAdjustment).isEqualTo(createdAdjustment.copy(unlawfullyAtLarge = UnlawfullyAtLargeDto(type = ESCAPE)))
  }

  @Test
  @Sql(
    "classpath:test_data/insert-nomis-ual.sql",
  )
  fun `Update a UAL Adjustment that has no UAL type (migrated from NOMIS)`() {
    val adjustment = getAdjustmentById(UUID.fromString("dfba24ef-a2d4-4b26-af63-4d9494dd5252"))

    putAdjustmentUpdate(adjustment.id!!, adjustment.copy(unlawfullyAtLarge = UnlawfullyAtLargeDto(type = RECALL)))

    val updatedAdjustment = getAdjustmentById(UUID.fromString("dfba24ef-a2d4-4b26-af63-4d9494dd5252"))
    assertThat(updatedAdjustment).isEqualTo(adjustment.copy(lastUpdatedBy = "Test User", unlawfullyAtLarge = UnlawfullyAtLargeDto(type = RECALL)))
  }

  private fun postCreateAdjustment(adjustmentDto: AdjustmentDto) = webTestClient
    .post()
    .uri("/adjustments")
    .headers(setAuthorisation())
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(adjustmentDto)
    .exchange()
    .expectStatus().isCreated
    .returnResult(CreateResponseDto::class.java)
    .responseBody.blockFirst()!!.adjustmentId

  private fun getAdjustmentById(adjustmentId: UUID) = webTestClient
    .get()
    .uri("/adjustments/$adjustmentId")
    .headers(setAuthorisation())
    .exchange()
    .expectStatus().isOk
    .returnResult(AdjustmentDto::class.java)
    .responseBody.blockFirst()!!

  private fun createAnAdjustment(person: String = "ABC123"): UUID {
    return webTestClient
      .post()
      .uri("/adjustments")
      .headers(
        setAuthorisation(),
      )
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(CREATED_ADJUSTMENT.copy(person = person))
      .exchange()
      .expectStatus().isCreated
      .returnResult(CreateResponseDto::class.java)
      .responseBody.blockFirst()!!.adjustmentId
  }
//
//  @Test
//  fun validate() {
//    val validationMessages = webTestClient
//      .post()
//      .uri("/adjustments/validate")
//      .headers(
//        setAuthorisation(),
//      )
//      .bodyValue(
//        CREATED_ADJUSTMENT.copy(
//          fromDate = LocalDate.now().plusYears(1),
//          toDate = null,
//          days = 25,
//          bookingId = PrisonApiExtension.BOOKING_ID,
//          adjustmentType = AdjustmentType.RESTORATION_OF_ADDITIONAL_DAYS_AWARDED,
//        ),
//      )
//      .exchange()
//      .expectStatus().isOk
//      .expectHeader().contentType(MediaType.APPLICATION_JSON)
//      .expectBody(object : ParameterizedTypeReference<List<ValidationMessage>>() {})
//      .returnResult().responseBody!!
//
//    assertThat(validationMessages.size).isEqualTo(2)
//    assertThat(validationMessages[0]).isEqualTo(ValidationMessage(ValidationCode.MORE_RADAS_THAN_ADAS))
//    assertThat(validationMessages[1]).isEqualTo(ValidationMessage(ValidationCode.RADA_DATE_CANNOT_BE_FUTURE))
//  }

  private fun putAdjustmentUpdate(
    adjustmentId: UUID,
    updateDto: AdjustmentDto,
  ) {
    webTestClient
      .put()
      .uri("/adjustments/$adjustmentId")
      .headers(
        setAuthorisation(),
      )
      .bodyValue(
        updateDto,
      )
      .exchange()
      .expectStatus().isOk
  }

  companion object {
    private val CREATED_ADJUSTMENT = AdjustmentDto(
      id = null,
      bookingId = 1,
      sentenceSequence = 1,
      person = "ABC123",
      adjustmentType = AdjustmentType.REMAND,
      fromDate = LocalDate.now().minusDays(5),
      toDate = LocalDate.now().minusDays(2),
      days = null,
      additionalDaysAwarded = null,
      unlawfullyAtLarge = null,
    )
  }
}
