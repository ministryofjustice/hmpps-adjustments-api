package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.adjustments.api.client.AdjudicationApiClient
import uk.gov.justice.digital.hmpps.adjustments.api.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.adjustments.api.client.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdditionalDaysAwarded
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjudicationCharges
import uk.gov.justice.digital.hmpps.adjustments.api.entity.Adjustment
import uk.gov.justice.digital.hmpps.adjustments.api.entity.AdjustmentType.ADDITIONAL_DAYS_AWARDED
import uk.gov.justice.digital.hmpps.adjustments.api.entity.ProspectiveAdaRejection
import uk.gov.justice.digital.hmpps.adjustments.api.enums.AdaStatus
import uk.gov.justice.digital.hmpps.adjustments.api.enums.ChargeStatus
import uk.gov.justice.digital.hmpps.adjustments.api.enums.InterceptType.FIRST_TIME
import uk.gov.justice.digital.hmpps.adjustments.api.enums.InterceptType.FIRST_TIME_WITH_NO_ADJUDICATION
import uk.gov.justice.digital.hmpps.adjustments.api.enums.InterceptType.NONE
import uk.gov.justice.digital.hmpps.adjustments.api.enums.InterceptType.PADA
import uk.gov.justice.digital.hmpps.adjustments.api.enums.InterceptType.PADAS
import uk.gov.justice.digital.hmpps.adjustments.api.enums.InterceptType.UPDATE
import uk.gov.justice.digital.hmpps.adjustments.api.model.additionaldays.Ada
import uk.gov.justice.digital.hmpps.adjustments.api.model.additionaldays.AdaAdjudicationDetails
import uk.gov.justice.digital.hmpps.adjustments.api.model.additionaldays.AdaIntercept
import uk.gov.justice.digital.hmpps.adjustments.api.model.additionaldays.AdasByDateCharged
import uk.gov.justice.digital.hmpps.adjustments.api.model.adjudications.Adjudication
import uk.gov.justice.digital.hmpps.adjustments.api.model.adjudications.AdjudicationResponse
import uk.gov.justice.digital.hmpps.adjustments.api.model.adjudications.Hearing
import uk.gov.justice.digital.hmpps.adjustments.api.model.adjudications.OutcomeAndHearing
import uk.gov.justice.digital.hmpps.adjustments.api.model.adjudications.Punishment
import uk.gov.justice.digital.hmpps.adjustments.api.model.adjudications.Schedule
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.Prison
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi.SentenceAndOffences
import uk.gov.justice.digital.hmpps.adjustments.api.model.prisonersearchapi.Prisoner
import uk.gov.justice.digital.hmpps.adjustments.api.respository.AdjustmentRepository
import uk.gov.justice.digital.hmpps.adjustments.api.respository.ProspectiveAdaRejectionRepository
import java.time.LocalDate
import java.time.LocalDateTime

class AdditionalDaysAwardedServiceTest {

  private val prisonService = mock<PrisonService>()
  private val adjustmentRepository = mock<AdjustmentRepository>()
  private val prisonApiClient = mock<PrisonApiClient>()
  private val prisonerSearchApiClient = mock<PrisonerSearchApiClient>()
  private val prospectiveAdaRejectionRepository = mock<ProspectiveAdaRejectionRepository>()
  private val adjudicationApiClient = mock<AdjudicationApiClient>()
  private val adjudicationsLookupService = AdjudicationsLookupService(adjudicationApiClient, prisonApiClient)

  private val additionalDaysAwardedService =
    AdditionalDaysAwardedService(prisonService, adjustmentRepository, prospectiveAdaRejectionRepository, prisonApiClient, prisonerSearchApiClient, adjudicationsLookupService)

  @BeforeEach
  fun setup() {
    whenever(prisonerSearchApiClient.findByPrisonerNumber(NOMS_ID)).thenReturn(
      Prisoner(
        bookingId = 123,
        prisonerNumber = NOMS_ID,
        firstName = "DEFAULT",
        lastName = "PRISONER",
        dateOfBirth = LocalDate.parse("1995-03-08"),
      ),
    )
    whenever(prisonApiClient.getPrison("MOR")).thenReturn(Prison(agencyId = "MOR", description = "Moorland (HMP & YOI)"))
  }

  @Nested
  inner class AdjudicationDetailsTests {

    @Test
    fun `Ada adjudication details where mix of consec concurrent`() {
      whenever(
        adjustmentRepository.findByPersonAndAdjustmentTypeAndStatusAndCurrentPeriodOfCustody(
          NOMS_ID,
          ADDITIONAL_DAYS_AWARDED,
        ),
      ).thenReturn(emptyList())

      whenever(adjudicationApiClient.getAdjudications(NOMS_ID)).thenReturn(AdjudicationResponse(listOf(adjudicationOne, adjudicationTwoConsecutiveToOne, adjudicationThreeConcurrentToOne)))
      whenever(prospectiveAdaRejectionRepository.findByPerson(NOMS_ID)).thenReturn(emptyList())
      whenever(prisonService.getSentencesAndStartDateDetails(NOMS_ID)).thenReturn(defaultSentenceDetail)

      val adaAdjudicationDetails = additionalDaysAwardedService.getAdaAdjudicationDetails(NOMS_ID)

      assertThat(
        adaAdjudicationDetails,
      ).isEqualTo(
        AdaAdjudicationDetails(
          awaitingApproval = listOf(
            AdasByDateCharged(
              dateChargeProved = LocalDate.of(2023, 8, 3),
              charges = mutableListOf(
                Ada(
                  dateChargeProved = LocalDate.of(2023, 8, 3),
                  chargeNumber = "MOR-1525916",
                  toBeServed = "Forthwith",
                  heardAt = "Moorland (HMP & YOI)",
                  status = ChargeStatus.AWARDED_OR_PENDING,
                  days = 5,
                  consecutiveToChargeNumber = null,
                ),
                Ada(
                  dateChargeProved = LocalDate.of(2023, 8, 3),
                  chargeNumber = "MOR-1525917",
                  toBeServed = "Consecutive to MOR-1525916",
                  heardAt = "Moorland (HMP & YOI)",
                  status = ChargeStatus.AWARDED_OR_PENDING,
                  days = 5,
                  consecutiveToChargeNumber = "MOR-1525916",
                ),
                Ada(
                  dateChargeProved = LocalDate.of(2023, 8, 3),
                  chargeNumber = "MOR-1525918",
                  toBeServed = "Concurrent",
                  heardAt = "Moorland (HMP & YOI)",
                  status = ChargeStatus.AWARDED_OR_PENDING,
                  days = 5,
                  consecutiveToChargeNumber = null,
                ),
              ),
              total = 10,
              status = AdaStatus.PENDING_APPROVAL,
              adjustmentId = null,
            ),
          ),
          totalAwaitingApproval = 10,
          intercept = AdaIntercept(type = UPDATE, number = 1, anyProspective = false, messageArguments = listOf()),
          earliestNonRecallSentenceDate = sentenceDate,
        ),
      )
    }

    @Test
    fun `Ada adjudication details where all awarded`() {
      whenever(
        adjustmentRepository.findByPersonAndAdjustmentTypeAndStatusAndCurrentPeriodOfCustody(
          NOMS_ID,
          ADDITIONAL_DAYS_AWARDED,
        ),
      ).thenReturn(listOf(BASE_10_DAY_ADJUSTMENT))
      whenever(adjudicationApiClient.getAdjudications(NOMS_ID)).thenReturn(AdjudicationResponse(listOf(adjudicationOne, adjudicationTwoConsecutiveToOne, adjudicationThreeConcurrentToOne)))
      whenever(prospectiveAdaRejectionRepository.findByPerson(NOMS_ID)).thenReturn(emptyList())
      whenever(prisonService.getSentencesAndStartDateDetails(NOMS_ID)).thenReturn(defaultSentenceDetail)

      val adaAdjudicationDetails = additionalDaysAwardedService.getAdaAdjudicationDetails(NOMS_ID)

      assertThat(
        adaAdjudicationDetails,
      ).isEqualTo(
        AdaAdjudicationDetails(
          awarded = listOf(
            AdasByDateCharged(
              dateChargeProved = LocalDate.of(2023, 8, 3),
              charges = mutableListOf(
                Ada(
                  dateChargeProved = LocalDate.of(2023, 8, 3),
                  chargeNumber = "MOR-1525916",
                  toBeServed = "Forthwith",
                  heardAt = "Moorland (HMP & YOI)",
                  status = ChargeStatus.AWARDED_OR_PENDING,
                  days = 5,
                  consecutiveToChargeNumber = null,
                ),
                Ada(
                  dateChargeProved = LocalDate.of(2023, 8, 3),
                  chargeNumber = "MOR-1525917",
                  toBeServed = "Consecutive to MOR-1525916",
                  heardAt = "Moorland (HMP & YOI)",
                  status = ChargeStatus.AWARDED_OR_PENDING,
                  days = 5,
                  consecutiveToChargeNumber = "MOR-1525916",
                ),
                Ada(
                  dateChargeProved = LocalDate.of(2023, 8, 3),
                  chargeNumber = "MOR-1525918",
                  toBeServed = "Concurrent",
                  heardAt = "Moorland (HMP & YOI)",
                  status = ChargeStatus.AWARDED_OR_PENDING,
                  days = 5,
                  consecutiveToChargeNumber = null,
                ),
              ),
              total = 10,
              status = AdaStatus.AWARDED,
              adjustmentId = BASE_10_DAY_ADJUSTMENT.id,
            ),
          ),
          totalAwarded = 10,
          intercept = AdaIntercept(type = NONE, number = 0, anyProspective = false, messageArguments = listOf()),
          earliestNonRecallSentenceDate = sentenceDate,
          totalExistingAdas = 10,
          showExistingAdaMessage = false,
        ),
      )
    }

    @Test
    fun `Ada adjudication details prospective and selected`() {
      whenever(
        adjustmentRepository.findByPersonAndAdjustmentTypeAndStatusAndCurrentPeriodOfCustody(
          NOMS_ID,
          ADDITIONAL_DAYS_AWARDED,
        ),
      ).thenReturn(
        emptyList(),
      )

      whenever(adjudicationApiClient.getAdjudications(NOMS_ID)).thenReturn(AdjudicationResponse(listOf(adjudicationOneProspective)))
      whenever(prospectiveAdaRejectionRepository.findByPerson(NOMS_ID)).thenReturn(emptyList())
      whenever(prisonService.getSentencesAndStartDateDetails(NOMS_ID)).thenReturn(defaultSentenceDetail)

      val adaAdjudicationDetails = additionalDaysAwardedService.getAdaAdjudicationDetails(NOMS_ID, selectedProspectiveAdaDates = listOf("2023-08-03"))

      assertThat(
        adaAdjudicationDetails,
      ).isEqualTo(
        AdaAdjudicationDetails(
          prospective = listOf(
            AdasByDateCharged(
              dateChargeProved = LocalDate.of(2023, 8, 3),
              charges = mutableListOf(
                Ada(
                  dateChargeProved = LocalDate.of(2023, 8, 3),
                  chargeNumber = "MOR-1525916",
                  toBeServed = "Forthwith",
                  heardAt = "Moorland (HMP & YOI)",
                  status = ChargeStatus.PROSPECTIVE,
                  days = 10,
                  consecutiveToChargeNumber = null,
                ),
              ),
              total = 10,
              status = AdaStatus.PENDING_APPROVAL,
              adjustmentId = null,
            ),
          ),
          totalProspective = 10,
          awaitingApproval = listOf(
            AdasByDateCharged(
              dateChargeProved = LocalDate.of(2023, 8, 3),
              charges = mutableListOf(
                Ada(
                  dateChargeProved = LocalDate.of(2023, 8, 3),
                  chargeNumber = "MOR-1525916",
                  toBeServed = "Forthwith",
                  heardAt = "Moorland (HMP & YOI)",
                  status = ChargeStatus.PROSPECTIVE,
                  days = 10,
                  consecutiveToChargeNumber = null,
                ),
              ),
              total = 10,
              status = AdaStatus.PENDING_APPROVAL,
              adjustmentId = null,
            ),
          ),
          totalAwaitingApproval = 10,
          intercept = AdaIntercept(type = UPDATE, number = 1, anyProspective = true, messageArguments = listOf()),
          earliestNonRecallSentenceDate = sentenceDate,
        ),
      )
    }

    @Test
    fun `Ada adjudication details prospective and awarded`() {
      whenever(
        adjustmentRepository.findByPersonAndAdjustmentTypeAndStatusAndCurrentPeriodOfCustody(
          NOMS_ID,
          ADDITIONAL_DAYS_AWARDED,
        ),
      ).thenReturn(
        listOf(
          BASE_10_DAY_ADJUSTMENT.copy(
            additionalDaysAwarded = AdditionalDaysAwarded(
              adjudicationCharges = mutableListOf(
                AdjudicationCharges("MOR-1525916"),
              ),
            ),
          ),
        ),
      )
      whenever(adjudicationApiClient.getAdjudications(NOMS_ID)).thenReturn(AdjudicationResponse(listOf(adjudicationOneProspective)))
      whenever(prospectiveAdaRejectionRepository.findByPerson(NOMS_ID)).thenReturn(emptyList())
      whenever(prisonService.getSentencesAndStartDateDetails(NOMS_ID)).thenReturn(defaultSentenceDetail)

      val adaAdjudicationDetails = additionalDaysAwardedService.getAdaAdjudicationDetails(NOMS_ID, selectedProspectiveAdaDates = listOf("2023-08-03"))

      assertThat(
        adaAdjudicationDetails,
      ).isEqualTo(
        AdaAdjudicationDetails(
          awarded = listOf(
            AdasByDateCharged(
              dateChargeProved = LocalDate.of(2023, 8, 3),
              charges = mutableListOf(
                Ada(
                  dateChargeProved = LocalDate.of(2023, 8, 3),
                  chargeNumber = "MOR-1525916",
                  toBeServed = "Forthwith",
                  heardAt = "Moorland (HMP & YOI)",
                  status = ChargeStatus.PROSPECTIVE,
                  days = 10,
                  consecutiveToChargeNumber = null,
                ),
              ),
              total = 10,
              status = AdaStatus.AWARDED,
              adjustmentId = BASE_10_DAY_ADJUSTMENT.id,
            ),
          ),
          totalAwarded = 10,
          totalExistingAdas = 10,
          intercept = AdaIntercept(type = NONE, number = 0, anyProspective = false, messageArguments = listOf()),
          earliestNonRecallSentenceDate = sentenceDate,
        ),
      )
    }

    @Test
    fun `Ada adjudication details ADAs fall in determinate period`() {
      whenever(
        adjustmentRepository.findByPersonAndAdjustmentTypeAndStatusAndCurrentPeriodOfCustody(
          NOMS_ID,
          ADDITIONAL_DAYS_AWARDED,
        ),
      ).thenReturn(emptyList())

      whenever(adjudicationApiClient.getAdjudications(NOMS_ID)).thenReturn(AdjudicationResponse(listOf(adjudicationOne, adjudicationTwoConsecutiveToOne, adjudicationThreeConcurrentToOne)))
      whenever(prospectiveAdaRejectionRepository.findByPerson(NOMS_ID)).thenReturn(emptyList())
      whenever(prisonService.getSentencesAndStartDateDetails(NOMS_ID)).thenReturn(recallSentenceDetail)

      val adaAdjudicationDetails = additionalDaysAwardedService.getAdaAdjudicationDetails(NOMS_ID)

      assertThat(
        adaAdjudicationDetails,
      ).isEqualTo(
        AdaAdjudicationDetails(
          intercept = AdaIntercept(NONE, 0, false, emptyList()),
          earliestNonRecallSentenceDate = null,
          showExistingAdaMessage = true,
          earliestRecallDate = recallDate,
        ),
      )
    }

    @Test
    fun `Ada adjudication details ADAs fall in determinate period of parallel sentences`() {
      whenever(
        adjustmentRepository.findByPersonAndAdjustmentTypeAndStatusAndCurrentPeriodOfCustody(
          NOMS_ID,
          ADDITIONAL_DAYS_AWARDED,
        ),
      ).thenReturn(emptyList())

      whenever(adjudicationApiClient.getAdjudications(NOMS_ID)).thenReturn(AdjudicationResponse(listOf(adjudicationOne, adjudicationTwoConsecutiveToOne, adjudicationThreeConcurrentToOne)))
      whenever(prospectiveAdaRejectionRepository.findByPerson(NOMS_ID)).thenReturn(emptyList())
      whenever(prisonService.getSentencesAndStartDateDetails(NOMS_ID)).thenReturn(parallelSentenceDetail)

      val adaAdjudicationDetails = additionalDaysAwardedService.getAdaAdjudicationDetails(NOMS_ID)

      assertThat(
        adaAdjudicationDetails,
      ).isEqualTo(
        AdaAdjudicationDetails(
          awaitingApproval = listOf(
            AdasByDateCharged(
              dateChargeProved = LocalDate.of(2023, 8, 3),
              charges = mutableListOf(
                Ada(
                  dateChargeProved = LocalDate.of(2023, 8, 3),
                  chargeNumber = "MOR-1525916",
                  toBeServed = "Forthwith",
                  heardAt = "Moorland (HMP & YOI)",
                  status = ChargeStatus.AWARDED_OR_PENDING,
                  days = 5,
                  consecutiveToChargeNumber = null,
                ),
                Ada(
                  dateChargeProved = LocalDate.of(2023, 8, 3),
                  chargeNumber = "MOR-1525917",
                  toBeServed = "Consecutive to MOR-1525916",
                  heardAt = "Moorland (HMP & YOI)",
                  status = ChargeStatus.AWARDED_OR_PENDING,
                  days = 5,
                  consecutiveToChargeNumber = "MOR-1525916",
                ),
                Ada(
                  dateChargeProved = LocalDate.of(2023, 8, 3),
                  chargeNumber = "MOR-1525918",
                  toBeServed = "Concurrent",
                  heardAt = "Moorland (HMP & YOI)",
                  status = ChargeStatus.AWARDED_OR_PENDING,
                  days = 5,
                  consecutiveToChargeNumber = null,
                ),
              ),
              total = 10,
              status = AdaStatus.PENDING_APPROVAL,
              adjustmentId = null,
            ),
          ),
          totalAwaitingApproval = 10,
          intercept = AdaIntercept(type = UPDATE, number = 1, anyProspective = false, messageArguments = listOf()),
          earliestNonRecallSentenceDate = sentenceDate,
          earliestRecallDate = recallDate,
        ),
      )
    }

    @Test
    fun `NOMIS adjustment but no adjudication`() {
      whenever(
        adjustmentRepository.findByPersonAndAdjustmentTypeAndStatusAndCurrentPeriodOfCustody(
          NOMS_ID,
          ADDITIONAL_DAYS_AWARDED,
        ),
      ).thenReturn(listOf(BASE_10_DAY_ADJUSTMENT.copy(additionalDaysAwarded = null)))
      whenever(adjudicationApiClient.getAdjudications(NOMS_ID)).thenReturn(AdjudicationResponse(listOf()))
      whenever(prospectiveAdaRejectionRepository.findByPerson(NOMS_ID)).thenReturn(emptyList())
      whenever(prisonService.getSentencesAndStartDateDetails(NOMS_ID)).thenReturn(defaultSentenceDetail)

      val adaAdjudicationDetails = additionalDaysAwardedService.getAdaAdjudicationDetails(NOMS_ID)

      assertThat(
        adaAdjudicationDetails,
      ).isEqualTo(
        AdaAdjudicationDetails(
          intercept = AdaIntercept(type = FIRST_TIME_WITH_NO_ADJUDICATION, number = 0, anyProspective = false, messageArguments = listOf()),
          totalExistingAdas = 10,
          showExistingAdaMessage = true,
          earliestNonRecallSentenceDate = sentenceDate,
        ),
      )
    }

    @Test
    fun `Should handle missing punishments`() {
      whenever(adjudicationApiClient.getAdjudications(NOMS_ID)).thenReturn(
        AdjudicationResponse(
          listOf(
            adjudicationOne.copy(
              punishments = listOf(adjudicationOne.punishments[0].copy(type = "NOT_AN_ADDITIONAL_DAY")),
            ),
          ),
        ),
      )
      whenever(prospectiveAdaRejectionRepository.findByPerson(NOMS_ID)).thenReturn(emptyList())
      whenever(prisonService.getSentencesAndStartDateDetails(NOMS_ID)).thenReturn(defaultSentenceDetail)

      val adaAdjudicationDetails = additionalDaysAwardedService.getAdaAdjudicationDetails(NOMS_ID)

      assertThat(
        adaAdjudicationDetails,
      ).isEqualTo(
        AdaAdjudicationDetails(
          earliestNonRecallSentenceDate = sentenceDate,
          showExistingAdaMessage = true,
        ),
      )
    }

    @Test
    fun `When a loop of consecutive adjudications is formed, it is handled correctly`() {
      val adjudicationOneConsecutiveToTwoCreatesLoop = adjudicationOne.copy(
        punishments = listOf(
          Punishment(
            type = "ADDITIONAL_DAYS",
            schedule = Schedule(15, null),
            consecutiveChargeNumber = "MOR-1525917",
          ),
        ),
      )

      whenever(
        adjustmentRepository.findByPersonAndAdjustmentTypeAndStatusAndCurrentPeriodOfCustody(
          NOMS_ID,
          ADDITIONAL_DAYS_AWARDED,
        ),
      ).thenReturn(
        emptyList(),
      )
      whenever(adjudicationApiClient.getAdjudications(NOMS_ID)).thenReturn(
        AdjudicationResponse(
          listOf(
            adjudicationOneConsecutiveToTwoCreatesLoop,
            adjudicationTwoConsecutiveToOne,
          ),
        ),
      )
      whenever(prospectiveAdaRejectionRepository.findByPerson(NOMS_ID)).thenReturn(emptyList())
      whenever(prisonService.getSentencesAndStartDateDetails(NOMS_ID)).thenReturn(defaultSentenceDetail)

      assertThat(additionalDaysAwardedService.getAdaAdjudicationDetails(NOMS_ID).totalAwaitingApproval).isEqualTo(20)
    }

    @Test
    fun `Two consecutive adjudications are handled correctly`() {
      val adjudicationThreeConsecutiveToOne = adjudicationOne.copy(
        chargeNumber = "MOR-1525918",
        punishments = listOf(
          Punishment(
            type = "ADDITIONAL_DAYS",
            schedule = Schedule(12, null),
            consecutiveChargeNumber = "MOR-1525916",
          ),
        ),
      )

      whenever(
        adjustmentRepository.findByPersonAndAdjustmentTypeAndStatusAndCurrentPeriodOfCustody(
          NOMS_ID,
          ADDITIONAL_DAYS_AWARDED,
        ),
      ).thenReturn(
        emptyList(),
      )
      whenever(adjudicationApiClient.getAdjudications(NOMS_ID)).thenReturn(
        AdjudicationResponse(
          listOf(adjudicationOne, adjudicationTwoConsecutiveToOne, adjudicationThreeConsecutiveToOne),
        ),
      )
      whenever(prospectiveAdaRejectionRepository.findByPerson(NOMS_ID)).thenReturn(emptyList())
      whenever(prisonService.getSentencesAndStartDateDetails(NOMS_ID)).thenReturn(defaultSentenceDetail)

      assertThat(additionalDaysAwardedService.getAdaAdjudicationDetails(NOMS_ID).totalAwaitingApproval).isEqualTo(17)
    }
  }

  @Nested
  inner class InterceptTests {

    @Test
    fun `Should not intercept if no sentence date`() {
      whenever(prisonService.getSentencesAndStartDateDetails(NOMS_ID)).thenReturn(
        SentenceAndStartDateDetails(),
      )

      val intercept = additionalDaysAwardedService.getAdaAdjudicationDetails(NOMS_ID).intercept

      assertThat(intercept).isEqualTo(AdaIntercept(NONE, 0, false, emptyList()))
    }

    @Test
    fun `Should not intercept if no missing recall outcome date, only recalls`() {
      whenever(prisonService.getSentencesAndStartDateDetails(NOMS_ID)).thenReturn(
        recallSentenceDetail.copy(
          earliestRecallDate = null,
        ),
      )
      whenever(
        adjustmentRepository.findByPersonAndAdjustmentTypeAndStatusAndCurrentPeriodOfCustody(
          NOMS_ID,
          ADDITIONAL_DAYS_AWARDED,
        ),
      ).thenReturn(emptyList())
      whenever(adjudicationApiClient.getAdjudications(NOMS_ID)).thenReturn(AdjudicationResponse(listOf(adjudicationOne, adjudicationTwoConsecutiveToOne, adjudicationThreeConcurrentToOne)))

      val details = additionalDaysAwardedService.getAdaAdjudicationDetails(NOMS_ID)

      assertThat(details.intercept).isEqualTo(AdaIntercept(NONE, 0, false, emptyList()))
      assertThat(details.recallWithMissingOutcome).isEqualTo(true)
    }

    @Test
    fun `Should not intercept if no missing recall outcome date but no adjudications`() {
      whenever(prisonService.getSentencesAndStartDateDetails(NOMS_ID)).thenReturn(
        recallSentenceDetail.copy(
          earliestRecallDate = null,
        ),
      )
      whenever(
        adjustmentRepository.findByPersonAndAdjustmentTypeAndStatusAndCurrentPeriodOfCustody(
          NOMS_ID,
          ADDITIONAL_DAYS_AWARDED,
        ),
      ).thenReturn(emptyList())
      whenever(adjudicationApiClient.getAdjudications(NOMS_ID)).thenReturn(AdjudicationResponse(emptyList()))

      val details = additionalDaysAwardedService.getAdaAdjudicationDetails(NOMS_ID)

      assertThat(details.intercept).isEqualTo(AdaIntercept(NONE, 0, false, emptyList()))
      assertThat(details.recallWithMissingOutcome).isEqualTo(false)
    }

    @Test
    fun `Should not intercept if no missing recall outcome date and ada before parallel sentence`() {
      whenever(prisonService.getSentencesAndStartDateDetails(NOMS_ID)).thenReturn(
        parallelSentenceDetail.copy(
          earliestRecallDate = null,
          earliestNonRecallSentenceDate = sentenceDate.plusYears(2),
        ),
      )
      whenever(
        adjustmentRepository.findByPersonAndAdjustmentTypeAndStatusAndCurrentPeriodOfCustody(
          NOMS_ID,
          ADDITIONAL_DAYS_AWARDED,
        ),
      ).thenReturn(emptyList())
      whenever(adjudicationApiClient.getAdjudications(NOMS_ID)).thenReturn(AdjudicationResponse(listOf(adjudicationOne, adjudicationTwoConsecutiveToOne, adjudicationThreeConcurrentToOne)))

      val details = additionalDaysAwardedService.getAdaAdjudicationDetails(NOMS_ID)

      assertThat(details.intercept).isEqualTo(AdaIntercept(NONE, 0, false, emptyList()))
      assertThat(details.recallWithMissingOutcome).isEqualTo(true)
    }

    @Test
    fun `Should intercept if any unlinked ADAs`() {
      whenever(
        adjustmentRepository.findByPersonAndAdjustmentTypeAndStatusAndCurrentPeriodOfCustody(
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
      whenever(adjudicationApiClient.getAdjudications(NOMS_ID)).thenReturn(AdjudicationResponse(listOf(adjudicationOne, adjudicationTwoConsecutiveToOne, adjudicationThreeConcurrentToOne)))
      whenever(prospectiveAdaRejectionRepository.findByPerson(NOMS_ID)).thenReturn(emptyList())
      whenever(prisonService.getSentencesAndStartDateDetails(NOMS_ID)).thenReturn(defaultSentenceDetail)

      val intercept = additionalDaysAwardedService.getAdaAdjudicationDetails(NOMS_ID).intercept

      assertThat(intercept).isEqualTo(AdaIntercept(FIRST_TIME, 1, false, emptyList()))
      assertThat(intercept.message).isEqualTo("This service has identified ADA adjustments that were created in NOMIS. You must review the adjudications with ADAs and approve them in this service.")
    }

    @Test
    fun `Should return First Time intercept if any unlinked ADAs because there is no additionalDaysAwarded object associated to the ADA`() {
      whenever(
        adjustmentRepository.findByPersonAndAdjustmentTypeAndStatusAndCurrentPeriodOfCustody(
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
      whenever(adjudicationApiClient.getAdjudications(NOMS_ID)).thenReturn(AdjudicationResponse(listOf(adjudicationOne, adjudicationTwoConsecutiveToOne, adjudicationThreeConcurrentToOne)))
      whenever(prospectiveAdaRejectionRepository.findByPerson(NOMS_ID)).thenReturn(emptyList())
      whenever(prisonService.getSentencesAndStartDateDetails(NOMS_ID)).thenReturn(defaultSentenceDetail)

      val intercept = additionalDaysAwardedService.getAdaAdjudicationDetails(NOMS_ID).intercept

      assertThat(intercept).isEqualTo(AdaIntercept(FIRST_TIME, 1, false, emptyList()))
      assertThat(intercept.message).isEqualTo("This service has identified ADA adjustments that were created in NOMIS. You must review the adjudications with ADAs and approve them in this service.")
    }

    @Test
    fun `Should intercept if there is a difference between the totalDays in adjustments and adjudications`() {
      whenever(
        adjustmentRepository.findByPersonAndAdjustmentTypeAndStatusAndCurrentPeriodOfCustody(
          NOMS_ID,
          ADDITIONAL_DAYS_AWARDED,
        ),
      ).thenReturn(
        listOf(BASE_10_DAY_ADJUSTMENT.copy(days = 11, effectiveDays = 11)),
      )
      whenever(adjudicationApiClient.getAdjudications(NOMS_ID)).thenReturn(AdjudicationResponse(listOf(adjudicationOne, adjudicationTwoConsecutiveToOne, adjudicationThreeConcurrentToOne)))
      whenever(prospectiveAdaRejectionRepository.findByPerson(NOMS_ID)).thenReturn(emptyList())
      whenever(prisonService.getSentencesAndStartDateDetails(NOMS_ID)).thenReturn(defaultSentenceDetail)

      val intercept = additionalDaysAwardedService.getAdaAdjudicationDetails(NOMS_ID).intercept

      assertUpdateIntercept(intercept, 1, false)
    }

    @Test
    fun `Should intercept mix of concurrent consec`() {
      whenever(
        adjustmentRepository.findByPersonAndAdjustmentTypeAndStatusAndCurrentPeriodOfCustody(
          NOMS_ID,
          ADDITIONAL_DAYS_AWARDED,
        ),
      ).thenReturn(emptyList())
      whenever(adjudicationApiClient.getAdjudications(NOMS_ID)).thenReturn(AdjudicationResponse(listOf(adjudicationOne, adjudicationTwoConsecutiveToOne, adjudicationThreeConcurrentToOne)))
      whenever(prospectiveAdaRejectionRepository.findByPerson(NOMS_ID)).thenReturn(emptyList())
      whenever(prisonService.getSentencesAndStartDateDetails(NOMS_ID)).thenReturn(defaultSentenceDetail)

      val intercept = additionalDaysAwardedService.getAdaAdjudicationDetails(NOMS_ID).intercept

      assertUpdateIntercept(intercept, 1, false)
    }

    @Test
    fun `Shouldnt intercept when already persisted in adjustment api`() {
      whenever(
        adjustmentRepository.findByPersonAndAdjustmentTypeAndStatusAndCurrentPeriodOfCustody(
          NOMS_ID,
          ADDITIONAL_DAYS_AWARDED,
        ),
      ).thenReturn(
        listOf(BASE_10_DAY_ADJUSTMENT),
      )
      whenever(adjudicationApiClient.getAdjudications(NOMS_ID)).thenReturn(AdjudicationResponse(listOf(adjudicationOne, adjudicationTwoConsecutiveToOne, adjudicationThreeConcurrentToOne)))
      whenever(prisonService.getSentencesAndStartDateDetails(NOMS_ID)).thenReturn(defaultSentenceDetail)

      val intercept = additionalDaysAwardedService.getAdaAdjudicationDetails(NOMS_ID).intercept

      assertThat(intercept).isEqualTo(AdaIntercept(NONE, 0, false))
    }

    @Test
    fun `Should intercept when already persisted adjustment has different days`() {
      whenever(
        adjustmentRepository.findByPersonAndAdjustmentTypeAndStatusAndCurrentPeriodOfCustody(
          NOMS_ID,
          ADDITIONAL_DAYS_AWARDED,
        ),
      ).thenReturn(
        listOf(BASE_10_DAY_ADJUSTMENT.copy(days = 5, effectiveDays = 5)),
      )
      whenever(adjudicationApiClient.getAdjudications(NOMS_ID)).thenReturn(AdjudicationResponse(listOf(adjudicationOne, adjudicationTwoConsecutiveToOne, adjudicationThreeConcurrentToOne)))
      whenever(prospectiveAdaRejectionRepository.findByPerson(NOMS_ID)).thenReturn(emptyList())
      whenever(prisonService.getSentencesAndStartDateDetails(NOMS_ID)).thenReturn(defaultSentenceDetail)

      val intercept = additionalDaysAwardedService.getAdaAdjudicationDetails(NOMS_ID).intercept

      assertUpdateIntercept(intercept, 1, false)
    }

    @Test
    fun `Should intercept if all adas quashed`() {
      whenever(
        adjustmentRepository.findByPersonAndAdjustmentTypeAndStatusAndCurrentPeriodOfCustody(
          NOMS_ID,
          ADDITIONAL_DAYS_AWARDED,
        ),
      ).thenReturn(
        listOf(BASE_10_DAY_ADJUSTMENT),
      )
      whenever(adjudicationApiClient.getAdjudications(NOMS_ID)).thenReturn(
        AdjudicationResponse(
          listOf(
            adjudicationOneQuashed,
            adjudicationTwoConsecutiveToOneQuashed,
            adjudicationThreeConcurrentToOneQuashed,
          ),
        ),
      )
      whenever(prospectiveAdaRejectionRepository.findByPerson(NOMS_ID)).thenReturn(emptyList())

      whenever(prisonService.getSentencesAndStartDateDetails(NOMS_ID)).thenReturn(defaultSentenceDetail)

      val intercept = additionalDaysAwardedService.getAdaAdjudicationDetails(NOMS_ID).intercept

      assertUpdateIntercept(intercept, 1, false)
    }

    @Test
    fun `Should intercept if any prospective`() {
      whenever(
        adjustmentRepository.findByPersonAndAdjustmentTypeAndStatusAndCurrentPeriodOfCustody(
          NOMS_ID,
          ADDITIONAL_DAYS_AWARDED,
        ),
      ).thenReturn(
        emptyList(),
      )
      whenever(adjudicationApiClient.getAdjudications(NOMS_ID)).thenReturn(AdjudicationResponse(listOf(adjudicationOneProspective)))
      whenever(prospectiveAdaRejectionRepository.findByPerson(NOMS_ID)).thenReturn(emptyList())
      whenever(prisonService.getSentencesAndStartDateDetails(NOMS_ID)).thenReturn(defaultSentenceDetail)

      val intercept = additionalDaysAwardedService.getAdaAdjudicationDetails(NOMS_ID).intercept

      assertPadaIntercept(intercept, 1, true)
    }

    @Test
    fun `Should intercept if any multiple prospective`() {
      whenever(
        adjustmentRepository.findByPersonAndAdjustmentTypeAndStatusAndCurrentPeriodOfCustody(
          NOMS_ID,
          ADDITIONAL_DAYS_AWARDED,
        ),
      ).thenReturn(
        emptyList(),
      )
      whenever(adjudicationApiClient.getAdjudications(NOMS_ID)).thenReturn(AdjudicationResponse(listOf(adjudicationOneProspective, adjudicationTwoProspective)))
      whenever(prospectiveAdaRejectionRepository.findByPerson(NOMS_ID)).thenReturn(emptyList())
      whenever(prisonService.getSentencesAndStartDateDetails(NOMS_ID)).thenReturn(defaultSentenceDetail)

      val intercept = additionalDaysAwardedService.getAdaAdjudicationDetails(NOMS_ID).intercept

      assertPadasIntercept(intercept, 2, true)
    }

    @Test
    fun `Shouldnt intercept if any prospective and rejected`() {
      whenever(
        adjustmentRepository.findByPersonAndAdjustmentTypeAndStatusAndCurrentPeriodOfCustody(
          NOMS_ID,
          ADDITIONAL_DAYS_AWARDED,
        ),
      ).thenReturn(
        emptyList(),
      )
      whenever(adjudicationApiClient.getAdjudications(NOMS_ID)).thenReturn(AdjudicationResponse(listOf(adjudicationOneProspective)))
      whenever(prospectiveAdaRejectionRepository.findByPerson(NOMS_ID)).thenReturn(listOf(ProspectiveAdaRejection(rejectionAt = sentenceDate.plusDays(5).atStartOfDay(), days = 10, dateChargeProved = LocalDate.of(2023, 8, 3))))
      whenever(prisonService.getSentencesAndStartDateDetails(NOMS_ID)).thenReturn(defaultSentenceDetail)

      val intercept = additionalDaysAwardedService.getAdaAdjudicationDetails(NOMS_ID).intercept

      assertThat(intercept.type).isEqualTo(NONE)
    }

    @Test
    fun `Shouldnt intercept if any prospective and rejected multiple times`() {
      whenever(
        adjustmentRepository.findByPersonAndAdjustmentTypeAndStatusAndCurrentPeriodOfCustody(
          NOMS_ID,
          ADDITIONAL_DAYS_AWARDED,
        ),
      ).thenReturn(
        emptyList(),
      )
      whenever(adjudicationApiClient.getAdjudications(NOMS_ID)).thenReturn(AdjudicationResponse(listOf(adjudicationOneProspective)))
      whenever(prospectiveAdaRejectionRepository.findByPerson(NOMS_ID)).thenReturn(listOf(ProspectiveAdaRejection(rejectionAt = sentenceDate.minusDays(5).atStartOfDay(), days = 10, dateChargeProved = LocalDate.of(2023, 8, 3))))
      whenever(prospectiveAdaRejectionRepository.findByPerson(NOMS_ID)).thenReturn(listOf(ProspectiveAdaRejection(rejectionAt = sentenceDate.plusDays(5).atStartOfDay(), days = 10, dateChargeProved = LocalDate.of(2023, 8, 3))))
      whenever(prisonService.getSentencesAndStartDateDetails(NOMS_ID)).thenReturn(defaultSentenceDetail)

      val intercept = additionalDaysAwardedService.getAdaAdjudicationDetails(NOMS_ID).intercept

      assertThat(intercept.type).isEqualTo(NONE)
    }

    @Test
    fun `Should intercept if any prospective and rejection before latest sentence`() {
      whenever(
        adjustmentRepository.findByPersonAndAdjustmentTypeAndStatusAndCurrentPeriodOfCustody(
          NOMS_ID,
          ADDITIONAL_DAYS_AWARDED,
        ),
      ).thenReturn(
        emptyList(),
      )
      whenever(adjudicationApiClient.getAdjudications(NOMS_ID)).thenReturn(AdjudicationResponse(listOf(adjudicationOneProspective)))
      whenever(prospectiveAdaRejectionRepository.findByPerson(NOMS_ID)).thenReturn(listOf(ProspectiveAdaRejection(rejectionAt = sentenceDate.minusDays(5).atStartOfDay(), days = 10, dateChargeProved = LocalDate.of(2023, 8, 3))))
      whenever(prisonService.getSentencesAndStartDateDetails(NOMS_ID)).thenReturn(defaultSentenceDetail)

      val intercept = additionalDaysAwardedService.getAdaAdjudicationDetails(NOMS_ID).intercept

      assertPadaIntercept(intercept, 1, true)
    }

    @Test
    fun `Should intercept if any prospective and rejection different days`() {
      whenever(
        adjustmentRepository.findByPersonAndAdjustmentTypeAndStatusAndCurrentPeriodOfCustody(
          NOMS_ID,
          ADDITIONAL_DAYS_AWARDED,
        ),
      ).thenReturn(
        emptyList(),
      )
      whenever(adjudicationApiClient.getAdjudications(NOMS_ID)).thenReturn(AdjudicationResponse(listOf(adjudicationOneProspective)))
      whenever(prospectiveAdaRejectionRepository.findByPerson(NOMS_ID)).thenReturn(listOf(ProspectiveAdaRejection(rejectionAt = LocalDate.of(2024, 1, 1).atStartOfDay(), days = 99, dateChargeProved = LocalDate.of(2023, 8, 3))))
      whenever(prisonService.getSentencesAndStartDateDetails(NOMS_ID)).thenReturn(defaultSentenceDetail)

      val intercept = additionalDaysAwardedService.getAdaAdjudicationDetails(NOMS_ID).intercept

      assertPadaIntercept(intercept, 1, true)
    }

    @Nested
    inner class FilterPADAsWithActiveLicenceRecalls {

      private fun setupMocks(
        adjudications: List<Adjudication>,
        hasRecall: Boolean,
        earliestNonRecallSentenceDate: LocalDate?,
        earliestRecallDate: LocalDate? = null,
      ) {
        whenever(adjustmentRepository.findByPersonAndAdjustmentTypeAndStatusAndCurrentPeriodOfCustody(NOMS_ID, ADDITIONAL_DAYS_AWARDED)).thenReturn(emptyList())
        whenever(adjudicationApiClient.getAdjudications(NOMS_ID)).thenReturn(AdjudicationResponse(adjudications))
        whenever(prospectiveAdaRejectionRepository.findByPerson(NOMS_ID)).thenReturn(emptyList())
        whenever(prisonService.getSentencesAndStartDateDetails(NOMS_ID)).thenReturn(
          SentenceAndStartDateDetails(
            sentences,
            earliestRecallDate = earliestRecallDate,
            latestSentenceDate = LocalDate.of(2023, 1, 1),
            earliestNonRecallSentenceDate = earliestNonRecallSentenceDate,
            hasRecall = hasRecall,
            earliestSentenceDate = LocalDate.of(2023, 1, 1),
          ),
        )
      }

      @Test
      fun `Should intercept if only prospective adas with non licence recalls`() {
        setupMocks(
          adjudications = listOf(adjudicationOneProspective),
          hasRecall = false,
          earliestNonRecallSentenceDate = LocalDate.of(2023, 1, 1),
        )

        val intercept = additionalDaysAwardedService.getAdaAdjudicationDetails(NOMS_ID).intercept

        assertPadaIntercept(intercept, 1, true)
      }

      @Test
      fun `Should not intercept if only prospective adas and licence recalls`() {
        setupMocks(
          adjudications = listOf(adjudicationOneProspective),
          hasRecall = true,
          earliestNonRecallSentenceDate = null,
          earliestRecallDate = LocalDate.of(2023, 1, 1),
        )

        val intercept = additionalDaysAwardedService.getAdaAdjudicationDetails(NOMS_ID).intercept

        assertThat(intercept).isEqualTo(
          AdaIntercept(
            NONE,
            0,
            false,
          ),
        )
      }

      @Test
      fun `Should intercept if there are prospective and none prospective adas with only licence recalls`() {
        setupMocks(
          adjudications = listOf(adjudicationOne, adjudicationOneProspective),
          hasRecall = true,
          earliestNonRecallSentenceDate = null,
          earliestRecallDate = LocalDate.of(2023, 1, 1),
        )

        val intercept = additionalDaysAwardedService.getAdaAdjudicationDetails(NOMS_ID).intercept

        assertThat(intercept).isEqualTo(
          AdaIntercept(
            UPDATE,
            1,
            false,
            messageArguments = listOf(),
          ),
        )
      }

      @Test
      fun `Should intercept if there are only prospective adas with licence recalls and other sentences`() {
        setupMocks(
          adjudications = listOf(adjudicationOneProspective),
          hasRecall = true,
          earliestNonRecallSentenceDate = LocalDate.of(2023, 2, 1),
          earliestRecallDate = LocalDate.of(2023, 1, 1),
        )

        val intercept = additionalDaysAwardedService.getAdaAdjudicationDetails(NOMS_ID).intercept

        assertThat(intercept).isEqualTo(
          AdaIntercept(
            PADA,
            1,
            true,
            messageArguments = listOf("Default Prisoner"),
          ),
        )
      }
    }

    private fun assertUpdateIntercept(intercept: AdaIntercept, number: Int, anyProspective: Boolean) {
      assertThat(intercept).isEqualTo(
        AdaIntercept(
          UPDATE,
          number,
          anyProspective,
          messageArguments = listOf(),
        ),
      )
      assertThat(intercept.message).isEqualTo("Updates have been made to ADA (Additional days awarded) information, which need to be approved.")
    }

    private fun assertPadaIntercept(intercept: AdaIntercept, number: Int, anyProspective: Boolean) {
      assertThat(intercept).isEqualTo(
        AdaIntercept(
          PADA,
          number,
          anyProspective,
          messageArguments = listOf("Default Prisoner"),
        ),
      )
      assertThat(intercept.message).isEqualTo("There is a PADA (Prospective additional days awarded) recorded for Default Prisoner. Review the PADA and approve if it's relevant to the current sentence.")
    }

    private fun assertPadasIntercept(intercept: AdaIntercept, number: Int, anyProspective: Boolean) {
      assertThat(intercept).isEqualTo(
        AdaIntercept(
          PADAS,
          number,
          anyProspective,
          messageArguments = listOf("Default Prisoner"),
        ),
      )
      assertThat(intercept.message).isEqualTo("There are PADAs (Prospective additional days awarded) recorded for Default Prisoner. Review the PADAs and approve the ones that are relevant to the current sentence.")
    }
  }

  companion object {
    const val NOMS_ID = "AA1234A"
    val BASE_10_DAY_ADJUSTMENT = Adjustment(
      person = NOMS_ID,
      days = 10,
      effectiveDays = 10,
      adjustmentType = ADDITIONAL_DAYS_AWARDED,
      fromDate = LocalDate.of(2023, 8, 3),
      additionalDaysAwarded = AdditionalDaysAwarded(
        adjudicationCharges = mutableListOf(
          AdjudicationCharges("MOR-1525916"),
          AdjudicationCharges("MOR-1525917"),
          AdjudicationCharges("MOR-1525918"),
        ),
      ),
    )
    val adjudicationOne = Adjudication(
      chargeNumber = "MOR-1525916",
      prisonerNumber = NOMS_ID,
      status = "CHARGE_PROVED",
      outcomes = listOf(
        OutcomeAndHearing(
          Hearing(
            dateTimeOfHearing = LocalDateTime.of(2023, 8, 3, 16, 45),
            agencyId = "MOR",
          ),
        ),
      ),
      punishments = listOf(
        Punishment(
          type = "ADDITIONAL_DAYS",
          schedule = Schedule(5, null),
          consecutiveChargeNumber = null,
        ),
      ),
    )
    val adjudicationThreeConcurrentToOne = Adjudication(
      chargeNumber = "MOR-1525918",
      prisonerNumber = NOMS_ID,
      status = "CHARGE_PROVED",
      outcomes = listOf(
        OutcomeAndHearing(
          Hearing(
            dateTimeOfHearing = LocalDateTime.of(2023, 8, 3, 16, 45),
            agencyId = "MOR",
          ),
        ),
      ),
      punishments = listOf(
        Punishment(
          type = "ADDITIONAL_DAYS",
          schedule = Schedule(5, null),
          consecutiveChargeNumber = null,
        ),
      ),
    )
    val adjudicationTwoConsecutiveToOne = Adjudication(
      chargeNumber = "MOR-1525917",
      prisonerNumber = NOMS_ID,
      status = "CHARGE_PROVED",
      outcomes = listOf(
        OutcomeAndHearing(
          Hearing(
            dateTimeOfHearing = LocalDateTime.of(2023, 8, 3, 16, 45),
            agencyId = "MOR",
          ),
        ),
      ),
      punishments = listOf(
        Punishment(
          type = "ADDITIONAL_DAYS",
          schedule = Schedule(5, null),
          consecutiveChargeNumber = "MOR-1525916",
        ),
      ),
    )

    val adjudicationOneQuashed = adjudicationOne.copy(status = "QUASHED")
    val adjudicationTwoConsecutiveToOneQuashed = adjudicationTwoConsecutiveToOne.copy(status = "QUASHED")
    val adjudicationThreeConcurrentToOneQuashed = adjudicationThreeConcurrentToOne.copy(status = "QUASHED")

    val adjudicationOneProspective = adjudicationOne.copy(
      punishments = listOf(
        Punishment(
          type = "PROSPECTIVE_DAYS",
          schedule = Schedule(10, null),
          consecutiveChargeNumber = null,
        ),
      ),
    )

    val adjudicationTwoProspective = Adjudication(
      chargeNumber = "MOR-2525916",
      prisonerNumber = NOMS_ID,
      status = "CHARGE_PROVED",
      outcomes = listOf(
        OutcomeAndHearing(
          Hearing(
            dateTimeOfHearing = LocalDateTime.of(2023, 9, 3, 16, 45),
            agencyId = "MOR",
          ),
        ),
      ),
      punishments = listOf(
        Punishment(
          type = "PROSPECTIVE_DAYS",
          schedule = Schedule(30, null),
          consecutiveChargeNumber = null,
        ),
      ),
    )

    private val recallSentenceDate = LocalDate.of(2022, 1, 1)
    private val sentenceDate = LocalDate.of(2023, 1, 1)
    private val recallDate = LocalDate.of(2024, 1, 1)
    private val sentences = listOf(SentenceAndOffences(sentenceDate = sentenceDate, bookingId = 1, sentenceSequence = 1, sentenceCalculationType = "ADIMP", sentenceStatus = "A"))
    private val recallSentences = listOf(SentenceAndOffences(sentenceDate = recallSentenceDate, bookingId = 1, sentenceSequence = 1, sentenceCalculationType = "LR", sentenceStatus = "A"))

    private val defaultSentenceDetail = SentenceAndStartDateDetails(
      sentences,
      earliestRecallDate = null,
      latestSentenceDate = sentenceDate,
      earliestNonRecallSentenceDate = sentenceDate,
      hasRecall = false,
      earliestSentenceDate = sentenceDate,
    )
    private val recallSentenceDetail = SentenceAndStartDateDetails(
      recallSentences,
      earliestRecallDate = recallDate,
      latestSentenceDate = sentenceDate,
      earliestNonRecallSentenceDate = null,
      hasRecall = true,
      earliestSentenceDate = recallSentenceDate,
    )
    private val parallelSentenceDetail = SentenceAndStartDateDetails(
      sentences + recallSentences,
      earliestRecallDate = recallDate,
      latestSentenceDate = sentenceDate,
      earliestNonRecallSentenceDate = sentenceDate,
      hasRecall = true,
      earliestSentenceDate = recallSentenceDate,
    )
  }
}
