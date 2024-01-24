package uk.gov.justice.digital.hmpps.adjustments.api.service

import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.adjustments.api.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.adjustments.api.error.NoActiveSentencesException
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.SentenceAndOffences
import java.time.LocalDate

@Service
class PrisonService(
  private val prisonApiClient: PrisonApiClient,
) {

  suspend fun getStartOfSentenceEnvelope(bookingId: Long): LocalDate {
    val sentences = getSentencesAndOffences(bookingId)
    if (sentences.isEmpty()) {
      throw NoActiveSentencesException("No active sentences on the booking $bookingId")
    }
    return sentences.minOf { it.sentenceDate }
  }

  suspend fun getSentencesAndOffences(bookingId: Long, filterActive: Boolean = true): List<SentenceAndOffences> {
    return prisonApiClient.getSentencesAndOffences(bookingId)
      .toList(mutableListOf())
      .filter { !filterActive || it.sentenceStatus == "A" }
  }

  suspend fun getStartOfSentenceEnvelope(person: String): LocalDate =
    getStartOfSentenceEnvelope(prisonApiClient.getPrisonerDetail(person).bookingId)
}
