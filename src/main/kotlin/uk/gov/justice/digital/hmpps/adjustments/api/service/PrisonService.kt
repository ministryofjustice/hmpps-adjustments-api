package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.adjustments.api.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.adjustments.api.error.NoActiveSentencesException
import uk.gov.justice.digital.hmpps.adjustments.api.model.SentenceInfo.Companion.isRecall
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.RECALL_COURT_EVENT
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.SentenceAndOffences
import java.time.LocalDate

@Service
class PrisonService(
  private val prisonApiClient: PrisonApiClient,
) {

  fun getStartOfSentenceEnvelope(bookingId: Long): LocalDate {
    val sentences = getSentencesAndOffences(bookingId)
    if (sentences.isEmpty()) {
      throw NoActiveSentencesException("No active sentences on the booking $bookingId")
    }
    return sentences.minOf { it.sentenceDate }
  }

  fun getSentencesAndOffences(bookingId: Long, filterActive: Boolean = true): List<SentenceAndOffences> {
    return prisonApiClient.getSentencesAndOffences(bookingId)
      .filter { !filterActive || it.sentenceStatus == "A" }
  }

  fun getSentencesAndStartDateDetails(personId: String): SentenceAndStartDateDetails {
    val bookingId = prisonApiClient.getPrisonerDetail(personId).bookingId
    val sentences = getSentencesAndOffences(bookingId)
    val hasRecall = sentences.any { isRecall(it) }
    val earliestRecallDate = if (hasRecall) {
      val recallChargeIds = sentences.filter { isRecall(it) }.flatMap { it.offences.map { off -> off.offenderChargeId } }
      val courtOutcomes = prisonApiClient.getCourtDateResults(personId)
      val recallOutcomes = courtOutcomes.filter { it.resultCode === RECALL_COURT_EVENT }
      val matchingOutcomes = recallOutcomes.filter { recallChargeIds.contains(it.charge.chargeId) }
      matchingOutcomes.minOfOrNull { it.date }
    } else {
      null
    }
    val latestSentenceDate = sentences.maxOfOrNull { it.sentenceDate }
    val earliestNonRecallSentenceDate = sentences.filter { !isRecall(it) }.minOfOrNull { it.sentenceDate }
    return SentenceAndStartDateDetails(
      sentences,
      hasRecall,
      earliestNonRecallSentenceDate,
      latestSentenceDate,
      earliestRecallDate,
    )
  }
}

data class SentenceAndStartDateDetails(
  val sentences: List<SentenceAndOffences>,
  val hasRecall: Boolean,
  val latestSentenceDate: LocalDate?,
  val earliestNonRecallSentenceDate: LocalDate?,
  val earliestRecallDate: LocalDate?,
)
