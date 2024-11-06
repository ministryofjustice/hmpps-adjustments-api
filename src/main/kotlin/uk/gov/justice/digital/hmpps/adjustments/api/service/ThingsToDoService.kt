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
