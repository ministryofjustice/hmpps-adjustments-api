package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.adjustments.api.client.CalculateReleaseDatesApiClient
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentSource
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.entity.UnusedDeductionsCalculationResult
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentEffectiveDaysDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.RemandDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.TaggedBailDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.UnusedDeductionCalculationResponse
import uk.gov.justice.digital.hmpps.adjustments.api.model.UnusedDeductionsCalculationStatus
import uk.gov.justice.digital.hmpps.adjustments.api.model.calculatereleasedatesapi.CrdsValidationMessage
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.SentenceAndOffences
import uk.gov.justice.digital.hmpps.adjustments.api.respository.UnusedDeductionsCalculationResultRepository
import java.time.LocalDate
import java.util.UUID

class UnusedDeductionsEventServiceTest {

  private val adjustmentService = mock<AdjustmentsService>()
  private val calculateReleaseDatesApiClient = mock<CalculateReleaseDatesApiClient>()
  private val prisonService = mock<PrisonService>()
  private val unusedDeductionsCalculationResultRepository = mock<UnusedDeductionsCalculationResultRepository>()

  private val unusedDeductionsService = UnusedDeductionsService(
    adjustmentService,
    prisonService,
    calculateReleaseDatesApiClient,
    unusedDeductionsCalculationResultRepository,
  )

  private val eventService = UnusedDeductionsEventService(
    unusedDeductionsService,
  )

  val person = "ABC123"
  private val ninetyDaysRemand = AdjustmentDto(
    UUID.randomUUID(),
    1,
    person,
    AdjustmentType.REMAND,
    LocalDate.now().minusDays(100),
    LocalDate.now().minusDays(9),
    90,
    remand = RemandDto(listOf(1L)),
    additionalDaysAwarded = null,
    unlawfullyAtLarge = null,
    lawfullyAtLarge = null,
    specialRemission = null,
    taggedBail = null,
    timeSpentInCustodyAbroad = null,
    effectiveDays = 90,
    sentenceSequence = 1,
    source = AdjustmentSource.DPS,
  )
  private val sentenceDate = LocalDate.of(2023, 1, 1)
  private val sentences = listOf(SentenceAndOffences(sentenceDate = sentenceDate, bookingId = 1, sentenceSequence = 1, sentenceCalculationType = "ADIMP", sentenceStatus = "A"))
  private val defaultSentenceDetail = SentenceAndStartDateDetails(
    sentences,
    earliestRecallDate = null,
    latestSentenceDate = sentenceDate,
    earliestNonRecallSentenceDate = sentenceDate,
    hasRecall = false,
    earliestSentenceDate = sentenceDate,
  )
  val expectedValidationMessage = CrdsValidationMessage("CUSTODIAL_PERIOD_EXTINGUISHED_TAGGED_BAIL", "Message", "VALIDATION", emptyList())

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
        fromDate = null,
        toDate = null,
        remand = null,
      )
      val adjustments = listOf(remand, taggedBail, unusedDeductions)

      whenever(prisonService.getSentencesAndStartDateDetails(person)).thenReturn(defaultSentenceDetail)
      whenever(adjustmentService.findCurrentAdjustments(person, AdjustmentStatus.ACTIVE, true, sentenceDate)).thenReturn(adjustments)
      whenever(calculateReleaseDatesApiClient.calculateUnusedDeductions(adjustments, person)).thenReturn(
        UnusedDeductionCalculationResponse(
          100,
          listOf(expectedValidationMessage),
        ),
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
      verify(unusedDeductionsCalculationResultRepository).save(argWhere { it.status == UnusedDeductionsCalculationStatus.CALCULATED })
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

      whenever(prisonService.getSentencesAndStartDateDetails(person)).thenReturn(defaultSentenceDetail)
      whenever(adjustmentService.findCurrentAdjustments(person, AdjustmentStatus.ACTIVE, true, sentenceDate)).thenReturn(adjustments)
      whenever(calculateReleaseDatesApiClient.calculateUnusedDeductions(adjustments, person)).thenReturn(
        UnusedDeductionCalculationResponse(
          100,
          listOf(expectedValidationMessage),
        ),
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
      verify(adjustmentService).create(
        listOf(
          remand.copy(
            id = null,
            toDate = null,
            fromDate = null,
            days = 100,
            adjustmentType = AdjustmentType.UNUSED_DEDUCTIONS,
            remand = null,
          ),
        ),
      )
      verify(unusedDeductionsCalculationResultRepository).save(argWhere { it.status == UnusedDeductionsCalculationStatus.CALCULATED })
    }

    @Test
    fun updateUnusedDeductions_ZeroCalculatedDays() {
      val person = "ABC123"
      val remand = ninetyDaysRemand.copy(
        effectiveDays = 80,
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

      whenever(prisonService.getSentencesAndStartDateDetails(person)).thenReturn(defaultSentenceDetail)
      whenever(adjustmentService.findCurrentAdjustments(person, AdjustmentStatus.ACTIVE, true, sentenceDate)).thenReturn(adjustments)
      whenever(calculateReleaseDatesApiClient.calculateUnusedDeductions(adjustments, person)).thenReturn(
        UnusedDeductionCalculationResponse(0, emptyList()),

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

      verify(adjustmentService).updateEffectiveDays(taggedBail.id!!, AdjustmentEffectiveDaysDto(taggedBail.id!!, 90, person))
      verify(adjustmentService).updateEffectiveDays(remand.id!!, AdjustmentEffectiveDaysDto(remand.id!!, 90, person))
      verify(adjustmentService).delete(unusedDeductions.id!!)
      verify(unusedDeductionsCalculationResultRepository).save(argWhere { it.status == UnusedDeductionsCalculationStatus.CALCULATED })
    }

    @Test
    fun updateUnusedDeductions_NoDeductionsButUnusedDeductions() {
      val person = "ABC123"
      val unusedDeductions = ninetyDaysRemand.copy(
        adjustmentType = AdjustmentType.UNUSED_DEDUCTIONS,
      )
      val adjustments = listOf(unusedDeductions)

      whenever(prisonService.getSentencesAndStartDateDetails(person)).thenReturn(defaultSentenceDetail)
      whenever(adjustmentService.findCurrentAdjustments(person, AdjustmentStatus.ACTIVE, true, sentenceDate)).thenReturn(adjustments)
      whenever(calculateReleaseDatesApiClient.calculateUnusedDeductions(adjustments, person)).thenReturn(
        UnusedDeductionCalculationResponse(0, emptyList()),
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

      verify(adjustmentService).delete(unusedDeductions.id!!)
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

      verifyNoInteractions(adjustmentService)
    }
  }

  @Nested
  inner class GeneralTests {
    @Test
    fun `Do not update unused deductions if adjustments arent DPS`() {
      val person = "ABC123"
      val remand = ninetyDaysRemand.copy(source = AdjustmentSource.NOMIS)
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

      whenever(prisonService.getSentencesAndStartDateDetails(person)).thenReturn(defaultSentenceDetail)
      whenever(adjustmentService.findCurrentAdjustments(person, AdjustmentStatus.ACTIVE, true, sentenceDate)).thenReturn(adjustments)

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

      verify(adjustmentService).findCurrentAdjustments(person, AdjustmentStatus.ACTIVE, true, sentenceDate)
      verifyNoMoreInteractions(adjustmentService)
    }
  }

  @Nested
  inner class PrisonerSearchTests {
    @Test
    fun `Handle prisoner event`() {
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
        fromDate = null,
        toDate = null,
        remand = null,
      )
      val adjustments = listOf(remand, taggedBail, unusedDeductions)

      whenever(prisonService.getSentencesAndStartDateDetails(person)).thenReturn(defaultSentenceDetail)
      whenever(adjustmentService.findCurrentAdjustments(person, AdjustmentStatus.ACTIVE, true, sentenceDate)).thenReturn(adjustments)
      whenever(calculateReleaseDatesApiClient.calculateUnusedDeductions(adjustments, person)).thenReturn(
        UnusedDeductionCalculationResponse(100, listOf(expectedValidationMessage)),
      )

      eventService.handlePrisonerSearchEvent(
        PrisonerSearchEvent(
          PrisonerSearchAdditionalInformation(
            nomsNumber = person,
            categoriesChanged = listOf("SENTENCE"),
          ),
        ),
      )

      verify(adjustmentService).updateEffectiveDays(taggedBail.id!!, AdjustmentEffectiveDaysDto(taggedBail.id!!, 80, person))
      verify(adjustmentService).updateEffectiveDays(remand.id!!, AdjustmentEffectiveDaysDto(remand.id!!, 0, person))
      verify(adjustmentService).update(unusedDeductions.id!!, unusedDeductions.copy(days = 100))
      verify(unusedDeductionsCalculationResultRepository).save(argWhere { it.status == UnusedDeductionsCalculationStatus.CALCULATED })
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

      verifyNoInteractions(adjustmentService)
    }
  }

  @Nested
  inner class UnusedDeductionsResultStatusTests {

    @Test
    fun `Only NOMIS adjustments, previously calculated`() {
      val person = "ABC123"
      val remand = ninetyDaysRemand.copy(source = AdjustmentSource.NOMIS)
      val adjustments = listOf(remand)

      whenever(prisonService.getSentencesAndStartDateDetails(person)).thenReturn(defaultSentenceDetail)
      whenever(adjustmentService.findCurrentAdjustments(person, AdjustmentStatus.ACTIVE, true, sentenceDate)).thenReturn(adjustments)
      whenever(unusedDeductionsCalculationResultRepository.findFirstByPerson(person)).thenReturn(
        UnusedDeductionsCalculationResult(status = UnusedDeductionsCalculationStatus.CALCULATED),
      )

      eventService.handlePrisonerSearchEvent(
        PrisonerSearchEvent(
          PrisonerSearchAdditionalInformation(
            nomsNumber = person,
            categoriesChanged = listOf("SENTENCE"),
          ),
        ),
      )

      verify(adjustmentService).findCurrentAdjustments(person, AdjustmentStatus.ACTIVE, true, sentenceDate)
      verifyNoMoreInteractions(adjustmentService)
      verify(unusedDeductionsCalculationResultRepository).save(argWhere { it.status == UnusedDeductionsCalculationStatus.NOMIS_ADJUSTMENT })
    }

    @Test
    fun `NOMIS adjustments`() {
      val person = "ABC123"
      val remand = ninetyDaysRemand.copy()
      val remandNomis = remand.copy(
        id = UUID.randomUUID(),
        source = AdjustmentSource.NOMIS,
      )
      val adjustments = listOf(remand, remandNomis)

      whenever(prisonService.getSentencesAndStartDateDetails(person)).thenReturn(defaultSentenceDetail)
      whenever(adjustmentService.findCurrentAdjustments(person, AdjustmentStatus.ACTIVE, true, sentenceDate)).thenReturn(adjustments)

      eventService.handlePrisonerSearchEvent(
        PrisonerSearchEvent(
          PrisonerSearchAdditionalInformation(
            nomsNumber = person,
            categoriesChanged = listOf("SENTENCE"),
          ),
        ),
      )

      verify(adjustmentService).findCurrentAdjustments(person, AdjustmentStatus.ACTIVE, true, sentenceDate)
      verifyNoMoreInteractions(adjustmentService)
      verify(unusedDeductionsCalculationResultRepository).save(argWhere { it.status == UnusedDeductionsCalculationStatus.NOMIS_ADJUSTMENT })
    }

    @Test
    fun `Unsupported calculation`() {
      val person = "ABC123"
      val remand = ninetyDaysRemand.copy()
      val adjustments = listOf(remand)

      whenever(prisonService.getSentencesAndStartDateDetails(person)).thenReturn(defaultSentenceDetail)
      whenever(adjustmentService.findCurrentAdjustments(person, AdjustmentStatus.ACTIVE, true, sentenceDate)).thenReturn(adjustments)
      whenever(calculateReleaseDatesApiClient.calculateUnusedDeductions(adjustments, person)).thenReturn(
        UnusedDeductionCalculationResponse(null, listOf(CrdsValidationMessage("CODE", "Message", "UNSUPPORTED", emptyList()))),
      )

      eventService.handlePrisonerSearchEvent(
        PrisonerSearchEvent(
          PrisonerSearchAdditionalInformation(
            nomsNumber = person,
            categoriesChanged = listOf("SENTENCE"),
          ),
        ),
      )

      verify(adjustmentService).findCurrentAdjustments(person, AdjustmentStatus.ACTIVE, true, sentenceDate)
      verifyNoMoreInteractions(adjustmentService)
      verify(unusedDeductionsCalculationResultRepository).save(argWhere { it.status == UnusedDeductionsCalculationStatus.UNSUPPORTED })
    }

    @Test
    fun `Validation calculation`() {
      val person = "ABC123"
      val remand = ninetyDaysRemand.copy()
      val adjustments = listOf(remand)

      whenever(prisonService.getSentencesAndStartDateDetails(person)).thenReturn(defaultSentenceDetail)
      whenever(adjustmentService.findCurrentAdjustments(person, AdjustmentStatus.ACTIVE, true, sentenceDate)).thenReturn(adjustments)
      whenever(calculateReleaseDatesApiClient.calculateUnusedDeductions(adjustments, person)).thenReturn(
        UnusedDeductionCalculationResponse(null, listOf(CrdsValidationMessage("CODE", "Message", "VALIDATION", emptyList()))),
      )

      eventService.handlePrisonerSearchEvent(
        PrisonerSearchEvent(
          PrisonerSearchAdditionalInformation(
            nomsNumber = person,
            categoriesChanged = listOf("SENTENCE"),
          ),
        ),
      )

      verify(adjustmentService).findCurrentAdjustments(person, AdjustmentStatus.ACTIVE, true, sentenceDate)
      verifyNoMoreInteractions(adjustmentService)
      verify(unusedDeductionsCalculationResultRepository).save(argWhere { it.status == UnusedDeductionsCalculationStatus.VALIDATION })
    }

    @Test
    fun `Recall result`() {
      val person = "ABC123"
      val remand = ninetyDaysRemand.copy()
      val adjustments = listOf(remand)

      whenever(prisonService.getSentencesAndStartDateDetails(person)).thenReturn(defaultSentenceDetail.copy(hasRecall = true))
      whenever(adjustmentService.findCurrentAdjustments(person, AdjustmentStatus.ACTIVE, true, sentenceDate)).thenReturn(adjustments)
      whenever(calculateReleaseDatesApiClient.calculateUnusedDeductions(adjustments, person)).thenReturn(
        UnusedDeductionCalculationResponse(null, listOf(CrdsValidationMessage("CODE", "Message", "VALIDATION", emptyList()))),
      )

      eventService.handlePrisonerSearchEvent(
        PrisonerSearchEvent(
          PrisonerSearchAdditionalInformation(
            nomsNumber = person,
            categoriesChanged = listOf("SENTENCE"),
          ),
        ),
      )

      verify(adjustmentService).findCurrentAdjustments(person, AdjustmentStatus.ACTIVE, true, sentenceDate)
      verifyNoMoreInteractions(adjustmentService)
      verify(unusedDeductionsCalculationResultRepository).save(argWhere { it.status == UnusedDeductionsCalculationStatus.RECALL })
    }
  }
}
