package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.adjustments.api.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.adjustments.api.client.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.adjustments.api.entity.Adjustment
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentHistory
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentSource
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus.ACTIVE
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentStatus.INACTIVE
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.entity.ChangeType
import uk.gov.justice.digital.hmpps.adjustments.api.entity.ReviewPreviousUalResult
import uk.gov.justice.digital.hmpps.adjustments.api.entity.ReviewPreviousUalStatus
import uk.gov.justice.digital.hmpps.adjustments.api.entity.UnlawfullyAtLarge
import uk.gov.justice.digital.hmpps.adjustments.api.enums.UnlawfullyAtLargeType
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentEventMetadata
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentEventType
import uk.gov.justice.digital.hmpps.adjustments.api.model.CreateResponseDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.UnlawfullyAtLargeDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.previousual.PreviousUnlawfullyAtLargeAdjustmentForReview
import uk.gov.justice.digital.hmpps.adjustments.api.model.previousual.PreviousUnlawfullyAtLargeReviewRequest
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.Prison
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.SentenceAndOffences
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonersearchapi.Prisoner
import uk.gov.justice.digital.hmpps.adjustments.api.respository.AdjustmentRepository
import uk.gov.justice.digital.hmpps.adjustments.api.respository.ReviewPreviousUalResultRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class ReviewPreviousUalServiceTest {
  private val prisonService: PrisonService = mock()
  private val prisonApiClient: PrisonApiClient = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()
  private val adjustmentRepository: AdjustmentRepository = mock()
  private val reviewPreviousUalResultRepository: ReviewPreviousUalResultRepository = mock()
  private val adjustmentsTransactionalService: AdjustmentsTransactionalService = mock()

  private val service =
    ReviewPreviousUalService(
      prisonService,
      prisonApiClient,
      prisonerSearchApiClient,
      adjustmentRepository,
      reviewPreviousUalResultRepository,
      adjustmentsTransactionalService,
    )

  @Test
  fun `should get previous UAL and enrich with prison names if the adjustment while looking up the sentence date if not passed in`() {
    val adjustmentWithAPrison = Adjustment(
      fromDate = LocalDate.of(2021, 1, 1),
      toDate = LocalDate.of(2021, 1, 11),
      effectiveDays = 10,
      adjustmentType = AdjustmentType.UNLAWFULLY_AT_LARGE,
      unlawfullyAtLarge = UnlawfullyAtLarge(type = UnlawfullyAtLargeType.RELEASE_IN_ERROR),
      adjustmentHistory = listOf(AdjustmentHistory(changeType = ChangeType.CREATE, prisonId = PRISON_ID)),
    )

    val adjustmentWithoutAPrison = Adjustment(
      fromDate = LocalDate.of(2021, 1, 1),
      toDate = LocalDate.of(2022, 1, 1),
      effectiveDays = 364,
      adjustmentType = AdjustmentType.UNLAWFULLY_AT_LARGE,
      unlawfullyAtLarge = UnlawfullyAtLarge(type = UnlawfullyAtLargeType.ESCAPE),
      adjustmentHistory = listOf(AdjustmentHistory(changeType = ChangeType.CREATE, prisonId = null)),
    )

    whenever(prisonService.getSentencesAndStartDateDetails(PERSON_ID)).thenReturn(SENTENCE_DETAILS)
    whenever(
      adjustmentRepository.findUnreviewedPreviousUALOverlappingSentenceDate(
        person = PERSON_ID,
        startOfSentenceEnvelope = SENTENCE_DATE,
        status = listOf(ACTIVE, INACTIVE),
        adjustmentType = AdjustmentType.UNLAWFULLY_AT_LARGE,
      ),
    ).thenReturn(listOf(adjustmentWithAPrison, adjustmentWithoutAPrison))
    whenever(prisonApiClient.getPrison(PRISON_ID)).thenReturn(Prison(PRISON_ID, "Brixton (HMP)"))

    val result = service.findPreviousUalToReview(PERSON_ID)

    assertThat(result).isEqualTo(
      listOf(
        PreviousUnlawfullyAtLargeAdjustmentForReview(
          id = adjustmentWithAPrison.id,
          fromDate = LocalDate.of(2021, 1, 1),
          toDate = LocalDate.of(2021, 1, 11),
          days = 10,
          type = UnlawfullyAtLargeType.RELEASE_IN_ERROR,
          prisonName = "Brixton (HMP)",
          prisonId = PRISON_ID,
        ),
        PreviousUnlawfullyAtLargeAdjustmentForReview(
          id = adjustmentWithoutAPrison.id,
          fromDate = LocalDate.of(2021, 1, 1),
          toDate = LocalDate.of(2022, 1, 1),
          days = 364,
          type = UnlawfullyAtLargeType.ESCAPE,
          prisonName = null,
          prisonId = null,
        ),
      ),
    )
    verify(prisonService).getSentencesAndStartDateDetails(PERSON_ID)
  }

  @Test
  fun `Use passed in sentence details if present to avoid loading sentences twice for things to do`() {
    val adjustment = Adjustment(
      fromDate = LocalDate.of(2021, 1, 1),
      toDate = LocalDate.of(2021, 1, 11),
      effectiveDays = 10,
      adjustmentType = AdjustmentType.UNLAWFULLY_AT_LARGE,
      unlawfullyAtLarge = UnlawfullyAtLarge(type = UnlawfullyAtLargeType.RELEASE_IN_ERROR),
      adjustmentHistory = listOf(AdjustmentHistory(changeType = ChangeType.CREATE, prisonId = PRISON_ID)),
    )

    whenever(prisonService.getSentencesAndStartDateDetails(PERSON_ID)).thenReturn(SENTENCE_DETAILS)
    whenever(
      adjustmentRepository.findUnreviewedPreviousUALOverlappingSentenceDate(
        person = PERSON_ID,
        startOfSentenceEnvelope = SENTENCE_DATE,
        status = listOf(ACTIVE, INACTIVE),
        adjustmentType = AdjustmentType.UNLAWFULLY_AT_LARGE,
      ),
    ).thenReturn(listOf(adjustment))
    whenever(prisonApiClient.getPrison(PRISON_ID)).thenReturn(Prison(PRISON_ID, "Brixton (HMP)"))

    val result = service.findPreviousUalToReview(PERSON_ID, SENTENCE_DETAILS)

    assertThat(result).hasSize(1)
    verify(prisonService, never()).getSentencesAndStartDateDetails(PERSON_ID)
  }

  @Test
  fun `Return nothing without lookup in the database if there are no sentences`() {
    whenever(prisonService.getSentencesAndStartDateDetails(PERSON_ID)).thenReturn(SentenceAndStartDateDetails())

    val result = service.findPreviousUalToReview(PERSON_ID)

    assertThat(result).hasSize(0)
    verify(adjustmentRepository, never()).findUnreviewedPreviousUALOverlappingSentenceDate(
      person = PERSON_ID,
      startOfSentenceEnvelope = SENTENCE_DATE,
      status = listOf(ACTIVE, INACTIVE),
      adjustmentType = AdjustmentType.UNLAWFULLY_AT_LARGE,
    )
  }

  @Test
  fun `should record review of previous UAL and copy accepted adjustments`() {
    val firstAcceptId = UUID.randomUUID()
    val secondAcceptId = UUID.randomUUID()
    val firstRejectId = UUID.randomUUID()
    val secondRejectId = UUID.randomUUID()
    val firstCreatedId = UUID.randomUUID()
    val secondCreatedId = UUID.randomUUID()

    val request = PreviousUnlawfullyAtLargeReviewRequest(
      acceptedAdjustmentIds = listOf(firstAcceptId, secondAcceptId),
      rejectedAdjustmentIds = listOf(firstRejectId, secondRejectId),
    )

    whenever(adjustmentsTransactionalService.getCurrentAuthenticationUsername()).thenReturn("USER1")
    whenever(prisonerSearchApiClient.findByPrisonerNumber(PERSON_ID)).thenReturn(PRISONER)
    whenever(reviewPreviousUalResultRepository.saveAll(anyList())).then { invocation -> invocation.arguments[0] }
    whenever(adjustmentRepository.getReferenceById(firstAcceptId)).thenReturn(
      Adjustment(
        id = firstAcceptId,
        fromDate = LocalDate.of(2021, 1, 1),
        toDate = LocalDate.of(2021, 1, 11),
        effectiveDays = 10,
        adjustmentType = AdjustmentType.UNLAWFULLY_AT_LARGE,
        unlawfullyAtLarge = UnlawfullyAtLarge(type = UnlawfullyAtLargeType.RELEASE_IN_ERROR),
      ),
    )
    whenever(adjustmentRepository.getReferenceById(secondAcceptId)).thenReturn(
      Adjustment(
        id = secondAcceptId,
        fromDate = LocalDate.of(2021, 1, 1),
        toDate = LocalDate.of(2022, 1, 1),
        effectiveDays = 364,
        adjustmentType = AdjustmentType.UNLAWFULLY_AT_LARGE,
        unlawfullyAtLarge = UnlawfullyAtLarge(type = UnlawfullyAtLargeType.ESCAPE),
      ),
    )
    whenever(adjustmentsTransactionalService.create(any())).thenReturn(
      CreateResponseDto(
        listOf(
          firstCreatedId,
          secondCreatedId,
        ),
      ),
    )

    val result = service.submitPreviousUalReview(PERSON_ID, request)

    assertReviewIsRecorded(firstRejectId, secondRejectId, firstAcceptId, secondAcceptId)
    assertCreatedAdjustments()
    assertEventsAreGenerated(result, firstCreatedId, secondCreatedId)
  }

  private fun assertCreatedAdjustments() {
    val createCaptor = argumentCaptor<List<AdjustmentDto>>()
    verify(adjustmentsTransactionalService).create(createCaptor.capture())
    assertThat(createCaptor.firstValue).containsExactlyInAnyOrder(
      AdjustmentDto(
        adjustmentType = AdjustmentType.UNLAWFULLY_AT_LARGE,
        bookingId = BOOKING_ID,
        person = PERSON_ID,
        fromDate = LocalDate.of(2021, 1, 1),
        toDate = LocalDate.of(2021, 1, 11),
        unlawfullyAtLarge = UnlawfullyAtLargeDto(type = UnlawfullyAtLargeType.RELEASE_IN_ERROR),
        prisonId = PRISON_ID,
        effectiveDays = null,
        id = null,
        days = null,
        remand = null,
        additionalDaysAwarded = null,
        lawfullyAtLarge = null,
        specialRemission = null,
        taggedBail = null,
        timeSpentInCustodyAbroad = null,
        timeSpentAsAnAppealApplicant = null,
        recallId = null,
      ),
      AdjustmentDto(
        adjustmentType = AdjustmentType.UNLAWFULLY_AT_LARGE,
        bookingId = BOOKING_ID,
        person = PERSON_ID,
        fromDate = LocalDate.of(2021, 1, 1),
        toDate = LocalDate.of(2022, 1, 1),
        unlawfullyAtLarge = UnlawfullyAtLargeDto(type = UnlawfullyAtLargeType.ESCAPE),
        prisonId = PRISON_ID,
        effectiveDays = null,
        id = null,
        days = null,
        remand = null,
        additionalDaysAwarded = null,
        lawfullyAtLarge = null,
        specialRemission = null,
        taggedBail = null,
        timeSpentInCustodyAbroad = null,
        timeSpentAsAnAppealApplicant = null,
        recallId = null,
      ),
    )
  }

  private fun assertEventsAreGenerated(
    result: List<AdjustmentEventMetadata>,
    firstCreatedId: UUID,
    secondCreatedId: UUID,
  ) {
    assertThat(result).isEqualTo(
      listOf(
        AdjustmentEventMetadata(
          eventType = AdjustmentEventType.ADJUSTMENT_REVIEWED_PREVIOUS_UAL_PERIODS,
          ids = emptyList(),
          person = PERSON_ID,
          source = AdjustmentSource.DPS,
          adjustmentType = null,
          isLast = false,
        ),
        AdjustmentEventMetadata(
          eventType = AdjustmentEventType.ADJUSTMENT_CREATED,
          ids = listOf(firstCreatedId),
          person = PERSON_ID,
          source = AdjustmentSource.DPS,
          adjustmentType = AdjustmentType.UNLAWFULLY_AT_LARGE,
          isLast = false,
        ),
        AdjustmentEventMetadata(
          eventType = AdjustmentEventType.ADJUSTMENT_CREATED,
          ids = listOf(secondCreatedId),
          person = PERSON_ID,
          source = AdjustmentSource.DPS,
          adjustmentType = AdjustmentType.UNLAWFULLY_AT_LARGE,
          isLast = true,
        ),
      ),
    )
  }

  private fun assertReviewIsRecorded(
    firstRejectId: UUID,
    secondRejectId: UUID,
    firstAcceptId: UUID,
    secondAcceptId: UUID,
  ) {
    val reviewCaptor = argumentCaptor<List<ReviewPreviousUalResult>>()
    verify(reviewPreviousUalResultRepository, times(2)).saveAll(reviewCaptor.capture())
    val baseResult = ReviewPreviousUalResult(
      id = UUID.randomUUID(),
      adjustmentId = UUID.randomUUID(),
      person = PERSON_ID,
      status = ReviewPreviousUalStatus.ACCEPTED,
      reviewedByUsername = "USER1",
      reviewedByPrisonId = PRISON_ID,
      reviewedAt = LocalDateTime.now(),
    )
    assertThat(reviewCaptor.allValues.flatten()).usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "reviewedAt")
      .containsExactlyInAnyOrder(
        baseResult.copy(adjustmentId = firstRejectId, status = ReviewPreviousUalStatus.REJECTED),
        baseResult.copy(adjustmentId = secondRejectId, status = ReviewPreviousUalStatus.REJECTED),
        baseResult.copy(adjustmentId = firstAcceptId, status = ReviewPreviousUalStatus.ACCEPTED),
        baseResult.copy(adjustmentId = secondAcceptId, status = ReviewPreviousUalStatus.ACCEPTED),
      )
  }

  companion object {
    private const val PERSON_ID = "A1234BC"
    private const val PRISON_ID = "BXI"
    private const val BOOKING_ID = 12345L
    private val SENTENCE_DATE = LocalDate.of(2025, 6, 7)
    private val SENTENCE_DETAILS = SentenceAndStartDateDetails(sentences = listOf(SentenceAndOffences(sentenceDate = SENTENCE_DATE, bookingId = 1, sentenceSequence = 1, sentenceCalculationType = "ADIMP", sentenceStatus = "A")), earliestSentenceDate = SENTENCE_DATE)

    private val PRISONER =
      Prisoner(
        prisonerNumber = PERSON_ID,
        bookingId = BOOKING_ID,
        prisonId = PRISON_ID,
        dateOfBirth = LocalDate.of(1991, 1, 1),
      )
  }
}
