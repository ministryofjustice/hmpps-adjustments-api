package uk.gov.justice.digital.hmpps.adjustments.api.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.adjustments.api.enums.UnlawfullyAtLargeType

@Schema(description = "The details of a UAL adjustment")
data class UnlawfullyAtLargeDto(
  @Schema(description = "The type of UAL")
  val type: UnlawfullyAtLargeType,
)
