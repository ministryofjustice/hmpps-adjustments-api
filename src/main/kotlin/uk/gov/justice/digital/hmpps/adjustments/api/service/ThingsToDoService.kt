package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.adjustments.api.config.FeatureToggles
import uk.gov.justice.digital.hmpps.adjustments.api.enums.InterceptType.NONE
import uk.gov.justice.digital.hmpps.adjustments.api.enums.ToDoType.ADA_INTERCEPT
import uk.gov.justice.digital.hmpps.adjustments.api.enums.ToDoType.PREVIOUS_PERIOD_OF_UAL_FOR_REVIEW
import uk.gov.justice.digital.hmpps.adjustments.api.model.ThingsToDo

@Service
class ThingsToDoService(
  private val additionalDaysAwardedService: AdditionalDaysAwardedService,
  private val reviewPreviousUalService: ReviewPreviousUalService,
  private val prisonService: PrisonService,
  private val featureToggles: FeatureToggles,
) {
  fun getToDoList(prisonerId: String): ThingsToDo {
    var thingsToDo = ThingsToDo(
      prisonerId = prisonerId,
      thingsToDo = emptyList(),
      adaIntercept = null,
    )
    val sentenceAndStartDateDetails = prisonService.getSentencesAndStartDateDetails(prisonerId)
    val ada = additionalDaysAwardedService.getAdaAdjudicationDetails(nomsId = prisonerId, sentenceAndStartDateDetails = sentenceAndStartDateDetails)

    if (ada.intercept.type != NONE) {
      thingsToDo = thingsToDo.copy(thingsToDo = thingsToDo.thingsToDo + ADA_INTERCEPT, adaIntercept = ada.intercept)
    }
    val previousUalForReview = if (featureToggles.checkForPreviousPeriodsOfUal) {
      reviewPreviousUalService.findPreviousUalToReview(prisonerId, sentenceAndStartDateDetails)
    } else {
      emptyList()
    }
    if (previousUalForReview.isNotEmpty()) {
      thingsToDo = thingsToDo.copy(thingsToDo = thingsToDo.thingsToDo + PREVIOUS_PERIOD_OF_UAL_FOR_REVIEW)
    }

    return thingsToDo
  }
}
