package uk.gov.justice.digital.hmpps.adjustments.api.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Details of the number of unused days")
data class ManualUnusedDeductionsDto(
  @Schema(description = "The number of unused days")
  val days: Int,
)
