package uk.gov.justice.digital.hmpps.adjustments.api.model

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "The adjustment and its identifier")
data class AdjustmentDto(
  @Schema(description = "The ID of the adjustment")
  val id: UUID?,
  @Schema(description = "The details of the adjustment")
  val adjustment: AdjustmentDetailsDto,
)
