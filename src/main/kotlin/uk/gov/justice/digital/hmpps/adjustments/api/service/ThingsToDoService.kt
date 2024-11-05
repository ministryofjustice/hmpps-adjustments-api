package uk.gov.justice.digital.hmpps.adjustments.api.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.adjustments.api.enums.InterceptType.NONE
import uk.gov.justice.digital.hmpps.adjustments.api.service.ToDoType.ADA_INTERCEPT

@Service
class ThingsToDoService(
  private val additionalDaysAwardedService: AdditionalDaysAwardedService
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



enum class ToDoType {
  ADA_INTERCEPT,
}

data class ThingsToDo(
  val prisonerId: String,
  val thingsToDo: List<ToDoType> = emptyList(),
)
