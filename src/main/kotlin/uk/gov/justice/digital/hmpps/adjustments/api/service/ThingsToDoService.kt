package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.adjustments.api.enums.InterceptType.NONE
import uk.gov.justice.digital.hmpps.adjustments.api.enums.ToDoType.ADA_INTERCEPT
import uk.gov.justice.digital.hmpps.adjustments.api.model.ThingsToDo

@Service
class ThingsToDoService(
  private val additionalDaysAwardedService: AdditionalDaysAwardedService,
) {

  // TODO This is a placeholder at the moment, the actual return object will contain more info, wil revisit after discussion with analyst/designer
  fun getToDoList(prisonerId: String): ThingsToDo {
    val ada = additionalDaysAwardedService.getAdaAdjudicationDetails(prisonerId)
    val thingsToDo = if (ada.intercept.type != NONE) {
      listOf(ADA_INTERCEPT)
    } else {
      emptyList()
    }

    return ThingsToDo(
      prisonerId = prisonerId,
      thingsToDo = thingsToDo,
    )
  }
}
