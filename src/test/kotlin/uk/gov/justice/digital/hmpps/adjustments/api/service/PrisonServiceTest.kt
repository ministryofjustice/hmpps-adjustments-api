package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.adjustments.api.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.adjustments.api.client.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.adjustments.api.client.RemandAndSentencingApiClient
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.CourtDateChargeAndOutcomes
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.CourtDateOutcome
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.OffenderOffence
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.RECALL_COURT_EVENT
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.SentenceAndOffences
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonersearchapi.Prisoner
import java.time.LocalDate

class PrisonServiceTest {

  private val prisonApiClient = mock<PrisonApiClient>()

  private val prisonerSearchApiClient = mock<PrisonerSearchApiClient>()

  private val remandAndSentencingApiClient = mock<RemandAndSentencingApiClient>()

  private val prisonService =
    PrisonService(prisonApiClient, prisonerSearchApiClient, remandAndSentencingApiClient)

  @Test
  fun `get sentence start details when mix of recall and determinate`() {
    val person = "ABC123"
    val bookingId = 1L
    val prisoner = Prisoner(
      bookingId = bookingId,
      prisonerNumber = person,
      dateOfBirth = LocalDate.of(2000, 1, 1),
    )
    val chargeId = 1L
    val sentenceDate = LocalDate.of(2023, 1, 1)
    val recallSentenceDate = LocalDate.of(2022, 1, 1)
    val recallDate = LocalDate.of(2024, 1, 1)
    val sentences = listOf(
      SentenceAndOffences(
        sentenceDate = sentenceDate,
        bookingId = 1,
        sentenceSequence = 1,
        sentenceStatus = "A",
        sentenceCalculationType = "ADIMP",
      ),
      SentenceAndOffences(
        sentenceDate = recallSentenceDate,
        bookingId = 1,
        sentenceSequence = 1,
        sentenceStatus = "A",
        sentenceCalculationType = "LR",
        offences = listOf(
          OffenderOffence(
            offenderChargeId = chargeId,
          ),
        ),
      ),
    )
    val courtEvents = listOf(
      CourtDateChargeAndOutcomes(
        chargeId = chargeId,
        outcomes = listOf(
          CourtDateOutcome(
            id = -1,
            resultCode = RECALL_COURT_EVENT,
            date = recallDate,
          ),
        ),
      ),
    )
    whenever(prisonerSearchApiClient.findByPrisonerNumber(person)).thenReturn(prisoner)
    whenever(prisonApiClient.getSentencesAndOffences(bookingId)).thenReturn(sentences)
    whenever(prisonApiClient.getCourtDateResults(person)).thenReturn(courtEvents)

    val result = prisonService.getSentencesAndStartDateDetails(person)

    assertThat(result).isEqualTo(
      SentenceAndStartDateDetails(
        sentences = sentences,
        hasRecall = true,
        earliestRecallDate = recallDate,
        earliestNonRecallSentenceDate = sentenceDate,
        latestSentenceDate = sentenceDate,
        earliestSentenceDate = recallSentenceDate,
      ),
    )
  }
}
