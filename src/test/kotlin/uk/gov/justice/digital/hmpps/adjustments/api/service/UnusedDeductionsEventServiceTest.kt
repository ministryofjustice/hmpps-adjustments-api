package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.adjustments.api.client.CalculateReleaseDatesApiClient
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentSource
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentEffectiveDaysDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.RemandDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.TaggedBailDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.UnusedDeductionCalculationResponse
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class UnusedDeductionsEventServiceTest {

  private val adjustmentService = mock<AdjustmentsService>()
  private val calculateReleaseDatesApiClient = mock<CalculateReleaseDatesApiClient>()
  private val adjustmentsDomainEventService = mock<AdjustmentsDomainEventService>()

  private val unusedDeductionsService = UnusedDeductionsService(
    adjustmentService,
    calculateReleaseDatesApiClient,
    adjustmentsDomainEventService
  )

  private val eventService = UnusedDeductionsEventService(
    unusedDeductionsService,
  )
  /*

  @Schema(description = "The details of a remand adjustment")
  val remand: RemandDto?,
  @Schema(description = "The details of an additional days awarded adjustments (ADA)")
  val additionalDaysAwarded: AdditionalDaysAwardedDto?,
  @Schema(description = "Additional details of a UAL adjustment")
  val unlawfullyAtLarge: UnlawfullyAtLargeDto?,
  @Schema(description = "The details of a tagged-bail adjustment")
  val taggedBail: TaggedBailDto?,
  @Schema(description = "The NOMIS sentence sequence of the adjustment")
  val sentenceSequence: Int? = null,
   */

  val person = "ABC123"
  private val ninetyDaysRemand = AdjustmentDto(
    UUID.randomUUID(), 1, person, AdjustmentType.REMAND, LocalDate.now().minusDays(100),
    LocalDate.now().minusDays(9),  90,  remand = RemandDto(listOf(1L)), additionalDaysAwarded = null, unlawfullyAtLarge = null, taggedBail = null,
    effectiveDays = 90,
    sentenceSequence = 1,
    source = AdjustmentSource.DPS,
  )
  @Nested
  inner class AdjustmentEventTests {
    @Test
    fun updateUnusedDeductions() {
      val remand = ninetyDaysRemand.copy()
      val taggedBail = ninetyDaysRemand.copy(
        id = UUID.randomUUID(),
        adjustmentType = AdjustmentType.TAGGED_BAIL,
        taggedBail = TaggedBailDto(1),
        remand = null,
      )
      val unusedDeductions = ninetyDaysRemand.copy(
        id = UUID.randomUUID(),
        adjustmentType = AdjustmentType.UNUSED_DEDUCTIONS,
        days = 10,
        effectiveDays = 10,
      )
      val adjustments = listOf(remand, taggedBail, unusedDeductions)

      whenever(adjustmentService.findCurrentAdjustments(person, AdjustmentStatus.ACTIVE)).thenReturn(adjustments)
      whenever(calculateReleaseDatesApiClient.calculateUnusedDeductions(adjustments, person)).thenReturn(
        UnusedDeductionCalculationResponse(100),
      )

      eventService.handleAdjustmentMessage(
        AdjustmentEvent(
          AdjustmentAdditionalInformation(
            id = UUID.randomUUID().toString(),
            offenderNo = person,
            source = "DPS",
            false,
          ),
        ),
      )

      verify(adjustmentService).updateEffectiveDays(taggedBail.id!!, AdjustmentEffectiveDaysDto(taggedBail.id!!, 80, person))
      verify(adjustmentService).updateEffectiveDays(remand.id!!, AdjustmentEffectiveDaysDto(remand.id!!, 0, person))
      verify(adjustmentService).update(unusedDeductions.id!!, unusedDeductions.copy(days = 100))
    }

    @Test
    fun updateUnusedDeductions_noExistingUnusedDeductionAdjustment() {
      val person = "ABC123"
      val remand = ninetyDaysRemand.copy()
      val taggedBail = remand.copy(
        id = UUID.randomUUID(),
        adjustmentType = AdjustmentType.TAGGED_BAIL,
        taggedBail = TaggedBailDto(1),
        remand = null,
      )
      val adjustments = listOf(remand, taggedBail)

      whenever(adjustmentsApiClient.getAdjustmentsByPerson(person)).thenReturn(adjustments)
      whenever(calculateReleaseDatesApiClient.calculateUnusedDeductions(adjustments, person)).thenReturn(
        UnusedDeductionCalculationResponse(100),
      )

      eventService.handleAdjustmentMessage(
        AdjustmentEvent(
          AdjustmentAdditionalInformation(
            id = UUID.randomUUID().toString(),
            offenderNo = person,
            source = "DPS",
            false,
          ),
        ),
      )

      verify(adjustmentsApiClient).updateEffectiveDays(AdjustmentEffectiveDays(taggedBail.id!!, 80, person))
      verify(adjustmentsApiClient).updateEffectiveDays(AdjustmentEffectiveDays(remand.id!!, 0, person))
      verify(adjustmentsApiClient).createAdjustment(
        remand.copy(
          id = null,
          toDate = null,
          fromDate = null,
          days = 100,
          adjustmentType = AdjustmentType.UNUSED_DEDUCTIONS,
        ),
      )
    }

    @Test
    fun updateUnusedDeductions_ZeroCalculatedDays() {
      val person = "ABC123"
      val remand = ninetyDaysRemand.copy(
        effectiveDays = 80
      )
      val taggedBail = remand.copy(
        id = UUID.randomUUID(),
        adjustmentType = AdjustmentType.TAGGED_BAIL,
        taggedBail = TaggedBailDto(1),
        remand = null,
      )
      val unusedDeductions = remand.copy(
        id = UUID.randomUUID(),
        adjustmentType = AdjustmentType.UNUSED_DEDUCTIONS,
        days = 10,
        effectiveDays = 10,
      )
      val adjustments = listOf(remand, taggedBail, unusedDeductions)

      whenever(adjustmentsApiClient.getAdjustmentsByPerson(person)).thenReturn(adjustments)
      whenever(calculateReleaseDatesApiClient.calculateUnusedDeductions(adjustments, person)).thenReturn(
        UnusedDeductionCalculationResponse(0),
      )

      eventService.handleAdjustmentMessage(
        AdjustmentEvent(
          AdjustmentAdditionalInformation(
            id = UUID.randomUUID().toString(),
            offenderNo = person,
            source = "DPS",
            false,
          ),
        ),
      )

      verify(adjustmentsApiClient).updateEffectiveDays(AdjustmentEffectiveDays(taggedBail.id!!, 90, person))
      verify(adjustmentsApiClient).updateEffectiveDays(AdjustmentEffectiveDays(remand.id!!, 90, person))
      verify(adjustmentsApiClient).deleteAdjustment(unusedDeductions.id!!)
    }

    @Test
    fun updateUnusedDeductions_NoDeductionsButUnusedDeductions() {
      val person = "ABC123"
      val unusedDeductions = ninetyDaysRemand.copy(
        adjustmentType = AdjustmentType.UNUSED_DEDUCTIONS
      )
      val adjustments = listOf(unusedDeductions)

      whenever(adjustmentsApiClient.getAdjustmentsByPerson(person)).thenReturn(adjustments)
      whenever(calculateReleaseDatesApiClient.calculateUnusedDeductions(adjustments, person)).thenReturn(
        UnusedDeductionCalculationResponse(0),
      )

      eventService.handleAdjustmentMessage(
        AdjustmentEvent(
          AdjustmentAdditionalInformation(
            id = UUID.randomUUID().toString(),
            offenderNo = person,
            source = "DPS",
            false,
          ),
        ),
      )

      verify(adjustmentsApiClient).deleteAdjustment(unusedDeductions.id!!)
    }

    @Test
    fun unusedDeductions_wonthandleeventsthatarentlast() {
      eventService.handleAdjustmentMessage(
        AdjustmentEvent(
          AdjustmentAdditionalInformation(
            id = UUID.randomUUID().toString(),
            offenderNo = "ASD",
            source = "DPS",
            false,
            false,
          ),
        ),
      )

      verifyNoInteractions(adjustmentsApiClient)
    }
  }

  @Nested
  inner class GeneralTests {
    @Test
    fun `Do not update unused deductions if adjustments arent DPS`() {
      val person = "ABC123"
      val remand = ninetyDaysRemand.copy()
      val taggedBail = remand.copy(
        id = UUID.randomUUID(),
        adjustmentType = AdjustmentType.TAGGED_BAIL,
        taggedBail = TaggedBailDto(1),
        remand = null,
      )
      val unusedDeductions = remand.copy(
        id = UUID.randomUUID(),
        adjustmentType = AdjustmentType.UNUSED_DEDUCTIONS,
        days = 10,
        effectiveDays = 10,
      )
      val adjustments = listOf(remand, taggedBail, unusedDeductions)

      whenever(adjustmentsApiClient.getAdjustmentsByPerson(person)).thenReturn(adjustments)

      eventService.handleAdjustmentMessage(
        AdjustmentEvent(
          AdjustmentAdditionalInformation(
            id = UUID.randomUUID().toString(),
            offenderNo = person,
            source = "DPS",
            false,
          ),
        ),
      )

      verify(adjustmentsApiClient).getAdjustmentsByPerson(person)
      verifyNoMoreInteractions(adjustmentsApiClient)
    }
  }

  @Nested
  inner class PrisonerSearchTests {
    @Test
    fun `Handle prisoner event`() {
      val person = "ABC123"
      val remand = Adjustment(
        UUID.randomUUID(), 1, 1, person, AdjustmentType.REMAND, LocalDate.now().minusDays(100),
        LocalDate.now().minusDays(9), LocalDateTime.now(), 90, 90, taggedBail = null, remand = RemandDto(listOf(1L)),
        source = AdjustmentSource.DPS,
      )
      val taggedBail = remand.copy(
        id = UUID.randomUUID(),
        adjustmentType = AdjustmentType.TAGGED_BAIL,
        taggedBail = TaggedBailDto(1),
        remand = null,
      )
      val unusedDeductions = remand.copy(
        id = UUID.randomUUID(),
        adjustmentType = AdjustmentType.UNUSED_DEDUCTIONS,
        days = 10,
        effectiveDays = 10,
      )
      val adjustments = listOf(remand, taggedBail, unusedDeductions)

      whenever(adjustmentsApiClient.getAdjustmentsByPerson(person)).thenReturn(adjustments)
      whenever(calculateReleaseDatesApiClient.calculateUnusedDeductions(adjustments, person)).thenReturn(
        UnusedDeductionCalculationResponse(100),
      )

      eventService.handlePrisonerSearchEvent(
        PrisonerSearchEvent(
          PrisonerSearchAdditionalInformation(
            nomsNumber = person,
            categoriesChanged = listOf("SENTENCE"),
          ),
        ),
      )

      verify(adjustmentsApiClient).updateEffectiveDays(AdjustmentEffectiveDays(taggedBail.id!!, 80, person))
      verify(adjustmentsApiClient).updateEffectiveDays(AdjustmentEffectiveDays(remand.id!!, 0, person))
      verify(adjustmentsApiClient).updateAdjustment(unusedDeductions.copy(days = 100))
    }

    @Test
    fun `Dont handle non sentence events`() {
      val person = "ABC123"

      eventService.handlePrisonerSearchEvent(
        PrisonerSearchEvent(
          PrisonerSearchAdditionalInformation(
            nomsNumber = person,
            categoriesChanged = listOf("SOMETHINGELSE"),
          ),
        ),
      )

      verifyNoInteractions(adjustmentsApiClient)
    }
  }
}
