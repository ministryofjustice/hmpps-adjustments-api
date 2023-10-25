package uk.gov.justice.digital.hmpps.adjustments.api.model

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Details of the adjustment and the number of effective days within a calculation.")
data class AdjustmentEffectiveDaysDto(
  @Schema(description = "The ID of the adjustment")
  val id: UUID,
  @Schema(description = "The number of days effective in a calculation. (for example remand minus any unused deductions)")
  val effectiveDays: Int,
  @Schema(description = "The NOMIS ID of the person this adjustment applies to")
  val person: String,
)
