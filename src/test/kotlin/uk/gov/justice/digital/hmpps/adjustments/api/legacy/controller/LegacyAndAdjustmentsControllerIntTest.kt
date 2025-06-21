package uk.gov.justice.digital.hmpps.adjustments.api.legacy.controller

import jakarta.transaction.Transactional
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.annotation.Rollback
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentSource
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.entity.ChangeType
import uk.gov.justice.digital.hmpps.adjustments.api.entity.UnusedDeductionsCalculationResult
import uk.gov.justice.digital.hmpps.adjustments.api.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyAdjustment
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyAdjustmentCreatedResponse
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyAdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.legacy.model.LegacyData
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentEffectiveDaysDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.CreateResponseDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.RemandDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.RestoreAdjustmentsDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.UnusedDeductionsCalculationStatus
import uk.gov.justice.digital.hmpps.adjustments.api.respository.AdjustmentRepository
import uk.gov.justice.digital.hmpps.adjustments.api.respository.UnusedDeductionsCalculationResultRepository
import uk.gov.justice.digital.hmpps.adjustments.api.wiremock.PrisonApiExtension
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

/*
This is testing the sync between NOMIS and our service.
 */
@Transactional
@Rollback
class LegacyAndAdjustmentsControllerIntTest : SqsIntegrationTestBase() {

  @Autowired
  private lateinit var adjustmentRepository: AdjustmentRepository

  @Autowired
  private lateinit var unusedDeductionsCalculationResultRepository: UnusedDeductionsCalculationResultRepository

  @Test
  fun `Create an adjustments from NOMIS and update it from DPS`() {
    val result = webTestClient
      .post()
      .uri("/legacy/adjustments")
      .headers(
        setLegacySynchronisationAuth(),
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
        setAdjustmentsRWAuth(),
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
    assertThat(adjustment.status).isEqualTo(AdjustmentStatus.ACTIVE)

    assertThat(adjustment.fromDate).isEqualTo(LocalDate.now().minusDays(5))
    assertThat(adjustment.toDate).isEqualTo(LocalDate.now().plusDays(2))
    assertThat(adjustment.days).isNull()
    assertThat(adjustment.daysCalculated).isEqualTo(8)
    assertThat(adjustment.effectiveDays).isEqualTo(8)

    val legacyData = objectMapper.convertValue(adjustment.legacyData, LegacyData::class.java)
    assertThat(legacyData).isEqualTo(LegacyData(bookingId = 123, sentenceSequence = 1, postedDate = LocalDate.now(), comment = "Created", type = null, migration = false, chargeIds = listOf(9991)))
  }

  @Test
  fun `Create an adjustments from NOMIS with minimal data and update it from DPS`() {
    val createResult = webTestClient
      .post()
      .uri("/legacy/adjustments")
      .headers(
        setLegacySynchronisationAuth(),
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
        setAdjustmentsRWAuth(),
      )
      .bodyValue(ADJUSTMENT)
      .exchange()
      .expectStatus().isOk

    val getResult = webTestClient
      .get()
      .uri("/legacy/adjustments/$id")
      .headers(
        setLegacySynchronisationAuth(),
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
        setLegacySynchronisationAuth(),
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
        setAdjustmentsRWAuth(),
      )
      .exchange()
      .expectStatus().isOk
  }

  @Test
  fun `Recall scenario`() {
    // Adjustment created in DPS
    val adjustment = ADJUSTMENT.copy(
      person = PrisonApiExtension.RECALL_PRISONER_ID,
      bookingId = PrisonApiExtension.RECALL_BOOKING_ID,
    )
    val id = postCreateAdjustments(listOf(adjustment))[0]

    // Person released. adjustment made inactive in NOMIS
    val legacyAdjustment = getLegacyAdjustment(id)
    updateLegacyAdjustment(id, legacyAdjustment.copy(active = false))

    // Person recalled. In NOMIS their sentence will be deleted which also deletes the adjustment
    deleteLegacyAdjustment(id)

    // User comes to DPS and is shown inactive deleted adjustments
    val adjustments = getAdjustmentsByPerson(PrisonApiExtension.RECALL_PRISONER_ID, AdjustmentStatus.INACTIVE_WHEN_DELETED)
    assertThat(adjustments.size).isEqualTo(1)
    assertThat(adjustments[0].id).isEqualTo(id)

    // User selects to restore adjustment as recall type
    postRestoreAdjustment(RestoreAdjustmentsDto(listOf(id)))
    assertThat(legacyAdjustment.adjustmentType).isEqualTo(LegacyAdjustmentType.RSR)
    assertThat(legacyAdjustment.active).isEqualTo(true)
  }

  @Test
  fun `Update an adjustment in NOMIS without changing number of days`() {
    // Create an adjustment int DPS with different calculated + effective days.
    unusedDeductionsCalculationResultRepository.save(UnusedDeductionsCalculationResult(person = ADJUSTMENT.person, status = UnusedDeductionsCalculationStatus.CALCULATED, calculationAt = LocalDateTime.now()))
    var adjustment = ADJUSTMENT.copy()
    val id = postCreateAdjustments(listOf(adjustment))[0]
    val totalDays = (ChronoUnit.DAYS.between(adjustment.fromDate, adjustment.toDate) + 1).toInt()
    postAdjustmentEffectiveDaysUpdate(id, AdjustmentEffectiveDaysDto(id, 1, adjustment.person))

    // Update the adjustment from NOMIS (set inactive)
    val legacyAdjustment = getLegacyAdjustment(id)
    updateLegacyAdjustment(id, legacyAdjustment.copy(active = false))

    // Adjustment should still have same total days
    adjustment = getAdjustmentById(id)
    val daysBetweenResult = (ChronoUnit.DAYS.between(adjustment.fromDate, adjustment.toDate) + 1).toInt()
    assertThat(daysBetweenResult).isEqualTo(totalDays)
    assertThat(adjustment.status).isEqualTo(AdjustmentStatus.INACTIVE)

    await untilAsserted {
      val unusedDeductionsCalculationResult =
        unusedDeductionsCalculationResultRepository.findFirstByPerson(ADJUSTMENT.person)
      assertThat(unusedDeductionsCalculationResult).isNotNull
      assertThat(unusedDeductionsCalculationResult!!.status).isEqualTo(UnusedDeductionsCalculationStatus.NOMIS_ADJUSTMENT)
    }
  }

  @Test
  fun `Update a DPS adjustment in NOMIS when DPS unused days is shared over multiple adjustments`() {
    // Create two adjustments with a used and unused part.
    unusedDeductionsCalculationResultRepository.save(UnusedDeductionsCalculationResult(person = ADJUSTMENT.person, status = UnusedDeductionsCalculationStatus.CALCULATED, calculationAt = LocalDateTime.now()))
    var adjustmentOne = ADJUSTMENT.copy()
    var adjustmentTwo = ADJUSTMENT.copy()
    val (idOne, idTwo) = postCreateAdjustments(listOf(adjustmentOne, adjustmentTwo))
    postAdjustmentEffectiveDaysUpdate(idOne, AdjustmentEffectiveDaysDto(idOne, 5, adjustmentOne.person))
    postAdjustmentEffectiveDaysUpdate(idTwo, AdjustmentEffectiveDaysDto(idTwo, 0, adjustmentTwo.person))

    // Update the adjustment from NOMIS and change days
    val legacyAdjustment = getLegacyAdjustment(idOne)
    updateLegacyAdjustment(idOne, legacyAdjustment.copy(adjustmentDays = 4))

    await untilAsserted {
      val unusedDeductionsCalculationResult =
        unusedDeductionsCalculationResultRepository.findFirstByPerson(ADJUSTMENT.person)
      assertThat(unusedDeductionsCalculationResult).isNotNull
      assertThat(unusedDeductionsCalculationResult!!.status).isEqualTo(UnusedDeductionsCalculationStatus.NOMIS_ADJUSTMENT)
    }
  }

  @Test
  fun `Create an adjustment in NOMIS when DPS unused days exists`() {
    // Create an DPS adjustment with used and unused part.
    unusedDeductionsCalculationResultRepository.save(UnusedDeductionsCalculationResult(person = ADJUSTMENT.person, status = UnusedDeductionsCalculationStatus.CALCULATED, calculationAt = LocalDateTime.now()))
    var adjustment = ADJUSTMENT.copy()
    val id = postCreateAdjustments(listOf(adjustment))[0]
    val totalDays = (ChronoUnit.DAYS.between(adjustment.fromDate, adjustment.toDate) + 1).toInt()
    val effectiveDays = 1
    postAdjustmentEffectiveDaysUpdate(id, AdjustmentEffectiveDaysDto(id, effectiveDays, adjustment.person))

    // Create new NOMIS adjustment
    postCreateLegacyAdjustment(LEGACY_ADJUSTMENT)

    await untilAsserted {
      val unusedDeductionsCalculationResult =
        unusedDeductionsCalculationResultRepository.findFirstByPerson(ADJUSTMENT.person)
      assertThat(unusedDeductionsCalculationResult).isNotNull
      assertThat(unusedDeductionsCalculationResult!!.status).isEqualTo(UnusedDeductionsCalculationStatus.NOMIS_ADJUSTMENT)
    }
  }

  @Test
  fun `Delete an adjustment in NOMIS when DPS unused days exists`() {
    // Create two adjustments with a used and unused part.
    unusedDeductionsCalculationResultRepository.save(UnusedDeductionsCalculationResult(person = ADJUSTMENT.person, status = UnusedDeductionsCalculationStatus.CALCULATED, calculationAt = LocalDateTime.now()))
    var adjustmentOne = ADJUSTMENT.copy()
    var adjustmentTwo = ADJUSTMENT.copy()
    val (idOne, idTwo) = postCreateAdjustments(listOf(adjustmentOne, adjustmentTwo))
    postAdjustmentEffectiveDaysUpdate(idOne, AdjustmentEffectiveDaysDto(idOne, 5, adjustmentOne.person))
    postAdjustmentEffectiveDaysUpdate(idTwo, AdjustmentEffectiveDaysDto(idTwo, 2, adjustmentTwo.person))

    // Delete one of the adjustments
    deleteLegacyAdjustment(idOne)

    await untilAsserted {
      val unusedDeductionsCalculationResult =
        unusedDeductionsCalculationResultRepository.findFirstByPerson(ADJUSTMENT.person)
      assertThat(unusedDeductionsCalculationResult).isNotNull
      assertThat(unusedDeductionsCalculationResult!!.status).isEqualTo(UnusedDeductionsCalculationStatus.NOMIS_ADJUSTMENT)
    }
  }

  @Test
  fun `Created a remand adjustment linked to inactive and active sentenced charges`() {
    val adjustment = ADJUSTMENT.copy(
      remand = RemandDto(chargeId = listOf(9991, 1111L)),
    )
    val (id) = postCreateAdjustments(listOf(adjustment))

    val legacyAdjustment = getLegacyAdjustment(id)

    assertThat(legacyAdjustment.sentenceSequence).isEqualTo(1)
  }

  private fun getAdjustmentById(adjustmentId: UUID) = webTestClient
    .get()
    .uri("/adjustments/$adjustmentId")
    .headers(setAdjustmentsRWAuth())
    .exchange()
    .expectStatus().isOk
    .returnResult(AdjustmentDto::class.java)
    .responseBody.blockFirst()!!

  private fun postAdjustmentEffectiveDaysUpdate(
    adjustmentId: UUID,
    updateDto: AdjustmentEffectiveDaysDto,
  ) {
    webTestClient
      .post()
      .uri("/adjustments/$adjustmentId/effective-days")
      .headers(
        setAdjustmentsRWAuth(),
      )
      .bodyValue(
        updateDto,
      )
      .exchange()
      .expectStatus().isOk
  }

  private fun postRestoreAdjustment(restoreDto: RestoreAdjustmentsDto) = webTestClient
    .post()
    .uri("/adjustments/restore")
    .headers(setAdjustmentsRWAuth())
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(restoreDto)
    .exchange()
    .expectStatus().isOk

  private fun getAdjustmentsByPerson(person: String, status: AdjustmentStatus? = null, startOfSentenceEnvelope: LocalDate? = null): List<AdjustmentDto> = webTestClient
    .get()
    .uri("/adjustments?person=$person${if (status != null) "&status=$status" else ""}${if (startOfSentenceEnvelope != null) "&sentenceEnvelopeDate=$startOfSentenceEnvelope" else ""}")
    .headers(setAdjustmentsRWAuth())
    .exchange()
    .expectStatus().isOk
    .expectBodyList<AdjustmentDto>()
    .returnResult()
    .responseBody

  private fun deleteLegacyAdjustment(id: UUID) = webTestClient
    .delete()
    .uri("/legacy/adjustments/$id")
    .headers(
      setLegacySynchronisationAuth(),
    )
    .header("Content-Type", LegacyController.LEGACY_CONTENT_TYPE)
    .exchange()
    .expectStatus().isOk

  private fun postCreateAdjustments(adjustmentDtos: List<AdjustmentDto>) = webTestClient
    .post()
    .uri("/adjustments")
    .headers(setAdjustmentsRWAuth())
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(adjustmentDtos)
    .exchange()
    .expectStatus().isCreated
    .returnResult(CreateResponseDto::class.java)
    .responseBody.blockFirst()!!.adjustmentIds

  private fun postCreateLegacyAdjustment(legacyAdjustment: LegacyAdjustment): LegacyAdjustmentCreatedResponse = webTestClient
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

  private fun updateLegacyAdjustment(id: UUID, legacyAdjustment: LegacyAdjustment) = webTestClient
    .put()
    .uri("/legacy/adjustments/$id")
    .headers(
      setLegacySynchronisationAuth(),
    )
    .header("Content-Type", LegacyController.LEGACY_CONTENT_TYPE)
    .bodyValue(legacyAdjustment)
    .exchange()
    .expectStatus().isOk

  companion object {
    private val LEGACY_ADJUSTMENT = LegacyAdjustment(
      bookingId = PrisonApiExtension.BOOKING_ID,
      sentenceSequence = 1,
      offenderNo = PrisonApiExtension.PRISONER_ID,
      adjustmentType = LegacyAdjustmentType.RSR,
      adjustmentDate = LocalDate.now(),
      adjustmentFromDate = LocalDate.now().minusDays(5),
      adjustmentDays = 3,
      comment = "Created",
      active = false,
      bookingReleased = false,
      currentTerm = true,
      agencyId = null,
    )

    private val ADJUSTMENT = AdjustmentDto(
      id = null,
      bookingId = PrisonApiExtension.BOOKING_ID,
      person = PrisonApiExtension.PRISONER_ID,
      adjustmentType = AdjustmentType.REMAND,
      fromDate = LocalDate.now().minusDays(5),
      toDate = LocalDate.now().plusDays(2),
      days = 8,
      additionalDaysAwarded = null,
      unlawfullyAtLarge = null,
      lawfullyAtLarge = null,
      specialRemission = null,
      remand = RemandDto(chargeId = listOf(9991)),
      taggedBail = null,
      timeSpentInCustodyAbroad = null,
      timeSpentAsAnAppealApplicant = null,
      lastUpdatedDate = LocalDateTime.now(),
      createdDate = LocalDateTime.now(),
      effectiveDays = 8,
      lastUpdatedBy = "Person",
      status = AdjustmentStatus.ACTIVE,
      recallId = null,
    )
  }
}
