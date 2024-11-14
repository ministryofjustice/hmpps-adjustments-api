package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.adjustments.api.enums.InterceptType.NONE
import uk.gov.justice.digital.hmpps.adjustments.api.enums.ToDoType.ADA_INTERCEPT
import uk.gov.justice.digital.hmpps.adjustments.api.model.ThingsToDo

@Service
class ThingsToDoService(
  private val additionalDaysAwardedService: AdditionalDaysAwardedService,
) {
  fun getToDoList(prisonerId: String): ThingsToDo {
    val ada = additionalDaysAwardedService.getAdaAdjudicationDetails(prisonerId)
    if (ada.intercept.type != NONE) {
      return ThingsToDo(
        prisonerId = prisonerId,
        thingsToDo = listOf(ADA_INTERCEPT),
        adaIntercept = ada.intercept,
      )
    }

    return ThingsToDo(
      prisonerId = prisonerId,
      thingsToDo = emptyList(),
    )
  }
}
