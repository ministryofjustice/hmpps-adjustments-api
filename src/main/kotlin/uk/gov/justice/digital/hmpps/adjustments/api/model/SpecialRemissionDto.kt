package uk.gov.justice.digital.hmpps.adjustments.api.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.adjustments.api.enums.SpecialRemissionType

@Schema(description = "The details of a Special Remission adjustment")
data class SpecialRemissionDto(
  @Schema(description = "The type of Special Remission")
  val type: SpecialRemissionType? = null,
)
