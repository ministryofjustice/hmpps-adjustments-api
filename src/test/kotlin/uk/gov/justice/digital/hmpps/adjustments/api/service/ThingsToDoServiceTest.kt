package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.adjustments.api.config.FeatureToggles
import uk.gov.justice.digital.hmpps.adjustments.api.enums.InterceptType
import uk.gov.justice.digital.hmpps.adjustments.api.enums.InterceptType.NONE
import uk.gov.justice.digital.hmpps.adjustments.api.enums.InterceptType.PADA
import uk.gov.justice.digital.hmpps.adjustments.api.enums.ToDoType.ADA_INTERCEPT
import uk.gov.justice.digital.hmpps.adjustments.api.enums.ToDoType.PREVIOUS_PERIOD_OF_UAL_FOR_REVIEW
import uk.gov.justice.digital.hmpps.adjustments.api.enums.UnlawfullyAtLargeType
import uk.gov.justice.digital.hmpps.adjustments.api.model.ThingsToDo
import uk.gov.justice.digital.hmpps.adjustments.api.model.additionaldays.AdaAdjudicationDetails
import uk.gov.justice.digital.hmpps.adjustments.api.model.additionaldays.AdaIntercept
import uk.gov.justice.digital.hmpps.adjustments.api.model.previousual.PreviousUnlawfullyAtLargeAdjustmentForReview
import java.time.LocalDate
import java.util.UUID

class ThingsToDoServiceTest {
  private val additionalDaysAwardedService = mock<AdditionalDaysAwardedService>()
  private val reviewPreviousUalService = mock<ReviewPreviousUalService>()
  private val prisonService = mock<PrisonService>()

  @Test
  fun `Get things to do for a prisoner where there are only ADA things to do`() {
    val sentenceAndStartDateDetails = SentenceAndStartDateDetails()
    whenever(prisonService.getSentencesAndStartDateDetails(NOMS_ID)).thenReturn(SentenceAndStartDateDetails())
    val adjudicationDetails = getAdaAdjudicationDetails(PADA)
    whenever(additionalDaysAwardedService.getAdaAdjudicationDetails(NOMS_ID, emptyList(), sentenceAndStartDateDetails)).thenReturn(
      adjudicationDetails,
    )
    whenever(reviewPreviousUalService.findPreviousUalToReview(NOMS_ID, sentenceAndStartDateDetails)).thenReturn(emptyList())

    val thingsToDo = thingsToDoService().getToDoList(NOMS_ID)

    assertThat(thingsToDo).isEqualTo(
      ThingsToDo(
        prisonerId = NOMS_ID,
        thingsToDo = listOf(ADA_INTERCEPT),
        adaIntercept = adjudicationDetails.intercept,
      ),
    )
  }

  @Test
  fun `Get things to do for a prisoner where there are only previous UAL things to do`() {
    val sentenceAndStartDateDetails = SentenceAndStartDateDetails()
    whenever(prisonService.getSentencesAndStartDateDetails(NOMS_ID)).thenReturn(SentenceAndStartDateDetails())
    whenever(additionalDaysAwardedService.getAdaAdjudicationDetails(NOMS_ID, emptyList(), sentenceAndStartDateDetails)).thenReturn(
      getAdaAdjudicationDetails(NONE),
    )
    whenever(reviewPreviousUalService.findPreviousUalToReview(NOMS_ID, sentenceAndStartDateDetails)).thenReturn(
      listOf(
        PreviousUnlawfullyAtLargeAdjustmentForReview(
          id = UUID.randomUUID(),
          fromDate = LocalDate.of(2021, 1, 1),
          toDate = LocalDate.of(2021, 1, 11),
          days = 10,
          type = UnlawfullyAtLargeType.RELEASE_IN_ERROR,
          prisonName = "Brixton (HMP)",
          prisonId = "BXI",
        ),
      ),
    )

    val thingsToDo = thingsToDoService().getToDoList(NOMS_ID)

    assertThat(thingsToDo).isEqualTo(
      ThingsToDo(
        prisonerId = NOMS_ID,
        thingsToDo = listOf(PREVIOUS_PERIOD_OF_UAL_FOR_REVIEW),
        adaIntercept = null,
      ),
    )
  }

  @Test
  fun `Don't check previous UAL things to do when the feature toggle is off`() {
    val sentenceAndStartDateDetails = SentenceAndStartDateDetails()
    whenever(prisonService.getSentencesAndStartDateDetails(NOMS_ID)).thenReturn(SentenceAndStartDateDetails())
    whenever(additionalDaysAwardedService.getAdaAdjudicationDetails(NOMS_ID, emptyList(), sentenceAndStartDateDetails)).thenReturn(
      getAdaAdjudicationDetails(NONE),
    )
    whenever(reviewPreviousUalService.findPreviousUalToReview(NOMS_ID, sentenceAndStartDateDetails)).thenReturn(emptyList())

    val thingsToDo = thingsToDoService(checkForPreviousPeriodsOfUal = false).getToDoList(NOMS_ID)

    assertThat(thingsToDo).isEqualTo(
      ThingsToDo(
        prisonerId = NOMS_ID,
        thingsToDo = emptyList(),
        adaIntercept = null,
      ),
    )

    verify(reviewPreviousUalService, never()).findPreviousUalToReview(any(), any())
  }

  @Test
  fun `Get things to do for a prisoner where there are ADA and previous UAL things to do`() {
    val sentenceAndStartDateDetails = SentenceAndStartDateDetails()
    whenever(prisonService.getSentencesAndStartDateDetails(NOMS_ID)).thenReturn(SentenceAndStartDateDetails())
    val adjudicationDetails = getAdaAdjudicationDetails(PADA)
    whenever(additionalDaysAwardedService.getAdaAdjudicationDetails(NOMS_ID, emptyList(), sentenceAndStartDateDetails)).thenReturn(
      adjudicationDetails,
    )
    whenever(reviewPreviousUalService.findPreviousUalToReview(NOMS_ID, sentenceAndStartDateDetails)).thenReturn(
      listOf(
        PreviousUnlawfullyAtLargeAdjustmentForReview(
          id = UUID.randomUUID(),
          fromDate = LocalDate.of(2021, 1, 1),
          toDate = LocalDate.of(2021, 1, 11),
          days = 10,
          type = UnlawfullyAtLargeType.RELEASE_IN_ERROR,
          prisonName = "Brixton (HMP)",
          prisonId = "BXI",
        ),
      ),
    )

    val thingsToDo = thingsToDoService().getToDoList(NOMS_ID)

    assertThat(thingsToDo).isEqualTo(
      ThingsToDo(
        prisonerId = NOMS_ID,
        thingsToDo = listOf(ADA_INTERCEPT, PREVIOUS_PERIOD_OF_UAL_FOR_REVIEW),
        adaIntercept = adjudicationDetails.intercept,
      ),
    )
  }

  @Test
  fun `Get things to do for a prisoner when there is nothing to do`() {
    val sentenceAndStartDateDetails = SentenceAndStartDateDetails()
    whenever(prisonService.getSentencesAndStartDateDetails(NOMS_ID)).thenReturn(sentenceAndStartDateDetails)
    whenever(additionalDaysAwardedService.getAdaAdjudicationDetails(NOMS_ID, emptyList(), sentenceAndStartDateDetails)).thenReturn(
      getAdaAdjudicationDetails(NONE),
    )
    whenever(reviewPreviousUalService.findPreviousUalToReview(NOMS_ID, sentenceAndStartDateDetails)).thenReturn(emptyList())

    val thingsToDo = thingsToDoService().getToDoList(NOMS_ID)

    assertThat(thingsToDo).isEqualTo(ThingsToDo(prisonerId = NOMS_ID))
  }

  private fun getAdaAdjudicationDetails(type: InterceptType) = AdaAdjudicationDetails(
    intercept = AdaIntercept(
      type = type,
      number = 1,
      anyProspective = true,
      messageArguments = emptyList(),
    ),
  )

  private fun thingsToDoService(checkForPreviousPeriodsOfUal: Boolean = true) = ThingsToDoService(
    additionalDaysAwardedService,
    reviewPreviousUalService,
    prisonService,
    FeatureToggles(checkForPreviousPeriodsOfUal = checkForPreviousPeriodsOfUal),
  )

  companion object {
    const val NOMS_ID = "AA1234A"
  }
}
