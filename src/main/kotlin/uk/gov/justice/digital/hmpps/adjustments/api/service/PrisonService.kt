package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.adjustments.api.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.adjustments.api.client.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.adjustments.api.error.NoActiveSentencesException
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.RECALL_COURT_EVENT
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.SentenceAndOffences
import java.time.LocalDate

@Service
class PrisonService(
  private val prisonApiClient: PrisonApiClient,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val sentenceInfoService: SentenceInfoService,
) {

  fun getStartOfSentenceEnvelope(bookingId: Long): LocalDate {
    val sentences = getSentencesAndOffences(bookingId)
    if (sentences.isEmpty()) {
      throw NoActiveSentencesException("No active sentences on the booking $bookingId")
    }
    return sentences.minOf { it.sentenceDate }
  }

  fun getSentencesAndOffences(bookingId: Long, filterActive: Boolean = true): List<SentenceAndOffences> = prisonApiClient.getSentencesAndOffences(bookingId)
    .filter { !filterActive || it.sentenceStatus == "A" }

  fun getSentencesAndStartDateDetails(personId: String): SentenceAndStartDateDetails {
    val bookingId = prisonerSearchApiClient.findByPrisonerNumber(personId).bookingId
    val sentences = getSentencesAndOffences(bookingId)
    val hasRecall = sentences.any { sentenceInfoService.isRecall(it.sentenceCalculationType) }
    val earliestRecallDate = if (hasRecall) {
      val recallChargeIds = sentences.filter { sentenceInfoService.isRecall(it.sentenceCalculationType) }.flatMap { it.offences.map { off -> off.offenderChargeId } }
      val courtCharges = prisonApiClient.getCourtDateResults(personId)
      val matchingCharges = courtCharges.filter { recallChargeIds.contains(it.chargeId) }
      val matchingOutcomes = matchingCharges.mapNotNull { it.outcomes.find { outcome -> outcome.resultCode == RECALL_COURT_EVENT } }
      matchingOutcomes.minOfOrNull { it.date!! }
    } else {
      null
    }
    val earliestNonRecallSentenceDate = sentences.filter { !sentenceInfoService.isRecall(it.sentenceCalculationType) }.minOfOrNull { it.sentenceDate }
    val earliestSentenceDate = sentences.minOfOrNull { it.sentenceDate }
    val latestSentenceDate = sentences.maxOfOrNull { it.sentenceDate }
    return SentenceAndStartDateDetails(
      sentences,
      hasRecall,
      latestSentenceDate,
      earliestSentenceDate,
      earliestNonRecallSentenceDate,
      earliestRecallDate,
    )
  }
}

data class SentenceAndStartDateDetails(
  val sentences: List<SentenceAndOffences> = emptyList(),
  val hasRecall: Boolean = false,
  val latestSentenceDate: LocalDate? = null,
  val earliestSentenceDate: LocalDate? = null,
  val earliestNonRecallSentenceDate: LocalDate? = null,
  val earliestRecallDate: LocalDate? = null,
)
