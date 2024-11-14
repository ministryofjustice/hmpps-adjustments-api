package uk.gov.justice.digital.hmpps.adjustments.api.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.adjustments.api.enums.ToDoType
import uk.gov.justice.digital.hmpps.adjustments.api.model.additionaldays.AdaIntercept

data class ThingsToDo(
  val prisonerId: String,
  val thingsToDo: List<ToDoType> = emptyList(),
  @Schema(description = "Will be populated if there is a ADA_INTERCEPT in the thingsToDo", nullable = true)
  val adaIntercept: AdaIntercept? = null,
)
