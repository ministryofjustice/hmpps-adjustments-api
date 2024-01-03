package uk.gov.justice.digital.hmpps.adjustments.api.model

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "The adjustments to restore")
data class RestoreAdjustmentsDto(
  @Schema(description = "The IDs of the adjustments to restore")
  val ids: List<UUID>,
)
