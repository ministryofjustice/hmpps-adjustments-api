package uk.gov.justice.digital.hmpps.adjustments.api.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.adjustments.api.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.adjustments.api.client.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.adjustments.api.entity.Adjustment
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentSource
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.adjustments.api.entity.ChangeType
import uk.gov.justice.digital.hmpps.adjustments.api.entity.ReviewPreviousUalResult
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentEventMetadata
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdjustmentEventType
import uk.gov.justice.digital.hmpps.adjustments.api.model.UnlawfullyAtLargeDto
import uk.gov.justice.digital.hmpps.adjustments.api.model.previousual.PreviousUnlawfullyAtLargeAdjustmentForReview
import uk.gov.justice.digital.hmpps.adjustments.api.model.previousual.PreviousUnlawfullyAtLargeReviewRequest
import uk.gov.justice.digital.hmpps.adjustments.api.respository.AdjustmentRepository
import uk.gov.justice.digital.hmpps.adjustments.api.respository.ReviewPreviousUalResultRepository

@Component
class ReviewPreviousUalService(
  private val prisonService: PrisonService,
  private val prisonApiClient: PrisonApiClient,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val adjustmentRepository: AdjustmentRepository,
  private val reviewPreviousUalResultRepository: ReviewPreviousUalResultRepository,
  private val adjustmentsTransactionalService: AdjustmentsTransactionalService,
) {

  fun findPreviousUalToReview(person: String): List<PreviousUnlawfullyAtLargeAdjustmentForReview> {
    val unreviewedPreviousUal = getUnreviewedUalFromPreviousPeriodThatOverlapsTheCurrentPeriod(person)
    return unreviewedPreviousUal.map { ual ->
      val latestHistory = ual.adjustmentHistory.sortedBy { it.changeAt }
        .last { it.changeType !in listOf(ChangeType.MERGE, ChangeType.RELEASE, ChangeType.ADMISSION) }
      val prisonDescription = latestHistory.prisonId?.let { prisonApiClient.getPrison(it).description }
      PreviousUnlawfullyAtLargeAdjustmentForReview(
        id = ual.id,
        fromDate = requireNotNull(ual.fromDate) { "UAL adjustments must have a from date" },
        toDate = requireNotNull(ual.toDate) { "UAL adjustments must have a to date" },
        days = requireNotNull(ual.effectiveDays) { "UAL adjustments must a number of effective days" },
        type = ual.unlawfullyAtLarge?.type,
        prisonName = prisonDescription,
        prisonId = latestHistory.prisonId,
      )
    }
  }

  @Transactional
  fun submitPreviousUalReview(
    person: String,
    request: PreviousUnlawfullyAtLargeReviewRequest,
  ): List<AdjustmentEventMetadata> {
    val prisoner = prisonerSearchApiClient.findByPrisonerNumber(person)
    val username = adjustmentsTransactionalService.getCurrentAuthenticationUsername()
    val prison = prisoner.prisonId
    request.rejectedAdjustmentIds.forEach { rejectedAdjustmentUuid ->
      reviewPreviousUalResultRepository.save(
        ReviewPreviousUalResult.rejected(
          adjustmentId = rejectedAdjustmentUuid,
          person = person,
          username = username,
          prison = prison,
        ),
      )
    }
    val adjustmentsToCreate = request.acceptedAdjustmentIds
      .onEach { acceptedAdjustmentUuid ->
        reviewPreviousUalResultRepository.save(
          ReviewPreviousUalResult.accepted(
            adjustmentId = acceptedAdjustmentUuid,
            person = person,
            username = username,
            prison = prison,
          ),
        )
      }
      .map { acceptedAdjustmentUuid ->
        val existing = adjustmentRepository.getReferenceById(acceptedAdjustmentUuid)
        AdjustmentDto(
          adjustmentType = AdjustmentType.UNLAWFULLY_AT_LARGE,
          bookingId = prisoner.bookingId,
          fromDate = existing.fromDate,
          toDate = existing.toDate,
          person = person,
          unlawfullyAtLarge = existing.unlawfullyAtLarge?.type?.let { UnlawfullyAtLargeDto(it) },
          prisonId = prisoner.prisonId,
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
        )
      }
    val createdAdjustmentIds = adjustmentsTransactionalService.create(adjustmentsToCreate).adjustmentIds
    return createdAdjustmentIds.mapIndexed { index, id ->
      AdjustmentEventMetadata(
        eventType = AdjustmentEventType.ADJUSTMENT_CREATED,
        ids = listOf(id),
        person = person,
        source = AdjustmentSource.DPS,
        adjustmentType = AdjustmentType.UNLAWFULLY_AT_LARGE,
        isLast = index == createdAdjustmentIds.size - 1,
      )
    }
  }

  private fun getUnreviewedUalFromPreviousPeriodThatOverlapsTheCurrentPeriod(person: String): List<Adjustment> {
    val bookingId = prisonerSearchApiClient.findByPrisonerNumber(person).bookingId
    val startOfSentenceEnvelope = prisonService.getStartOfSentenceEnvelope(bookingId)
    val unreviewedPreviousUal =
      adjustmentRepository.findUnreviewedPreviousUALOverlappingSentenceDate(person, startOfSentenceEnvelope)
    return unreviewedPreviousUal
  }
}
