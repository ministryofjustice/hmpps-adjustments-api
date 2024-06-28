package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.adjustments.api.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.OffenderOffence
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.PrisonApiCharge
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.PrisonApiCourtDateResult
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.PrisonerDetails
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.RECALL_COURT_EVENT
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.SentenceAndOffences
import java.time.LocalDate

class PrisonServiceTest {

  private val prisonApiClient = mock<PrisonApiClient>()

  private val prisonService =
    PrisonService(prisonApiClient)

  @Test
  fun `get sentence start details when mix of recall and determinate`() {
    val person = "ABC123"
    val bookingId = 1L
    val prisoner = PrisonerDetails(
      bookingId = bookingId,
      offenderNo = person,
      dateOfBirth = LocalDate.of(2000, 1, 1),
    )
    val chargeId = 1L
    val sentenceDate = LocalDate.of(2023, 1, 1)
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
        sentenceDate = sentenceDate.minusYears(1),
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
      PrisonApiCourtDateResult(
        resultCode = RECALL_COURT_EVENT,
        date = recallDate,
        charge = PrisonApiCharge(
          chargeId = chargeId,
        ),
      ),
    )
    whenever(prisonApiClient.getPrisonerDetail(person)).thenReturn(prisoner)
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
      ),
    )
  }
}
