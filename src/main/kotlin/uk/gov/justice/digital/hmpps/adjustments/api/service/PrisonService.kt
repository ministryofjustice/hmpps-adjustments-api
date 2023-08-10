package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.adjustments.api.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.SentenceAndOffences
import java.time.LocalDate

@Service
class PrisonService(
  private val prisonApiClient: PrisonApiClient,
) {

  fun getStartOfSentenceEnvelope(bookingId: Long): LocalDate {
    return getSentencesAndOffences(bookingId).minOf { it.sentenceDate }
  }

  fun getSentencesAndOffences(bookingId: Long, filterActive: Boolean = true): List<SentenceAndOffences> {
    return prisonApiClient.getSentencesAndOffences(bookingId)
      .filter { !filterActive || it.sentenceStatus == "A" }
  }

  fun getStartOfSentenceEnvelope(person: String): LocalDate =
    getStartOfSentenceEnvelope(prisonApiClient.getPrisonerDetail(person).bookingId)
}
