package uk.gov.justice.digital.hmpps.adjustments.api.model

import uk.gov.justice.digital.hmpps.adjustments.api.enums.ToDoType

data class ThingsToDo(
  val prisonerId: String,
  val thingsToDo: List<ToDoType> = emptyList(),
)
