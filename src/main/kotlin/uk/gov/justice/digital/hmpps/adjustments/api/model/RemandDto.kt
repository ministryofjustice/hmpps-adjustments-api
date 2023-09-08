package uk.gov.justice.digital.hmpps.adjustments.api.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "The details of a remand adjustment")
data class RemandDto(
  @Schema(description = "Number of days for unused remand.")
  val unusedDays: Int,
)
