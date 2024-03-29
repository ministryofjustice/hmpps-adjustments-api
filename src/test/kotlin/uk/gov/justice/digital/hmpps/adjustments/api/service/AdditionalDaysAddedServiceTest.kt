package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.adjustments.api.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdditionalDaysAwarded
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjudicationCharges
import uk.gov.justice.digital.hmpps.adjustments.api.entity.Adjustment
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType.ADDITIONAL_DAYS_AWARDED
import uk.gov.justice.digital.hmpps.adjustments.api.enums.InterceptType.FIRST_TIME
import uk.gov.justice.digital.hmpps.adjustments.api.enums.InterceptType.NONE
import uk.gov.justice.digital.hmpps.adjustments.api.enums.InterceptType.PADA
import uk.gov.justice.digital.hmpps.adjustments.api.enums.InterceptType.UPDATE
import uk.gov.justice.digital.hmpps.adjustments.api.model.AdaIntercept
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.Adjudication
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.AdjudicationDetail
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.AdjudicationSearchResponse
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.Hearing
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.HearingResult
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.PrisonerDetails
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.Sanction
import uk.gov.justice.digital.hmpps.adjustments.api.respository.AdjustmentRepository
import java.time.LocalDate
import java.time.LocalDateTime

class AdditionalDaysAddedServiceTest {

  private val prisonService = mock<PrisonService>()
  private val adjustmentRepository = mock<AdjustmentRepository>()
  private val prisonApiClient = mock<PrisonApiClient>()
  private val additionalDaysAwardedService =
    AdditionalDaysAwardedService(prisonService, adjustmentRepository, prisonApiClient)

  @Nested
  inner class InterceptTests {
    @BeforeEach
    fun setup() {
      whenever(prisonApiClient.getPrisonerDetail(NOMS_ID)).thenReturn(
        PrisonerDetails(
          bookingId = 123,
          offenderNo = NOMS_ID,
          firstName = "DEFAULT",
          lastName = "PRISONER",
          dateOfBirth = LocalDate.parse("1995-03-08"),
        ),
      )
    }

    @Test
    fun `Should not intercept if no sentence date`() {
      whenever(prisonService.getStartOfSentenceEnvelopeExcludingRecalls(NOMS_ID)).thenReturn(null)

      val intercept = additionalDaysAwardedService.determineAdaIntercept(NOMS_ID)

      assertThat(intercept).isEqualTo(AdaIntercept(NONE, 0, false, emptyList()))
    }

    @Test
    fun `Should intercept if any unlinked ADAs`() {
      whenever(
        adjustmentRepository.findByPersonAndAdjustmentTypeAndStatus(
          NOMS_ID,
          ADDITIONAL_DAYS_AWARDED,
        ),
      ).thenReturn(
        listOf(
          BASE_10_DAY_ADJUSTMENT.copy(
            additionalDaysAwarded = AdditionalDaysAwarded(adjudicationCharges = mutableListOf()),
            effectiveDays = 9,
          ),
        ),
      )
      whenever(prisonApiClient.getAdjudications(NOMS_ID)).thenReturn(threeAdjudicationsSearchResponse)
      whenever(prisonApiClient.getAdjudication(NOMS_ID, 1525916)).thenReturn(adjudicationOne)
      whenever(prisonApiClient.getAdjudication(NOMS_ID, 1525917)).thenReturn(adjudicationTwoConsecutiveToOne)
      whenever(prisonApiClient.getAdjudication(NOMS_ID, 1525918)).thenReturn(adjudicationThreeConcurrentToOne)
      whenever(prisonService.getStartOfSentenceEnvelopeExcludingRecalls(NOMS_ID)).thenReturn(LocalDate.of(2023, 1, 1))

      val intercept = additionalDaysAwardedService.determineAdaIntercept(NOMS_ID)

      assertThat(intercept).isEqualTo(AdaIntercept(FIRST_TIME, 1, false, emptyList()))
      assertThat(intercept.message).isEqualTo("The first time you use the adjustments service, you need to check if the existing adjustment information from NOMIS is correct.")
    }

    @Test
    fun `Should return First Time intercept if any unlinked ADAs because there is no additionalDaysAwarded object associated to the ADA`() {
      whenever(
        adjustmentRepository.findByPersonAndAdjustmentTypeAndStatus(
          NOMS_ID,
          ADDITIONAL_DAYS_AWARDED,
        ),
      ).thenReturn(
        listOf(
          BASE_10_DAY_ADJUSTMENT.copy(
            additionalDaysAwarded = null,
            effectiveDays = 9,
          ),
        ),
      )
      whenever(prisonApiClient.getAdjudications(NOMS_ID)).thenReturn(threeAdjudicationsSearchResponse)
      whenever(prisonApiClient.getAdjudication(NOMS_ID, 1525916)).thenReturn(adjudicationOne)
      whenever(prisonApiClient.getAdjudication(NOMS_ID, 1525917)).thenReturn(adjudicationTwoConsecutiveToOne)
      whenever(prisonApiClient.getAdjudication(NOMS_ID, 1525918)).thenReturn(adjudicationThreeConcurrentToOne)
      whenever(prisonService.getStartOfSentenceEnvelopeExcludingRecalls(NOMS_ID)).thenReturn(LocalDate.of(2023, 1, 1))

      val intercept = additionalDaysAwardedService.determineAdaIntercept(NOMS_ID)

      assertThat(intercept).isEqualTo(AdaIntercept(FIRST_TIME, 0, false, emptyList()))
      assertThat(intercept.message).isEqualTo("The first time you use the adjustments service, you need to check if the existing adjustment information from NOMIS is correct.")
    }

    @Test
    fun `Should intercept if there is a difference between the totalDays in adjustments and adjudications`() {
      whenever(
        adjustmentRepository.findByPersonAndAdjustmentTypeAndStatus(
          NOMS_ID,
          ADDITIONAL_DAYS_AWARDED,
        ),
      ).thenReturn(
        listOf(BASE_10_DAY_ADJUSTMENT.copy(days = 11)),
      )
      whenever(prisonApiClient.getAdjudications(NOMS_ID)).thenReturn(threeAdjudicationsSearchResponse)
      whenever(prisonApiClient.getAdjudication(NOMS_ID, 1525916)).thenReturn(adjudicationOne)
      whenever(prisonApiClient.getAdjudication(NOMS_ID, 1525917)).thenReturn(adjudicationTwoConsecutiveToOne)
      whenever(prisonApiClient.getAdjudication(NOMS_ID, 1525918)).thenReturn(adjudicationThreeConcurrentToOne)
      whenever(prisonService.getStartOfSentenceEnvelopeExcludingRecalls(NOMS_ID)).thenReturn(LocalDate.of(2023, 1, 1))

      val intercept = additionalDaysAwardedService.determineAdaIntercept(NOMS_ID)

      assertUpdateIntercept(intercept, 1, false)
    }

    @Test
    fun `Should intercept mix of concurrent consec`() {
      whenever(
        adjustmentRepository.findByPersonAndAdjustmentTypeAndStatus(
          NOMS_ID,
          ADDITIONAL_DAYS_AWARDED,
        ),
      ).thenReturn(emptyList())
      whenever(prisonApiClient.getAdjudications(NOMS_ID)).thenReturn(threeAdjudicationsSearchResponse)
      whenever(prisonApiClient.getAdjudication(NOMS_ID, 1525916)).thenReturn(adjudicationOne)
      whenever(prisonApiClient.getAdjudication(NOMS_ID, 1525917)).thenReturn(adjudicationTwoConsecutiveToOne)
      whenever(prisonApiClient.getAdjudication(NOMS_ID, 1525918)).thenReturn(adjudicationThreeConcurrentToOne)
      whenever(prisonService.getStartOfSentenceEnvelopeExcludingRecalls(NOMS_ID)).thenReturn(LocalDate.of(2023, 1, 1))

      val intercept = additionalDaysAwardedService.determineAdaIntercept(NOMS_ID)

      assertUpdateIntercept(intercept, 1, false)
    }

    @Test
    fun `Shouldnt intercept when already persisted in adjustment api`() {
      whenever(
        adjustmentRepository.findByPersonAndAdjustmentTypeAndStatus(
          NOMS_ID,
          ADDITIONAL_DAYS_AWARDED,
        ),
      ).thenReturn(
        listOf(BASE_10_DAY_ADJUSTMENT),
      )
      whenever(prisonApiClient.getAdjudications(NOMS_ID)).thenReturn(threeAdjudicationsSearchResponse)
      whenever(prisonApiClient.getAdjudication(NOMS_ID, 1525916)).thenReturn(adjudicationOne)
      whenever(prisonApiClient.getAdjudication(NOMS_ID, 1525917)).thenReturn(adjudicationTwoConsecutiveToOne)
      whenever(prisonApiClient.getAdjudication(NOMS_ID, 1525918)).thenReturn(adjudicationThreeConcurrentToOne)
      whenever(prisonService.getStartOfSentenceEnvelopeExcludingRecalls(NOMS_ID)).thenReturn(LocalDate.of(2023, 1, 1))

      val intercept = additionalDaysAwardedService.determineAdaIntercept(NOMS_ID)

      assertThat(intercept).isEqualTo(AdaIntercept(NONE, 0, false))
    }

    @Test
    fun `Should intercept when already persisted adjustment has different days`() {
      whenever(
        adjustmentRepository.findByPersonAndAdjustmentTypeAndStatus(
          NOMS_ID,
          ADDITIONAL_DAYS_AWARDED,
        ),
      ).thenReturn(
        listOf(BASE_10_DAY_ADJUSTMENT.copy(days = 5)),
      )
      whenever(prisonApiClient.getAdjudications(NOMS_ID)).thenReturn(threeAdjudicationsSearchResponse)
      whenever(prisonApiClient.getAdjudication(NOMS_ID, 1525916)).thenReturn(adjudicationOne)
      whenever(prisonApiClient.getAdjudication(NOMS_ID, 1525917)).thenReturn(adjudicationTwoConsecutiveToOne)
      whenever(prisonApiClient.getAdjudication(NOMS_ID, 1525918)).thenReturn(adjudicationThreeConcurrentToOne)
      whenever(prisonService.getStartOfSentenceEnvelopeExcludingRecalls(NOMS_ID)).thenReturn(LocalDate.of(2023, 1, 1))

      val intercept = additionalDaysAwardedService.determineAdaIntercept(NOMS_ID)

      assertUpdateIntercept(intercept, 1, false)
    }

    @Test
    fun `Should intercept if all adas quashed`() {
      whenever(
        adjustmentRepository.findByPersonAndAdjustmentTypeAndStatus(
          NOMS_ID,
          ADDITIONAL_DAYS_AWARDED,
        ),
      ).thenReturn(
        listOf(BASE_10_DAY_ADJUSTMENT),
      )
      whenever(prisonApiClient.getAdjudications(NOMS_ID)).thenReturn(threeAdjudicationsSearchResponse)
      whenever(prisonApiClient.getAdjudication(NOMS_ID, 1525916)).thenReturn(adjudicationOneQuashed)
      whenever(prisonApiClient.getAdjudication(NOMS_ID, 1525917)).thenReturn(adjudicationTwoConsecutiveToOneQuashed)
      whenever(prisonApiClient.getAdjudication(NOMS_ID, 1525918)).thenReturn(adjudicationThreeConcurrentToOneQuashed)
      whenever(prisonService.getStartOfSentenceEnvelopeExcludingRecalls(NOMS_ID)).thenReturn(LocalDate.of(2023, 1, 1))

      val intercept = additionalDaysAwardedService.determineAdaIntercept(NOMS_ID)

      assertUpdateIntercept(intercept, 1, false)
    }

    @Test
    fun `Should intercept if any prospective`() {
      whenever(
        adjustmentRepository.findByPersonAndAdjustmentTypeAndStatus(
          NOMS_ID,
          ADDITIONAL_DAYS_AWARDED,
        ),
      ).thenReturn(
        emptyList(),
      )
      whenever(prisonApiClient.getAdjudications(NOMS_ID)).thenReturn(oneAdjudicationSearchResponse)
      whenever(prisonApiClient.getAdjudication(NOMS_ID, 1525916)).thenReturn(adjudicationOneProspective)
      whenever(prisonService.getStartOfSentenceEnvelopeExcludingRecalls(NOMS_ID)).thenReturn(LocalDate.of(2023, 1, 1))

      val intercept = additionalDaysAwardedService.determineAdaIntercept(NOMS_ID)

      assertPadaIntercept(intercept, 1, true)
    }

    private fun assertUpdateIntercept(intercept: AdaIntercept, number: Int, anyProspective: Boolean) {
      assertThat(intercept).isEqualTo(
        AdaIntercept(
          UPDATE,
          number,
          anyProspective,
          messageArguments = listOf("Prisoner, Default"),
        ),
      )
      assertThat(intercept.message).isEqualTo("Updates have been made to Prisoner, Default's adjustment information, which need to be approved.")
    }

    private fun assertPadaIntercept(intercept: AdaIntercept, number: Int, anyProspective: Boolean) {
      assertThat(intercept).isEqualTo(
        AdaIntercept(
          PADA,
          number,
          anyProspective,
          messageArguments = listOf("Prisoner, Default"),
        ),
      )
      assertThat(intercept.message).isEqualTo("There is a prospective ADA recorded for Prisoner, Default")
    }
  }

  companion object {
    const val NOMS_ID = "AA1234A"
    val BASE_10_DAY_ADJUSTMENT = Adjustment(
      person = "AA1234A",
      days = 10,
      adjustmentType = ADDITIONAL_DAYS_AWARDED,
      fromDate = LocalDate.of(2023, 8, 3),
      additionalDaysAwarded = AdditionalDaysAwarded(
        adjudicationCharges = mutableListOf(
          AdjudicationCharges(1525916),
          AdjudicationCharges(1525917),
          AdjudicationCharges(1525918),
        ),
      ),
    )
    private val adjudication1SearchResponse = Adjudication(
      adjudicationNumber = 1525916,
      reportTime = LocalDateTime.parse("2023-08-02T09:09:00"),
      agencyIncidentId = 1503215,
      agencyId = "MDI",
      partySeq = 1,
    )
    private val adjudication2SearchResponse = Adjudication(
      adjudicationNumber = 1525917,
      reportTime = LocalDateTime.parse("2023-08-02T09:09:00"),
      agencyIncidentId = 1503215,
      agencyId = "MDI",
      partySeq = 1,
    )
    private val adjudication3SearchResponse = Adjudication(
      adjudicationNumber = 1525918,
      reportTime = LocalDateTime.parse("2023-08-02T09:09:00"),
      agencyIncidentId = 1503215,
      agencyId = "MDI",
      partySeq = 1,
    )
    val adjudicationOne = AdjudicationDetail(
      adjudicationNumber = 1525916,
      hearings = listOf(
        Hearing(
          hearingTime = LocalDateTime.of(2023, 8, 3, 16, 45),
          results = listOf(
            HearingResult(
              offenceType = "Prison Rule 51",
              offenceDescription = "Intentionally or recklessly sets fire to any part of a prison or any other property, whether or not his own",
              plea = "Guilty",
              finding = "Charge Proved",
              sanctions = listOf(
                Sanction(
                  sanctionType = "Additional Days Added",
                  sanctionDays = 5,
                  status = "Immediate",
                  sanctionSeq = 15,
                ),
              ),
            ),
          ),
          establishment = "Moorland (HMP & YOI)",
        ),
      ),
      incidentTime = LocalDateTime.of(2023, 8, 1, 9, 0),
      establishment = "Moorland (HMP & YOI)",
      interiorLocation = "Circuit",
      incidentDetails = "some details",
      reportNumber = 1503215,
      reportType = "Governor's Report",
      reporterFirstName = "TIM",
      reporterLastName = "WRIGHT",
      reportTime = LocalDateTime.of(2023, 8, 2, 9, 9),
    )
    val adjudicationThreeConcurrentToOne = AdjudicationDetail(
      adjudicationNumber = 1525918,
      incidentTime = LocalDateTime.parse("2023-08-01T09:00:00"),
      establishment = "Moorland (HMP & YOI)",
      interiorLocation = "Circuit",
      incidentDetails = "some details",
      reportNumber = 1503215,
      reportType = "Governor's Report",
      reporterFirstName = "TIM",
      reporterLastName = "WRIGHT",
      reportTime = LocalDateTime.parse("2023-08-02T09:09:00"),
      hearings = listOf(
        Hearing(
          hearingTime = LocalDateTime.parse("2023-08-03T16:45:00"),
          establishment = "Moorland (HMP & YOI)",
          results = listOf(
            HearingResult(
              offenceType = "Prison Rule 51",
              offenceDescription = "Intentionally or recklessly sets fire to any part of a prison or any other property, whether or not his own",
              plea = "Guilty",
              finding = "Charge Proved",
              sanctions = listOf(
                Sanction(
                  sanctionType = "Additional Days Added",
                  sanctionDays = 5,
                  status = "Immediate",
                  sanctionSeq = 17,
                ),
              ),
            ),
          ),
        ),
      ),
    )
    val adjudicationTwoConsecutiveToOne = AdjudicationDetail(
      adjudicationNumber = 1525917,
      incidentTime = LocalDateTime.parse("2023-08-01T09:00:00"),
      establishment = "Moorland (HMP & YOI)",
      interiorLocation = "Circuit",
      incidentDetails = "some details",
      reportNumber = 1503215,
      reportType = "Governor's Report",
      reporterFirstName = "TIM",
      reporterLastName = "WRIGHT",
      reportTime = LocalDateTime.parse("2023-08-02T09:09:00"),
      hearings = listOf(
        Hearing(
          hearingTime = LocalDateTime.parse("2023-08-03T16:45:00"),
          establishment = "Moorland (HMP & YOI)",
          results = listOf(
            HearingResult(
              offenceType = "Prison Rule 51",
              offenceDescription = "Intentionally or recklessly sets fire to any part of a prison or any other property, whether or not his own",
              plea = "Guilty",
              finding = "Charge Proved",
              sanctions = listOf(
                Sanction(
                  sanctionType = "Additional Days Added",
                  sanctionDays = 5,
                  status = "Immediate",
                  sanctionSeq = 16,
                  consecutiveSanctionSeq = 15,
                ),
              ),
            ),
          ),
        ),
      ),
    )
    val adjudicationOneQuashed = AdjudicationDetail(
      adjudicationNumber = 1525916,
      incidentTime = LocalDateTime.parse("2023-08-01T09:00:00"),
      establishment = "Moorland (HMP & YOI)",
      interiorLocation = "Circuit",
      incidentDetails = "some details",
      reportNumber = 1503215,
      reportType = "Governor's Report",
      reporterFirstName = "TIM",
      reporterLastName = "WRIGHT",
      reportTime = LocalDateTime.parse("2023-08-02T09:09:00"),
      hearings = listOf(
        Hearing(
          hearingTime = LocalDateTime.parse("2023-08-03T16:45:00"),
          establishment = "Moorland (HMP & YOI)",
          results = listOf(
            HearingResult(
              offenceType = "Prison Rule 51",
              offenceDescription = "Intentionally or recklessly sets fire to any part of a prison or any other property, whether or not his own",
              plea = "Guilty",
              finding = "Charge Proved",
              sanctions = listOf(
                Sanction(
                  sanctionType = "Additional Days Added",
                  sanctionDays = 5,
                  status = "Quashed",
                  sanctionSeq = 15,
                ),
              ),
            ),
          ),
        ),
      ),
    )
    val adjudicationTwoConsecutiveToOneQuashed = AdjudicationDetail(
      adjudicationNumber = 1525917,
      incidentTime = LocalDateTime.parse("2023-08-01T09:00:00"),
      establishment = "Moorland (HMP & YOI)",
      interiorLocation = "Circuit",
      incidentDetails = "some details",
      reportNumber = 1503215,
      reportType = "Governor's Report",
      reporterFirstName = "TIM",
      reporterLastName = "WRIGHT",
      reportTime = LocalDateTime.parse("2023-08-02T09:09:00"),
      hearings = listOf(
        Hearing(
          hearingTime = LocalDateTime.parse("2023-08-03T16:45:00"),
          establishment = "Moorland (HMP & YOI)",
          results = listOf(
            HearingResult(
              offenceType = "Prison Rule 51",
              offenceDescription = "Intentionally or recklessly sets fire to any part of a prison or any other property, whether or not his own",
              plea = "Guilty",
              finding = "Charge Proved",
              sanctions = listOf(
                Sanction(
                  sanctionType = "Additional Days Added",
                  sanctionDays = 5,
                  status = "Quashed",
                  sanctionSeq = 16,
                  consecutiveSanctionSeq = 15,
                ),
              ),
            ),
          ),
        ),
      ),
    )
    val adjudicationThreeConcurrentToOneQuashed = AdjudicationDetail(
      adjudicationNumber = 1525918,
      incidentTime = LocalDateTime.parse("2023-08-01T09:00:00"),
      establishment = "Moorland (HMP & YOI)",
      interiorLocation = "Circuit",
      incidentDetails = "some details",
      reportNumber = 1503215,
      reportType = "Governor's Report",
      reporterFirstName = "TIM",
      reporterLastName = "WRIGHT",
      reportTime = LocalDateTime.parse("2023-08-02T09:09:00"),
      hearings = listOf(
        Hearing(
          hearingTime = LocalDateTime.parse("2023-08-03T16:45:00"),
          establishment = "Moorland (HMP & YOI)",
          results = listOf(
            HearingResult(
              offenceType = "Prison Rule 51",
              offenceDescription = "Intentionally or recklessly sets fire to any part of a prison or any other property, whether or not his own",
              plea = "Guilty",
              finding = "Charge Proved",
              sanctions = listOf(
                Sanction(
                  sanctionType = "Additional Days Added",
                  sanctionDays = 5,
                  status = "Quashed",
                  sanctionSeq = 17,
                ),
              ),
            ),
          ),
        ),
      ),
    )
    val adjudicationOneProspective = AdjudicationDetail(
      adjudicationNumber = 1525916,
      incidentTime = LocalDateTime.parse("2023-08-01T09:00:00"),
      establishment = "Moorland (HMP & YOI)",
      interiorLocation = "Circuit",
      incidentDetails = "some details",
      reportNumber = 1503215,
      reportType = "Governor's Report",
      reporterFirstName = "TIM",
      reporterLastName = "WRIGHT",
      reportTime = LocalDateTime.parse("2023-08-02T09:09:00"),
      hearings = listOf(
        Hearing(
          hearingTime = LocalDateTime.parse("2023-08-03T16:45:00"),
          establishment = "Moorland (HMP & YOI)",
          results = listOf(
            HearingResult(
              offenceType = "Prison Rule 51",
              offenceDescription = "Intentionally or recklessly sets fire to any part of a prison or any other property, whether or not his own",
              plea = "Guilty",
              finding = "Charge Proved",
              sanctions = listOf(
                Sanction(
                  sanctionType = "Additional Days Added",
                  sanctionDays = 10,
                  status = "Prospective",
                  sanctionSeq = 15,
                ),
              ),
            ),
          ),
        ),
      ),
    )

    private val oneAdjudicationSearchResponse = AdjudicationSearchResponse(
      listOf(
        adjudication1SearchResponse,
      ),
    )
    private val threeAdjudicationsSearchResponse = AdjudicationSearchResponse(
      listOf(
        adjudication1SearchResponse,
        adjudication2SearchResponse,
        adjudication3SearchResponse,
      ),
    )
  }
}
